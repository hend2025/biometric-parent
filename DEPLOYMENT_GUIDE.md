# 生物识别系统部署指南

## 📦 部署概述

本指南详细说明如何在生产环境中部署生物识别系统，支持单节点和多节点分布式部署。

---

## 🎯 部署架构

### 单节点架构
```
┌─────────────────┐
│   Nginx/LB      │
│   (Optional)    │
└────────┬────────┘
         │
┌────────▼──────────┐
│   应用节点-1       │
│   - 应用服务       │
│   - Hazelcast     │
│   - 全量数据       │
└───────────────────┘
         │
┌────────▼──────────┐
│   MySQL数据库      │
└───────────────────┘
```

### 多节点分布式架构 (推荐)
```
┌─────────────────────────────┐
│       Nginx 负载均衡器        │
└──────┬──────────┬───────────┘
       │          │
   ┌───▼──┐   ┌──▼───┐
   │节点-1│───│节点-2│  (Hazelcast集群)
   └───┬──┘   └──┬───┘
       │         │
       └────┬────┘
            │
      ┌─────▼─────┐
      │   MySQL   │
      └───────────┘

特点:
- 每个节点只加载部分数据 (数据分片)
- 自动故障转移
- 水平扩展能力
- 负载均衡
```

---

## 🔧 环境准备

### 1. 系统要求

#### 操作系统
- CentOS 7/8
- Ubuntu 18.04/20.04
- RHEL 7/8

#### 软件依赖
- Java 8 或更高版本
- MySQL 5.7 或更高版本
- Nginx (可选，用于负载均衡)

### 2. 安装 Java

```bash
# CentOS/RHEL
sudo yum install java-1.8.0-openjdk-devel -y

# Ubuntu/Debian
sudo apt-get update
sudo apt-get install openjdk-8-jdk -y

# 验证安装
java -version
```

### 3. 安装 MySQL

```bash
# CentOS/RHEL 8
sudo yum install mysql-server -y
sudo systemctl start mysqld
sudo systemctl enable mysqld

# Ubuntu/Debian
sudo apt-get install mysql-server -y
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

### 4. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE medicare_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER 'biometric'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON medicare_test.* TO 'biometric'@'%';
FLUSH PRIVILEGES;

-- 导入表结构
USE medicare_test;
SOURCE /path/to/schema.sql;

-- 创建索引
CREATE INDEX idx_face_bosg_id ON bosg_face_ftur_d(FACE_BOSG_ID);
CREATE INDEX idx_psn_tmpl_no ON bosg_face_ftur_d(PSN_TMPL_NO);
CREATE INDEX idx_vali_flag_status ON bosg_face_ftur_d(VALI_FLAG, FACE_TMPL_STAS);
```

---

## 📥 应用部署

### 1. 编译打包

```bash
# 进入项目目录
cd biometric-parent

# Maven 打包
mvn clean package -DskipTests

# 打包完成后，jar文件位于:
# biometric-serv/target/biometric-serv.jar
```

### 2. 目录结构

```bash
# 创建应用目录
sudo mkdir -p /opt/biometric
sudo mkdir -p /opt/biometric/logs
sudo mkdir -p /opt/biometric/config

# 复制文件
sudo cp biometric-serv/target/biometric-serv.jar /opt/biometric/
sudo cp biometric-serv/src/main/resources/application.yml /opt/biometric/config/

# 设置权限
sudo chown -R biometric:biometric /opt/biometric
```

### 3. 配置文件

编辑 `/opt/biometric/config/application.yml`:

#### 单节点配置
```yaml
server:
  port: 7082

spring:
  application:
    name: biometric-serv
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://localhost:3306/medicare_test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true
    username: biometric
    password: your_secure_password
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50

hazelcast:
  cluster:
    name: biometric-cluster
  members: 127.0.0.1

biometric:
  face:
    recognition:
      threshold: 0.6
      topN: 10
    autoload: true
    partition: false    # 单节点不需要分区
    load:
      batchSize: 500
      parallelThreads: 4
```

#### 多节点配置

**节点1 (192.168.57.225)**:
```yaml
server:
  port: 7082

spring:
  datasource:
    url: jdbc:mysql://192.168.10.147:3306/medicare_test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true
    username: biometric
    password: your_secure_password
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50

hazelcast:
  cluster:
    name: biometric-cluster
  members: 192.168.57.225,192.168.57.100

biometric:
  face:
    autoload: true
    partition: true     # 启用数据分片
    load:
      batchSize: 500
      parallelThreads: 4
```

**节点2 (192.168.57.100)**: 配置相同，只需确保 `hazelcast.members` 包含所有节点

### 4. 防火墙配置

```bash
# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=7082/tcp
sudo firewall-cmd --permanent --add-port=5701-5801/tcp
sudo firewall-cmd --reload

# Ubuntu/Debian
sudo ufw allow 7082/tcp
sudo ufw allow 5701:5801/tcp
sudo ufw reload
```

---

## 🚀 启动应用

### 1. 创建启动脚本

创建 `/opt/biometric/start.sh`:

```bash
#!/bin/bash

APP_NAME=biometric-serv
APP_HOME=/opt/biometric
JAR_FILE=$APP_HOME/$APP_NAME.jar
CONFIG_FILE=$APP_HOME/config/application.yml
LOG_DIR=$APP_HOME/logs
PID_FILE=$APP_HOME/$APP_NAME.pid

# JVM 参数
JVM_OPTS="-server"
JVM_OPTS="$JVM_OPTS -Xms4g -Xmx4g"
JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=200"
JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS="$JVM_OPTS -XX:HeapDumpPath=$LOG_DIR"
JVM_OPTS="$JVM_OPTS -XX:+PrintGCDetails"
JVM_OPTS="$JVM_OPTS -XX:+PrintGCDateStamps"
JVM_OPTS="$JVM_OPTS -Xloggc:$LOG_DIR/gc.log"

# 应用参数
APP_OPTS="--spring.config.location=$CONFIG_FILE"

# 启动
echo "启动 $APP_NAME ..."
nohup java $JVM_OPTS -jar $JAR_FILE $APP_OPTS > $LOG_DIR/console.log 2>&1 &
echo $! > $PID_FILE

sleep 3

if [ -f $PID_FILE ]; then
    PID=$(cat $PID_FILE)
    if ps -p $PID > /dev/null; then
        echo "$APP_NAME 启动成功, PID: $PID"
    else
        echo "$APP_NAME 启动失败"
        exit 1
    fi
fi
```

创建 `/opt/biometric/stop.sh`:

```bash
#!/bin/bash

APP_NAME=biometric-serv
APP_HOME=/opt/biometric
PID_FILE=$APP_HOME/$APP_NAME.pid

if [ -f $PID_FILE ]; then
    PID=$(cat $PID_FILE)
    echo "停止 $APP_NAME (PID: $PID) ..."
    kill $PID
    
    # 等待进程结束
    for i in {1..30}; do
        if ! ps -p $PID > /dev/null; then
            echo "$APP_NAME 已停止"
            rm -f $PID_FILE
            exit 0
        fi
        sleep 1
    done
    
    # 强制终止
    echo "强制停止 $APP_NAME ..."
    kill -9 $PID
    rm -f $PID_FILE
else
    echo "$APP_NAME 未运行"
fi
```

设置执行权限:
```bash
sudo chmod +x /opt/biometric/start.sh
sudo chmod +x /opt/biometric/stop.sh
```

### 2. 启动应用

```bash
# 启动
sudo -u biometric /opt/biometric/start.sh

# 查看日志
tail -f /opt/biometric/logs/console.log

# 查看GC日志
tail -f /opt/biometric/logs/gc.log
```

### 3. 验证启动

```bash
# 检查端口
netstat -tlnp | grep 7082
netstat -tlnp | grep 5701

# 健康检查
curl http://localhost:7082/api/monitor/health

# 集群信息
curl http://localhost:7082/api/monitor/cluster
```

---

## 🔄 配置 Systemd 服务

### 1. 创建服务文件

创建 `/etc/systemd/system/biometric.service`:

```ini
[Unit]
Description=Biometric Recognition Service
After=network.target mysql.service

[Service]
Type=simple
User=biometric
Group=biometric
WorkingDirectory=/opt/biometric

Environment="JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk"
Environment="JVM_OPTS=-server -Xms4g -Xmx4g -XX:+UseG1GC"

ExecStart=/usr/bin/java $JVM_OPTS -jar /opt/biometric/biometric-serv.jar --spring.config.location=/opt/biometric/config/application.yml

StandardOutput=journal
StandardError=journal
SyslogIdentifier=biometric

Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 2. 启用服务

```bash
# 重新加载配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start biometric

# 设置开机自启
sudo systemctl enable biometric

# 查看状态
sudo systemctl status biometric

# 查看日志
sudo journalctl -u biometric -f
```

---

## 🌐 配置负载均衡

### Nginx 配置

安装 Nginx:
```bash
# CentOS/RHEL
sudo yum install nginx -y

# Ubuntu/Debian
sudo apt-get install nginx -y
```

编辑 `/etc/nginx/conf.d/biometric.conf`:

```nginx
upstream biometric_cluster {
    least_conn;
    server 192.168.57.225:7082 weight=1 max_fails=3 fail_timeout=30s;
    server 192.168.57.100:7082 weight=1 max_fails=3 fail_timeout=30s;
    
    keepalive 32;
}

server {
    listen 80;
    server_name biometric.example.com;

    access_log /var/log/nginx/biometric_access.log;
    error_log /var/log/nginx/biometric_error.log;

    location /api/ {
        proxy_pass http://biometric_cluster;
        
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        proxy_connect_timeout 30s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }

    location /druid/ {
        deny all;
    }

    location /api/monitor/health {
        proxy_pass http://biometric_cluster;
        access_log off;
    }
}
```

启动 Nginx:
```bash
# 测试配置
sudo nginx -t

# 启动
sudo systemctl start nginx
sudo systemctl enable nginx

# 重新加载
sudo systemctl reload nginx
```

---

## 🔍 部署验证

### 1. 功能测试

```bash
# 健康检查
curl http://负载均衡器IP/api/monitor/health

# 集群状态
curl http://负载均衡器IP/api/monitor/cluster

# 性能指标
curl http://负载均衡器IP/api/monitor/metrics

# 人脸识别测试 (需要准备测试数据)
curl -X POST http://负载均衡器IP/api/biometric/face/recognize \
  -H "Content-Type: application/json" \
  -d '{}'
```

### 2. 压力测试

使用 Apache Bench:
```bash
# 安装 ab
sudo yum install httpd-tools -y

# 压力测试
ab -n 1000 -c 10 http://负载均衡器IP/api/monitor/health
```

使用 JMeter 或 Gatling 进行更详细的性能测试。

---

## 📊 监控配置

### 1. Druid 监控

访问: `http://节点IP:7082/druid/`
- 用户名: admin
- 密码: admin

**建议**: 生产环境应修改默认密码并限制访问IP

### 2. 应用监控

定期调用监控接口:
```bash
# 每分钟检查一次
*/1 * * * * curl -s http://localhost:7082/api/monitor/health | logger -t biometric-health

# 每5分钟收集指标
*/5 * * * * curl -s http://localhost:7082/api/monitor/metrics | logger -t biometric-metrics
```

### 3. 系统监控

安装监控工具:
```bash
# 安装 htop
sudo yum install htop -y

# 安装 iotop
sudo yum install iotop -y

# 安装 nethogs
sudo yum install nethogs -y
```

---

## 🔄 滚动升级

### 多节点环境滚动升级步骤:

#### 1. 准备新版本
```bash
# 编译新版本
mvn clean package -DskipTests

# 备份旧版本
sudo cp /opt/biometric/biometric-serv.jar /opt/biometric/biometric-serv.jar.bak
```

#### 2. 逐个升级节点

**节点1**:
```bash
# 停止节点1
sudo systemctl stop biometric

# 替换jar包
sudo cp new-version/biometric-serv.jar /opt/biometric/

# 启动节点1
sudo systemctl start biometric

# 等待节点加入集群 (约30秒)
sleep 30

# 验证节点1状态
curl http://192.168.57.225:7082/api/monitor/health
```

**节点2**:
```bash
# 等待5分钟确保节点1稳定
sleep 300

# 重复上述步骤升级节点2
sudo systemctl stop biometric
sudo cp new-version/biometric-serv.jar /opt/biometric/
sudo systemctl start biometric

# 验证节点2状态
curl http://192.168.57.100:7082/api/monitor/health
```

#### 3. 验证集群
```bash
# 检查集群状态
curl http://负载均衡器IP/api/monitor/cluster

# 确认数据完整性
# 对比升级前后的缓存数据量
```

---

## 🐛 故障排查

### 常见问题

#### 1. 应用无法启动

**检查日志**:
```bash
tail -100 /opt/biometric/logs/console.log
sudo journalctl -u biometric -n 100
```

**常见原因**:
- 端口被占用
- 数据库连接失败
- 配置文件错误
- 内存不足

**解决方法**:
```bash
# 检查端口
netstat -tlnp | grep 7082

# 测试数据库连接
mysql -h 数据库IP -u biometric -p

# 检查内存
free -h
```

#### 2. 节点无法加入集群

**检查网络**:
```bash
# 测试连通性
ping 目标节点IP

# 测试端口
telnet 目标节点IP 5701
```

**检查防火墙**:
```bash
sudo firewall-cmd --list-ports
sudo iptables -L -n
```

#### 3. 性能问题

**检查资源使用**:
```bash
# CPU
top -H -p $(pgrep -f biometric-serv)

# 内存
jmap -heap $(pgrep -f biometric-serv)

# GC
tail -f /opt/biometric/logs/gc.log
```

---

## 📋 部署检查清单

### 部署前
- [ ] Java 环境已安装并配置
- [ ] MySQL 数据库已安装
- [ ] 数据库表结构已创建
- [ ] 数据库索引已创建
- [ ] 防火墙规则已配置
- [ ] 应用目录已创建
- [ ] 配置文件已准备

### 部署后
- [ ] 应用成功启动
- [ ] 健康检查通过
- [ ] 集群节点已连接
- [ ] 数据已成功加载
- [ ] 识别接口可用
- [ ] 监控接口可用
- [ ] 日志正常输出
- [ ] 性能指标正常

### 生产环境额外检查
- [ ] 负载均衡器已配置
- [ ] SSL证书已配置
- [ ] 备份策略已实施
- [ ] 监控告警已配置
- [ ] 文档已更新
- [ ] 团队已培训

---

## 📞 获取帮助

### 日志位置
- 应用日志: `/opt/biometric/logs/console.log`
- GC日志: `/opt/biometric/logs/gc.log`
- 系统日志: `journalctl -u biometric`
- Nginx日志: `/var/log/nginx/`

### 有用的命令
```bash
# 查看应用进程
ps aux | grep biometric

# 查看端口占用
netstat -tlnp | grep -E '(7082|5701)'

# 查看集群状态
curl http://localhost:7082/api/monitor/cluster | jq

# 查看性能指标
curl http://localhost:7082/api/monitor/metrics | jq

# 实时查看日志
tail -f /opt/biometric/logs/console.log | grep ERROR
```

---

**部署指南版本**: v2.0.0

**最后更新**: 2024年11月

