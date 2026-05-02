-- ============================================
-- 管理助手功能迁移脚本
-- 用于添加管理端会话和消息表
-- 与用户端数据完全隔离
-- ============================================

USE ai_knowledge_db;

-- 1. 管理员会话表（与用户会话完全隔离）
CREATE TABLE IF NOT EXISTS `admin_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `admin_id` BIGINT NOT NULL COMMENT '关联的管理员ID',
    `title` VARCHAR(255) DEFAULT '新建会话' COMMENT '会话标题',
    `is_pinned` TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_admin_id` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='管理员会话表';

-- 2. 管理员消息表（与用户消息完全隔离）
CREATE TABLE IF NOT EXISTS `admin_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` BIGINT NOT NULL COMMENT '关联会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色（user/assistant）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `sources` TEXT COMMENT '参考来源（JSON格式）',
    `task_type` VARCHAR(50) DEFAULT 'unknown' COMMENT '任务类型（chitchat/knowledge_qa/admin_copilot/knowledge_inspection/unknown）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
    PRIMARY KEY (`id`),
    INDEX `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='管理员消息表';

-- 3. Agent执行记录表（用于追踪Agent执行过程）
CREATE TABLE IF NOT EXISTS `agent_run` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '运行ID',
    `admin_id` BIGINT NOT NULL COMMENT '触发该运行的管理员ID',
    `conversation_id` BIGINT DEFAULT NULL COMMENT '关联的会话ID',
    `task_type` VARCHAR(50) NOT NULL COMMENT '任务类型',
    `input_text` TEXT NOT NULL COMMENT '输入文本',
    `output_text` TEXT COMMENT '输出文本',
    `status` VARCHAR(20) DEFAULT 'COMPLETED' COMMENT '运行状态（RUNNING/COMPLETED/FAILED）',
    `error_message` TEXT COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_admin_id` (`admin_id`),
    INDEX `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Agent执行记录表';

-- 4. Agent步骤记录表（记录Agent执行中的每一步）
CREATE TABLE IF NOT EXISTS `agent_step` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
    `run_id` BIGINT NOT NULL COMMENT '关联的运行ID',
    `step_name` VARCHAR(100) NOT NULL COMMENT '步骤名称',
    `step_type` VARCHAR(50) NOT NULL COMMENT '步骤类型',
    `input_data` TEXT COMMENT '输入数据（JSON格式）',
    `output_data` TEXT COMMENT '输出数据（JSON格式）',
    `status` VARCHAR(20) DEFAULT 'COMPLETED' COMMENT '步骤状态',
    `duration_ms` INT DEFAULT 0 COMMENT '执行耗时（毫秒）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_run_id` (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Agent步骤记录表';

SELECT '✅ 管理助手相关表创建成功' AS message;
