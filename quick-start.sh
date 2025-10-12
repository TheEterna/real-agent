#!/bin/bash

# Real-Agent 快速启动脚本
# 使用方法: ./quick-start.sh [环境名称]
# 例如: ./quick-start.sh dev

set -e

# 默认环境为 dev
PROFILE=${1:-dev}

echo "🚀 Real-Agent 快速启动脚本"
echo "================================"
echo "目标环境: $PROFILE"
echo ""

# 检查 Java 环境
echo "📋 检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装或未配置在 PATH 中"
    echo "请安装 Java 17 或更高版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 版本过低，当前版本: $JAVA_VERSION，需要 Java 17+"
    exit 1
fi
echo "✅ Java 版本检查通过: $(java -version 2>&1 | head -n 1)"

# 检查 Maven 环境
echo ""
echo "📋 检查 Maven 环境..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装或未配置在 PATH 中"
    echo "请安装 Maven 3.6 或更高版本"
    exit 1
fi
echo "✅ Maven 版本检查通过: $(mvn -version | head -n 1)"

# 检查环境变量文件
echo ""
echo "📋 检查环境配置..."
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        echo "⚠️  未找到 .env 文件，正在从 .env.example 复制..."
        cp .env.example .env
        echo "✅ 已创建 .env 文件，请编辑此文件设置您的配置"
        echo "🔧 主要配置项："
        echo "   - DASHSCOPE_API_KEY: 通义千问 API 密钥"
        echo "   - DB_PASSWORD: 数据库密码"
        echo "   - BAIDU_MAP_API_KEY: 百度地图 API 密钥 (可选)"
        echo ""
        read -p "请编辑 .env 文件后按 Enter 继续，或按 Ctrl+C 退出..."
    else
        echo "❌ 未找到 .env 或 .env.example 文件"
        exit 1
    fi
else
    echo "✅ 找到 .env 配置文件"
fi

# 加载环境变量
echo ""
echo "📋 加载环境变量..."
if [ -f ".env" ]; then
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
    echo "✅ 环境变量加载完成"
else
    echo "⚠️  未找到 .env 文件，将使用默认配置"
fi

# 检查必需的环境变量
echo ""
echo "📋 验证关键配置..."
if [ -z "$DASHSCOPE_API_KEY" ] && [ -z "$OPENAI_API_KEY" ]; then
    echo "⚠️  警告: 未设置 LLM API 密钥 (DASHSCOPE_API_KEY 或 OPENAI_API_KEY)"
    echo "   应用可能无法正常工作"
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "⚠️  警告: 未设置数据库密码 (DB_PASSWORD)"
    echo "   如果使用数据库功能，请确保配置正确"
fi

# 清理并编译项目
echo ""
echo "🔨 编译项目..."
echo "正在执行: mvn clean compile -Dspring-javaformat.skip=true"
if mvn clean compile -Dspring-javaformat.skip=true; then
    echo "✅ 项目编译成功"
else
    echo "❌ 项目编译失败"
    exit 1
fi

# 运行测试 (可选)
echo ""
read -p "🧪 是否运行测试? [y/N]: " run_tests
if [[ $run_tests =~ ^[Yy]$ ]]; then
    echo "正在运行测试..."
    if mvn test -Dspring.profiles.active=$PROFILE; then
        echo "✅ 测试通过"
    else
        echo "❌ 测试失败，但将继续启动应用"
    fi
fi

# 启动应用
echo ""
echo "🚀 启动应用..."
echo "环境: $PROFILE"
echo "配置文件: application-$PROFILE.yml"
echo ""
echo "正在执行: mvn spring-boot:run -pl real-agent-web -Dspring-boot.run.profiles=$PROFILE"
echo ""
echo "📱 应用启动后可以通过以下地址访问:"
echo "   - 主服务: http://localhost:${SERVER_PORT:-8080}"
echo "   - 监控端点: http://localhost:${MANAGEMENT_SERVER_PORT:-8081}/actuator"
echo "   - Chat API: http://localhost:${SERVER_PORT:-8080}/api/agent/react/stream"
echo ""
echo "按 Ctrl+C 停止应用"
echo "================================"

cd real-agent-web
exec mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE