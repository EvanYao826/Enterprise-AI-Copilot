package com.demo.aiknowledge.service;

public interface AiService {
    void parseDocument(String filePath, Long docId);
    /**
     * 根据上下文回答问题
     * @param question 用户问题
     * @param context 相关文档上下文
     * @return AI回答
     */
    String ask(String question, String context);

    /**
     * 生成会话标题并更新数据库
     * @param conversationId 会话ID
     * @param question 用户问题
     */
    void generateTitle(Long conversationId, String question);
}
