# Biometric Serv - 人脸识别业务服务

基于 Spring Boot + MySQL + MyBatis Plus 的人脸识别业务服务，提供完整的 RESTful API 接口。

## 功能特性

- **用户管理**: 用户的增删改查
- **人脸注册**: 注册用户人脸信息
- **人脸识别**: 1:N 人脸识别
- **人脸验证**: 1:1 人脸验证
- **日志记录**: 记录所有识别操作日志
- **数据持久化**: MySQL 数据库存储

## 数据库表结构

### tb_user - 用户表
- id: 用户ID（主键）
- username: 用户名（唯一）
- real_name: 真实姓名
- id_card: 身份证号
- mobile: 手机号
- email: 邮箱
- status: 状态（0-禁用，1-启用）
- create_time: 创建时间
- update_time: 更新时间

### tb_face_record - 人脸记录表
- id: 记录ID（主键）
- face_id: 人脸ID（对应算法服务）
- user_id: 用户ID
- image_url: 图片URL
- feature_vector: 特征向量（JSON格式）
- status: 状态（0-无效，1-有效）
- remark: 备注
- create_time: 创建时间
- update_time: 更新时间

### tb_recognition_log - 识别日志表
- id: 日志ID（主键）
- user_id: 用户ID
- face_id: 人脸ID
- similarity: 相似度分数
- recognition_type: 识别类型（1-1:N识别，2-1:1验证）
- recognition_result: 识别结果（0-失败，1-成功）
- query_image_url: 查询图片URL
- cost_time: 耗时（毫秒）
- create_time: 创建时间

## 启动方式

### 1. 初始化数据库

```bash
mysql -u root -p
source biometric-serv/src/main/resources/db/schema.sql
```

### 2. 修改配置

编辑 `application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/biometric
    username: root
    password: your_password
```

### 3. 启动服务

```bash
java -jar biometric-serv-1.0.0.jar
```

访问: http://localhost:8080

## API 使用示例

### 创建用户

```bash
curl -X POST http://localhost:8080/api/user \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "realName": "张三",
    "mobile": "13800138000"
  }'
```

### 注册人脸

```bash
curl -X POST http://localhost:8080/api/biometric/face/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "featureVector": [0.123, 0.456, ...],
    "imageUrl": "http://example.com/face.jpg"
  }'
```

### 人脸识别

```bash
curl -X POST http://localhost:8080/api/biometric/face/recognize \
  -H "Content-Type: application/json" \
  -d '{
    "featureVector": [0.123, 0.456, ...]
  }'
```

### 人脸验证

```bash
curl -X POST http://localhost:8080/api/biometric/face/verify \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "featureVector": [0.123, 0.456, ...]
  }'
```

### 查询识别日志

```bash
curl "http://localhost:8080/api/biometric/log/list?pageNum=1&pageSize=10"
```

## 配置说明

```yaml
# 服务端口
server:
  port: 8080

# 数据源配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/biometric
    username: root
    password: root

# 算法服务地址
biometric:
  algo:
    url: http://localhost:8081
```

## 依赖服务

- MySQL 5.7+
- biometric-algo 算法服务

## 注意事项

1. 确保 MySQL 服务已启动
2. 确保 biometric-algo 算法服务已启动
3. 人脸特征向量通常为 128 维或 512 维浮点数组
4. 建议使用专业的人脸检测和特征提取库（如 OpenCV、dlib、face_recognition 等）

