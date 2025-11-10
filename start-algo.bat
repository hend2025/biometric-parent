@echo off
REM Start biometric-algo service on Windows

echo Starting Biometric Algo Service...

cd biometric-algo

REM Check if jar exists
if not exist "target\biometric-algo-1.0.0.jar" (
    echo Building project...
    cd ..
    call mvn clean package -DskipTests
    cd biometric-algo
)

REM Start with optimized JVM parameters
java -Xmx4g -Xms4g -XX:+UseG1GC -jar target\biometric-algo-1.0.0.jar

pause

