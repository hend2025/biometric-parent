#!/bin/bash
# Build all modules on Linux/Mac

echo "Building Biometric Parent Project..."

# Clean and package
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "Build Success!"
    echo "========================================"
    echo ""
    echo "JAR files location:"
    echo "- biometric-algo: biometric-algo/target/biometric-algo-1.0.0.jar"
    echo "- biometric-serv: biometric-serv/target/biometric-serv-1.0.0.jar"
    echo ""
else
    echo ""
    echo "========================================"
    echo "Build Failed!"
    echo "========================================"
    echo ""
fi

