# Real-Agent 配置文件使用指南

## 📋 目录
- [配置文件架构](#配置文件架构)
- [配置文件层次结构](#配置文件层次结构)
- [环境配置](#环境配置)
- [模块化配置](#模块化配置)
- [配置优先级](#配置优先级)
- [常用配置场景](#常用配置场景)
- [配置验证](#配置验证)
- [故障排除](#故障排除)

## 🏗️ 配置文件架构

Real-Agent 采用分层模块化的配置架构，遵循以下设计原则：

### 设计原则
1. **关注点分离** - 按功能模块分离配置
2. **环境隔离** - 开发、测试、生产环境独立配置
3. **安全优先** - 敏感信息通过环境变量管理
4. **可扩展性** - 支持新模块和新环境的配置扩展

### 架构图
```
application.yml (基础配置)
├── config/core/ (核心模块配置)
│   ├── agent-config.yml
│   ├── tool-config.yml
│   └── strategy-config.yml
├── config/infrastructure/ (基础设施配置)
│   ├── database-config.yml
│   ├── ai-model-config.yml
│   ├── mcp-config.yml
│   └── monitoring-config.yml
└── config/environments/ (环境特定配置)
    ├── application-dev.yml
    ├── application-test.yml
    └── application-prod.yml
```

## 📚 配置文件层次结构

### 1. 基础配置 (`application.yml`)
包含所有环境通用的基础配置：
- 应用名称和基本信息
- 服务器端口和路径配置
- 日志基础配置
- 配置文件导入声明

### 2. 核心模块配置 (`config/core/`)

#### `agent-config.yml`
- Agent 实例配置
- ReAct 策略参数
- 工具审批和执行配置
- 上下文管理配置

**主要配置项：**
```yaml
agent:
  action:
    tools:
      approval-mode: AUTO  # AUTO | REQUIRE_APPROVAL | DISABLED
      execution-timeout: 30s
  react:
    max-iterations: 10
  instances:
    thinking:
      enabled: true
      keywords: 思考,分析,推理,planning
```

#### `tool-config.yml`
- 工具注册和发现配置
- 内置工具配置（TaskDone、TimeNow、HTTP 等）
- 工具执行和监控配置
- 工具安全沙箱配置

**主要配置项：**
```yaml
tool:
  registration:
    auto-discovery: true
  system:
    http-request:
      sandbox:
        enabled: true
        allowed-domains: ""
        blocked-domains: "localhost,127.0.0.1"
```

#### `strategy-config.yml`
- 各种 Agent 策略配置
- 策略选择算法配置
- 性能和重试配置
- 实验性策略配置

### 3. 基础设施配置 (`config/infrastructure/`)

#### `database-config.yml`
- R2DBC 响应式数据库配置
- 连接池配置
- 事务和缓存配置
- 数据库监控配置

#### `ai-model-config.yml`
- Spring AI 和 LLM 配置
- 向量数据库配置
- 语音功能配置
- 模型性能和缓存配置

#### `mcp-config.yml`
- MCP 客户端配置
- 外部工具服务器连接
- SSE 和 STDIO 配置
- MCP 安全和监控配置

#### `monitoring-config.yml`
- Spring Boot Actuator 配置
- 健康检查配置
- 监控指标配置
- 分布式追踪配置

### 4. 环境特定配置 (`config/environments/`)

#### `application-dev.yml` (开发环境)
- 调试友好的日志级别
- 本地数据库连接
- 开发工具配置
- 热重载配置

#### `application-test.yml` (测试环境)
- 测试数据配置
- 模拟服务配置
- 快速执行配置
- 性能测试配置

#### `application-prod.yml` (生产环境)
- 安全配置强化
- 性能优化配置
- 监控告警配置
- 备份和审计配置

## 🌍 环境配置

### 环境激活方式

#### 1. 通过环境变量
```bash
export SPRING_PROFILES_ACTIVE=dev
java -jar real-agent-web.jar
```

#### 2. 通过 JVM 参数
```bash
java -Dspring.profiles.active=dev -jar real-agent-web.jar
```

#### 3. 通过 Maven
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 4. 通过 IDE 配置
在 IDE 中设置 VM Options：
```
-Dspring.profiles.active=dev
```

### 多环境配置
支持同时激活多个配置文件：
```bash
export SPRING_PROFILES_ACTIVE=dev,debug,local
```

配置加载顺序：`application.yml` → `application-dev.yml` → `application-debug.yml` → `application-local.yml`

## ⚙️ 模块化配置

### 配置文件导入机制

Spring Boot 2.4+ 的配置导入功能：
```yaml
spring:
  config:
    import:
      - optional:classpath:config/core/agent-config.yml
      - optional:classpath:config/infrastructure/database-config.yml
```

**关键特性：**
- `optional:` 前缀表示文件不存在时不会报错
- 支持相对路径和绝对路径
- 支持多种格式（.yml, .yaml, .properties）

### 条件配置

使用 `@ConditionalOnProperty` 实现条件配置：
```yaml
# 仅在开发环境启用某些功能
dev:
  mock-data:
    enabled: true  # 只在 dev profile 下生效
```

### 配置绑定

通过 `@ConfigurationProperties` 绑定配置类：
```java
@ConfigurationProperties(prefix = "agent")
@Data
public class AgentProperties {
    private Action action = new Action();
    private React react = new React();

    @Data
    public static class Action {
        private Tools tools = new Tools();
    }
}
```

## 🔄 配置优先级

Spring Boot 配置优先级（从高到低）：

1. **命令行参数**
   ```bash
   java -jar app.jar --server.port=9000
   ```

2. **JVM 系统属性**
   ```bash
   java -Dserver.port=9000 -jar app.jar
   ```

3. **操作系统环境变量**
   ```bash
   export SERVER_PORT=9000
   ```

4. **application-{profile}.yml**
   ```yaml
   # application-dev.yml
   server:
     port: 8080
   ```

5. **application.yml**
   ```yaml
   # application.yml
   server:
     port: 8080
   ```

6. **@PropertySource 注解**

7. **默认值**

### 环境变量命名规则
Spring Boot 支持以下环境变量命名：
```bash
# YAML: server.port
SERVER_PORT=8080

# YAML: spring.ai.openai.api-key
SPRING_AI_OPENAI_API_KEY=sk-xxx

# YAML: agent.action.tools.approval-mode
AGENT_ACTION_TOOLS_APPROVAL_MODE=AUTO
```

## 📝 常用配置场景

### 1. 本地开发配置

创建 `application-local.yml`：
```yaml
spring:
  profiles:
    include: dev  # 继承 dev 配置

# 本地特定覆盖
spring:
  r2dbc:
    password: local_password

logging:
  level:
    com.ai.agent: TRACE  # 更详细的本地调试日志
```

使用方式：
```bash
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

### 2. 容器化部署配置

`docker-compose.yml`：
```yaml
version: '3.8'
services:
  real-agent:
    image: real-agent:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - DB_PASSWORD=${DB_PASSWORD}
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
    depends_on:
      - mysql
```

### 3. Kubernetes 配置

ConfigMap：
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: real-agent-config
data:
  application.yml: |
    spring:
      profiles:
        active: k8s
```

Secret：
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: real-agent-secrets
type: Opaque
data:
  db-password: <base64-encoded>
  api-key: <base64-encoded>
```

### 4. 多数据源配置

`application-multi-db.yml`：
```yaml
spring:
  r2dbc:
    primary:
      url: r2dbc:mysql://primary-db:3306/real_agent
      username: primary_user
      password: ${PRIMARY_DB_PASSWORD}

    secondary:
      url: r2dbc:mysql://secondary-db:3306/real_agent_readonly
      username: readonly_user
      password: ${SECONDARY_DB_PASSWORD}
```

### 5. 外部配置中心集成

Spring Cloud Config：
```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
  cloud:
    config:
      name: real-agent
      profile: ${SPRING_PROFILES_ACTIVE:dev}
```

## ✅ 配置验证

### 1. 配置验证注解

```java
@ConfigurationProperties(prefix = "real-agent")
@Validated
public class AgentProperties {

    @NotNull
    @Valid
    private Action action;

    @Data
    public static class Action {
        @NotNull
        @Pattern(regexp = "AUTO|REQUIRE_APPROVAL|DISABLED")
        private String approvalMode;

        @Positive
        @Max(300)
        private Duration executionTimeout;
    }
}
```

### 2. 配置健康检查

创建自定义健康指示器：
```java
@Component
public class ConfigHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // 验证关键配置项
        if (StringUtils.hasText(apiKey)) {
            return Health.up()
                .withDetail("api-key", "configured")
                .build();
        }
        return Health.down()
            .withDetail("api-key", "missing")
            .build();
    }
}
```

### 3. 启动时配置检查

```java
@Component
public class ConfigValidationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 验证必需的配置项
        validateRequiredProperties();
    }
}
```

## 🔧 故障排除

### 1. 配置加载问题

**问题：配置文件未被加载**
```bash
# 检查配置文件路径
java -jar app.jar --debug

# 输出所有配置源
java -jar app.jar --spring.output.ansi.enabled=always
```

**解决方案：**
- 确认文件路径正确
- 检查 `spring.config.import` 配置
- 使用 `--spring.config.location` 指定配置路径

### 2. 环境变量问题

**问题：环境变量未生效**
```bash
# 检查环境变量
printenv | grep SPRING
echo $SPRING_PROFILES_ACTIVE
```

**解决方案：**
- 确认环境变量命名正确（下划线转换）
- 检查变量作用域（系统环境 vs Shell 环境）
- 使用 `spring.config.name` 自定义配置文件名

### 3. 配置优先级问题

**问题：配置被意外覆盖**

使用 Spring Boot Actuator 查看配置源：
```bash
curl http://localhost:8081/actuator/configprops
curl http://localhost:8081/actuator/env
```

**解决方案：**
- 理解配置优先级顺序
- 使用 `--spring.config.on-not-found=fail` 强制检查
- 检查是否有多个配置文件定义了相同属性

### 4. 配置格式问题

**问题：YAML 格式错误**
```bash
# 使用在线 YAML 验证器
# 或使用命令行工具
python -c "import yaml; yaml.safe_load(open('application.yml'))"
```

**解决方案：**
- 检查缩进（使用空格，不用 Tab）
- 检查特殊字符转义
- 使用引号包围包含特殊字符的值

### 5. 配置刷新问题

**问题：配置更改后未生效**

使用 Spring Cloud 的配置刷新：
```bash
# 触发配置刷新
curl -X POST http://localhost:8081/actuator/refresh
```

**解决方案：**
- 重启应用
- 使用 `@RefreshScope` 注解
- 配置热重载机制

### 6. 常见错误信息

| 错误信息 | 原因 | 解决方案 |
|---------|------|----------|
| `Could not resolve placeholder` | 环境变量未设置 | 设置环境变量或提供默认值 |
| `Configuration property name is not valid` | 属性名格式错误 | 检查属性名命名规范 |
| `Failed to bind properties` | 类型转换失败 | 检查数据类型匹配 |
| `Circular placeholder reference` | 配置循环引用 | 检查配置引用关系 |

## 📖 配置文件示例

### 开发环境快速配置
```yaml
# .env 文件
SPRING_PROFILES_ACTIVE=dev
DASHSCOPE_API_KEY=sk-your-dev-api-key
DB_PASSWORD=dev_password
LOG_LEVEL_AGENT=DEBUG
```

### 生产环境安全配置
```yaml
# Kubernetes Secret
apiVersion: v1
kind: Secret
metadata:
  name: real-agent-prod-secrets
type: Opaque
stringData:
  SPRING_PROFILES_ACTIVE: prod
  DASHSCOPE_API_KEY: sk-your-prod-api-key
  DB_PASSWORD: your-secure-password
  REDIS_PASSWORD: your-redis-password
```

### Docker Compose 全栈配置
```yaml
version: '3.8'
services:
  real-agent:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=mysql
      - REDIS_HOST=redis
      - MCP_BAIDU_MAPS_ENABLED=true
    depends_on:
      - mysql
      - redis
```

---

通过这套模块化配置架构，Real-Agent 项目实现了清晰的配置管理，支持多环境部署，确保了开发、测试和生产环境的配置隔离与安全性。