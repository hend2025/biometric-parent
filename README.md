# Biometric Parent - 分布式人脸识别系统

基于 JDK 1.8 + Spring Boot 的分布式人脸识别系统，采用 Hazelcast 实现多节点分布式计算，提供高性能的 1:N 人脸识别能力。

## 项目架构

```
biometric-parent
├── biometric-algo    # 算法库（Hazelcast分布式人脸识别）- 作为 jar 包集成到 serv 中
└── biometric-serv    # 业务服务（MySQL + MyBatis Plus + RESTful API + 算法服务）
```

> **重要变更**: biometric-algo 已经从独立的微服务改为 jar 包库，集成到 biometric-serv 中。现在只需要启动一个服务即可。

### 技术栈

- **JDK**: 1.8
- **Spring Boot**: 2.3.12.RELEASE
- **数据库**: MySQL 5.7
- **ORM**: MyBatis Plus 3.4.3.4
- **分布式缓存**: Hazelcast 4.2.8
- **连接池**: Druid
- **序列化**: FastJSON

## 模块说明

### biometric-algo（算法库）

提供分布式人脸识别算法能力，作为 jar 包集成到 serv 模块中。

**功能特性：**
- 1:N 人脸识别（在所有人脸库中搜索）
- 1:1 人脸验证（验证指定用户）
- 人脸特征管理（添加、删除、查询）
- 余弦相似度计算
- 分布式内存存储（Hazelcast）

### biometric-serv（业务服务）

提供完整的人脸识别业务接口，对外提供 RESTful API 服务。已集成算法库。

**功能特性：**
- 用户管理（CRUD）
- 人脸注册与管理
- 人脸识别与验证
- 识别日志记录
- 数据持久化
- 内置算法服务（Hazelcast）

**端口**: 
- HTTP 服务: 7082
- Hazelcast: 5702

## 快速开始

### 1. 环境要求

- JDK 1.8+
- Maven 3.3+
- MySQL 5.7+

### 2. 数据库初始化

```bash
# 登录MySQL
mysql -u root -p

# 执行建表脚本
source biometric-serv/src/main/resources/db/schema.sql
```

或者直接导入：
```sql
CREATE DATABASE IF NOT EXISTS biometric DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后执行 `biometric-serv/src/main/resources/db/schema.sql` 中的 SQL 语句。

### 3. 配置修改

修改 `biometric-serv/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/biometric?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password  # 修改为你的MySQL密码

# Hazelcast配置（多节点部署）
hazelcast:
  cluster:
    name: biometric-cluster
  port: 5702
  members: 192.168.57.100,192.168.57.225  # 配置所有节点的IP

# 人脸识别参数
face:
  recognition:
    threshold: 0.6  # 匹配阈值，相似度大于此值认为匹配成功
    topN: 10  # 返回前N个最匹配的结果
```

### 4. 编译打包

#### Windows
```bash
# 使用提供的批处理脚本
build.bat

# 或手动执行
mvn clean package -DskipTests
```

#### Linux/Mac
```bash
# 使用提供的Shell脚本
./build.sh

# 或手动执行
mvn clean package -DskipTests
```

### 5. 启动服务

#### 方式一：IDE 启动（开发环境）

运行 `biometric-serv/src/main/java/com/biometric/serv/BiometricServApplication.java`

#### 方式二：命令行启动（生产环境）

##### Windows
```bash
# 使用批处理脚本
start-serv.bat

# 或手动执行
cd biometric-serv
java -Xmx4g -Xms4g -XX:+UseG1GC -jar target/biometric-serv-1.0.0.jar
```

##### Linux/Mac
```bash
# 使用Shell脚本
./start-serv.sh

# 或手动执行
cd biometric-serv
java -Xmx4g -Xms4g -XX:+UseG1GC -jar target/biometric-serv-1.0.0.jar
```

#### 方式三：多节点部署（Hazelcast 集群）

```bash
# 节点1（192.168.57.100）
java -jar biometric-serv-1.0.0.jar \
  --server.port=7082 \
  --hazelcast.port=5702 \
  --hazelcast.members=192.168.57.100,192.168.57.225

# 节点2（192.168.57.225）
java -jar biometric-serv-1.0.0.jar \
  --server.port=7082 \
  --hazelcast.port=5702 \
  --hazelcast.members=192.168.57.100,192.168.57.225
```

#### 方式四：Docker Compose 部署

```bash
# 启动所有服务（包括3个节点的集群 + MySQL）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## API 文档

### 用户管理 API

#### 创建用户
```
POST /api/user
Content-Type: application/json

{
    "username": "zhangsan",
    "realName": "张三",
    "idCard": "110101199001011234",
    "mobile": "13800138000",
    "email": "zhangsan@example.com"
}
```

#### 查询用户列表
```
GET /api/user/list?pageNum=1&pageSize=10&keyword=张三
```

#### 查询用户详情
```
GET /api/user/{id}
```

#### 更新用户
```
PUT /api/user/{id}
Content-Type: application/json

{
    "username": "zhangsan",
    "realName": "张三",
    "mobile": "13800138000"
}
```

#### 删除用户
```
DELETE /api/user/{id}
```

### 人脸识别 API

#### 注册人脸
```
POST /api/biometric/face/register
Content-Type: application/json

{
    "psnNo": 1,
    "featureVector": [0.123, 0.456, ..., 0.789],  // 人脸特征向量（通常为128维或512维）
    "imageUrl": "http://example.com/face.jpg",
    "remark": "正面照"
}
```

#### 1:N 人脸识别（在所有人脸库中搜索）
```
POST /api/biometric/face/recognize
Content-Type: application/json

{
    "featureVector": [0x12, 0x34, ..., 0x56],  // byte[] 格式的特征向量
    "queryImageUrl": "http://example.com/query.jpg"
}
```

返回示例：
```json
{
    "code": 200,
    "message": "识别完成",
    "data": {
        "results": [
            {
                "faceId": "uuid-xxx",
                "psnNo": 1,
                "similarity": 0.95,
                "imageUrl": "http://example.com/face.jpg",
                "matched": true
            }
        ],
        "count": 1,
        "costTime": 125
    }
}
```

#### 1:1 人脸验证（验证指定用户）
```
POST /api/biometric/face/verify
Content-Type: application/json

{
    "psnNo": 1,
    "featureVector": [0.123, 0.456, ..., 0.789],
    "queryImageUrl": "http://example.com/query.jpg"
}
```

#### 查询用户的人脸列表
```
GET /api/biometric/face/user/{psnNo}
```

#### 删除人脸
```
DELETE /api/biometric/face/{faceId}
```

#### 删除用户的所有人脸
```
DELETE /api/biometric/face/user/{psnNo}
```

#### 查询识别日志
```
GET /api/biometric/log/list?pageNum=1&pageSize=10&psnNo=1&recognitionType=1
```

参数说明：
- `recognitionType`: 1-1:N识别，2-1:1验证

## 配置说明

### 人脸识别参数调优

在 `biometric-serv/src/main/resources/application.yml` 中配置：

```yaml
face:
  recognition:
    threshold: 0.6  # 匹配阈值，建议范围 0.5-0.8
    topN: 10        # 返回前N个最匹配的结果
```

- `threshold`: 相似度阈值，值越高要求越严格，false positive越低
- `topN`: 返回识别结果的数量，建议10-50

### Hazelcast 配置调优

```yaml
hazelcast:
  cluster:
    name: biometric-cluster  # 集群名称，相同集群名称的节点会自动组网
  port: 5702                # Hazelcast通信端口
  members: 192.168.57.100,192.168.57.225  # 集群节点列表
```

## 性能优化建议

### 1. 数据库优化
- 为常用查询字段添加索引
- 使用读写分离
- 启用 MyBatis Plus 的二级缓存

### 2. 算法服务优化
- 多节点部署，提高并发处理能力
- 调整 Hazelcast 内存大小：`-Xmx4g -Xms4g`
- 优化特征向量维度（推荐512维）

### 3. 网络优化
- 使用 Nginx 做负载均衡
- 启用 HTTP/2
- 配置合理的超时时间

## 监控与运维

### JVM 参数建议

```bash
# 业务服务（包含算法服务，内存密集型）
java -Xmx4g -Xms4g -XX:+UseG1GC -jar biometric-serv-1.0.0.jar
```

### 健康检查

```bash
# 检查服务状态
curl http://localhost:7082/api/user/list
```

## 常见问题

### 1. Hazelcast 集群无法组网

**问题**：多个节点启动后无法形成集群

**解决方案**：
- 检查网络是否互通：`ping 192.168.57.100`
- 检查防火墙是否开放 5702 端口
- 确认所有节点配置了相同的 `cluster.name`
- 检查 `members` 配置是否包含所有节点

### 2. MySQL 连接失败

**问题**：`Access denied for user 'root'@'localhost'`

**解决方案**：
- 检查数据库用户名和密码
- 确认数据库已创建：`CREATE DATABASE biometric;`
- 检查 MySQL 服务是否启动

### 3. 内存溢出

**问题**：`java.lang.OutOfMemoryError: Java heap space`

**解决方案**：
- 增加 JVM 堆内存：`-Xmx4g -Xms4g`
- 调整 Hazelcast Map 大小限制
- 定期清理过期数据

## 项目结构

```
biometric-parent/
├── biometric-algo/                      # 算法库模块
│   ├── src/main/java/
│   │   └── com/biometric/algo/
│   │       ├── config/
│   │       │   └── HazelcastConfig.java # Hazelcast配置
│   │       ├── model/
│   │       │   ├── FaceFeature.java
│   │       │   └── FaceMatchResult.java
│   │       ├── service/
│   │       │   └── FaceRecognitionService.java
│   │       └── util/
│   │           └── Face303JavaCalcuater.java
│   └── pom.xml
│
├── biometric-serv/                      # 业务服务模块
│   ├── src/main/java/
│   │   └── com/biometric/serv/
│   │       ├── BiometricServApplication.java
│   │       ├── controller/
│   │       │   ├── BiometricController.java
│   │       │   ├── FaceFeatureLoadController.java
│   │       │   └── UserController.java
│   │       ├── dto/                     # 数据传输对象
│   │       ├── entity/                  # 实体类
│   │       ├── mapper/                  # MyBatis Mapper
│   │       ├── service/                 # 业务服务
│   │       │   ├── BiometricService.java
│   │       │   └── FaceFeatureLoadService.java
│   │       └── vo/                      # 视图对象
│   └── src/main/resources/
│       ├── application.yml
│       └── db/
│           └── schema.sql               # 数据库建表脚本
│
├── pom.xml                              # 父项目POM
├── build.bat / build.sh                 # 编译脚本
├── start-serv.bat / start-serv.sh       # 启动脚本
├── docker-compose.yml                   # Docker Compose 配置
└── README.md                            # 项目文档
```

## 架构变更历史

### v2.0.0 (当前版本)
- **重大变更**: 将 biometric-algo 从独立微服务改为 jar 包库
- biometric-algo 现在作为依赖集成到 biometric-serv 中
- 简化了部署架构，只需启动一个服务
- Hazelcast 集群仍然支持多节点部署
- 移除了 algo 服务的 HTTP Controller 层
- 服务间不再通过 HTTP 调用，改为直接方法调用，提升性能

### v1.0.0 (历史版本)
- biometric-algo 作为独立微服务部署
- biometric-serv 通过 WebClient 调用 algo 服务的 HTTP 接口
- 需要分别启动两个服务

## 许可证

MIT License

## 联系方式

如有问题，请提交 Issue 或 Pull Request。
