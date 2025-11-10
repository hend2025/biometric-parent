# 部署指南

本文档介绍如何在不同环境下部署 biometric-parent 项目。

## 目录

- [开发环境部署](#开发环境部署)
- [生产环境部署](#生产环境部署)
- [Docker 部署](#docker-部署)
- [多节点分布式部署](#多节点分布式部署)
- [负载均衡配置](#负载均衡配置)

## 开发环境部署

### 1. 环境准备

- JDK 1.8+
- Maven 3.3+
- MySQL 5.7+
- IDE（IntelliJ IDEA 或 Eclipse）

### 2. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 执行建表脚本
source biometric-serv/src/main/resources/db/schema.sql
```

### 3. 修改配置

修改 `biometric-serv/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    username: root
    password: your_password
```

### 4. 启动服务

#### 使用 IDE 启动

1. 运行 `BiometricAlgoApplication.java` (端口 8081)
2. 运行 `BiometricServApplication.java` (端口 8080)

#### 使用命令行启动

```bash
# 编译打包
mvn clean package -DskipTests

# 启动算法服务
cd biometric-algo
mvn spring-boot:run

# 新开终端，启动业务服务
cd biometric-serv
mvn spring-boot:run
```

#### 使用启动脚本

**Windows:**
```bash
build.bat          # 编译打包
start-algo.bat     # 启动算法服务
start-serv.bat     # 启动业务服务
```

**Linux/Mac:**
```bash
chmod +x *.sh
./build.sh         # 编译打包
./start-algo.sh    # 启动算法服务
./start-serv.sh    # 启动业务服务
```

## 生产环境部署

### 1. 编译打包

```bash
mvn clean package -DskipTests
```

生成的 jar 包位置：
- `biometric-algo/target/biometric-algo-1.0.0.jar`
- `biometric-serv/target/biometric-serv-1.0.0.jar`

### 2. 准备配置文件

创建外部配置文件 `application-prod.yml`：

**算法服务配置 (algo-application-prod.yml):**
```yaml
server:
  port: 8081

hazelcast:
  cluster:
    name: biometric-cluster
  port: 5701
  members: 192.168.1.10,192.168.1.11,192.168.1.12

face:
  recognition:
    threshold: 0.65
    topN: 20
```

**业务服务配置 (serv-application-prod.yml):**
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://192.168.1.100:3306/biometric?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: biometric_user
    password: secure_password
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50

biometric:
  algo:
    url: http://192.168.1.10:8081

logging:
  level:
    com.biometric: INFO
  file:
    name: logs/biometric-serv.log
```

### 3. 启动脚本

**算法服务启动脚本 (start-algo-prod.sh):**
```bash
#!/bin/bash

APP_NAME="biometric-algo"
JAR_FILE="biometric-algo-1.0.0.jar"
CONFIG_FILE="algo-application-prod.yml"
LOG_DIR="logs"
PID_FILE="${APP_NAME}.pid"

# 创建日志目录
mkdir -p ${LOG_DIR}

# 检查是否已运行
if [ -f ${PID_FILE} ]; then
    PID=$(cat ${PID_FILE})
    if ps -p ${PID} > /dev/null; then
        echo "${APP_NAME} is already running (PID: ${PID})"
        exit 1
    fi
fi

# 启动应用
nohup java -Xmx4g -Xms4g \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=${LOG_DIR}/heap_dump.hprof \
    -Dspring.config.location=${CONFIG_FILE} \
    -jar ${JAR_FILE} \
    > ${LOG_DIR}/${APP_NAME}.log 2>&1 &

echo $! > ${PID_FILE}
echo "${APP_NAME} started (PID: $!)"
```

**业务服务启动脚本 (start-serv-prod.sh):**
```bash
#!/bin/bash

APP_NAME="biometric-serv"
JAR_FILE="biometric-serv-1.0.0.jar"
CONFIG_FILE="serv-application-prod.yml"
LOG_DIR="logs"
PID_FILE="${APP_NAME}.pid"

# 创建日志目录
mkdir -p ${LOG_DIR}

# 检查是否已运行
if [ -f ${PID_FILE} ]; then
    PID=$(cat ${PID_FILE})
    if ps -p ${PID} > /dev/null; then
        echo "${APP_NAME} is already running (PID: ${PID})"
        exit 1
    fi
fi

# 启动应用
nohup java -Xmx2g -Xms2g \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=${LOG_DIR}/heap_dump.hprof \
    -Dspring.config.location=${CONFIG_FILE} \
    -jar ${JAR_FILE} \
    > ${LOG_DIR}/${APP_NAME}.log 2>&1 &

echo $! > ${PID_FILE}
echo "${APP_NAME} started (PID: $!)"
```

**停止脚本 (stop.sh):**
```bash
#!/bin/bash

APP_NAME=$1
PID_FILE="${APP_NAME}.pid"

if [ ! -f ${PID_FILE} ]; then
    echo "${APP_NAME} is not running"
    exit 1
fi

PID=$(cat ${PID_FILE})
if ps -p ${PID} > /dev/null; then
    kill ${PID}
    echo "Stopping ${APP_NAME} (PID: ${PID})"
    
    # 等待进程结束
    count=0
    while ps -p ${PID} > /dev/null && [ ${count} -lt 30 ]; do
        sleep 1
        count=$((count+1))
    done
    
    if ps -p ${PID} > /dev/null; then
        kill -9 ${PID}
        echo "Force killed ${APP_NAME}"
    else
        echo "${APP_NAME} stopped"
    fi
    
    rm -f ${PID_FILE}
else
    echo "${APP_NAME} is not running (stale PID file)"
    rm -f ${PID_FILE}
fi
```

### 4. 使用 systemd 管理服务（推荐）

**算法服务 (biometric-algo.service):**
```ini
[Unit]
Description=Biometric Algo Service
After=network.target

[Service]
Type=simple
User=biometric
WorkingDirectory=/opt/biometric-algo
ExecStart=/usr/bin/java -Xmx4g -Xms4g -XX:+UseG1GC -Dspring.config.location=/opt/biometric-algo/application-prod.yml -jar /opt/biometric-algo/biometric-algo-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**业务服务 (biometric-serv.service):**
```ini
[Unit]
Description=Biometric Serv Service
After=network.target mysql.service biometric-algo.service
Requires=mysql.service
Wants=biometric-algo.service

[Service]
Type=simple
User=biometric
WorkingDirectory=/opt/biometric-serv
ExecStart=/usr/bin/java -Xmx2g -Xms2g -XX:+UseG1GC -Dspring.config.location=/opt/biometric-serv/application-prod.yml -jar /opt/biometric-serv/biometric-serv-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**安装和启动:**
```bash
# 复制 service 文件
sudo cp biometric-algo.service /etc/systemd/system/
sudo cp biometric-serv.service /etc/systemd/system/

# 重载配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start biometric-algo
sudo systemctl start biometric-serv

# 设置开机自启
sudo systemctl enable biometric-algo
sudo systemctl enable biometric-serv

# 查看状态
sudo systemctl status biometric-algo
sudo systemctl status biometric-serv
```

## Docker 部署

### 1. 构建镜像

```bash
# 先编译项目
mvn clean package -DskipTests

# 构建镜像
docker build -t biometric-algo:1.0.0 ./biometric-algo
docker build -t biometric-serv:1.0.0 ./biometric-serv
```

### 2. 使用 Docker Compose 启动

```bash
# 启动所有服务（3个算法节点 + 1个业务节点 + MySQL）
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

### 3. 单独启动服务

```bash
# 启动 MySQL
docker run -d --name biometric-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=biometric \
  -p 3306:3306 \
  mysql:5.7

# 启动算法服务
docker run -d --name biometric-algo \
  -p 8081:8081 \
  -p 5701:5701 \
  -e JAVA_OPTS="-Xmx4g -Xms4g" \
  biometric-algo:1.0.0

# 启动业务服务
docker run -d --name biometric-serv \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://mysql:3306/biometric" \
  -e BIOMETRIC_ALGO_URL="http://algo:8081" \
  --link biometric-mysql:mysql \
  --link biometric-algo:algo \
  biometric-serv:1.0.0
```

## 多节点分布式部署

### 架构图

```
                    ┌─────────────┐
                    │   Nginx LB  │
                    │  (80/443)   │
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐       ┌────▼────┐      ┌────▼────┐
    │ Serv-1  │       │ Serv-2  │      │ Serv-3  │
    │  8080   │       │  8080   │      │  8080   │
    └────┬────┘       └────┬────┘      └────┬────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐       ┌────▼────┐      ┌────▼────┐
    │ Algo-1  │◄─────►│ Algo-2  │◄────►│ Algo-3  │
    │ 8081    │ Hazel │ 8081    │ cast │ 8081    │
    │ 5701    │ cast  │ 5702    │ Clust│ 5703    │
    └─────────┘       └─────────┘  er  └─────────┘
                           │
                    ┌──────▼──────┐
                    │   MySQL     │
                    │    3306     │
                    └─────────────┘
```

### 部署步骤

#### 1. 算法服务集群（3个节点）

**节点1 (192.168.1.10):**
```bash
java -Xmx4g -Xms4g -XX:+UseG1GC \
  -Dserver.port=8081 \
  -Dhazelcast.port=5701 \
  -Dhazelcast.members=192.168.1.10,192.168.1.11,192.168.1.12 \
  -jar biometric-algo-1.0.0.jar
```

**节点2 (192.168.1.11):**
```bash
java -Xmx4g -Xms4g -XX:+UseG1GC \
  -Dserver.port=8081 \
  -Dhazelcast.port=5701 \
  -Dhazelcast.members=192.168.1.10,192.168.1.11,192.168.1.12 \
  -jar biometric-algo-1.0.0.jar
```

**节点3 (192.168.1.12):**
```bash
java -Xmx4g -Xms4g -XX:+UseG1GC \
  -Dserver.port=8081 \
  -Dhazelcast.port=5701 \
  -Dhazelcast.members=192.168.1.10,192.168.1.11,192.168.1.12 \
  -jar biometric-algo-1.0.0.jar
```

#### 2. 业务服务集群（3个节点）

**节点1 (192.168.1.20):**
```bash
java -Xmx2g -Xms2g -XX:+UseG1GC \
  -Dserver.port=8080 \
  -Dspring.datasource.url="jdbc:mysql://192.168.1.100:3306/biometric" \
  -Dbiometric.algo.url="http://192.168.1.10:8081" \
  -jar biometric-serv-1.0.0.jar
```

**节点2 (192.168.1.21):**
```bash
java -Xmx2g -Xms2g -XX:+UseG1GC \
  -Dserver.port=8080 \
  -Dspring.datasource.url="jdbc:mysql://192.168.1.100:3306/biometric" \
  -Dbiometric.algo.url="http://192.168.1.11:8081" \
  -jar biometric-serv-1.0.0.jar
```

**节点3 (192.168.1.22):**
```bash
java -Xmx2g -Xms2g -XX:+UseG1GC \
  -Dserver.port=8080 \
  -Dspring.datasource.url="jdbc:mysql://192.168.1.100:3306/biometric" \
  -Dbiometric.algo.url="http://192.168.1.12:8081" \
  -jar biometric-serv-1.0.0.jar
```

## 负载均衡配置

### Nginx 配置

```nginx
upstream biometric_serv_backend {
    least_conn;
    server 192.168.1.20:8080 weight=1 max_fails=3 fail_timeout=30s;
    server 192.168.1.21:8080 weight=1 max_fails=3 fail_timeout=30s;
    server 192.168.1.22:8080 weight=1 max_fails=3 fail_timeout=30s;
}

upstream biometric_algo_backend {
    least_conn;
    server 192.168.1.10:8081 weight=1 max_fails=3 fail_timeout=30s;
    server 192.168.1.11:8081 weight=1 max_fails=3 fail_timeout=30s;
    server 192.168.1.12:8081 weight=1 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name biometric.example.com;

    # 业务服务 API
    location /api/ {
        proxy_pass http://biometric_serv_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 算法服务 API（如果需要直接访问）
    location /api/algo/ {
        proxy_pass http://biometric_algo_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 健康检查
    location /health {
        access_log off;
        return 200 "OK";
    }
}
```

## 监控和日志

### 1. 健康检查

```bash
# 检查业务服务
curl http://localhost:8080/api/user/list

# 检查算法服务
curl http://localhost:8081/api/algo/face/feature/count
```

### 2. 日志查看

```bash
# 查看最新日志
tail -f logs/biometric-serv.log
tail -f logs/biometric-algo.log

# 查看错误日志
grep ERROR logs/biometric-serv.log
```

### 3. JVM 监控

使用 JConsole 或 VisualVM 连接：
```bash
# 启动时添加 JMX 参数
java -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -jar biometric-serv-1.0.0.jar
```

## 故障排查

### 常见问题

1. **Hazelcast 集群无法组网**
   - 检查防火墙是否开放 5701 端口
   - 检查网络是否互通
   - 确认 cluster.name 一致

2. **数据库连接失败**
   - 检查数据库是否启动
   - 确认用户名密码正确
   - 检查网络连接

3. **内存溢出**
   - 增加堆内存：`-Xmx4g -Xms4g`
   - 分析堆转储文件

4. **服务无响应**
   - 检查 CPU 和内存使用率
   - 查看日志文件
   - 检查网络连接

## 安全建议

1. **数据库安全**
   - 使用专用数据库用户，授予最小权限
   - 定期备份数据库
   - 启用 SSL 连接

2. **应用安全**
   - 使用 HTTPS
   - 添加认证和授权机制
   - 限制 API 访问频率

3. **网络安全**
   - 配置防火墙规则
   - 使用 VPN 或专用网络
   - 定期更新系统补丁

