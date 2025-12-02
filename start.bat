@echo off
chcp 65001
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
java -jar -Xms10g -Xmx10g -XX:+UseG1GC -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 biometric-serv\target\biometric-serv-1.0.0.jar
pause
