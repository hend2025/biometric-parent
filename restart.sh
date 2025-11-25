#!/bin/bash

echo "正在重启应用..."

# 停止应用
./stop.sh

# 等待2秒
sleep 2

# 启动应用
./start-daemon.sh

echo "应用重启完成"
