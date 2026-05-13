-- ============================================
-- AgentCraft 数据库初始化脚本（合并版）
-- 执行方式：mysql -u root -p < sql/init.sql
-- ============================================

CREATE DATABASE IF NOT EXISTS ai_knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_knowledge_db;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 管理员表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL COMMENT '管理员用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `role` VARCHAR(20) DEFAULT 'admin' COMMENT '角色',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ============================================
-- 3. 会话表
-- ============================================
CREATE TABLE IF NOT EXISTS `conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '关联的用户ID',
    `title` VARCHAR(255) DEFAULT '新建会话' COMMENT '会话标题',
    `is_pinned` TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- ============================================
-- 4. 消息表
-- ============================================
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` BIGINT NOT NULL COMMENT '关联会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色（user/assistant）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `sources` TEXT COMMENT '参考来源（JSON格式）',
    `task_type` VARCHAR(50) DEFAULT 'unknown' COMMENT '任务类型：chitchat/knowledge_qa/admin_copilot/knowledge_inspection/unknown',
    `feedback_type` VARCHAR(20) DEFAULT NULL COMMENT '反馈类型（like/dislike）',
    `feedback_time` DATETIME DEFAULT NULL COMMENT '反馈时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
    PRIMARY KEY (`id`),
    INDEX `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- ============================================
-- 5. 管理员会话表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `admin_id` BIGINT NOT NULL COMMENT '关联的管理员ID',
    `title` VARCHAR(255) DEFAULT '新建会话' COMMENT '会话标题',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_admin_id` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员会话表';

-- ============================================
-- 6. 管理员消息表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` BIGINT NOT NULL COMMENT '关联会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色（user/assistant）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
    PRIMARY KEY (`id`),
    INDEX `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员消息表';

-- ============================================
-- 7. 知识库分类表
-- ============================================
CREATE TABLE IF NOT EXISTS `knowledge_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分类表';

-- ============================================
-- 8. 知识库文档表
-- ============================================
CREATE TABLE IF NOT EXISTS `knowledge_doc` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `doc_name` VARCHAR(255) NOT NULL COMMENT '文档名称',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文档存储路径',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '文档解析状态（PENDING/COMPLETED/FAILED）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '文档上传时间',
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- ============================================
-- 9. 文档切片表
-- ============================================
CREATE TABLE IF NOT EXISTS `knowledge_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `doc_id` BIGINT NOT NULL COMMENT '来源文档ID',
    `chunk_text` TEXT COMMENT '文档片段内容',
    `chunk_index` INT DEFAULT 0 COMMENT '切片顺序',
    `page_number` INT DEFAULT 1 COMMENT '页码',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片表';

-- ============================================
-- 10. 文档摘要表
-- ============================================
CREATE TABLE IF NOT EXISTS `doc_summary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `doc_id` BIGINT NOT NULL COMMENT '文档ID',
    `summary` TEXT COMMENT '摘要内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档摘要表';

-- ============================================
-- 11. 问答日志表
-- ============================================
CREATE TABLE IF NOT EXISTS `qa_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '关联的用户ID',
    `question` TEXT NOT NULL COMMENT '用户提问',
    `answer` TEXT NOT NULL COMMENT 'AI 回答',
    `feedback_type` VARCHAR(20) DEFAULT NULL COMMENT '反馈类型（like/dislike）',
    `feedback_time` DATETIME DEFAULT NULL COMMENT '反馈时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答日志表';

-- ============================================
-- 12. 未命中问题表
-- ============================================
CREATE TABLE IF NOT EXISTS `qa_unanswered` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `question` TEXT NOT NULL COMMENT '未命中问题',
    `hit_count` INT DEFAULT 1 COMMENT '出现次数',
    `first_seen` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次出现时间',
    `last_seen` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最近出现时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='未命中问题表';

-- ============================================
-- 13. 对话上下文表
-- ============================================
CREATE TABLE IF NOT EXISTS `conversation_context` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` BIGINT NOT NULL COMMENT '对话ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `summary` TEXT COMMENT '对话摘要（长期记忆）',
    `embedding` TEXT COMMENT '对话向量（JSON格式）',
    `window_size` INT DEFAULT 10 COMMENT '上下文窗口大小',
    `importance_score` DOUBLE DEFAULT 0.5 COMMENT '重要性评分',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_conversation_id` (`conversation_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话上下文表';

-- ============================================
-- 14. 公告表
-- ============================================
CREATE TABLE IF NOT EXISTS `notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `title` VARCHAR(255) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告内容',
    `admin_id` BIGINT DEFAULT NULL COMMENT '发布管理员ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ============================================
-- 15. Agent 运行记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `agent_run` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    `run_id` VARCHAR(64) NOT NULL COMMENT '运行ID',
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `conversation_id` VARCHAR(64) DEFAULT NULL COMMENT '会话ID',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '运行状态',
    `goal` TEXT DEFAULT NULL COMMENT '运行目标',
    `error_message` TEXT COMMENT '错误信息',
    `error_code` VARCHAR(64) DEFAULT NULL COMMENT '错误码',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_run_id` (`run_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent运行记录表';

-- ============================================
-- 16. Agent 步骤记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `agent_step` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    `run_id` VARCHAR(64) NOT NULL COMMENT '关联的运行ID',
    `step_type` VARCHAR(64) DEFAULT NULL COMMENT '步骤类型',
    `step_name` VARCHAR(128) NOT NULL COMMENT '步骤名称',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '步骤状态',
    `input_data` JSON COMMENT '输入数据',
    `output_data` JSON COMMENT '输出数据',
    `error_message` TEXT COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
    `tool_call_id` VARCHAR(64) DEFAULT NULL COMMENT '关联的工具调用ID',
    INDEX `idx_run_id` (`run_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent步骤记录表';

-- ============================================
-- 17. 工具调用记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `tool_call` (
    `id` VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    `tool_call_id` VARCHAR(36) NOT NULL COMMENT '工具调用唯一标识',
    `run_id` VARCHAR(64) NOT NULL COMMENT '关联的运行ID',
    `tool_name` VARCHAR(128) NOT NULL COMMENT '工具名称',
    `input_parameters` JSON COMMENT '输入参数',
    `output_result` JSON COMMENT '输出结果',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '调用状态',
    `error_message` TEXT COMMENT '错误信息',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_run_id` (`run_id`),
    INDEX `idx_tool_name` (`tool_name`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具调用记录表';

-- ============================================
-- 18. 文档查看日志表
-- ============================================
CREATE TABLE IF NOT EXISTS `doc_view_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `doc_id` BIGINT NOT NULL COMMENT '文档ID',
    `view_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '查看时间',
    PRIMARY KEY (`id`),
    INDEX `idx_doc_id` (`doc_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档查看日志表';

-- ============================================
-- 初始数据
-- ============================================

-- 默认管理员账号（密码：admin123）
INSERT INTO `admin` (`username`, `password`, `role`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin')
ON DUPLICATE KEY UPDATE `username` = `username`;
