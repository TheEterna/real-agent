# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 在此代码库中工作时提供指导。

## 项目概述

Real-Agent 是一个基于 Spring AI 和 MCP（Model Context Protocol）的多 Agent 协作框架，核心实现了 ReAct（Reasoning and Acting）策略，支持工具调用、流式响应和响应式编程。

**技术栈**：
- Java 17 + Spring Boot 3.4.4
- Spring AI 1.1.0-SNAPSHOT（集成 LLM 和 Function Calling）
- MCP 0.13.1（工具协议）
- Project Reactor（响应式编程）
- R2DBC（响应式数据库访问）
- Maven 多模块项目

## 常用命令

### 构建与测试

```bash
# 编译整个项目
mvn clean compile

# 跳过格式检查编译（开发时使用）
mvn clean compile -Dspring-javaformat.skip=true

# 运行测试
mvn test

# 测试覆盖率报告
mvn test jacoco:report

# 打包（生成可执行 jar）
mvn clean package -DskipTests

# 代码格式检查
mvn spring-javaformat:validate

# 自动格式化代码
mvn spring-javaformat:apply
```

### 运行应用

```bash
# 运行主应用（需要在 real-agent-web 模块）
cd real-agent-web
mvn spring-boot:run

# 指定配置文件运行
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 单模块开发

```bash
# 只编译特定模块（如 core）
mvn clean install -pl real-agent-core -am

# 只测试特定模块
mvn test -pl real-agent-application
```

## 架构设计

### 模块职责

项目采用 DDD 分层架构，模块依赖关系如下：

```
real-agent-web (接入层)
    ↓ 依赖
real-agent-application (应用层) ← 
    ↓ 依赖                              ↓
real-agent-domain (领域层)       tool (工具系统)
    ↓ 依赖                       agent (Agent系统)
real-agent-contract (契约层)     ... （其他服务）
    ↓
real-agent-common (通用层)
```

**核心模块说明**：

- **real-agent-core/real-agent-agent**：Agent 抽象和策略实现
  - `Agent.java`：所有 Agent 的基类，定义 `executeStream()` 方法
  - `strategy/`：各种 Agent 策略（ReAct、单Agent、协作、竞争等）
  - `impl/`：具体 Agent 实现（ThinkingAgent、ActionAgent、ObservationAgent、FinalAgent）

- **real-agent-core/real-agent-tool**：工具系统和 MCP 集成
  - `ToolService`：工具注册、查询、执行的核心服务
  - `mcp/`：MCP 协议客户端配置
  - `system/impl/`：内置工具（TaskDoneTool、TimeNowTool、HttpRequestTool）

- **real-agent-application**：业务服务层
  - `service/`：Roleplay 会话、角色、消息的管理服务
  - 使用响应式 API（返回 `Mono<T>` 或 `Flux<T>`）

- **real-agent-domain**：数据访问层
  - `entity/`：实体类（User、PlaygroundRoleplayRole、Session、Message）
  - `repository/`：R2DBC Repository 接口

- **real-agent-web**：Web 控制器和配置
  - `controller/AgentChatController`：核心聊天接口，支持 SSE 流式响应
  - `config/AgentBeanConfig`：Agent 和策略的 Bean 配置

### 核心流程：ReAct 策略

ReAct 是项目的核心执行策略，流程如下：

```
用户请求 → [思考-行动-观察] 循环（最多10轮） → 最终总结
```

**关键类**：
- `real-agent-core/real-agent-agent/src/main/java/com/ai/agent/real/agent/strategy/ReActAgentStrategy.java`：策略编排器
- `ThinkingAgent`：分析任务，决定使用什么工具
- `ActionAgent`：执行工具调用（Function Calling）
- `ObservationAgent`：分析工具执行结果
- `FinalAgent`：整合所有结果并生成最终回复

**循环终止条件**：
1. ActionAgent 调用 `task_done` 工具（推荐方式）
2. 达到最大迭代次数（10轮）

**上下文管理**：
- `AgentContext`：保存对话历史、追踪信息、工具参数、任务状态
- 每个 Agent 阶段使用独立的上下文副本（通过 `AgentUtils.createAgentContext()`）
- 通过 `FluxUtils.handleContext()` 将结果合并回主上下文

### 工具系统架构

**核心抽象**：
- `AgentTool`：工具接口，定义 `execute()` 和 `executeAsync()` 方法
- `ToolSpec`：工具元数据（name、description、inputSchema、category）
- `ToolService`：工具管理服务，负责注册、查询、执行

**工具注册机制**：
```java
// 基于关键词注册
toolService.registerToolWithKeywords(tool, Set.of("时间", "日期", "当前"));

// Agent 构造时自动绑定工具
public Agent(String agentId, String name, String description,
             ChatModel chatModel, ToolService toolService, Set<String> keywords) {
    this.availableTools = toolService.getToolsByKeywords(keywords);
}
```

**MCP 集成**：
- MCP 工具通过 SSE 连接到外部服务器（配置在 `application.yml`）
- `ToolService.listAllAgentToolsRefreshAsync()` 动态获取 MCP 工具
- `ToolUtils.convertToolCallback2AgentTool()` 将 MCP 工具转换为 `AgentTool`

**内置工具**：
- `TaskDoneTool`：标记任务完成（设置 `context.setTaskCompleted(true)`）
- `TimeNowTool`：获取当前时间
- `HttpRequestTool`：HTTP 请求（带沙箱策略）
- `MathEvalTool`：数学表达式计算

### 响应式流处理

项目全面采用 Project Reactor，关键工具类：

**FluxUtils** (`real-agent-common/src/main/java/com/ai/agent/real/common/utils/FluxUtils.java`)：

1. **executeWithToolSupport**：流式执行包装器
   - 调用 `chatModel.stream(prompt)` 获取 LLM 流式响应
   - 检测 Function Calling 并自动执行工具
   - 将响应转换为 `AgentExecutionEvent` 事件流

2. **handleContext**：上下文处理器
   - 收集 AI 回复内容和工具调用
   - 在流结束时按正确顺序更新对话历史：
     1. 先添加 `AssistantMessage`（包含 `tool_calls` 元数据）
     2. 再添加 `ToolResponseMessage`（工具执行结果）
   - 这个顺序对 Spring AI 的 Function Calling 至关重要

3. **stage**：阶段封装
   - 统一封装各个 Agent 阶段的流处理
   - 应用上下文合并和日志回调

**AgentUtils** (`real-agent-common/src/main/java/com/ai/agent/real/common/utils/AgentUtils.java`)：

1. **buildPromptWithContextAndTools**：构建带工具的 Prompt
   - 渲染工具列表到系统提示词（替换 `<TOOLS>` 标签）
   - 转换对话历史为 Spring AI 消息格式
   - 配置工具调用选项

2. **toSpringAiMessages**：消息转换
   - `AgentMessage.TOOL` → `ToolResponseMessage`
   - `AgentMessage.ASSISTANT` → `AssistantMessage`（包含 `tool_calls`）
   - 确保消息格式符合 OpenAI API 规范

3. **createAgentContext**：创建上下文副本
   - 为每个 Agent 阶段创建独立的追踪信息
   - 共享对话历史和工具参数（浅拷贝）

### 事件驱动模型

**AgentExecutionEvent** (`real-agent-common/src/main/java/com/ai/agent/real/common/protocol/AgentExecutionEvent.java`)：

事件类型：
- `STARTED`：开始执行
- `THINKING`：思考阶段的流式内容
- `ACTION` / `ACTING`：行动阶段
- `TOOL`：工具调用结果
- `OBSERVING`：观察阶段的流式内容
- `DONE`：任务完成
- `DONEWITHWARNING`：完成但超过最大迭代次数
- `ERROR`：执行错误
- `COMPLETED`：所有流程完成

工厂方法：
```java
AgentExecutionEvent.thinking(context, "思考内容")
AgentExecutionEvent.tool(context, toolResponse, message, meta)
AgentExecutionEvent.done(context, "完成消息")
```

### Web 层 SSE 流式响应

**AgentChatController** (`real-agent-web/src/main/java/com/ai/agent/real/web/controller/AgentChatController.java`)：

```java
@PostMapping(value = "/react/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<AgentExecutionEvent> executeReActStream(@RequestBody ChatRequest request) {
    AgentContext context = createContext(request);
    return reactStrategy.executeStream(request.getMessage(), agents, context)
        .onErrorResume(error -> Flux.just(AgentExecutionEvent.error(error.getMessage())));
}
```

前端通过 EventSource 接收事件流，实现实时交互。

## 重要设计决策

### 1. 为什么分离 ThinkingAgent 和 ActionAgent？

- **职责单一**：Thinking 专注决策，Action 专注执行
- **提示词优化**：每个 Agent 有针对性的超长系统提示词
- **可测试性**：独立测试思考逻辑和执行逻辑
- **灵活组合**：可以替换或增强某个阶段

### 2. 为什么使用响应式编程？

- **非阻塞 IO**：提升并发处理能力
- **背压机制**：自动处理生产者-消费者速度不匹配
- **SSE 原生支持**：Spring WebFlux 天然支持流式响应
- **组合性**：丰富的操作符支持复杂流处理

### 3. 上下文合并的顺序为什么重要？

Spring AI 的 Function Calling 要求特定的消息顺序：
1. `AssistantMessage`（必须包含 `tool_calls` 元数据，即使内容为空）
2. `ToolResponseMessage`（工具执行结果）

违反这个顺序会导致 LLM 无法正确理解工具调用上下文。

### 4. 为什么使用 task_done 工具而不是文本匹配？

- **可靠性**：避免文本匹配的歧义（如"任务完成"、"done"等）
- **结构化**：通过 Function Calling 机制传递完成信号
- **可扩展**：可以传递额外的完成信息（finishContent）

## 配置要点

### application.yml 关键配置

```yaml
# LLM 配置（通义千问示例）
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        options:
          model: qwen-max

# MCP 工具服务器
spring:
  ai:
    mcp:
      client:
        enabled: true
        type: ASYNC
        request-timeout: 100s
        sse:
          connections:
            baidu-maps:
              url: https://mcp.map.baidu.com?ak=xxx
              sse-endpoint: /sse

# R2DBC 响应式数据库
spring:
  r2dbc:
    url: r2dbc:mysql://host:port/db
    username: root
    password: xxx

# 工具审批模式
real-agent:
  action:
    tools:
      approval-mode: AUTO  # AUTO/REQUIRE_APPROVAL/DISABLED
```

### Bean 配置

**AgentBeanConfig** (`real-agent-web/src/main/java/com/ai/agent/real/web/config/AgentBeanConfig.java`)：

所有 Agent 和策略都注册为 Spring Bean：
```java
@Bean ThinkingAgent thinkingAgent(...)
@Bean ActionAgent actionAgent(...)
@Bean ObservationAgent observationAgent(...)
@Bean FinalAgent finalAgent(...)
@Bean ReActAgentStrategy reactAgentStrategy(...)
```



## 开发规范

### 代码风格

项目使用 `spring-javaformat-maven-plugin` 强制代码格式：
- 编译前自动检查格式（`validate` 阶段）
- 使用 `mvn spring-javaformat:apply` 自动格式化
- 开发时可以用 `-Dspring-javaformat.skip=true` 跳过检查

### 响应式编程规范

1. **返回类型**：
   - 单个结果用 `Mono<T>`
   - 多个结果或流用 `Flux<T>`
   - 避免阻塞调用（不要用 `.block()`）

2. **错误处理**：
   - 使用 `.onErrorResume()` 处理错误
   - 转换为业务异常或事件
   - 避免让流中断

3. **组合操作**：
   - 顺序执行：`.concatMap()`
   - 并行执行：`.flatMap()`
   - 延迟执行：`.defer()`
   - 条件终止：`.takeUntil()`

### Agent 开发规范

新增 Agent 的步骤：

1. **继承 Agent 基类**：
```java
public class CustomAgent extends Agent {
    public CustomAgent(ChatModel chatModel, ToolService toolService) {
        super("custom-id", "Custom Agent", "描述", chatModel, toolService,
              Set.of("关键词1", "关键词2"));
    }
}
```

2. **实现 executeStream 方法**：
```java
@Override
public Flux<AgentExecutionEvent> executeStream(String task, AgentContext context) {
    Prompt prompt = AgentUtils.buildPromptWithContextAndTools(
        availableTools, context, SYSTEM_PROMPT, task
    );
    return FluxUtils.executeWithToolSupport(
        chatModel, prompt, context, agentId,
        toolService, toolApprovalMode, EventType.THINKING
    );
}
```

3. **定义系统提示词**：
```java
private static final String SYSTEM_PROMPT = """
    你是一个专业的 XXX Agent。
    你的职责是 XXX。

    <TOOLS>  # 工具列表占位符，会自动替换
    """;
```

4. **注册为 Spring Bean**：
```java
@Bean
public CustomAgent customAgent(ChatModel chatModel, ToolService toolService) {
    return new CustomAgent(chatModel, toolService);
}
```

### 工具开发规范

新增工具的步骤：

1. **实现 AgentTool 接口**：
```java
public class CustomTool implements AgentTool {
    @Override
    public String getId() { return "custom-tool"; }

    @Override
    public ToolSpec getSpec() {
        return ToolSpec.builder()
            .name("custom_tool")
            .description("工具描述")
            .category(ToolCategory.UTILITY)
            .inputSchemaClass(CustomToolInput.class)
            .build();
    }

    @Override
    public Mono<ToolResult<Object>> executeAsync(AgentContext ctx) {
        CustomToolInput input = ctx.getStructuralToolArgs(CustomToolInput.class);
        // 执行逻辑
        return Mono.just(ToolResult.success("结果"));
    }
}
```

2. **定义输入参数类**：
```java
@JsonClassDescription("工具参数描述")
public record CustomToolInput(
    @JsonProperty(required = true)
    @JsonPropertyDescription("参数描述")
    String param1
) {}
```

3. **注册工具**：
```java
@Bean
public CustomTool customTool(ToolService toolService) {
    CustomTool tool = new CustomTool();
    toolService.registerToolWithKeywords(tool, Set.of("关键词1", "关键词2"));
    return tool;
}
```

## 调试技巧

### 查看对话历史

在任何 Agent 中可以打印当前上下文：
```java
log.info("Conversation history: {}",
    context.getConversationHistory().stream()
        .map(msg -> msg.getRole() + ": " + msg.getContent())
        .collect(Collectors.joining("\n"))
);
```

### 追踪执行流程

每个事件都包含追踪信息：
```java
AgentExecutionEvent event = ...;
log.info("Trace: sessionId={}, turnId={}, spanId={}, messageId={}, agentId={}",
    event.getSessionId(), event.getTraceId(), event.getSpanId(),
    event.getMessageId(), event.getAgentId()
);
```

### 查看工具调用

在 `FluxUtils.executeWithToolSupport()` 中有详细的工具调用日志：
```
Executing tool: weather_api with args: {"location": "Beijing"}
Tool result: {"temperature": 25, "condition": "Sunny"}
```

### 前端 SSE 调试

使用浏览器或 curl 测试 SSE：
```bash
curl -N -H "Content-Type: application/json" \
  -d '{"message":"今天北京天气怎么样？"}' \
  http://localhost:8080/api/agent/react/stream
```

## Function Calling 注意事项

### Spring AI 的工具调用机制

1. **工具定义**：通过 `inputSchemaClass` 自动生成 JSON Schema
2. **工具绑定**：在 Prompt 中设置 `options.functionCallbacks`
3. **自动执行**：Spring AI 默认自动执行工具并发送结果（2-4 步骤封装）
4. **原生模式**：使用 `internalToolExecutionEnabled(false)` 禁用自动执行

### 项目的处理方式

项目**禁用了 Spring AI 的自动工具执行**，改为手动控制：
- 通过 `FluxUtils.executeWithToolSupport()` 检测工具调用
- 手动调用 `ToolService.executeToolAsync()` 执行工具
- 手动构建 `ToolResponseMessage` 并添加到历史
- **优势**：更灵活的流程控制、支持工具审批、可以插入自定义逻辑

### 消息格式要求

正确的消息序列：
```java
// 1. 用户消息
UserMessage("今天天气怎么样？")

// 2. AI 回复（带 tool_calls）
AssistantMessage("", metadata={tool_calls=[{name: "weather", args: "{}"}]})

// 3. 工具结果
ToolResponseMessage([ToolResponse(id, name, responseData)])

// 4. AI 最终回复
AssistantMessage("今天天气晴朗...")
```

**常见错误**：
- 忘记添加 `tool_calls` 元数据
- `ToolResponseMessage` 在 `AssistantMessage` 之前
- 工具结果的 `id` 与 `tool_calls` 中的 `id` 不匹配

## 常见问题

### 1. 为什么有些策略被注释了？

项目重点实现了 ReAct 策略，其他策略（Collaborative、Competitive、Pipeline）已实现但暂未启用。如需使用，取消注释并注册为 Bean 即可。

### 2. 如何切换不同的 LLM？

修改 `application.yml` 中的 `spring.ai.openai` 配置：
```yaml
spring:
  ai:
    openai:
      base-url: https://api.openai.com/v1  # OpenAI
      # 或
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1  # 通义千问
```

### 3. 如何限制最大迭代次数？

修改 `ReActAgentStrategy.MAX_ITERATIONS` 常量（默认 10）。

### 4. 如何添加自定义提示词？

在 Agent 的 `SYSTEM_PROMPT` 中修改，注意保留 `<TOOLS>` 占位符。

### 5. R2DBC 连接问题

确保 MySQL 开启了 `allowPublicKeyRetrieval=true` 和 SSL 配置：
```yaml
spring:
  r2dbc:
    url: r2dbc:mysql://host:port/db?allowPublicKeyRetrieval=true&useSSL=false
```

### 6. MCP 工具连接失败

检查 MCP 服务器的 URL 和端点是否正确，超时时间是否足够（默认 100s）。

## 参考资源

- **Spring AI 文档**：https://docs.spring.io/spring-ai/reference/
- **MCP 协议规范**：https://modelcontextprotocol.io/
- **Project Reactor 文档**：https://projectreactor.io/docs/core/release/reference/
- **R2DBC 文档**：https://r2dbc.io/

## 项目文档

- `README.md`：项目概述和快速开始
- `project.md`：详细的架构设计文档（包含 DDD 分层、数据库设计、API 规范）
- `TODO.md`：待办事项

---

**最后更新**：2025-10-12
