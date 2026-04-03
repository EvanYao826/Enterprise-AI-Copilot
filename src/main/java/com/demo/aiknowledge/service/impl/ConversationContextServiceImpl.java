package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiknowledge.config.CacheConfig;
import com.demo.aiknowledge.entity.ConversationContext;
import com.demo.aiknowledge.entity.Message;
import com.demo.aiknowledge.mapper.ConversationContextMapper;
import com.demo.aiknowledge.mapper.MessageMapper;
import com.demo.aiknowledge.service.CacheService;
import com.demo.aiknowledge.service.ConversationContextService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * 对话上下文服务实现
 * 实现短期记忆（Redis缓存）和长期记忆（数据库）管理
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationContextServiceImpl implements ConversationContextService {

    private final MessageMapper messageMapper;
    private final ConversationContextMapper conversationContextMapper;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    // 默认上下文窗口大小
    private static final int DEFAULT_WINDOW_SIZE = 10;
    // 最大上下文窗口大小
    private static final int MAX_WINDOW_SIZE = 20;
    // 最小上下文窗口大小
    private static final int MIN_WINDOW_SIZE = 5;

    @Override
    public List<Message> getConversationContext(Long conversationId, int maxMessages) {
        String cacheKey = CacheConfig.CacheConstants.KEY_CONVERSATION_CONTEXT + conversationId;

        // 1. 先查 Redis (获取原始 Object 列表，避免直接强转泛型导致 ClassCastException)
        // 注意：这里泛型指定为 List.class，内部元素会是 LinkedHashMap
        List<Object> cachedRawObjects = cacheService.get(
                CacheConfig.CacheConstants.CACHE_CONVERSATION_CONTEXT,
                cacheKey,
                List.class
        );

        if (cachedRawObjects != null && !cachedRawObjects.isEmpty()) {
            log.debug("Retrieved conversation context from cache - conversationId: {}, raw size: {}",
                    conversationId, cachedRawObjects.size());

            // 【核心修复】手动将 List<Object> (内部是 LinkedHashMap) 转换为 List<Message>
            List<Message> convertedMessages = convertToMessageList(cachedRawObjects);

            if (!convertedMessages.isEmpty()) {
                return limitMessages(convertedMessages, maxMessages);
            }
        }

        // 2. 缓存未命中或转换失败，从数据库获取
        log.debug("Cache miss or conversion failed, fetching from DB - conversationId: {}", conversationId);
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByDesc(Message::getCreateTime)
                        .last("LIMIT " + Math.max(maxMessages, DEFAULT_WINDOW_SIZE))
        );

        if (messages != null && !messages.isEmpty()) {
            // 回写缓存 (存入实体对象，Redis 序列化器会处理，但读取时仍可能变 Map，所以读取端必须兼容)
            cacheService.set(
                    CacheConfig.CacheConstants.CACHE_CONVERSATION_CONTEXT,
                    cacheKey,
                    messages,
                    CacheConfig.CacheConstants.TTL_CONVERSATION_CONTEXT,
                    TimeUnit.SECONDS
            );
            return limitMessages(messages, maxMessages);
        }

        return new ArrayList<>();
    }


    /**
     * 安全地将缓存中的 Object 列表转换为 Message 实体列表
     * 解决 Redis 读取后变成 LinkedHashMap 导致强转失败的问题
     */
    private List<Message> convertToMessageList(List<Object> rawObjects) {
        List<Message> result = new ArrayList<>();
        for (Object obj : rawObjects) {
            if (obj instanceof Message) {
                // 如果已经是 Message 类型（极少情况，取决于序列化配置）
                result.add((Message) obj);
            } else if (obj instanceof Map) {
                try {
                    // 核心逻辑：使用 ObjectMapper 将 Map 转换为 Message 实体
                    Message message = objectMapper.convertValue(obj, Message.class);
                    result.add(message);
                } catch (IllegalArgumentException e) {
                    log.error("Failed to convert cache map to Message entity: {}", obj, e);
                    // 忽略单个转换错误的消息，防止整个流程崩溃
                }
            } else {
                log.warn("Unexpected object type in conversation cache: {}", obj.getClass());
            }
        }
        return result;
    }




    @Override
    @Transactional
    public void updateConversationContext(Long conversationId, Long userId, Message newMessage) {
        String cacheKey = CacheConfig.CacheConstants.KEY_CONVERSATION_CONTEXT + conversationId;

        // 1. 获取当前上下文
        List<Message> currentContext = getConversationContext(conversationId, MAX_WINDOW_SIZE);

        // 2. 添加新消息
        List<Message> updatedContext = new ArrayList<>(currentContext);
        updatedContext.add(newMessage);

        // 3. 应用滑动窗口
        int windowSize = getContextWindowSize(conversationId);
        if (updatedContext.size() > windowSize) {
            updatedContext = updatedContext.subList(updatedContext.size() - windowSize, updatedContext.size());
        }

        // 4. 更新缓存
        cacheService.set(
                CacheConfig.CacheConstants.CACHE_CONVERSATION_CONTEXT,
                cacheKey,
                updatedContext,
                CacheConfig.CacheConstants.TTL_CONVERSATION_CONTEXT,
                TimeUnit.SECONDS
        );

        // 5. 检查是否需要生成摘要（每5条消息或重要消息）
        if (shouldGenerateSummary(conversationId, newMessage)) {
            generateConversationSummary(conversationId);
        }

        // 6. 更新长期记忆（每10条消息或对话结束时）
        if (shouldSaveLongTermMemory(conversationId)) {
            saveLongTermMemory(conversationId, userId);
        }

        log.debug("Updated conversation context - conversationId: {}, new message: {}, context size: {}",
                conversationId, newMessage.getRole(), updatedContext.size());
    }

    @Override
    public String generateConversationSummary(Long conversationId) {
        try {
            // 1. 获取对话历史
            List<Message> messages = messageMapper.selectList(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getConversationId, conversationId)
                            .orderByAsc(Message::getCreateTime)
            );

            if (messages.isEmpty()) {
                return "";
            }

            // 2. 提取关键信息（简化版，实际应该调用AI服务）
            StringBuilder summary = new StringBuilder();
            Set<String> topics = new HashSet<>();
            List<String> decisions = new ArrayList<>();
            Map<String, Integer> userPreferences = new HashMap<>();

            for (Message message : messages) {
                if ("user".equals(message.getRole())) {
                    // 提取用户问题中的关键词
                    extractKeywords(message.getContent(), topics);
                    // 识别用户偏好
                    detectUserPreferences(message.getContent(), userPreferences);
                } else if ("assistant".equals(message.getRole())) {
                    // 识别AI的建议和决策
                    detectDecisions(message.getContent(), decisions);
                }
            }

            // 3. 构建摘要
            summary.append("关键话题: ").append(String.join("、", topics)).append("\n");
            if (!decisions.isEmpty()) {
                summary.append("关键决策: ").append(String.join("；", decisions)).append("\n");
            }
            if (!userPreferences.isEmpty()) {
                summary.append("用户偏好: ");
                userPreferences.forEach((key, count) ->
                        summary.append(key).append("(").append(count).append("次) "));
                summary.append("\n");
            }

            String summaryText = summary.toString();

            // 4. 保存到数据库
            ConversationContext context = getOrCreateContext(conversationId);
            context.setSummary(summaryText);
            context.setUpdateTime(LocalDateTime.now());
            conversationContextMapper.updateById(context);

            log.info("Generated conversation summary - conversationId: {}, summary length: {}",
                    conversationId, summaryText.length());

            return summaryText;

        } catch (Exception e) {
            log.error("Failed to generate conversation summary - conversationId: {}", conversationId, e);
            return "";
        }
    }

    @Override
    public String getConversationSummary(Long conversationId) {
        ConversationContext context = conversationContextMapper.selectOne(
                new LambdaQueryWrapper<ConversationContext>()
                        .eq(ConversationContext::getConversationId, conversationId)
        );

        if (context != null && context.getSummary() != null) {
            return context.getSummary();
        }

        // 如果没有摘要，生成一个
        return generateConversationSummary(conversationId);
    }

    @Override
    @Transactional
    public void saveLongTermMemory(Long conversationId, Long userId) {
        try {
            // 1. 获取或创建上下文记录
            ConversationContext context = getOrCreateContext(conversationId);
            context.setUserId(userId);

            // 2. 生成或更新摘要
            if (context.getSummary() == null) {
                String summary = generateConversationSummary(conversationId);
                context.setSummary(summary);
            }

            // 3. 计算重要性评分
            List<Message> messages = messageMapper.selectList(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getConversationId, conversationId)
            );

            double importanceScore = calculateConversationImportance(messages);
            context.setImportanceScore(importanceScore);

            // 4. 设置窗口大小（根据重要性动态调整）
            int windowSize = calculateOptimalWindowSize(importanceScore, messages.size());
            context.setWindowSize(windowSize);

            // 5. 保存到数据库
            context.setUpdateTime(LocalDateTime.now());
            if (context.getId() == null) {
                context.setCreateTime(LocalDateTime.now());
                conversationContextMapper.insert(context);
            } else {
                conversationContextMapper.updateById(context);
            }

            log.info("Saved long-term memory - conversationId: {}, userId: {}, importance: {:.2f}",
                    conversationId, userId, importanceScore);

        } catch (Exception e) {
            log.error("Failed to save long-term memory - conversationId: {}, userId: {}",
                    conversationId, userId, e);
        }
    }

    @Override
    public List<ConversationContext> findSimilarConversations(Long userId, String query, int topK) {
        // 简化版：按关键词匹配
        // 实际应该使用向量搜索（Milvus/FAISS）

        List<ConversationContext> contexts = conversationContextMapper.selectList(
                new LambdaQueryWrapper<ConversationContext>()
                        .eq(ConversationContext::getUserId, userId)
                        .orderByDesc(ConversationContext::getImportanceScore)
                        .last("LIMIT " + topK)
        );

        // 简单的文本相似度匹配
        return contexts.stream()
                .filter(context -> {
                    if (context.getSummary() == null) return false;
                    // 检查摘要中是否包含查询关键词
                    String[] keywords = query.toLowerCase().split("\\s+");
                    String summary = context.getSummary().toLowerCase();
                    return Arrays.stream(keywords)
                            .anyMatch(summary::contains);
                })
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public void cleanupExpiredContexts(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        // 删除过期的上下文记录（保留重要对话）
        int deleted = conversationContextMapper.delete(
                new LambdaQueryWrapper<ConversationContext>()
                        .lt(ConversationContext::getUpdateTime, cutoffDate)
                        .lt(ConversationContext::getImportanceScore, 0.3) // 低重要性
        );

        log.info("Cleaned up expired contexts - deleted: {}, days: {}", deleted, days);
    }

    @Override
    public int getContextWindowSize(Long conversationId) {
        ConversationContext context = conversationContextMapper.selectOne(
                new LambdaQueryWrapper<ConversationContext>()
                        .eq(ConversationContext::getConversationId, conversationId)
        );

        if (context != null && context.getWindowSize() != null) {
            return Math.min(Math.max(context.getWindowSize(), MIN_WINDOW_SIZE), MAX_WINDOW_SIZE);
        }

        return DEFAULT_WINDOW_SIZE;
    }

    @Override
    public void adjustContextWindowSize(Long conversationId, int windowSize) {
        // 确保窗口大小在合理范围内
        int adjustedSize = Math.min(Math.max(windowSize, MIN_WINDOW_SIZE), MAX_WINDOW_SIZE);

        ConversationContext context = getOrCreateContext(conversationId);
        context.setWindowSize(adjustedSize);
        context.setUpdateTime(LocalDateTime.now());

        if (context.getId() == null) {
            conversationContextMapper.insert(context);
        } else {
            conversationContextMapper.updateById(context);
        }

        log.debug("Adjusted context window size - conversationId: {}, windowSize: {}",
                conversationId, adjustedSize);
    }

    @Override
    public double calculateMessageImportance(Message message) {
        // 简化的重要性计算
        double importance = 0.5; // 基础分数

        // 1. 内容长度（越长可能越重要）
        if (message.getContent() != null) {
            int length = message.getContent().length();
            if (length > 100) importance += 0.2;
            else if (length > 50) importance += 0.1;
        }

        // 2. 是否有来源（有来源的回答更重要）
        if (message.getSources() != null && !message.getSources().isEmpty()) {
            importance += 0.2;
        }

        // 3. 用户消息 vs AI消息
        if ("user".equals(message.getRole())) {
            importance += 0.1; // 用户消息稍微重要一些
        }

        // 4. 包含关键词
        String[] importantKeywords = {"重要", "关键", "必须", "紧急", "决定", "选择"};
        if (message.getContent() != null) {
            String content = message.getContent().toLowerCase();
            for (String keyword : importantKeywords) {
                if (content.contains(keyword)) {
                    importance += 0.1;
                    break;
                }
            }
        }

        return Math.min(importance, 1.0);
    }

    @Override
    public int compressConversationHistory(Long conversationId) {
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreateTime)
        );

        if (messages.size() <= MAX_WINDOW_SIZE) {
            return messages.size(); // 无需压缩
        }

        // 计算每条消息的重要性
        Map<Long, Double> importanceScores = new HashMap<>();
        for (Message message : messages) {
            importanceScores.put(message.getId(), calculateMessageImportance(message));
        }

        // 按重要性排序，保留最重要的消息
        List<Message> importantMessages = messages.stream()
                .sorted((m1, m2) -> {
                    double score1 = importanceScores.getOrDefault(m1.getId(), 0.0);
                    double score2 = importanceScores.getOrDefault(m2.getId(), 0.0);
                    return Double.compare(score2, score1); // 降序
                })
                .limit(MAX_WINDOW_SIZE)
                .sorted(Comparator.comparing(Message::getCreateTime)) // 恢复时间顺序
                .collect(Collectors.toList());

        // 这里可以添加逻辑来删除不重要的消息
        // 但为了数据完整性，我们只标记而不删除

        log.info("Compressed conversation history - conversationId: {}, original: {}, compressed: {}",
                conversationId, messages.size(), importantMessages.size());

        return importantMessages.size();
    }

    // ========== 私有辅助方法 ==========

    private List<Message> limitMessages(List<Message> messages, int maxMessages) {
        if (messages == null || messages.size() <= maxMessages) {
            return messages != null ? messages : Collections.emptyList();
        }
        return messages.subList(messages.size() - maxMessages, messages.size());
    }

    private ConversationContext getOrCreateContext(Long conversationId) {
        ConversationContext context = conversationContextMapper.selectOne(
                new LambdaQueryWrapper<ConversationContext>()
                        .eq(ConversationContext::getConversationId, conversationId)
        );

        if (context == null) {
            context = new ConversationContext();
            context.setConversationId(conversationId);
            context.setWindowSize(DEFAULT_WINDOW_SIZE);
            context.setCreateTime(LocalDateTime.now());
            context.setUpdateTime(LocalDateTime.now());
        }

        return context;
    }

    private boolean shouldGenerateSummary(Long conversationId, Message newMessage) {
        // 每5条消息生成一次摘要，或者遇到重要消息
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
        );

        if (messages.size() % 5 == 0) {
            return true;
        }

        // 重要消息触发摘要生成
        double importance = calculateMessageImportance(newMessage);
        return importance > 0.7;
    }

    private boolean shouldSaveLongTermMemory(Long conversationId) {
        // 每10条消息保存一次长期记忆，或者对话结束
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
        );

        return messages.size() % 10 == 0;
    }

    private void extractKeywords(String content, Set<String> topics) {
        // 简化的关键词提取
        String[] commonTopics = {"项目", "技术", "代码", "问题", "方案", "设计", "测试", "部署"};
        String lowerContent = content.toLowerCase();

        for (String topic : commonTopics) {
            if (lowerContent.contains(topic.toLowerCase())) {
                topics.add(topic);
            }
        }
    }

    private void detectUserPreferences(String content, Map<String, Integer> preferences) {
        // 简化的偏好检测
        String[] preferenceKeywords = {"喜欢", "偏好", "常用", "习惯", "希望", "想要"};

        for (String keyword : preferenceKeywords) {
            if (content.contains(keyword)) {
                preferences.put(keyword, preferences.getOrDefault(keyword, 0) + 1);
            }
        }
    }

    private void detectDecisions(String content, List<String> decisions) {
        // 简化的决策检测
        String[] decisionKeywords = {"建议", "推荐", "选择", "决定", "方案", "计划"};

        for (String keyword : decisionKeywords) {
            if (content.contains(keyword)) {
                // 提取包含关键词的句子
                String[] sentences = content.split("[。！？]");
                for (String sentence : sentences) {
                    if (sentence.contains(keyword) && sentence.length() > 10) {
                        decisions.add(sentence.trim());
                        break;
                    }
                }
            }
        }
    }

    private double calculateConversationImportance(List<Message> messages) {
        if (messages.isEmpty()) {
            return 0.0;
        }

        double totalImportance = 0.0;
        int importantCount = 0;

        for (Message message : messages) {
            double importance = calculateMessageImportance(message);
            totalImportance += importance;

            if (importance > 0.7) {
                importantCount++;
            }
        }

        double avgImportance = totalImportance / messages.size();

        // 重要性计算：平均重要性 + 重要消息比例
        double importanceScore = avgImportance * 0.7 + (importantCount / (double) messages.size()) * 0.3;

        return Math.min(importanceScore, 1.0);
    }

    private int calculateOptimalWindowSize(double importanceScore, int messageCount) {
        // 根据对话重要性和消息数量动态计算窗口大小
        int baseSize = DEFAULT_WINDOW_SIZE;

        // 重要性越高，窗口越大（保留更多上下文）
        if (importanceScore > 0.8) {
            baseSize = MAX_WINDOW_SIZE; // 重要对话保留最大窗口
        } else if (importanceScore > 0.5) {
            baseSize = DEFAULT_WINDOW_SIZE + 5; // 中等重要性对话增加窗口
        }

        // 消息数量较少时，适当减小窗口
        if (messageCount < baseSize) {
            return Math.max(messageCount, MIN_WINDOW_SIZE);
        }

        return Math.min(baseSize, MAX_WINDOW_SIZE);
    }
}