-- 为 message 表添加 task_type 列
-- 执行时间: 2026-05-02

-- 添加 task_type 列（任务类型：chitchat/knowledge_qa/admin_copilot/knowledge_inspection/unknown）
ALTER TABLE message 
ADD COLUMN task_type VARCHAR(50) 
DEFAULT 'unknown' 
COMMENT '任务类型：chitchat/knowledge_qa/admin_copilot/knowledge_inspection/unknown'
AFTER sources;

-- 执行完成提示
SELECT '✅ task_type 列添加成功' AS message;