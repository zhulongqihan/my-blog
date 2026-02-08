@echo off
chcp 65001 >nul
REM 个人博客项目停止脚本 (Windows)
REM 使用方法: stop.bat

echo =========================================
echo   停止个人博客项目
echo =========================================
echo.

REM 停止后端 (Spring Boot 通常运行在 8080 端口)
echo 停止后端服务...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
    if %errorlevel% equ 0 (
        echo [成功] 后端服务已停止
    )
)

REM 停止前端 (Vite 通常运行在 5173 端口)
echo 停止前端服务...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
    if %errorlevel% equ 0 (
        echo [成功] 前端服务已停止
    )
)

echo.
echo 项目已停止
echo.
pause
