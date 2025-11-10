-- 用户表
CREATE TABLE IF NOT EXISTS `tb_user` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `real_name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
    `mobile` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_mobile` (`mobile`),
    KEY `idx_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 人脸记录表
CREATE TABLE IF NOT EXISTS `tb_face_record` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `face_id` VARCHAR(64) NOT NULL COMMENT '人脸ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
    `feature_vector` TEXT NOT NULL COMMENT '特征向量（JSON格式）',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-无效，1-有效',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_face_id` (`face_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸记录表';

-- 识别日志表
CREATE TABLE IF NOT EXISTS `tb_recognition_log` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT(20) DEFAULT NULL COMMENT '用户ID',
    `face_id` VARCHAR(64) DEFAULT NULL COMMENT '人脸ID',
    `similarity` DOUBLE DEFAULT NULL COMMENT '相似度分数',
    `recognition_type` TINYINT(1) NOT NULL COMMENT '识别类型：1-1:N识别，2-1:1验证',
    `recognition_result` TINYINT(1) NOT NULL COMMENT '识别结果：0-失败，1-成功',
    `query_image_url` VARCHAR(500) DEFAULT NULL COMMENT '查询图片URL',
    `cost_time` BIGINT(20) DEFAULT NULL COMMENT '耗时（毫秒）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_recognition_type` (`recognition_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='识别日志表';

-- 插入测试数据
INSERT INTO `tb_user` (`username`, `real_name`, `id_card`, `mobile`, `email`, `status`) VALUES
('zhangsan', '张三', '110101199001011234', '13800138000', 'zhangsan@example.com', 1),
('lisi', '李四', '110101199002021234', '13800138001', 'lisi@example.com', 1),
('wangwu', '王五', '110101199003031234', '13800138002', 'wangwu@example.com', 1);

