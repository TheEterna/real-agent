@echo off
REM Real-Agent Windows 快速启动脚本
REM 使用方法: quick-start.bat [环境名称]
REM 例如: quick-start.bat dev

setlocal enabledelayedexpansion

REM 默认环境为 dev
if "%1"=="" (
    set PROFILE=dev
) else (
    set PROFILE=%1
)

echo 🚀 Real-Agent 快速启动脚本
echo ================================
echo 目标环境: %PROFILE%
echo.

REM 检查 Java 环境
echo 📋 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java 未安装或未配置在 PATH 中
    echo 请安装 Java 17 或更高版本
    pause
    exit /b 1
)

for /f "tokens=3" %%a in ('java -version 2^>^&1 ^| findstr "version"') do (
    set JAVA_VERSION_STRING=%%a
    set JAVA_VERSION_STRING=!JAVA_VERSION_STRING:"=!
    for /f "tokens=1,2 delims=." %%b in ("!JAVA_VERSION_STRING!") do (
        if %%b geq 17 (
            echo ✅ Java 版本检查通过
        ) else (
            echo ❌ Java 版本过低，需要 Java 17+
            pause
            exit /b 1
        )
    )
)

REM 检查 Maven 环境
echo.
echo 📋 检查 Maven 环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Maven 未安装或未配置在 PATH 中
    echo 请安装 Maven 3.6 或更高版本
    pause
    exit /b 1
)
echo ✅ Maven 版本检查通过

REM 检查环境变量文件
echo.
echo 📋 检查环境配置...
if not exist ".env" (
    if exist ".env.example" (
        echo ⚠️  未找到 .env 文件，正在从 .env.example 复制...
        copy ".env.example" ".env" >nul
        echo ✅ 已创建 .env 文件，请编辑此文件设置您的配置
        echo 🔧 主要配置项：
        echo    - DASHSCOPE_API_KEY: 通义千问 API 密钥
        echo    - DB_PASSWORD: 数据库密码
        echo    - BAIDU_MAP_API_KEY: 百度地图 API 密钥 (可选)
        echo.
        pause
    ) else (
        echo ❌ 未找到 .env 或 .env.example 文件
        pause
        exit /b 1
    )
) else (
    echo ✅ 找到 .env 配置文件
)

REM 清理并编译项目
echo.
echo 🔨 编译项目...
echo 正在执行: mvn clean compile -Dspring-javaformat.skip=true
call mvn clean compile -Dspring-javaformat.skip=true
if errorlevel 1 (
    echo ❌ 项目编译失败
    pause
    exit /b 1
)
echo ✅ 项目编译成功

REM 运行测试 (可选)
echo.
set /p run_tests="🧪 是否运行测试? [y/N]: "
if /i "%run_tests%"=="y" (
    echo 正在运行测试...
    call mvn test -Dspring.profiles.active=%PROFILE%
    if errorlevel 1 (
        echo ❌ 测试失败，但将继续启动应用
    ) else (
        echo ✅ 测试通过
    )
)

REM 启动应用
echo.
echo 🚀 启动应用...
echo 环境: %PROFILE%
echo 配置文件: application-%PROFILE%.yml
echo.
echo 正在执行: mvn spring-boot:run -pl real-agent-web -Dspring-boot.run.profiles=%PROFILE%
echo.
echo 📱 应用启动后可以通过以下地址访问:
echo    - 主服务: http://localhost:8080
echo    - 监控端点: http://localhost:8081/actuator
echo    - Chat API: http://localhost:8080/api/agent/react/stream
echo.
echo 按 Ctrl+C 停止应用
echo ================================

cd real-agent-web
call mvn spring-boot:run -Dspring-boot.run.profiles=%PROFILE%

pause