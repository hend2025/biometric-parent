FROM openjdk:8-jdk-alpine

LABEL maintainer="biometric-serv"

# 设置工作目录
WORKDIR /app

# 复制jar包
COPY target/biometric-serv-1.0.0.jar app.jar

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

