# 人脸特征加载功能 - 快速开始

## 快速测试步骤

### 1. 准备数据库

确保数据库中有 `bosg_face_ftur_d` 表，并且有测试数据。

```sql
-- 查询有效的人脸特征数据数量
SELECT COUNT(*) FROM bosg_face_ftur_d 
WHERE VALI_FLAG = '1' 
  AND FACE_TMPL_STAS = '1' 
  AND FACE_FTUR_DATA IS NOT NULL;
```

### 2. 配置服务

#### biometric-serv 配置 (application.yml)

```yaml
server:
  port: 7082

spring:
  datasource:
    url: jdbc:mysql://192.168.10.147:3306/medicare_test
    username: root
    password: 123456

biometric:
  algo:
    url: http://localhost:7081  # 算法服务地址
  face:
    autoload: true  # 启用自动加载
    load:
      delay: 5000  # 延迟5秒后开始加载
```

#### biometric-algo 配置 (application.yml)

```yaml
server:
  port: 7081

hazelcast:
  cluster:
    name: biometric-cluster
  port: 5701
  members: 127.0.0.1
```

### 3. 启动服务

**重要：必须先启动算法服务，再启动业务服务**

#### Windows:

```bash
# 终端 1: 启动算法服务
cd biometric-algo
mvn spring-boot:run

# 终端 2: 启动业务服务
cd biometric-serv
mvn spring-boot:run
```

#### Linux/Mac:

```bash
# 终端 1: 启动算法服务
cd biometric-algo
mvn spring-boot:run

# 终端 2: 启动业务服务
cd biometric-serv
mvn spring-boot:run
```

### 4. 观察日志

业务服务启动后，会自动触发加载，观察日志输出：

```
2025-11-11 10:00:05.123  INFO - 应用启动完成，准备加载人脸特征数据到 Hazelcast
2025-11-11 10:00:10.456  INFO - 数据库中共有 15000 条有效人脸特征数据
2025-11-11 10:00:10.789  INFO - ========== 开始加载人脸特征数据到 Hazelcast ==========
2025-11-11 10:00:10.890  INFO - 查询到 15000 条人脸特征数据，开始加载到 Hazelcast
2025-11-11 10:01:20.123  INFO - 已成功加载 100 条人脸特征数据
2025-11-11 10:02:30.234  INFO - 已成功加载 200 条人脸特征数据
...
2025-11-11 10:15:45.678  INFO - ========== 人脸特征数据加载完成 ==========
2025-11-11 10:15:45.679  INFO - 总数: 15000, 成功: 14998, 失败: 2
```

### 5. 验证加载结果

#### 方法 1: 调用算法服务 API

```bash
# 查询 Hazelcast 中的人脸特征总数
curl http://localhost:7081/api/algo/face/feature/count
```

**期望响应：**
```json
{
  "success": true,
  "count": 14998
}
```

#### 方法 2: 调用业务服务 API

```bash
# 查询数据库中的人脸特征总数
curl http://localhost:7082/api/face/load/count
```

**期望响应：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalCount": 15000
  }
}
```

## 手动触发加载

如果禁用了自动加载，或者需要重新加载数据，可以手动触发：

### 全量加载

```bash
curl -X POST http://localhost:7082/api/face/load/all
```

### 分批加载

```bash
# 加载前 1000 条
curl -X POST "http://localhost:7082/api/face/load/batch?batchSize=1000&offset=0"

# 加载接下来的 1000 条
curl -X POST "http://localhost:7082/api/face/load/batch?batchSize=1000&offset=1000"
```

## 常见问题

### Q1: 日志显示 "调用算法服务失败"

**原因：** 算法服务未启动或配置错误

**解决：**
1. 检查算法服务是否启动：`curl http://localhost:7081/api/algo/face/feature/count`
2. 检查配置文件中的 `biometric.algo.url` 是否正确

### Q2: 加载成功但数量为 0

**原因：** 数据库中没有满足条件的数据

**解决：**
1. 检查数据库查询条件
2. 确认数据的 `VALI_FLAG` 和 `FACE_TMPL_STAS` 字段值

### Q3: 服务启动很慢

**原因：** 数据加载阻塞了启动流程

**解决：**
1. 确认 `@EnableAsync` 注解已添加
2. 确认监听器使用了 `@Async` 注解
3. 增加 `biometric.face.load.delay` 的值

## 测试脚本

### 完整测试脚本 (test_face_loading.sh)

```bash
#!/bin/bash

echo "========== 人脸特征加载功能测试 =========="

# 1. 检查算法服务状态
echo ""
echo "1. 检查算法服务状态..."
curl -s http://localhost:7081/api/algo/face/feature/count
echo ""

# 2. 查询数据库中的人脸特征总数
echo ""
echo "2. 查询数据库中的人脸特征总数..."
curl -s http://localhost:7082/api/face/load/count
echo ""

# 3. 手动触发加载
echo ""
echo "3. 手动触发加载..."
curl -s -X POST http://localhost:7082/api/face/load/all
echo ""

# 4. 等待 10 秒
echo ""
echo "4. 等待 10 秒，让加载完成..."
sleep 10

# 5. 再次查询 Hazelcast 中的数量
echo ""
echo "5. 查询 Hazelcast 中的人脸特征总数..."
curl -s http://localhost:7081/api/algo/face/feature/count
echo ""

echo ""
echo "========== 测试完成 =========="
```

### Windows 测试脚本 (test_face_loading.bat)

```batch
@echo off
echo ========== 人脸特征加载功能测试 ==========

echo.
echo 1. 检查算法服务状态...
curl -s http://localhost:7081/api/algo/face/feature/count
echo.

echo.
echo 2. 查询数据库中的人脸特征总数...
curl -s http://localhost:7082/api/face/load/count
echo.

echo.
echo 3. 手动触发加载...
curl -s -X POST http://localhost:7082/api/face/load/all
echo.

echo.
echo 4. 等待 10 秒，让加载完成...
timeout /t 10 /nobreak

echo.
echo 5. 查询 Hazelcast 中的人脸特征总数...
curl -s http://localhost:7081/api/algo/face/feature/count
echo.

echo.
echo ========== 测试完成 ==========
pause
```

## 生产环境部署建议

### 1. 禁用自动加载

生产环境建议禁用自动加载，改为手动控制：

```yaml
biometric:
  face:
    autoload: false  # 禁用自动加载
```

### 2. 分批加载

使用脚本分批加载，避免一次性加载大量数据：

```bash
#!/bin/bash

TOTAL=100000  # 总数据量
BATCH_SIZE=1000  # 每批加载数量
OFFSET=0

while [ $OFFSET -lt $TOTAL ]; do
    echo "加载批次: offset=$OFFSET, batchSize=$BATCH_SIZE"
    curl -X POST "http://localhost:7082/api/face/load/batch?batchSize=$BATCH_SIZE&offset=$OFFSET"
    
    OFFSET=$((OFFSET + BATCH_SIZE))
    sleep 2  # 每批之间延迟 2 秒
done

echo "所有数据加载完成"
```

### 3. 监控和告警

- 监控加载失败率
- 设置告警阈值
- 记录加载性能指标

### 4. 定时重新加载

使用定时任务定期重新加载数据，确保数据一致性：

```bash
# crontab 配置 - 每天凌晨 2 点重新加载
0 2 * * * /path/to/reload_face_features.sh
```

## 下一步

- 查看详细文档：[FACE_FEATURE_LOADING.md](FACE_FEATURE_LOADING.md)
- 查看 API 测试文档：[../API-TEST.md](../API-TEST.md)
- 查看部署指南：[../DEPLOYMENT.md](../DEPLOYMENT.md)

