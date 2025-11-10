# Biometric Algo - 分布式人脸识别算法服务

基于 Hazelcast 的分布式人脸识别算法服务，提供高性能的 1:N 和 1:1 人脸识别能力。

## 功能特性

- **分布式存储**: 使用 Hazelcast 分布式 Map 存储人脸特征数据
- **多节点部署**: 支持横向扩展，提高处理能力
- **1:N 识别**: 在整个人脸库中搜索最匹配的人脸
- **1:1 验证**: 验证指定用户的人脸是否匹配
- **余弦相似度**: 使用余弦相似度算法计算人脸特征相似度
- **可配置阈值**: 支持自定义匹配阈值和返回结果数量

## 启动方式

### 单节点启动

```bash
java -jar biometric-algo-1.0.0.jar
```

访问: http://localhost:8081

### 多节点启动

节点配置需要相同的集群名称和成员列表。

**节点1:**
```bash
java -jar biometric-algo-1.0.0.jar \
  --server.port=8081 \
  --hazelcast.port=5701 \
  --hazelcast.members=192.168.1.10,192.168.1.11,192.168.1.12
```

**节点2:**
```bash
java -jar biometric-algo-1.0.0.jar \
  --server.port=8081 \
  --hazelcast.port=5702 \
  --hazelcast.members=192.168.1.10,192.168.1.11,192.168.1.12
```

**节点3:**
```bash
java -jar biometric-algo-1.0.0.jar \
  --server.port=8081 \
  --hazelcast.port=5703 \
  --hazelcast.members=192.168.1.10,192.168.1.11,192.168.1.12
```

## API 接口

### 添加人脸特征

```http
POST /api/algo/face/feature
Content-Type: application/json

{
    "userId": 1,
    "featureVector": [0.123, 0.456, ..., 0.789],
    "imageUrl": "http://example.com/face.jpg"
}
```

### 删除人脸特征

```http
DELETE /api/algo/face/feature/{faceId}
```

### 1:N 人脸识别

```http
POST /api/algo/face/recognize
Content-Type: application/json

{
    "featureVector": [0.123, 0.456, ..., 0.789]
}
```

### 1:1 人脸验证

```http
POST /api/algo/face/verify
Content-Type: application/json

{
    "userId": 1,
    "featureVector": [0.123, 0.456, ..., 0.789]
}
```

### 查询统计信息

```http
GET /api/algo/face/feature/count
GET /api/algo/face/feature/user/{userId}
```

## 配置参数

```yaml
# 人脸识别参数
face:
  recognition:
    threshold: 0.6  # 匹配阈值（0-1），推荐 0.5-0.8
    topN: 10        # 返回前N个结果

# Hazelcast配置
hazelcast:
  cluster:
    name: biometric-cluster
  port: 5701
  members: 127.0.0.1  # 集群节点，多个用逗号分隔
```

## 性能优化

1. **内存配置**: 建议配置 4GB 以上堆内存
   ```bash
   java -Xmx4g -Xms4g -jar biometric-algo-1.0.0.jar
   ```

2. **GC 调优**: 使用 G1 垃圾回收器
   ```bash
   java -XX:+UseG1GC -jar biometric-algo-1.0.0.jar
   ```

3. **多节点部署**: 建议至少部署3个节点以保证高可用

4. **网络优化**: 确保节点间网络延迟低于 10ms

