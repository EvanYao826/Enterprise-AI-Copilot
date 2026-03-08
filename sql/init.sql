-- 数据库初始化脚本

CREATE DATABASE IF NOT EXISTS ai_knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_knowledge_db;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 会话表
CREATE TABLE IF NOT EXISTS `conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id` BIGINT NOT NULL COMMENT '关联的用户ID',
    `title` VARCHAR(255) DEFAULT '新建会话' COMMENT '会话标题',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 3. 消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` BIGINT NOT NULL COMMENT '关联会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色（user/assistant）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
    PRIMARY KEY (`id`),
    INDEX `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 4. 知识库分类表
CREATE TABLE IF NOT EXISTS `knowledge_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分类表';

-- 5. 知识库文档表
CREATE TABLE IF NOT EXISTS `knowledge_doc` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `doc_name` VARCHAR(255) NOT NULL COMMENT '文档名称',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文档存储路径',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '文档解析状态（PENDING:解析中, COMPLETED:解析完成, FAILED:解析失败）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '文档上传时间',
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- 6. 问答日志表
CREATE TABLE IF NOT EXISTS `qa_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '关联的用户ID',
    `question` TEXT NOT NULL COMMENT '用户提问',
    `answer` TEXT NOT NULL COMMENT 'AI 回答',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答日志表';
