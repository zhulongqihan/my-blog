@echo off
chcp 65001 >nul
REM 个人博客项目启动脚本 (Windows)
REM 使用方法: start.bat

echo =========================================
echo   个人博客项目启动脚本
echo =========================================
echo.

REM 检查 Java
echo 检查 Java 环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未安装 Java 或未配置环境变量
    pause
    exit /b 1
)
echo [成功] Java 环境正常
echo.

REM 检查 Node.js
echo 检查 Node.js 环境...
node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未安装 Node.js 或未配置环境变量
    pause
    exit /b 1
)
echo [成功] Node.js 环境正常
echo.

REM 检查 Maven
echo 检查 Maven 环境...
mvn -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未安装 Maven 或未配置环境变量
    pause
    exit /b 1
)
echo [成功] Maven 环境正常
echo.

echo =========================================
echo   环境检查通过，开始启动项目...
echo =========================================
echo.

REM 创建日志目录
if not exist logs mkdir logs

REM 启动后端
echo [1/3] 启动后端服务...
cd backend
start "博客后端" /min cmd /c "mvn spring-boot:run > ..\logs\backend.log 2>&1"
cd ..
echo 后端服务已在新窗口启动
echo.

REM 等待后端启动
echo 等待后端启动（约30秒）...
timeout /t 30 /nobreak >nul
echo.

REM 检查前端依赖
if not exist "frontend\node_modules" (
    echo [2/3] 安装前端依赖...
    cd frontend
    call npm install
    cd ..
    echo.
)

REM 启动前端
echo [3/3] 启动前端服务...
cd frontend
start "博客前端" cmd /c "npm run dev"
cd ..
echo 前端服务已在新窗口启动
echo.

echo =========================================
echo   项目启动成功！
echo =========================================
echo.
echo 访问地址:
echo   前端: http://localhost:5173
echo   后端: http://localhost:8080
echo   H2控制台: http://localhost:8080/h2-console
echo.
echo 日志文件:
echo   后端: logs\backend.log
echo.
echo 停止服务:
echo   关闭启动的命令行窗口或运行 stop.bat
echo.
echo =========================================
echo.
pause
