#!/bin/bash
# Start biometric-serv service on Linux/Mac

echo "Starting Biometric Serv Service..."

cd biometric-serv

# Check if jar exists
if [ ! -f "target/biometric-serv-1.0.0.jar" ]; then
    echo "Building project..."
    cd ..
    mvn clean package -DskipTests
    cd biometric-serv
fi

# Start with optimized JVM parameters
java -Xmx2g -Xms2g -XX:+UseG1GC -jar target/biometric-serv-1.0.0.jar

