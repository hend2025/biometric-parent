#!/bin/bash
# Start biometric-algo service on Linux/Mac

echo "Starting Biometric Algo Service..."

cd biometric-algo

# Check if jar exists
if [ ! -f "target/biometric-algo-1.0.0.jar" ]; then
    echo "Building project..."
    cd ..
    mvn clean package -DskipTests
    cd biometric-algo
fi

# Start with optimized JVM parameters
java -Xmx4g -Xms4g -XX:+UseG1GC -jar target/biometric-algo-1.0.0.jar

