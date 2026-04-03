-- 对话上下文表 - 用于管理对话的短期记忆和长期记忆
CREATE TABLE IF NOT EXISTS `conversation_context` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` BIGINT NOT NULL COMMENT '对话ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `summary` TEXT COMMENT '对话摘要（长期记忆）',
    `embedding` TEXT COMMENT '对话向量（JSON格式，用于相似对话检索）',
    `window_size` INT DEFAULT 10 COMMENT '上下文窗口大小',
    `importance_score` DOUBLE DEFAULT 0.5 COMMENT '重要性评分（0-1）',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation_id` (`conversation_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_importance_score` (`importance_score`),
    INDEX `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话上下文表';

-- 为现有对话创建上下文记录（可选）
INSERT INTO `conversation_context` (conversation_id, user_id, window_size, importance_score)
SELECT
    c.id as conversation_id,
    c.user_id,
    10 as window_size,
    0.5 as importance_score
FROM `conversation` c
LEFT JOIN `conversation_context` cc ON c.id = cc.conversation_id
WHERE cc.id IS NULL;

-- 更新消息表，增加重要性评分字段（可选）
ALTER TABLE `message` ADD COLUMN `importance_score` DOUBLE DEFAULT 0.5 COMMENT '消息重要性评分（0-1）' AFTER `sources`;

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_message_conversation_time ON `message` (conversation_id, create_time);
CREATE INDEX IF NOT EXISTS idx_message_importance ON `message` (importance_score);