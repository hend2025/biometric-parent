@echo off
REM Build all modules on Windows

echo Building Biometric Parent Project...

REM Clean and package
call mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Build Success!
    echo ========================================
    echo.
    echo JAR files location:
    echo - biometric-algo: biometric-algo\target\biometric-algo-1.0.0.jar
    echo - biometric-serv: biometric-serv\target\biometric-serv-1.0.0.jar
    echo.
) else (
    echo.
    echo ========================================
    echo Build Failed!
    echo ========================================
    echo.
)

pause

