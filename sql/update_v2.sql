-- 1. 文档切片表
CREATE TABLE IF NOT EXISTS `knowledge_chunk` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` bigint(20) NOT NULL COMMENT '来源文档ID',
  `chunk_text` text COMMENT '文档片段内容',
  `chunk_index` int(11) DEFAULT 0 COMMENT '切片顺序',
  `page_number` int(11) DEFAULT 1 COMMENT '页码',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片表';

-- 2. 文档摘要表
CREATE TABLE IF NOT EXISTS `doc_summary` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` bigint(20) NOT NULL COMMENT '文档ID',
  `summary` text COMMENT '文档摘要',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档摘要表';

-- 3. 未命中问题表
CREATE TABLE IF NOT EXISTS `qa_unanswered` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `question` varchar(500) NOT NULL COMMENT '问题内容',
  `count` int(11) DEFAULT 1 COMMENT '提问次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '首次提问时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question` (`question`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='未命中问题表';

-- 4. 企业通知表
CREATE TABLE IF NOT EXISTS `notice` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) NOT NULL COMMENT '通知标题',
  `content` text COMMENT '通知内容',
  `file_path` varchar(500) DEFAULT NULL COMMENT '附件路径',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `is_active` tinyint(1) DEFAULT 1 COMMENT '是否有效 1:有效 0:无效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业通知表';

-- 5. 文档查看日志表
CREATE TABLE IF NOT EXISTS `doc_view_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` bigint(20) NOT NULL COMMENT '文档ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '查看时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_id` (`doc_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档查看日志表';

-- 6. 修改消息表，增加来源字段
ALTER TABLE `message` ADD COLUMN `sources` text COMMENT '来源JSON' AFTER `content`;
