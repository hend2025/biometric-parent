# 人脸特征数据加载功能说明

## 功能概述

本功能用于在 `biometric-serv` 服务启动时，自动从数据库表 `bosg_face_ftur_d` 中读取人脸特征数据，并通过 HTTP API 调用将数据加载到 `biometric-algo` 服务的 Hazelcast 分布式缓存中。

## 架构设计

```
┌──────────────────┐         HTTP API          ┌──────────────────┐
│                  │ ────────────────────────> │                  │
│ biometric-serv   │  POST /face/feature/add   │ biometric-algo   │
│  (业务服务)       │                            │  (算法服务)       │
│                  │                            │                  │
└────────┬─────────┘                            └────────┬─────────┘
         │                                               │
         │ 读取                                          │ 存储
         ▼                                               ▼
  ┌─────────────┐                              ┌──────────────────┐
  │   MySQL     │                              │   Hazelcast      │
  │ bosg_face_  │                              │ faceFeatureMap   │
  │   ftur_d    │                              │  (分布式缓存)     │
  └─────────────┘                              └──────────────────┘
```

## 核心组件

### 1. 数据实体类

**BosgFaceFturD.java**
- 对应数据库表 `bosg_face_ftur_d`
- 包含所有表字段的映射
- 使用 MyBatis Plus 注解

### 2. 数据访问层

**BosgFaceFturDMapper.java**
- 继承 MyBatis Plus 的 BaseMapper
- 提供基础的 CRUD 操作

### 3. 数据加载服务

**FaceFeatureLoadService.java**

主要功能：
- `loadFaceFeaturesToHazelcast()`: 全量加载人脸特征数据
- `loadFaceFeaturesBatch(batchSize, offset)`: 分批加载人脸特征数据
- `getTotalFaceFeatureCount()`: 获取数据库中人脸特征总数
- `bytesToFloatArray(bytes)`: 将 BLOB 数据转换为浮点数组
- `addFaceFeatureToAlgoService()`: 调用算法服务 API 添加人脸特征

### 4. 启动监听器

**FaceFeatureLoadListener.java**
- 监听 Spring Boot `ApplicationReadyEvent` 事件
- 在应用启动完成后自动触发数据加载
- 支持异步执行，不阻塞应用启动

### 5. 控制器接口

**FaceFeatureLoadController.java**

提供手动触发加载的 REST API：
- `POST /api/face/load/all`: 全量加载
- `POST /api/face/load/batch`: 分批加载
- `GET /api/face/load/count`: 查询数据总数
- `GET /api/face/load/health`: 健康检查

## 配置说明

### application.yml 配置

```yaml
# 算法服务配置
biometric:
  algo:
    url: http://localhost:7081  # 算法服务地址
  # 人脸特征加载配置
  face:
    autoload: true  # 是否在启动时自动加载（默认：true）
    load:
      delay: 5000  # 启动延迟加载时间（毫秒），确保算法服务已准备好（默认：5000）
```

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `biometric.algo.url` | 算法服务地址 | http://localhost:7081 |
| `biometric.face.autoload` | 是否自动加载 | true |
| `biometric.face.load.delay` | 延迟加载时间（毫秒） | 5000 |

## 数据加载逻辑

### 加载条件

从数据库中查询满足以下条件的人脸特征数据：
1. `VALI_FLAG = '1'` - 有效标志为有效
2. `FACE_TMPL_STAS = '1'` - 人脸模板状态为有效
3. `FACE_FTUR_DATA IS NOT NULL` - 人脸特征数据不为空

### 数据转换

1. 从数据库读取 `FACE_FTUR_DATA` 字段（BLOB 类型）
2. 将 byte[] 转换为 float[] 数组（人脸特征向量）
3. 使用 `ByteBuffer` 进行字节序转换（默认使用 LITTLE_ENDIAN）

### 数据传输

调用算法服务的 API 接口：
```
POST http://localhost:7081/face/feature/add

Body:
{
  "faceId": "原始人脸特征ID",
  "userId": "人员模板号",
  "featureVector": [浮点数数组],
  "imageUrl": "人脸图像URL"
}
```

## 使用方法

### 方式一：自动加载（推荐）

1. 确保配置文件中 `biometric.face.autoload: true`
2. 先启动 `biometric-algo` 服务
3. 再启动 `biometric-serv` 服务
4. 服务启动完成后会自动加载人脸特征数据

**启动日志示例：**
```
2025-11-11 10:00:05.123  INFO - 应用启动完成，准备加载人脸特征数据到 Hazelcast
2025-11-11 10:00:10.456  INFO - 数据库中共有 15000 条有效人脸特征数据
2025-11-11 10:00:10.789  INFO - ========== 开始加载人脸特征数据到 Hazelcast ==========
2025-11-11 10:00:10.890  INFO - 查询到 15000 条人脸特征数据，开始加载到 Hazelcast
2025-11-11 10:01:20.123  INFO - 已成功加载 100 条人脸特征数据
...
2025-11-11 10:05:45.678  INFO - ========== 人脸特征数据加载完成 ==========
2025-11-11 10:05:45.679  INFO - 总数: 15000, 成功: 14998, 失败: 2
```

### 方式二：手动触发

#### 1. 全量加载

```bash
curl -X POST http://localhost:7082/api/face/load/all
```

**响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalCount": 15000,
    "message": "人脸特征数据加载已启动，请稍后查询加载进度",
    "costTime": 125
  }
}
```

#### 2. 分批加载

```bash
# 加载第 1 批（0-999）
curl -X POST "http://localhost:7082/api/face/load/batch?batchSize=1000&offset=0"

# 加载第 2 批（1000-1999）
curl -X POST "http://localhost:7082/api/face/load/batch?batchSize=1000&offset=1000"
```

**响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "batchSize": 1000,
    "offset": 0,
    "message": "批次加载已启动",
    "costTime": 50
  }
}
```

#### 3. 查询数据总数

```bash
curl -X GET http://localhost:7082/api/face/load/count
```

**响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalCount": 15000
  }
}
```

#### 4. 健康检查

```bash
curl -X GET http://localhost:7082/api/face/load/health
```

**响应示例：**
```json
{
  "code": 0,
  "message": "success",
  "data": "Face feature load service is running"
}
```

## 性能优化建议

### 1. 分批加载

对于大量数据（超过 10 万条），建议使用分批加载：
- 每批加载 1000-5000 条数据
- 批次之间适当延迟，避免过度占用资源

### 2. 调整延迟时间

根据实际情况调整 `biometric.face.load.delay` 配置：
- 开发环境：5000ms（5 秒）
- 生产环境：10000ms（10 秒）或更长

### 3. 异步加载

- 数据加载使用异步线程执行
- 不会阻塞应用启动和正常业务
- 可以通过日志查看加载进度

### 4. 错误处理

- 加载失败的数据会记录到日志
- 可以通过手动触发接口重新加载失败的数据

## 注意事项

### 1. 服务启动顺序

**必须先启动 `biometric-algo` 服务，再启动 `biometric-serv` 服务**

原因：
- `biometric-serv` 需要调用 `biometric-algo` 的 API
- 如果算法服务未启动，数据加载会失败

### 2. 网络配置

确保两个服务之间网络可达：
- 检查防火墙规则
- 确认端口配置正确
- 验证服务地址配置

### 3. 数据库连接

确保数据库配置正确：
```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.10.147:3306/medicare_test
    username: root
    password: 123456
```

### 4. 内存配置

Hazelcast 需要足够的内存来存储人脸特征：
- 每条人脸特征约占用 2-4 KB 内存
- 10 万条数据约需要 200-400 MB 内存
- 建议 JVM 堆内存至少 4 GB

### 5. 字节序问题

人脸特征数据的字节序可能因算法而异：
- 默认使用 `ByteOrder.LITTLE_ENDIAN`
- 如果数据转换异常，尝试修改为 `ByteOrder.BIG_ENDIAN`

修改位置：`FaceFeatureLoadService.bytesToFloatArray()` 方法

### 6. 日志级别

调试时可以调整日志级别：
```yaml
logging:
  level:
    com.biometric.serv: DEBUG
```

## 故障排查

### 问题 1：数据加载失败

**现象：** 日志显示加载失败，失败数量较多

**可能原因：**
1. 算法服务未启动或无法访问
2. 网络连接问题
3. 人脸特征数据格式错误

**解决方法：**
1. 检查算法服务状态：`curl http://localhost:7081/api/algo/face/feature/count`
2. 检查网络连通性
3. 查看详细错误日志

### 问题 2：服务启动慢

**现象：** 服务启动时间过长

**可能原因：**
- 数据加载使用了同步方式
- 延迟时间设置过长

**解决方法：**
1. 确认使用了 `@Async` 注解（已默认启用）
2. 调整 `biometric.face.load.delay` 配置
3. 使用分批加载策略

### 问题 3：内存溢出

**现象：** 服务运行一段时间后出现 OutOfMemoryError

**可能原因：**
- JVM 堆内存不足
- Hazelcast 缓存数据过多

**解决方法：**
1. 增加 JVM 堆内存：`-Xmx4g -Xms4g`
2. 配置 Hazelcast 驱逐策略（已在 HazelcastConfig 中配置）
3. 定期清理无效数据

## 扩展开发

### 添加数据验证

在 `FaceFeatureLoadService` 中添加数据验证逻辑：

```java
private boolean validateFaceFeature(BosgFaceFturD faceFeature) {
    // 验证必填字段
    if (faceFeature.getFaceBosgId() == null || 
        faceFeature.getPsnTmplNo() == null ||
        faceFeature.getFaceFturData() == null) {
        return false;
    }
    
    // 验证特征向量长度（128 或 512 维）
    float[] vector = bytesToFloatArray(faceFeature.getFaceFturData());
    if (vector == null || (vector.length != 128 && vector.length != 512)) {
        return false;
    }
    
    return true;
}
```

### 添加进度回调

实现加载进度监控：

```java
public interface LoadProgressCallback {
    void onProgress(int current, int total);
    void onComplete(int success, int fail);
}
```

### 支持增量更新

监听数据库变化，实时更新 Hazelcast：

```java
@Scheduled(fixedDelay = 60000)  // 每分钟检查一次
public void incrementalUpdate() {
    // 查询最近更新的数据
    // 更新到 Hazelcast
}
```

## 相关文档

- [部署指南](../DEPLOYMENT.md)
- [API 文档](../API-TEST.md)
- [项目 README](../README.md)

