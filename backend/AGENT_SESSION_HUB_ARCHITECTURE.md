# AgentSessionHub 架构升级文档

## 概述

本次架构升级将 `AgentChatController` 从直接返回 `Flux` 的模式升级为使用 `AgentSessionHub` 管理会话的模式，支持工具审批中断和恢复执行。

## 核心设计理念

### 1. Sink 粒度：单轮对话
- **Sink 创建时机**：用户发送消息时（POST `/api/agent/chat/react/stream`）
- **Sink 关闭时机**：任务完成 或 遇到需要审批的情况时暂停
- **生命周期**：一个 Sink 对应一个完整的任务执行周期

### 2. 多轮对话
- 用户发送新问题 = 新的 POST 请求到 `/react/stream`
- 每次请求创建新的 Sink 和执行流程

### 3. 工具审批
- 通过独立接口 `/react/tool_approval` 处理审批
- 审批通过后，从暂停点恢复执行
- 审批拒绝后，推送拒绝消息并继续下一轮迭代

## 架构组件

### 1. AgentSessionHub（会话管理中心）

**职责**：
- 管理每个会话的 Sink、上下文和恢复点
- 处理工具审批请求
- 协调 ReActAgentStrategy 的执行和恢复

**核心方法**：
```java
// 订阅会话的SSE流
Flux<ServerSentEvent<AgentExecutionEvent>> subscribe(String sessionId, String message, AgentContext context)

// 暂停执行，等待工具审批
void pauseForToolApproval(String sessionId, String toolCallId, String toolName, Map<String, Object> toolArgs, AgentContext context)

// 处理工具审批
void approveToolExecution(String sessionId, String toolCallId, boolean approved, String reason)

// 关闭会话
void closeSession(String sessionId)
```

**SessionState 结构**：
```java
class SessionState {
    String sessionId;                                          // 会话ID
    Sinks.Many<ServerSentEvent<AgentExecutionEvent>> sink;    // SSE推送Sink
    AgentContext context;                                      // 执行上下文
    Disposable currentExecution;                               // 当前执行的订阅句柄
    ResumePoint resumePoint;                                   // 恢复点
    boolean closed;                                            // 是否已关闭
    LocalDateTime createdAt;                                   // 创建时间
}
```

### 2. ResumePoint（恢复点模型）

**职责**：
- 保存暂停时的执行状态
- 支持从特定阶段恢复执行

**核心字段**：
```java
class ResumePoint {
    String resumeId;                    // 恢复点ID（通常是toolCallId）
    String sessionId;                   // 会话ID
    int currentIteration;               // 当前迭代次数
    ReActStage pausedStage;            // 暂停的阶段（THINKING/ACTION/OBSERVATION）
    String toolCallId;                  // 待执行的工具调用ID
    String toolName;                    // 工具名称
    Map<String, Object> toolArgs;       // 工具参数
    AgentContext context;               // 执行上下文
    String originalTask;                // 原始任务描述
    ApprovalResult approvalResult;      // 审批结果
}
```

### 3. ReActAgentStrategy.resumeFromToolApproval

**职责**：
- 从工具审批后恢复执行
- 根据审批结果决定执行路径

**执行流程**：
```
审批通过：
  1. 执行工具
  2. 将工具结果添加到上下文
  3. 执行观察阶段
  4. 继续剩余迭代
  5. 最终总结

审批拒绝：
  1. 推送拒绝消息
  2. 继续下一轮迭代（让LLM重新思考）
  3. 最终总结
```

### 4. ToolApprovalCallback（工具审批回调）

**职责**：
- 解耦 FluxUtils 和 AgentSessionHub
- 在工具执行需要审批时通知上层

**实现方式**：
```java
@FunctionalInterface
public interface ToolApprovalCallback {
    void requestApproval(String sessionId, String toolCallId, String toolName, 
                        Map<String, Object> toolArgs, AgentContext context);
}
```

**传递方式**：
- 通过 `AgentContext.toolApprovalCallback` 字段传递
- FluxUtils 从上下文中获取回调并调用

## 执行流程

### 正常执行流程

```
用户发送消息
  ↓
POST /api/agent/chat/react/stream
  ↓
AgentChatController 创建 AgentContext
  ↓
设置 ToolApprovalCallback 到上下文
  ↓
AgentSessionHub.subscribe()
  ↓
创建 SessionState 和 Sink
  ↓
ReActAgentStrategy.executeStream()
  ↓
Thinking → Action → Observation → 循环
  ↓
推送事件到 Sink → SSE 到前端
  ↓
任务完成 → 关闭 Sink
```

### 工具审批流程

```
Action 阶段遇到需要审批的工具
  ↓
FluxUtils.executeToolCall() 检测到 REQUIRE_APPROVAL
  ↓
调用 ToolApprovalCallback.requestApproval()
  ↓
AgentSessionHub.pauseForToolApproval()
  ↓
创建 ResumePoint 并保存到 SessionState
  ↓
推送 TOOL_APPROVAL 事件到前端
  ↓
返回 Flux.empty()（暂停当前执行）
  ↓
【等待用户审批】
  ↓
用户审批 → POST /api/agent/chat/react/tool_approval
  ↓
AgentSessionHub.approveToolExecution()
  ↓
设置审批结果到 ResumePoint
  ↓
调用 ReActAgentStrategy.resumeFromToolApproval()
  ↓
执行工具 → 观察阶段 → 继续迭代
  ↓
推送事件到原 Sink → SSE 到前端
  ↓
任务完成 → 关闭 Sink
```

## API 接口

### 1. 执行 ReAct 任务（流式）

**接口**：`POST /api/agent/chat/react/stream`

**请求体**：
```json
{
  "message": "用户消息",
  "sessionId": "session-123",
  "userId": "user-456",
  "agentType": "ReActAgentStrategy"
}
```

**响应**：SSE 流
```
event: PROGRESS
data: {"type":"PROGRESS","message":"ReAct任务开始执行",...}

event: THINKING
data: {"type":"THINKING","message":"正在思考...",...}

event: ACTION
data: {"type":"ACTION","message":"执行行动...",...}

event: TOOL_APPROVAL
data: {"type":"TOOL_APPROVAL","message":"get_weather","data":{"toolCallId":"call_123","toolName":"get_weather","toolArgs":{...}}}

event: COMPLETED
data: {"type":"COMPLETED","message":null,...}
```

### 2. 工具审批

**接口**：`POST /api/agent/chat/react/tool_approval`

**请求体**：
```json
{
  "sessionId": "session-123",
  "toolCallId": "call_123",
  "approved": true,
  "reason": "用户拒绝原因（如果拒绝）"
}
```

**响应**：
```json
{
  "success": true,
  "message": "工具审批通过，正在恢复执行",
  "sessionId": "session-123",
  "timestamp": "2025-10-22T16:00:00"
}
```

## 关键设计决策

### 1. 为什么使用回调而不是直接依赖？

**问题**：FluxUtils 在 contract 模块，AgentSessionHub 在 web 模块，不能反向依赖。

**解决方案**：使用 `ToolApprovalCallback` 接口 + `AgentContext` 传递回调。

**优点**：
- 保持模块依赖关系清晰
- FluxUtils 不需要知道 AgentSessionHub 的存在
- 支持不同的审批处理策略

### 2. 为什么选择方案1（重新执行Agent）？

**方案1**：审批后重新执行当前 Agent
**方案2**：保存执行状态，精确恢复

**选择方案1的原因**：
- 实现相对简单
- 符合 ReAct 的设计理念
- LLM 可以根据审批结果做出更智能的决策
- Token 消耗在可接受范围内（只重新执行当前 Agent，不是整个 ReAct 循环）

### 3. 为什么 Sink 不在审批时关闭？

**原因**：
- 需要在审批通过后继续推送事件到同一个 SSE 连接
- 前端保持连接，可以实时接收恢复执行后的事件
- 避免前端需要重新建立 SSE 连接的复杂性

## 配置变更

### AgentBeanConfig.java

```java
@Bean
public ReActAgentStrategy reactAgentStrategy(ThinkingAgent thinkingAgent, ActionAgent actionAgent,
        ObservationAgent observationAgent, FinalAgent finalAgent, ToolService toolService) {
    return new ReActAgentStrategy(thinkingAgent, actionAgent, observationAgent, finalAgent, toolService);
}
```

**变更说明**：增加 `ToolService` 参数，用于 Resume 时执行工具。

## 测试建议

### 1. 正常流程测试
- 发送消息，验证 SSE 流正常推送
- 验证任务完成后 Sink 正常关闭

### 2. 工具审批测试
- 配置 `ToolApprovalMode.REQUIRE_APPROVAL`
- 发送需要工具调用的消息
- 验证收到 `TOOL_APPROVAL` 事件
- 调用审批接口（批准）
- 验证恢复执行并完成任务

### 3. 工具拒绝测试
- 发送需要工具调用的消息
- 调用审批接口（拒绝）
- 验证推送拒绝消息
- 验证继续下一轮迭代

### 4. 并发测试
- 多个会话同时执行
- 验证会话隔离
- 验证 Sink 不会混淆

## 注意事项

### 1. 上下文深拷贝
当前 `ResumePoint` 中的 `AgentContext` 是直接引用，在生产环境中应该实现深拷贝，避免状态污染。

### 2. 会话超时
建议实现会话超时机制，自动清理长时间未活动的会话。

### 3. 错误处理
确保所有异常都被正确捕获并转换为错误事件，避免 SSE 连接异常断开。

### 4. 日志记录
关键节点都有详细日志，便于问题排查。

## 总结

本次架构升级实现了：
✅ Sink 管理和生命周期控制
✅ 工具审批中断和恢复机制
✅ 清晰的模块职责划分
✅ 可扩展的回调机制
✅ 完整的 API 接口

这个架构为后续功能扩展（如多轮对话历史、会话持久化、更复杂的审批策略等）奠定了良好的基础。
