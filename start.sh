#!/bin/bash

# 设置UTF-8编码
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

# 运行应用
java -jar -Xmx50G -Xms32G \
  -Dfile.encoding=UTF-8 \
  -Dsun.jnu.encoding=UTF-8 \
  biometric-serv-1.0.0.jar
