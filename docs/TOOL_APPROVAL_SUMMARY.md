# 工具审批机制 - 实现总结

## 已完成的工作

### ✅ 后端实现

1. **核心架构**
   - `AgentSessionManagerService`: 会话管理中心，管理 Sink、ResumePoint 和恢复逻辑
   - `ResumePoint`: 保存暂停时的完整执行状态
   - `ToolApprovalCallback`: 审批回调接口，解耦模块依赖
   - `InteractionRequest/Response`: 通用的交互协议

2. **工作流程**
   - ✅ 检测工具调用 (`FluxUtils.executeToolCall`)
   - ✅ 暂停执行并保存状态 (`pauseForToolApproval`)
   - ✅ 推送审批请求到前端 (SSE 事件)
   - ✅ 接收用户响应 (`handleInteractionResponse`)
   - ✅ 恢复执行 (`resumeFromToolApproval`)

3. **API 接口**
   - ✅ `POST /api/agent/chat/react-plus/stream` - SSE 流式对话
   - ✅ `POST /api/agent/chat/react-plus/interaction_response` - 提交审批决策

---

## 核心设计要点

### 1. 为什么不能用 Spring AI 的自动工具调用?

**原因:** Spring AI 的 `stream()` 方法期望工具函数立即返回结果，无法等待异步的 HTTP 审批请求。

**解决方案:** 手动编排聊天循环，在检测到工具调用时返回空流并保存状态。

### 2. 如何实现"暂停"?

**不是真正的暂停线程**，而是:
1. 检测到需要审批时，返回审批请求事件后结束当前流
2. 保存 `ResumePoint` 到 `SessionState`
3. SSE 连接保持打开，等待用户响应

```java
case REQUIRE_APPROVAL:
    approvalCallback.requestApproval(...);  // 通知上层
    return Flux.just(AgentExecutionEvent.toolApproval(...));  // 返回审批事件，然后结束
```

### 3. 如何实现"恢复"?

**核心思想:** 从 `ResumePoint` 中取出保存的上下文，执行工具，将结果添加到上下文，继续执行。

```java
// 1. 执行工具
toolService.executeToolAsync(toolName, context)

// 2. 将工具结果添加到上下文
context.addMessage(AgentMessage.tool(toolResponse.responseData(), ...))

// 3. 继续执行后续阶段（AI 会看到工具结果）
return Flux.concat(
    toolExecutionFlux,
    observationAgent.executeStream(task, context),  // 传入包含工具结果的上下文
    ...
)
```

### 4. 上下文管理的重要性

`ResumePoint.context` 必须包含完整的聊天历史:
- 用户消息
- AI 回复
- **AI 的工具调用请求** (关键!)
- 工具结果

恢复时，AI 会看到完整历史，知道"我刚才请求了什么工具，现在工具返回了什么结果"。

---

## 前端集成要点

### 1. 连接 SSE 流

```javascript
fetch('/api/agent/chat/react-plus/stream', {
    method: 'POST',
    body: JSON.stringify({ sessionId, message })
})
```

### 2. 处理 TOOL_APPROVAL 事件

```javascript
if (event.type === 'TOOL_APPROVAL') {
    showApprovalDialog({
        requestId: event.data.requestId,
        toolName: event.data.context.toolName,
        toolArgs: event.data.context.toolArgs,
        options: event.data.options
    });
}
```

### 3. 提交审批决策

```javascript
fetch('/api/agent/chat/react-plus/interaction_response', {
    method: 'POST',
    body: JSON.stringify({
        sessionId,
        requestId,
        selectedOptionId: 'approve',  // approve / reject / terminate
        feedback: '请继续'
    })
})
```

### 4. 继续接收后续事件

审批提交后，SSE 连接会继续推送后续事件（工具结果、观察、完成等）。

---

## 审批选项

| 选项 ID | 标签 | 动作 | 说明 |
|---------|------|------|------|
| approve | 同意执行 | APPROVE_AND_EXECUTE | 直接执行工具 |
| reject | 拒绝并说明理由 | REJECT_WITH_REASON | 不执行，反馈给 AI |
| terminate | 拒绝并终止对话 | TERMINATE | 不执行，结束会话 |

---

## 测试建议

### 1. 基础流程测试
```bash
# 1. 启动对话
curl -X POST http://localhost:8080/api/agent/chat/react-plus/stream \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test-001","message":"帮我查询杭州的天气"}'

# 2. 观察 SSE 事件，等待 TOOL_APPROVAL 事件

# 3. 提交审批
curl -X POST http://localhost:8080/api/agent/chat/react-plus/interaction_response \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test-001","requestId":"call_xxx","selectedOptionId":"approve"}'

# 4. 继续观察 SSE 事件，应该看到工具执行结果
```

### 2. 拒绝场景测试
```bash
# 提交拒绝决策
curl -X POST http://localhost:8080/api/agent/chat/react-plus/interaction_response \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId":"test-001",
    "requestId":"call_xxx",
    "selectedOptionId":"reject",
    "feedback":"这个工具不安全，请使用其他方法"
  }'
```

### 3. 终止场景测试
```bash
# 提交终止决策
curl -X POST http://localhost:8080/api/agent/chat/react-plus/interaction_response \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId":"test-001",
    "requestId":"call_xxx",
    "selectedOptionId":"terminate",
    "feedback":"我不需要这个功能了"
  }'
```

---

## 后续优化建议

### 1. 超时机制
- 审批请求超过 N 分钟未响应，自动拒绝或终止
- 在 `ResumePoint` 中添加 `expiresAt` 字段

### 2. 审批历史
- 记录所有审批决策，用于审计和分析
- 添加 `ApprovalHistory` 实体

### 3. 权限控制
- 基于用户角色的自动审批白名单
- 某些工具对特定用户自动通过

### 4. 批量审批
- 一次性审批多个工具调用
- 优化用户体验

### 5. 审批模板
- 预定义常见场景的审批模板
- 快速审批

---

## 文档清单

1. ✅ `TOOL_APPROVAL_USAGE.md` - 前端集成指南（包含完整代码示例）
2. ✅ `TOOL_APPROVAL_SUMMARY.md` - 实现总结（本文档）
3. 📝 `TOOL_APPROVAL_ARCHITECTURE.md` - 架构设计详解（可选）

---

## 关键代码位置

### 后端
- `AgentSessionManagerService.java` - 会话管理核心
- `ReActAgentStrategy.java` - ReAct 执行策略
- `FluxUtils.java` - 工具调用检测和处理
- `ReActPlusAgentController.java` - API 接口

### 前端（待实现）
- 参考 `TOOL_APPROVAL_USAGE.md` 中的示例代码

---

## 总结

你的实现已经非常完整和优雅了！核心架构设计合理，代码结构清晰，完全解决了 WebFlux 环境下的异步人机交互难题。

**核心亮点:**
1. ✅ 手动编排聊天循环，完全掌控执行流程
2. ✅ 使用 `ResumePoint` 保存完整状态，支持精确恢复
3. ✅ 通过 `ToolApprovalCallback` 解耦模块依赖
4. ✅ 通用的 `InteractionRequest/Response` 协议，易于扩展
5. ✅ 支持多种审批动作（同意/拒绝/终止）

现在只需要前端实现对应的 UI 和交互逻辑即可完整使用这个功能！
