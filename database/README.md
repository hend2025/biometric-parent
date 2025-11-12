# 数据库配置说明

## 数据库初始化

### 1. 执行初始化脚本

```bash
mysql -u root -p < schema.sql
```

### 2. 创建应用用户

```sql
CREATE USER 'biometric'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON medicare_test.* TO 'biometric'@'%';
FLUSH PRIVILEGES;
```

### 3. 验证表结构

```sql
USE medicare_test;
SHOW TABLES;
DESC bosg_face_ftur_d;
SHOW INDEX FROM bosg_face_ftur_d;
```

## 性能优化

### 索引说明

| 索引名 | 列 | 用途 |
|-------|---|------|
| PRIMARY | FACE_BOSG_ID | 主键索引 |
| idx_psn_tmpl_no | PSN_TMPL_NO | 按人员查询 |
| idx_vali_flag_status | VALI_FLAG, FACE_TMPL_STAS | 数据加载查询 |
| idx_load_query | VALI_FLAG, FACE_TMPL_STAS, DELETED | 优化加载性能 |

### 查询优化建议

```sql
-- 查看执行计划
EXPLAIN SELECT * FROM bosg_face_ftur_d 
WHERE VALI_FLAG='1' AND FACE_TMPL_STAS='1' AND FACE_FTUR_DATA IS NOT NULL;

-- 查看索引使用情况
SELECT * FROM sys.schema_unused_indexes WHERE object_schema = 'medicare_test';

-- 分析表
ANALYZE TABLE bosg_face_ftur_d;

-- 优化表
OPTIMIZE TABLE bosg_face_ftur_d;
```

## 备份和恢复

### 备份

```bash
# 完整备份
mysqldump -u biometric -p medicare_test > backup_$(date +%Y%m%d).sql

# 仅备份表结构
mysqldump -u biometric -p --no-data medicare_test > schema_backup.sql

# 仅备份数据
mysqldump -u biometric -p --no-create-info medicare_test > data_backup.sql
```

### 恢复

```bash
mysql -u biometric -p medicare_test < backup_20241112.sql
```

## 监控

### 关键指标

```sql
-- 表大小
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'medicare_test'
ORDER BY (data_length + index_length) DESC;

-- 连接数
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';

-- 慢查询
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';
```

## 故障排查

### 常见问题

#### 1. 连接超时
```sql
-- 检查最大连接数
SHOW VARIABLES LIKE 'max_connections';

-- 增加最大连接数
SET GLOBAL max_connections = 500;
```

#### 2. 查询慢
```sql
-- 启用慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- 查看慢查询
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;
```

