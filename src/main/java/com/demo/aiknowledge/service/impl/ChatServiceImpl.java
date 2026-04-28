package com.demo.aiknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.aiknowledge.config.CacheConfig;
import com.demo.aiknowledge.dto.AiResponse;
import com.demo.aiknowledge.entity.Conversation;
import com.demo.aiknowledge.entity.Message;
import com.demo.aiknowledge.entity.QaLog;
import com.demo.aiknowledge.mapper.ConversationMapper;
import com.demo.aiknowledge.mapper.MessageMapper;
import com.demo.aiknowledge.mapper.QaLogMapper;
import com.demo.aiknowledge.service.AiService;
import com.demo.aiknowledge.service.CacheService;
import com.demo.aiknowledge.service.ChatService;
import com.demo.aiknowledge.service.ConversationContextService;
import com.demo.aiknowledge.service.QaUnansweredService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final QaLogMapper qaLogMapper;
    private final AiService aiService;
    private final QaUnansweredService qaUnansweredService;
    private final ConversationContextService conversationContextService;
    private final ObjectMapper objectMapper;
    private final CacheService cacheService;

    @Override
    public Conversation createConversation(Long userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(title != null ? title : "New Chat " + LocalDateTime.now());
        conversation.setCreateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public List<Conversation> getHistory(Long userId) {
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getIsPinned) // 先按置顶排序
                .orderByDesc(Conversation::getCreateTime)); // 再按时间排序
    }

    @Override
    public Conversation updateConversation(Long conversationId, String title, Boolean isPinned) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            if (title != null) {
                conversation.setTitle(title);
            }
            if (isPinned != null) {
                conversation.setIsPinned(isPinned);
            }
            conversationMapper.updateById(conversation);
        }
        return conversation;
    }

    @Override
    @Transactional
    public Message sendMessage(Long userId, Long conversationId, String content) {
        // 1. 保存用户消息
        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 1.1 更新对话上下文（用户消息）
        conversationContextService.updateConversationContext(conversationId, userId, userMsg);

        // 检查是否为第一条消息，如果是则生成标题
        Long msgCount = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId));
        if (msgCount == 1) { // 明确判断是否为第一条消息
             // 异步生成标题，避免阻塞
             aiService.generateTitle(conversationId, content);
        }

        // 2. 获取对话上下文（获取最近10条消息，包含刚插入的用户消息）
        List<Message> contextMessages = conversationContextService.getConversationContext(conversationId, 10);
        // 构建上下文字符串
        StringBuilder contextBuilder = new StringBuilder();
        for (Message msg : contextMessages) {
            contextBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        String conversationContext = contextBuilder.toString();
        log.debug("对话上下文构建完成，长度: {}，内容: {}", conversationContext.length(), conversationContext);

        // 3. 调用 AI 服务获取回答（传入对话上下文）
        AiResponse aiResponse = aiService.ask(content, conversationContext, userId);
        String answer = aiResponse.getAnswer();
        String sourcesJson = null;

        if (aiResponse.getSources() != null && !aiResponse.getSources().isEmpty()) {
            try {
                sourcesJson = objectMapper.writeValueAsString(aiResponse.getSources());
            } catch (Exception e) {
                log.error("Failed to serialize sources", e);
            }
        } else {
            // 如果没有 sources 或者 answer 看起来像不知道，记录到 unanswered
            // 简单的判断逻辑：如果 answer 包含 "不知道" 或 sources 为空且 answer 很短?
            // 这里假设 sources 为空且 answer 是兜底回复时记录
            if (answer.contains("抱歉") || answer.contains("无法回答")) {
                 qaUnansweredService.recordUnansweredQuestion(content);
            }
        }

        // 3. 保存 AI 回答
        Message aiMsg = new Message();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(answer);
        aiMsg.setSources(sourcesJson);
        aiMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(aiMsg);

        // 3.1 更新对话上下文（AI消息）
        conversationContextService.updateConversationContext(conversationId, userId, aiMsg);

        // 4. 记录 QA 日志
        QaLog qaLog = new QaLog();
        qaLog.setUserId(userId);
        qaLog.setQuestion(content);
        qaLog.setAnswer(answer);
        qaLog.setCreateTime(LocalDateTime.now());
        qaLogMapper.insert(qaLog);

        return aiMsg; // 返回 AI 的回答
    }

    @Override
    public List<Message> getMessages(Long conversationId) {
        // 使用对话上下文服务获取消息，支持滑动窗口和缓存
        return conversationContextService.getConversationContext(conversationId, 20);
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId) {
        // 删除会话相关的消息
        messageMapper.delete(new LambdaQueryWrapper<Message>().eq(Message::getConversationId, conversationId));
        // 删除会话本身
        conversationMapper.deleteById(conversationId);
    }

    @Override
    @Transactional
    public Message submitFeedback(Long messageId, String feedbackType) {
        // 1. 查找消息
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }

        // 2. 更新反馈字段
        message.setFeedbackType(feedbackType);
        message.setFeedbackTime(LocalDateTime.now());
        messageMapper.updateById(message);

        // 3. 清除该会话的缓存，确保下次获取时从数据库读取最新数据
        String cacheKey = CacheConfig.CacheConstants.KEY_CONVERSATION_CONTEXT + message.getConversationId();
        cacheService.delete(CacheConfig.CacheConstants.CACHE_CONVERSATION_CONTEXT, cacheKey);
        log.debug("Cleared conversation context cache for conversationId: {}", message.getConversationId());

        // 4. 如果是AI消息，同步更新QA日志的反馈
        if ("assistant".equals(message.getRole())) {
            QaLog qaLog = qaLogMapper.selectOne(new LambdaQueryWrapper<QaLog>()
                    .eq(QaLog::getAnswer, message.getContent())
                    .orderByDesc(QaLog::getCreateTime)
                    .last("LIMIT 1"));
            if (qaLog != null) {
                qaLog.setFeedbackType(feedbackType);
                qaLog.setFeedbackTime(LocalDateTime.now());
                qaLogMapper.updateById(qaLog);
            }
        }

        return message;
    }
}
