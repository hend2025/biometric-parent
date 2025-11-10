@echo off
REM Start biometric-serv service on Windows

echo Starting Biometric Serv Service...

cd biometric-serv

REM Check if jar exists
if not exist "target\biometric-serv-1.0.0.jar" (
    echo Building project...
    cd ..
    call mvn clean package -DskipTests
    cd biometric-serv
)

REM Start with optimized JVM parameters
java -Xmx2g -Xms2g -XX:+UseG1GC -jar target\biometric-serv-1.0.0.jar

pause

