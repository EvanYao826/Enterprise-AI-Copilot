-- 1. 会话表增加置顶字段
ALTER TABLE `conversation` ADD COLUMN `is_pinned` tinyint(1) DEFAULT 0 COMMENT '是否置顶';
