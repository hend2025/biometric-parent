-- 生物识别系统数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS medicare_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE medicare_test;

-- 人脸特征数据表 (示例结构)
CREATE TABLE IF NOT EXISTS bosg_face_ftur_d (
    FACE_BOSG_ID VARCHAR(50) PRIMARY KEY COMMENT '人脸唯一标识',
    PSN_TMPL_NO VARCHAR(50) NOT NULL COMMENT '人员模板号',
    FACE_FTUR_DATA BLOB COMMENT '人脸特征数据(512字节)',
    FACE_IMG_URL VARCHAR(500) COMMENT '人脸图片URL',
    FACE_TMPL_STAS VARCHAR(2) DEFAULT '1' COMMENT '模板状态: 1-有效, 0-无效',
    VALI_FLAG VARCHAR(2) DEFAULT '1' COMMENT '有效标志: 1-有效, 0-无效',
    CRTE_TIME DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UPDT_TIME DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    DELETED TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_face_bosg_id (FACE_BOSG_ID),
    INDEX idx_psn_tmpl_no (PSN_TMPL_NO),
    INDEX idx_vali_flag_status (VALI_FLAG, FACE_TMPL_STAS),
    INDEX idx_deleted (DELETED)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人脸特征数据表';

-- 性能优化建议索引
-- 用于加速数据加载查询
CREATE INDEX idx_load_query ON bosg_face_ftur_d(VALI_FLAG, FACE_TMPL_STAS, DELETED);

-- 组合索引用于常见查询场景
CREATE INDEX idx_psn_status ON bosg_face_ftur_d(PSN_TMPL_NO, VALI_FLAG, FACE_TMPL_STAS);

-- 数据库优化配置建议
-- 在 my.cnf 中添加以下配置:

-- [mysqld]
-- 连接数
-- max_connections = 500
-- 
-- InnoDB 缓冲池大小 (设置为物理内存的70-80%)
-- innodb_buffer_pool_size = 4G
-- 
-- InnoDB 日志文件大小
-- innodb_log_file_size = 512M
-- 
-- InnoDB 刷新方法
-- innodb_flush_method = O_DIRECT
-- 
-- 查询缓存
-- query_cache_size = 0
-- query_cache_type = 0
-- 
-- 临时表大小
-- tmp_table_size = 256M
-- max_heap_table_size = 256M
-- 
-- 排序缓冲区
-- sort_buffer_size = 4M
-- read_rnd_buffer_size = 4M
-- 
-- 线程缓存
-- thread_cache_size = 100

-- 示例数据 (仅用于测试)
-- INSERT INTO bosg_face_ftur_d (FACE_BOSG_ID, PSN_TMPL_NO, FACE_FTUR_DATA, FACE_IMG_URL, FACE_TMPL_STAS, VALI_FLAG)
-- VALUES ('TEST001', 'PSN001', REPEAT('x', 512), 'http://example.com/face1.jpg', '1', '1');

