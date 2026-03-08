-- 7. 管理员表
CREATE TABLE IF NOT EXISTS `admin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL COMMENT '管理员用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `role` VARCHAR(20) DEFAULT 'admin' COMMENT '角色',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 插入默认管理员账号 (密码: admin123)
INSERT INTO `admin` (`username`, `password`) VALUES ('admin', 'admin123');

-- 更新用户表，增加状态字段
ALTER TABLE `user` ADD COLUMN `status` INT DEFAULT 1 COMMENT '状态：1-正常，0-封禁';
