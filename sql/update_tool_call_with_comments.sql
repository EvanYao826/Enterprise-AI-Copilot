-- ===============================================
-- 表结构说明：Agent 相关表
-- ===============================================

-- ------------------------------------------------
-- tool_call 表：工具调用记录表
-- 功能：记录每次工具调用的详细信息
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS tool_call (
    id VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    tool_call_id VARCHAR(36) NOT NULL COMMENT '工具调用唯一标识',
    run_id VARCHAR(36) NOT NULL COMMENT '所属Agent运行ID',
    tool_name VARCHAR(100) NOT NULL COMMENT '工具名称',
    input_params TEXT COMMENT '输入参数（JSON格式）',
    output TEXT COMMENT '输出结果（JSON格式）',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：pending/success/failed',
    duration_ms BIGINT COMMENT '执行时长（毫秒）',
    error_message TEXT COMMENT '错误信息（如果执行失败）',
    timestamp DATETIME NOT NULL COMMENT '执行时间',
    created_at DATETIME NOT NULL COMMENT '记录创建时间',
    INDEX idx_run_id (run_id) COMMENT '按运行ID索引',
    INDEX idx_status (status) COMMENT '按状态索引',
    INDEX idx_tool_name (tool_name) COMMENT '按工具名称索引',
    INDEX idx_timestamp (timestamp) COMMENT '按执行时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具调用记录表';

-- ------------------------------------------------
-- agent_run 表：Agent运行记录表
-- 功能：记录Agent运行的整体信息
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS agent_run (
    id VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    run_id VARCHAR(36) NOT NULL COMMENT '运行唯一标识',
    conversation_id VARCHAR(36) COMMENT '关联的会话ID',
    user_id VARCHAR(36) COMMENT '用户ID',
    status VARCHAR(20) NOT NULL COMMENT '运行状态：pending/running/success/failed',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    input TEXT COMMENT '输入内容',
    output TEXT COMMENT '输出内容',
    error_message TEXT COMMENT '错误信息（如果运行失败）',
    created_at DATETIME NOT NULL COMMENT '记录创建时间',
    INDEX idx_run_id (run_id) COMMENT '按运行ID索引',
    INDEX idx_status (status) COMMENT '按状态索引',
    INDEX idx_conversation_id (conversation_id) COMMENT '按会话ID索引',
    INDEX idx_start_time (start_time) COMMENT '按开始时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent运行记录表';

-- ------------------------------------------------
-- agent_step 表：Agent执行步骤表
-- 功能：记录Agent执行的每个步骤信息
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS agent_step (
    id VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    run_id VARCHAR(36) NOT NULL COMMENT '所属运行ID',
    step_name VARCHAR(100) NOT NULL COMMENT '步骤名称',
    status VARCHAR(20) NOT NULL COMMENT '步骤状态：pending/running/success/failed',
    input TEXT COMMENT '步骤输入',
    output TEXT COMMENT '步骤输出',
    error_message TEXT COMMENT '错误信息（如果步骤失败）',
    start_time DATETIME NOT NULL COMMENT '步骤开始时间',
    end_time DATETIME COMMENT '步骤结束时间',
    created_at DATETIME NOT NULL COMMENT '记录创建时间',
    INDEX idx_run_id (run_id) COMMENT '按运行ID索引',
    INDEX idx_step_name (step_name) COMMENT '按步骤名称索引',
    INDEX idx_status (status) COMMENT '按状态索引',
    INDEX idx_start_time (start_time) COMMENT '按开始时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent执行步骤表';
