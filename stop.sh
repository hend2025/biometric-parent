#!/bin/bash

APP_NAME="biometric-serv"
PID_FILE="${APP_NAME}.pid"

if [ ! -f ${PID_FILE} ]; then
    echo "PID文件不存在，应用可能未运行"
    exit 1
fi

PID=$(cat ${PID_FILE})

if ps -p ${PID} > /dev/null; then
    echo "正在停止应用 (PID: ${PID})..."
    kill ${PID}
    
    # 等待进程结束
    for i in {1..30}; do
        if ! ps -p ${PID} > /dev/null; then
            echo "应用已停止"
            rm -f ${PID_FILE}
            exit 0
        fi
        sleep 1
    done
    
    # 强制停止
    echo "强制停止应用..."
    kill -9 ${PID}
    rm -f ${PID_FILE}
    echo "应用已强制停止"
else
    echo "进程 ${PID} 不存在，清理PID文件"
    rm -f ${PID_FILE}
fi
