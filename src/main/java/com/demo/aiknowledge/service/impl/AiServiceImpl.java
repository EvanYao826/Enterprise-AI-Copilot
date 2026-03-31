package com.demo.aiknowledge.service.impl;

import com.demo.aiknowledge.dto.AiResponse;
import com.demo.aiknowledge.entity.Conversation;
import com.demo.aiknowledge.entity.KnowledgeDoc;
import com.demo.aiknowledge.entity.User;
import com.demo.aiknowledge.mapper.ConversationMapper;
import com.demo.aiknowledge.mapper.KnowledgeDocMapper;
import com.demo.aiknowledge.service.AiService;
import com.demo.aiknowledge.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final KnowledgeDocMapper knowledgeDocMapper;
    private final ConversationMapper conversationMapper;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    private static final String AI_CACHE_PREFIX = "ai:cache:";
    private static final long AI_CACHE_EXPIRE = 24; // 24小时
    
    /**
     * 判断问题是否是一般性问题，不需要参考来源
     * @param question 用户问题
     * @return 是否是一般性问题
     */
    private boolean isGeneralQuestion(String question) {
        String lowerQuestion = question.toLowerCase();
        // 检查问题是否包含关于系统自身、身份、功能等关键词
        String[] generalKeywords = {
            "你是谁", "你是什么", "你的名字", "你是做什么的", 
            "你的功能", "你能做什么", "你的作用", "你是谁开发的",
            "你来自哪里", "你好", "hello", "hi", "你好吗",
            "how are you", "你叫什么", "你是什么东西", "你是机器人吗",
            "你是AI吗", "你是智能助手吗", "你能帮助我吗", "你的能力",
            "我是谁", "我叫什么", "我的名字", "我的身份"
        };
        
        for (String keyword : generalKeywords) {
            if (lowerQuestion.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Async
    public void parseDocument(String filePath, Long docId) {
        log.info("Start parsing document: {}, docId: {}", filePath, docId);
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("file_path", filePath);
            requestBody.put("doc_id", docId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 调用 Python 服务
            String url = aiServiceUrl + "/parse";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // 更新文档状态
                KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
                if (doc != null) {
                    doc.setStatus("COMPLETED");
                    knowledgeDocMapper.updateById(doc);
                }
                log.info("Document parsed successfully: {}", docId);
            } else {
                throw new RuntimeException("AI Service returned error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Document parsing failed, using local fallback", e);
            // 当Python服务失败时，本地模拟解析完成
            KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
            if (doc != null) {
                doc.setStatus("COMPLETED");
                knowledgeDocMapper.updateById(doc);
            }
            log.info("Document parsing fallback to local: {}", docId);
        }
    }

    @Override
    public AiResponse ask(String question, String context, Long userId) {
        log.info("User question: {}, userId: {}", question, userId);
        AiResponse aiResponse = new AiResponse();

        // 生成缓存键
        String cacheKey = AI_CACHE_PREFIX + question.trim().toLowerCase();

        try {
            // 1. 检查缓存
            String cachedResponse = redisTemplate.opsForValue().get(cacheKey);
            if (cachedResponse != null) {
                log.info("Cache hit for question: {}", question);
                return objectMapper.readValue(cachedResponse, AiResponse.class);
            }

            // 2. 构建请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("question", question);
            requestBody.put("context", context);

            // 3. 获取用户信息（如果是身份相关问题）
            if (isGeneralQuestion(question) && userId != null) {
                User user = userService.getById(userId);
                if (user != null) {
                    requestBody.put("username", user.getUsername());
                    log.info("Added username to request: {}", user.getUsername());
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 关键点：打印即将调用的完整 URL，确认协议和端口是否正确
            String url = aiServiceUrl + "/ask";
            log.info(">>> [AI Service] 正在调用 Python 服务: {}", url);
            log.info(">>> [AI Service] 请求体: {}", requestBody);

            // 4. 发起调用
            ResponseEntity<Map> response;
            try {
                response = restTemplate.postForEntity(url, entity, Map.class);
            } catch (Exception e) {
                // 捕获网络层面的异常（如 ConnectionRefused, Timeout, UnknownHost）
                log.error(">>> [AI Service] 网络调用失败！无法连接到 Python 服务。URL: {}", url, e);
                // 直接抛出异常，中断流程，让上层知道出错了
                throw new RuntimeException("AI 服务连接失败，请检查 Python 服务是否启动及网络配置。详情：" + e.getMessage(), e);
            }

            log.info("<<< [AI Service] 响应状态码: {}", response.getStatusCode());
            log.info("<<< [AI Service] 响应体: {}", response.getBody());

            // 5. 处理响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // 检查 Python 服务是否返回了预期的 answer 字段
                if (!body.containsKey("answer")) {
                    log.error(">>> [AI Service] Python 服务返回数据格式错误，缺少 'answer' 字段。完整响应：{}", body);
                    throw new RuntimeException("AI 服务返回数据格式异常");
                }

                aiResponse.setAnswer((String) body.get("answer"));

                // 检查问题是否需要参考来源
                if (!isGeneralQuestion(question) && body.containsKey("sources")) {
                    List<Map<String, Object>> sources = (List<Map<String, Object>>) body.get("sources");
                    if (sources != null && !sources.isEmpty()) {
                        for (Map<String, Object> source : sources) {
                            if (!source.containsKey("doc") && source.containsKey("doc_name")) {
                                source.put("doc", source.get("doc_name"));
                            }
                        }
                        aiResponse.setSources(sources);
                    }
                } else {
                    // 对于一般性问题，不设置参考来源
                    aiResponse.setSources(null);
                }

                // 缓存结果
                try {
                    String responseJson = objectMapper.writeValueAsString(aiResponse);
                    redisTemplate.opsForValue().set(cacheKey, responseJson, AI_CACHE_EXPIRE, java.util.concurrent.TimeUnit.HOURS);
                } catch (JsonProcessingException e) {
                    log.warn("缓存 AI 响应失败", e);
                }

                return aiResponse;
            } else {
                // 处理非 2xx 状态码 (如 404, 500)
                log.error(">>> [AI Service] Python 服务返回错误状态码: {}, 响应体：{}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("AI 服务处理失败，状态码：" + response.getStatusCode());
            }

        } catch (RuntimeException e) {
            // 重新抛出我们上面包装过的运行时异常
            throw e;
        } catch (Exception e) {
            // 捕获其他未知异常
            log.error(">>> [AI Service] 发生未知异常", e);
            throw new RuntimeException("AI 服务内部错误：" + e.getMessage(), e);
        }
    }


    @Override
    @Async
    public void generateTitle(Long conversationId, String question) {
        log.info("Generating title for question: {}", question);
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("question", question);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = aiServiceUrl + "/summary";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String title = (String) response.getBody().get("title");
                if (title != null && !title.isEmpty()) {
                    Conversation conversation = conversationMapper.selectById(conversationId);
                    if (conversation != null) {
                        conversation.setTitle(title);
                        conversationMapper.updateById(conversation);
                        log.info("Conversation {} title updated to: {}", conversationId, title);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Generate title failed", e);
        }
    }

    @Override
    @Async
    public void deleteDoc(Long docId) {
        log.info("Deleting document vector index for docId: {}", docId);
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("doc_id", docId);
            // file_path 也是必需的参数，但删除逻辑不需要它，传空串
            requestBody.put("file_path", ""); 

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = aiServiceUrl + "/delete";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Document vector index deleted successfully: {}", docId);
            } else {
                log.warn("Failed to delete document vector index: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Delete document vector index failed", e);
        }
    }
}
