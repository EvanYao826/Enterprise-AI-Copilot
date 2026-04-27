-- ===============================================
-- 索引创建脚本：为 Agent 相关表创建所有必要的索引
-- ===============================================

-- ------------------------------------------------
-- agent_run 表索引
-- ------------------------------------------------
-- 按运行ID查询（最常用）
CREATE INDEX IF NOT EXISTS idx_agent_run_run_id ON agent_run(run_id);
-- 按状态查询（用于统计和监控）
CREATE INDEX IF NOT EXISTS idx_agent_run_status ON agent_run(status);
-- 按会话ID查询（关联会话）
CREATE INDEX IF NOT EXISTS idx_agent_run_conversation_id ON agent_run(conversation_id);
-- 按用户ID查询（用户维度统计）
CREATE INDEX IF NOT EXISTS idx_agent_run_user_id ON agent_run(user_id);
-- 按时间查询（时间范围统计）
CREATE INDEX IF NOT EXISTS idx_agent_run_start_time ON agent_run(start_time);
CREATE INDEX IF NOT EXISTS idx_agent_run_end_time ON agent_run(end_time);

-- ------------------------------------------------
-- agent_step 表索引
-- ------------------------------------------------
-- 按运行ID查询（最常用，关联步骤）
CREATE INDEX IF NOT EXISTS idx_agent_step_run_id ON agent_step(run_id);
-- 按步骤名称查询（步骤类型统计）
CREATE INDEX IF NOT EXISTS idx_agent_step_step_name ON agent_step(step_name);
-- 按状态查询（步骤执行情况）
CREATE INDEX IF NOT EXISTS idx_agent_step_status ON agent_step(status);
-- 按时间查询（步骤执行时间分析）
CREATE INDEX IF NOT EXISTS idx_agent_step_start_time ON agent_step(start_time);
CREATE INDEX IF NOT EXISTS idx_agent_step_end_time ON agent_step(end_time);

-- ------------------------------------------------
-- tool_call 表索引
-- ------------------------------------------------
-- 按运行ID查询（最常用，关联工具调用）
CREATE INDEX IF NOT EXISTS idx_tool_call_run_id ON tool_call(run_id);
-- 按工具名称查询（工具使用统计）
CREATE INDEX IF NOT EXISTS idx_tool_call_tool_name ON tool_call(tool_name);
-- 按状态查询（工具执行情况）
CREATE INDEX IF NOT EXISTS idx_tool_call_status ON tool_call(status);
-- 按时间查询（工具执行时间分析）
CREATE INDEX IF NOT EXISTS idx_tool_call_timestamp ON tool_call(timestamp);
-- 按工具调用ID查询（唯一标识）
CREATE INDEX IF NOT EXISTS idx_tool_call_tool_call_id ON tool_call(tool_call_id);

-- ------------------------------------------------
-- 复合索引（提高查询性能）
-- ------------------------------------------------
-- agent_run：按状态和时间查询
CREATE INDEX IF NOT EXISTS idx_agent_run_status_start_time ON agent_run(status, start_time);
-- agent_step：按运行ID和状态查询
CREATE INDEX IF NOT EXISTS idx_agent_step_run_id_status ON agent_step(run_id, status);
-- tool_call：按运行ID和工具名称查询
CREATE INDEX IF NOT EXISTS idx_tool_call_run_id_tool_name ON tool_call(run_id, tool_name);

-- ===============================================
-- 索引创建完成
-- ===============================================
