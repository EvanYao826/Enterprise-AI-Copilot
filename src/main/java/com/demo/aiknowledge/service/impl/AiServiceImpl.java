package com.demo.aiknowledge.service.impl;

import com.demo.aiknowledge.dto.AiResponse;
import com.demo.aiknowledge.entity.Conversation;
import com.demo.aiknowledge.entity.KnowledgeDoc;
import com.demo.aiknowledge.mapper.ConversationMapper;
import com.demo.aiknowledge.mapper.KnowledgeDocMapper;
import com.demo.aiknowledge.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${ai.service.url}")
    private String aiServiceUrl;

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
            log.error("Document parsing failed", e);
            KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
            if (doc != null) {
                doc.setStatus("FAILED");
                knowledgeDocMapper.updateById(doc);
            }
        }
    }

    @Override
    public AiResponse ask(String question, String context) {
        log.info("User question: {}", question);
        AiResponse aiResponse = new AiResponse();
        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("question", question);
            // context 可以暂时留空，或者传递额外上下文
            requestBody.put("context", context);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 调用 Python 服务
            String url = aiServiceUrl + "/ask";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                aiResponse.setAnswer((String) body.get("answer"));
                if (body.containsKey("sources")) {
                    List<Map<String, Object>> sources = (List<Map<String, Object>>) body.get("sources");
                    // 确保每个 source map 中都有 doc 字段，用于前端显示
                    for (Map<String, Object> source : sources) {
                        if (!source.containsKey("doc") && source.containsKey("doc_name")) {
                            source.put("doc", source.get("doc_name"));
                        }
                    }
                    aiResponse.setSources(sources);
                }
            } else {
                aiResponse.setAnswer("抱歉，AI 服务暂时不可用，请稍后再试。");
            }
        } catch (Exception e) {
            log.error("AI QA failed", e);
            aiResponse.setAnswer("抱歉，发生了一些错误，请稍后再试。");
        }
        return aiResponse;
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
}
