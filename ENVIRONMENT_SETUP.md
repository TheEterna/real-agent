# Real-Agent 环境设置与使用指南

## 📋 目录
- [快速开始](#快速开始)
- [环境要求](#环境要求)
- [环境变量配置](#环境变量配置)
- [数据库设置](#数据库设置)
- [API 密钥配置](#api-密钥配置)
- [启动应用](#启动应用)
- [开发工作流](#开发工作流)
- [常见问题](#常见问题)

## 🚀 快速开始

### 自动化启动 (推荐)

**Linux/Mac:**
```bash
chmod +x quick-start.sh
./quick-start.sh dev
```

**Windows:**
```cmd
quick-start.bat dev
```

脚本会自动检查环境、编译项目并启动应用。

### 手动启动

1. **环境准备**
```bash
# 复制环境配置文件
cp .env.example .env

# 编辑配置文件
vim .env  # 或使用其他编辑器
```

2. **编译项目**
```bash
mvn clean compile -Dspring-javaformat.skip=true
```

3. **启动应用**
```bash
cd real-agent-web
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 🔧 环境要求

### 基础环境
- **Java**: 17 或更高版本
- **Maven**: 3.6 或更高版本
- **MySQL**: 8.0 或更高版本 (可选，用于数据持久化)

### 验证环境
```bash
java --version    # 应显示 17.x.x 或更高
mvn --version     # 应显示 3.6.x 或更高
mysql --version   # 如果使用数据库功能
```

## 🌐 环境变量配置

### 必需配置

**LLM API 配置 (必需)**
```bash
# 通义千问 (推荐)
DASHSCOPE_API_KEY=sk-your-dashscope-api-key

# 或者 OpenAI 兼容
OPENAI_API_KEY=sk-your-openai-api-key
OPENAI_BASE_URL=https://api.openai.com/v1
```

### 可选配置

**数据库配置 (可选)**
```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=real-agent
DB_USERNAME=root
DB_PASSWORD=your-password
```

**工具配置 (可选)**
```bash
# 百度地图工具
BAIDU_MAP_API_KEY=your-baidu-map-key

# MCP 自定义服务器
MCP_SERVER_URL=http://localhost:8888
MCP_SERVER_ENDPOINT=/sse
```

**应用配置**
```bash
SPRING_PROFILES_ACTIVE=dev
AGENT_TOOLS_APPROVAL_MODE=AUTO
SERVER_PORT=8080
```

## 🗃️ 数据库设置

### MySQL 安装与配置

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install mysql-server
sudo mysql_secure_installation
```

**Windows:**
下载并安装 [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)

**macOS:**
```bash
brew install mysql
brew services start mysql
```

### 创建数据库
```sql
CREATE DATABASE real_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'real_agent'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON real_agent.* TO 'real_agent'@'localhost';
FLUSH PRIVILEGES;
```

### R2DBC 配置验证
```bash
# 测试数据库连接
mysql -h localhost -u real_agent -p real_agent
```

## 🔑 API 密钥配置

### 通义千问 (阿里云)

1. 访问 [阿里云 DashScope](https://dashscope.aliyun.com/)
2. 注册并登录账号
3. 创建 API 密钥
4. 设置环境变量:
```bash
DASHSCOPE_API_KEY=sk-your-api-key
```

### OpenAI (可选)

1. 访问 [OpenAI Platform](https://platform.openai.com/)
2. 创建 API 密钥
3. 设置环境变量:
```bash
OPENAI_API_KEY=sk-your-api-key
OPENAI_BASE_URL=https://api.openai.com/v1
```

### 百度地图 (可选)

1. 访问 [百度地图开放平台](https://lbsyun.baidu.com/)
2. 注册开发者账号
3. 创建应用获取 AK
4. 设置环境变量:
```bash
BAIDU_MAP_API_KEY=your-baidu-ak
```

## 🚀 启动应用

### 开发环境启动

**方式 1: 使用 Maven**
```bash
# 切换到 web 模块
cd real-agent-web

# 启动开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 带环境变量启动
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

**方式 2: 使用 JAR 包**
```bash
# 先打包
mvn clean package -DskipTests

# 运行 JAR
java -jar real-agent-web/target/real-agent-web-*.jar --spring.profiles.active=dev
```

**方式 3: IDE 启动**
- 主类: `com.ai.agent.real.web.RealAgentWebApplication`
- VM Options: `-Dspring.profiles.active=dev`
- Environment Variables: 设置 `.env` 中的变量

### 验证启动

启动成功后，访问以下端点验证:

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 应用信息
curl http://localhost:8081/actuator/info

# Chat API 测试
curl -X POST http://localhost:8080/api/agent/react/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请介绍一下自己"}'
```

## 💻 开发工作流

### 代码格式化
```bash
# 开发时跳过格式检查 (提高编译速度)
mvn clean compile -Dspring-javaformat.skip=true

# 格式化代码
mvn spring-javaformat:apply

# 验证格式
mvn spring-javaformat:validate
```

### 单模块开发
```bash
# 只编译 Agent 模块
mvn clean install -pl real-agent-core/real-agent-agent -am

# 只编译应用层
mvn clean install -pl real-agent-application -am

# 只测试特定模块
mvn test -pl real-agent-core/real-agent-tool
```

### 测试驱动开发
```bash
# 运行所有测试
mvn test

# 测试覆盖率
mvn test jacoco:report

# 集成测试
mvn verify
```

### 热重载开发
```bash
# 使用 Spring Boot DevTools
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或者在 IDE 中启用自动重新加载
```

## 🐛 常见问题

### 1. Java 版本问题
```bash
# 错误: 不支持的 Java 版本
# 解决: 升级到 Java 17+
sdk install java 17.0.8-oracle  # 使用 SDKMAN
# 或者设置 JAVA_HOME
export JAVA_HOME=/path/to/java17
```

### 2. Maven 依赖问题
```bash
# 清除本地缓存
mvn dependency:purge-local-repository

# 强制更新快照
mvn clean install -U
```

### 3. 数据库连接问题
```bash
# 检查 MySQL 是否运行
sudo systemctl status mysql  # Linux
brew services list | grep mysql  # macOS

# 测试连接
mysql -h localhost -u root -p
```

### 4. API 密钥问题
```bash
# 验证密钥是否设置
echo $DASHSCOPE_API_KEY

# 测试 API 连接
curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions \
  -H "Authorization: Bearer $DASHSCOPE_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen-max","messages":[{"role":"user","content":"test"}]}'
```

### 5. 端口占用问题
```bash
# 查看端口占用
lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows

# 修改端口
export SERVER_PORT=8090
```

### 6. 内存不足问题
```bash
# 增加 JVM 堆内存
export MAVEN_OPTS="-Xmx2048m -Xms1024m"

# 或在启动时指定
java -Xmx2048m -jar real-agent-web/target/*.jar
```

## 📱 API 使用示例

### Chat API
```bash
# SSE 流式聊天
curl -N -X POST http://localhost:8080/api/agent/react/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "帮我查询北京今天的天气",
    "sessionId": "test-session-123"
  }'
```

### WebSocket (如果启用)
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/chat');
ws.onmessage = (event) => {
  console.log('Received:', JSON.parse(event.data));
};
ws.send(JSON.stringify({
  message: "你好",
  sessionId: "test-session"
}));
```

## 🔧 高级配置

### 多环境配置
- `application.yml`: 通用配置
- `application-dev.yml`: 开发环境
- `application-test.yml`: 测试环境
- `application-prod.yml`: 生产环境

### 性能调优
```yaml
# application.yml
spring:
  codec:
    max-in-memory-size: 20MB
  r2dbc:
    pool:
      initial-size: 10
      max-size: 50
      max-idle-time: 30m
```

### 监控配置
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
```

---

## 📚 相关文档

- [项目架构文档](project.md)
- [开发规范](CLAUDE.md)
- [API 文档](docs/api.md)
- [部署指南](docs/deployment.md)

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 📄 许可证

本项目基于 MIT 许可证开源。