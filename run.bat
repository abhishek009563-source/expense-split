@echo off
title Expense Splitter - Smart Group Expense Management System
echo =====================================================================
echo           EXPENSE SPLITTER - JAVA DESKTOP APPLICATION
echo =====================================================================
echo.

where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed or not in PATH!
    echo Please install Java 17 or later (Java 21 LTS recommended).
    pause
    exit /b 1
)

echo [INFO] Detected Java environment:
java -version
echo.

cd /d "%~dp0"

if exist "target\expense-splitter-1.0.0.jar" (
    echo [INFO] Found pre-built executable fat JAR.
    echo [INFO] Launching Expense Splitter...
    echo.
    java -jar target\expense-splitter-1.0.0.jar
) else (
    echo [INFO] Standalone JAR not found. Building project with Maven...
    call mvn clean package -DskipTests
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo [INFO] Build successful! Launching Expense Splitter...
        java -jar target\expense-splitter-1.0.0.jar
    ) else (
        echo.
        echo [ERROR] Maven build failed. Attempting direct javafx:run...
        call mvn javafx:run
    )
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [NOTE] Application exited with code %ERRORLEVEL%.
    pause
)

