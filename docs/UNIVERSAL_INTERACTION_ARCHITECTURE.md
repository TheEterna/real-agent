# 通用交互中断-恢复架构文档

## 概述

本架构实现了一个**通用的、可扩展的用户交互中断-恢复机制**，支持工具审批、缺少信息、用户确认等多种交互场景，并提供灵活的恢复策略。

## 核心设计理念

### 1. 通用性
- 将所有类型的用户交互抽象为统一的 `InteractionRequest` 模型
- 支持任意类型的交互场景，无需修改核心代码

### 2. 可扩展性
- 通过 `InteractionType` 枚举扩展新的交互类型
- 通过 `InteractionAction` 枚举扩展新的恢复动作
- 通过 `InteractionOption` 灵活配置选项

### 3. 多种恢复策略
- **同意并执行**：直接执行原计划
- **重新执行**：让 Agent 重新思考
- **拒绝并说明理由**：拒绝并提供反馈
- **终止对话**：直接结束任务
- **提供信息**：补充缺失信息后继续
- **跳过**：跳过当前操作

## 架构组件

### 1. 交互模型（contract 模块）

#### InteractionType（交互类型）
```java
public enum InteractionType {
    TOOL_APPROVAL,      // 工具审批
    MISSING_INFO,       // 缺少信息
    USER_CONFIRMATION,  // 用户确认
    USER_CHOICE,        // 用户选择
    USER_INPUT,         // 用户输入
    CUSTOM              // 自定义
}
```

#### InteractionAction（交互动作）
```java
public enum InteractionAction {
    APPROVE_AND_EXECUTE,    // 同意并执行
    RETRY_WITH_FEEDBACK,    // 重新执行
    REJECT_WITH_REASON,     // 拒绝并说明理由
    TERMINATE,              // 终止对话
    PROVIDE_INFO,           // 提供信息
    SKIP,                   // 跳过
    CUSTOM                  // 自定义
}
```

#### InteractionOption（交互选项）
```java
public class InteractionOption {
    String optionId;                // 选项ID
    String label;                   // 显示文本
    String description;             // 详细描述
    InteractionAction action;       // 执行动作
    boolean isDefault;              // 是否为默认选项
    boolean isDangerous;            // 是否为危险操作
    boolean requiresInput;          // 是否需要用户输入
    String inputPrompt;             // 输入提示
    Map<String, Object> metadata;   // 元数据
}
```

#### InteractionRequest（交互请求）
```java
public class InteractionRequest {
    String requestId;                       // 请求ID
    String sessionId;                       // 会话ID
    InteractionType type;                   // 交互类型
    String title;                           // 标题
    String message;                         // 提示消息
    String details;                         // 详细描述
    Map<String, Object> context;            // 上下文数据
    List<InteractionOption> options;        // 可选项列表
    boolean required;                       // 是否必须响应
    Integer timeoutSeconds;                 // 超时时间
    String defaultOptionId;                 // 默认选项ID
}
```

#### InteractionResponse（交互响应）
```java
public class InteractionResponse {
    String requestId;                       // 对应的请求ID
    String sessionId;                       // 会话ID
    String selectedOptionId;                // 选中的选项ID
    Map<String, Object> data;               // 用户提供的数据
    String feedback;                        // 用户反馈
}
```

### 2. ResumePoint（恢复点）

```java
public class ResumePoint {
    String resumeId;                        // 恢复点ID
    String sessionId;                       // 会话ID
    int currentIteration;                   // 当前迭代次数
    ReActStage pausedStage;                // 暂停的阶段
    InteractionRequest interactionRequest;  // 交互请求（通用）
    InteractionResponse userResponse;       // 用户响应
    AgentContext context;                   // 执行上下文
    String originalTask;                    // 原始任务描述
}
```

### 3. AgentSessionHub（会话管理中心）

#### 核心方法

**请求用户交互（通用方法）**
```java
public void requestInteraction(String sessionId, InteractionRequest request)
```

**处理用户响应（通用方法）**
```java
public void handleInteractionResponse(String sessionId, InteractionResponse response)
```

**便捷方法：工具审批**
```java
public void pauseForToolApproval(String sessionId, String toolCallId, 
                                  String toolName, Map<String, Object> toolArgs, 
                                  AgentContext context)
```

#### 恢复策略实现

- `resumeWithExecution()` - 同意并执行
- `resumeWithRetry()` - 重新执行
- `resumeWithRejection()` - 拒绝并说明理由
- `terminateSession()` - 终止对话
- `resumeWithProvidedInfo()` - 提供信息
- `resumeWithSkip()` - 跳过

### 4. ReActAgentStrategy（执行策略）

#### 核心方法

**从交互请求后恢复执行**
```java
public Flux<AgentExecutionEvent> resumeFromToolApproval(ResumePoint resumePoint, 
                                                         ToolApprovalCallback approvalCallback)
```

**内部方法**
- `resumeFromToolApprovalInternal()` - 工具审批恢复
- `continueNextIteration()` - 继续下一轮迭代

### 5. AgentChatController（控制器）

#### API 接口

**执行 ReAct 任务**
```
POST /api/agent/chat/react/stream
Content-Type: application/json

{
  "message": "用户消息",
  "sessionId": "session-123",
  "userId": "user-456"
}

Response: SSE Stream
```

**处理用户交互响应**
```
POST /api/agent/chat/react/interaction_response
Content-Type: application/json

{
  "sessionId": "session-123",
  "requestId": "request-456",
  "selectedOptionId": "approve",
  "feedback": "用户反馈（可选）",
  "data": {
    "key": "value"
  }
}

Response: JSON
{
  "success": true,
  "message": "用户响应已处理，正在恢复执行",
  "sessionId": "session-123",
  "timestamp": "2025-10-22T17:00:00"
}
```

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

### 工具审批流程（四个选项）

```
Action 阶段遇到需要审批的工具
  ↓
FluxUtils.executeToolCall() 检测到 REQUIRE_APPROVAL
  ↓
调用 ToolApprovalCallback.requestApproval()
  ↓
AgentSessionHub.pauseForToolApproval()
  ↓
构建 InteractionRequest（4个选项）
  ├─ 同意执行
  ├─ 重新执行
  ├─ 拒绝并说明理由
  └─ 拒绝并终止对话
  ↓
创建 ResumePoint 并保存到 SessionState
  ↓
推送 INTERACTION 事件到前端
  ↓
返回 Flux.empty()（暂停当前执行）
  ↓
【等待用户响应】
  ↓
用户选择 → POST /api/agent/chat/react/interaction_response
  ↓
AgentSessionHub.handleInteractionResponse()
  ↓
根据 selectedOptionId 获取 InteractionOption
  ↓
根据 action 类型分发到不同的恢复策略
  ├─ APPROVE_AND_EXECUTE → resumeWithExecution()
  ├─ RETRY_WITH_FEEDBACK → resumeWithRetry()
  ├─ REJECT_WITH_REASON → resumeWithRejection()
  ├─ TERMINATE → terminateSession()
  ├─ PROVIDE_INFO → resumeWithProvidedInfo()
  └─ SKIP → resumeWithSkip()
  ↓
调用 ReActAgentStrategy.resumeFromToolApproval()
  ↓
根据交互类型处理
  ├─ TOOL_APPROVAL → 执行工具 → 观察阶段 → 继续迭代
  └─ 其他类型 → 继续下一轮迭代
  ↓
推送事件到原 Sink → SSE 到前端
  ↓
任务完成 → 关闭 Sink
```

## 扩展示例

### 示例1：缺少 API Key

```java
InteractionRequest request = new InteractionRequest();
request.setRequestId("missing-api-key-" + System.currentTimeMillis());
request.setSessionId(sessionId);
request.setType(InteractionType.MISSING_INFO);
request.setTitle("缺少 API Key");
request.setMessage("执行该工具需要 OpenWeather API Key");

request.addOption(new InteractionOption()
    .setOptionId("provide")
    .setLabel("提供 API Key")
    .setDescription("输入您的 API Key")
    .setAction(InteractionAction.PROVIDE_INFO)
    .setRequiresInput(true)
    .setInputPrompt("请输入 OpenWeather API Key"));

request.addOption(new InteractionOption()
    .setOptionId("skip")
    .setLabel("跳过该工具")
    .setDescription("跳过天气查询，继续其他任务")
    .setAction(InteractionAction.SKIP));

agentSessionHub.requestInteraction(sessionId, request);
```

### 示例2：用户确认（删除文件）

```java
InteractionRequest request = new InteractionRequest();
request.setRequestId("confirm-delete-" + System.currentTimeMillis());
request.setSessionId(sessionId);
request.setType(InteractionType.USER_CONFIRMATION);
request.setTitle("确认删除文件");
request.setMessage("即将删除文件: /path/to/file.txt");
request.setDetails("此操作不可撤销，请谨慎操作");

request.addOption(new InteractionOption()
    .setOptionId("confirm")
    .setLabel("确认删除")
    .setDescription("删除该文件")
    .setAction(InteractionAction.APPROVE_AND_EXECUTE)
    .setDangerous(true));

request.addOption(new InteractionOption()
    .setOptionId("cancel")
    .setLabel("取消")
    .setDescription("不删除文件")
    .setAction(InteractionAction.REJECT_WITH_REASON));

agentSessionHub.requestInteraction(sessionId, request);
```

### 示例3：用户选择（多个方案）

```java
InteractionRequest request = new InteractionRequest();
request.setRequestId("choose-plan-" + System.currentTimeMillis());
request.setSessionId(sessionId);
request.setType(InteractionType.USER_CHOICE);
request.setTitle("选择实现方案");
request.setMessage("Agent 提供了3个实现方案，请选择一个");

request.addOption(new InteractionOption()
    .setOptionId("plan-a")
    .setLabel("方案A：使用 Redis 缓存")
    .setDescription("性能最好，但需要额外的 Redis 服务")
    .setAction(InteractionAction.APPROVE_AND_EXECUTE)
    .addMetadata("plan", "redis"));

request.addOption(new InteractionOption()
    .setOptionId("plan-b")
    .setLabel("方案B：使用本地缓存")
    .setDescription("实现简单，但性能一般")
    .setAction(InteractionAction.APPROVE_AND_EXECUTE)
    .addMetadata("plan", "local"));

request.addOption(new InteractionOption()
    .setOptionId("plan-c")
    .setLabel("方案C：不使用缓存")
    .setDescription("最简单，但性能最差")
    .setAction(InteractionAction.APPROVE_AND_EXECUTE)
    .addMetadata("plan", "none"));

request.addOption(new InteractionOption()
    .setOptionId("rethink")
    .setLabel("让 Agent 重新思考")
    .setDescription("这些方案都不满意，让 Agent 提供其他方案")
    .setAction(InteractionAction.RETRY_WITH_FEEDBACK)
    .setRequiresInput(true)
    .setInputPrompt("请说明您的需求"));

agentSessionHub.requestInteraction(sessionId, request);
```

## 关键设计决策

### 1. 为什么使用通用的 InteractionRequest？

**问题**：不同的交互场景（工具审批、缺少信息、用户确认等）有不同的需求。

**解决方案**：使用通用的 `InteractionRequest` 模型，通过 `InteractionType` 区分类型，通过 `InteractionOption` 灵活配置选项。

**优点**：
- 统一的接口，易于扩展
- 前端可以根据类型渲染不同的 UI
- 后端无需为每种交互类型编写单独的逻辑

### 2. 为什么在 AgentSessionHub 中分发恢复策略？

**问题**：不同的用户响应需要不同的恢复策略。

**解决方案**：在 `AgentSessionHub.handleInteractionResponse()` 中根据 `InteractionAction` 分发到不同的恢复方法。

**优点**：
- 职责清晰：AgentSessionHub 负责会话管理和策略分发
- ReActAgentStrategy 只负责执行逻辑
- 易于扩展新的恢复策略

### 3. 为什么保留 pauseForToolApproval 便捷方法？

**问题**：工具审批是最常见的场景，每次都构建 `InteractionRequest` 太繁琐。

**解决方案**：提供 `pauseForToolApproval()` 便捷方法，内部构建标准的工具审批请求。

**优点**：
- 向后兼容，减少代码修改
- 提供标准化的工具审批流程
- 开发者可以选择使用便捷方法或直接使用通用方法

## 前端集成指南

### 1. 监听 SSE 事件

```javascript
const eventSource = new EventSource('/api/agent/chat/react/stream');

eventSource.addEventListener('INTERACTION', (event) => {
  const interactionRequest = JSON.parse(event.data).data;
  
  // 渲染交互 UI
  renderInteractionUI(interactionRequest);
});
```

### 2. 发送用户响应

```javascript
async function handleUserResponse(requestId, selectedOptionId, feedback, data) {
  const response = await fetch('/api/agent/chat/react/interaction_response', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      sessionId: currentSessionId,
      requestId: requestId,
      selectedOptionId: selectedOptionId,
      feedback: feedback,
      data: data
    })
  });
  
  const result = await response.json();
  console.log(result.message);
}
```

### 3. 渲染交互 UI

```javascript
function renderInteractionUI(request) {
  const container = document.getElementById('interaction-container');
  
  // 标题和消息
  container.innerHTML = `
    <h3>${request.title}</h3>
    <p>${request.message}</p>
    ${request.details ? `<p class="details">${request.details}</p>` : ''}
  `;
  
  // 渲染选项
  request.options.forEach(option => {
    const button = document.createElement('button');
    button.textContent = option.label;
    button.title = option.description;
    button.className = option.isDangerous ? 'btn-danger' : 'btn-primary';
    
    button.onclick = () => {
      if (option.requiresInput) {
        const feedback = prompt(option.inputPrompt);
        handleUserResponse(request.requestId, option.optionId, feedback, {});
      } else {
        handleUserResponse(request.requestId, option.optionId, null, {});
      }
    };
    
    container.appendChild(button);
  });
}
```

## 总结

本架构实现了：
✅ 通用的交互中断-恢复机制
✅ 支持多种交互类型（工具审批、缺少信息、用户确认等）
✅ 灵活的恢复策略（同意、重试、拒绝、终止、提供信息、跳过）
✅ 高扩展性（新增交互类型和动作无需修改核心代码）
✅ 清晰的职责划分（AgentSessionHub 管理会话，ReActAgentStrategy 执行逻辑）
✅ 完整的 API 接口和前端集成指南

这个架构为后续功能扩展（如更复杂的交互场景、超时处理、会话持久化等）奠定了良好的基础。
