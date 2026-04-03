package com.demo.aiknowledge.service;

import com.demo.aiknowledge.entity.ConversationContext;
import com.demo.aiknowledge.entity.Message;

import java.util.List;

/**
 * 对话上下文服务
 * 管理对话的短期记忆和长期记忆
 */
public interface ConversationContextService {

    /**
     * 获取对话上下文
     *
     * @param conversationId 对话ID
     * @param maxMessages    最大消息数
     * @return 上下文消息列表
     */
    List<Message> getConversationContext(Long conversationId, int maxMessages);

    /**
     * 更新对话上下文
     *
     * @param conversationId 对话ID
     * @param userId        用户ID
     * @param newMessage    新消息
     */
    void updateConversationContext(Long conversationId, Long userId, Message newMessage);

    /**
     * 生成对话摘要
     *
     * @param conversationId 对话ID
     * @return 对话摘要
     */
    String generateConversationSummary(Long conversationId);

    /**
     * 获取对话摘要
     *
     * @param conversationId 对话ID
     * @return 对话摘要
     */
    String getConversationSummary(Long conversationId);

    /**
     * 保存长期记忆
     *
     * @param conversationId 对话ID
     * @param userId        用户ID
     */
    void saveLongTermMemory(Long conversationId, Long userId);

    /**
     * 检索相似对话
     *
     * @param userId    用户ID
     * @param query     查询内容
     * @param topK      返回数量
     * @return 相似对话列表
     */
    List<ConversationContext> findSimilarConversations(Long userId, String query, int topK);

    /**
     * 清理过期上下文
     *
     * @param days 保留天数
     */
    void cleanupExpiredContexts(int days);

    /**
     * 获取上下文窗口大小
     *
     * @param conversationId 对话ID
     * @return 窗口大小
     */
    int getContextWindowSize(Long conversationId);

    /**
     * 调整上下文窗口大小
     *
     * @param conversationId 对话ID
     * @param windowSize    新的窗口大小
     */
    void adjustContextWindowSize(Long conversationId, int windowSize);

    /**
     * 计算消息重要性
     *
     * @param message 消息
     * @return 重要性评分
     */
    double calculateMessageImportance(Message message);

    /**
     * 压缩对话历史
     *
     * @param conversationId 对话ID
     * @return 压缩后的消息数量
     */
    int compressConversationHistory(Long conversationId);
}