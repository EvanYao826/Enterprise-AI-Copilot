-- 检查 message 表是否有 feedback_type 字段
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'message' AND COLUMN_NAME = 'feedback_type';

-- 如果没有，则添加
ALTER TABLE `message`
ADD COLUMN `feedback_type` VARCHAR(20) DEFAULT NULL COMMENT '反馈类型（like/dislike）',
ADD COLUMN `feedback_time` DATETIME DEFAULT NULL COMMENT '反馈时间';

-- 检查 qa_log 表是否有 feedback_type 字段
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'qa_log' AND COLUMN_NAME = 'feedback_type';

-- 如果没有，则添加
ALTER TABLE `qa_log`
ADD COLUMN `feedback_type` VARCHAR(20) DEFAULT NULL COMMENT '反馈类型（like/dislike）',
ADD COLUMN `feedback_time` DATETIME DEFAULT NULL COMMENT '反馈时间';