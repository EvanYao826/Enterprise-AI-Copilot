package com.demo.aiknowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话上下文实体
 * 用于管理对话的短期记忆和长期记忆
 */
@Data
@TableName("conversation_context")
public class ConversationContext {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对话ID
     */
    private Long conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 对话摘要（长期记忆）
     * 存储对话的核心内容和关键信息
     */
    private String summary;

    /**
     * 对话向量（用于相似对话检索）
     * 存储对话内容的向量表示，JSON格式
     */
    private String embedding;

    /**
     * 上下文窗口大小
     */
    private Integer windowSize;

    /**
     * 重要性评分
     * 用于决定哪些对话需要长期保存
     */
    private Double importanceScore;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 内部类：对话消息
     */
    @Data
    public static class ContextMessage {
        private String role; // user or assistant
        private String content;
        private String sources; // 来源信息，JSON格式
        private LocalDateTime timestamp;
        private Double importance; // 消息重要性评分
    }

    /**
     * 内部类：对话摘要
     */
    @Data
    public static class ConversationSummary {
        private String keyTopics; // 关键话题
        private String keyDecisions; // 关键决策
        private String userPreferences; // 用户偏好
        private String actionItems; // 待办事项
        private LocalDateTime lastUpdated;
    }
}