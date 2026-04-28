ALTER TABLE agent_run 
ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL AFTER run_id,
ADD COLUMN goal TEXT DEFAULT NULL AFTER status,
ADD COLUMN error_code VARCHAR(64) DEFAULT NULL AFTER error_message;

ALTER TABLE agent_step 
ADD COLUMN step_type VARCHAR(64) DEFAULT NULL AFTER run_id,
ADD COLUMN duration_ms BIGINT DEFAULT NULL AFTER created_at,
ADD COLUMN tool_call_id VARCHAR(64) DEFAULT NULL AFTER duration_ms;

CREATE TABLE IF NOT EXISTS intermediate_conclusion (
    id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    step_id VARCHAR(64) NOT NULL,
    conclusion_type VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    confidence DECIMAL(5,4) DEFAULT 0.0,
    sources TEXT DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_run_id (run_id),
    INDEX idx_step_id (step_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;