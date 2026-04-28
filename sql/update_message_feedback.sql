-- 兼容所有 MySQL 版本的更新脚本
-- 使用存储过程检查并添加字段

DELIMITER $$

-- 创建存储过程来安全添加字段
CREATE PROCEDURE IF NOT EXISTS AddFeedbackColumns()
BEGIN
    -- 检查 qa_log 表是否存在 feedback_type 字段
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_name = 'qa_log' AND column_name = 'feedback_type'
    ) THEN
        ALTER TABLE `qa_log` ADD COLUMN `feedback_type` VARCHAR(20) DEFAULT NULL COMMENT '反馈类型（like/dislike）';
    END IF;
    
    -- 检查 qa_log 表是否存在 feedback_time 字段
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE table_name = 'qa_log' AND column_name = 'feedback_time'
    ) THEN
        ALTER TABLE `qa_log` ADD COLUMN `feedback_time` DATETIME NULL DEFAULT NULL COMMENT '反馈时间';
    END IF;
END$$

DELIMITER ;

-- 执行存储过程
CALL AddFeedbackColumns();

-- 删除存储过程（可选）
DROP PROCEDURE IF EXISTS AddFeedbackColumns;

-- 创建消息反馈统计表（如果不存在）
CREATE TABLE IF NOT EXISTS `message_feedback_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `message_id` BIGINT NOT NULL COMMENT '关联消息ID',
    `feedback_type` VARCHAR(20) NOT NULL COMMENT '反馈类型',
    `user_id` BIGINT NULL COMMENT '反馈用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
    PRIMARY KEY (`id`),
    KEY `idx_message_id` (`message_id`),
    KEY `idx_feedback_type` (`feedback_type`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息反馈统计表';