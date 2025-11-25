#!/bin/bash

# 设置UTF-8编码
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

# 应用配置
APP_NAME="biometric-serv"
JAR_FILE="biometric-serv-1.0.0.jar"
LOG_DIR="logs"
PID_FILE="${APP_NAME}.pid"

# 创建日志目录
mkdir -p ${LOG_DIR}

# 启动应用（后台运行）
nohup java -jar -Xmx50G -Xms32G \
  -Dfile.encoding=UTF-8 \
  -Dsun.jnu.encoding=UTF-8 \
  ${JAR_FILE} \
  > ${LOG_DIR}/console.log 2>&1 &

# 保存进程ID
echo $! > ${PID_FILE}

echo "应用已启动，PID: $(cat ${PID_FILE})"
echo "日志文件: ${LOG_DIR}/biometric-serv.log"
echo "控制台输出: ${LOG_DIR}/console.log"
