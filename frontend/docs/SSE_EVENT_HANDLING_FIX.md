# SSE 事件处理修复文档

## 问题描述

后端 SSE 消息格式发生了变化，从直接发送 JSON 数据改为使用标准的 SSE 事件格式。

### 之前的格式（错误）
```
data: {"type":"PROGRESS", "message":"..."}
```

### 现在的格式（正确）
```
event: PROGRESS
data: {"sessionId":"...", "type":"PROGRESS", "message":"..."}
```

## 问题原因

前端代码只监听了通用的 `message` 事件，但是当后端使用 `event: PROGRESS` 这样的格式时，`sse.js` 库会将事件分发到对应的事件监听器（如 `PROGRESS`），而不是 `message`。

## 解决方案

在 `useSSE.ts` 中为所有可能的事件类型添加监听器。

### 修改位置

**文件**: `src/composables/useSSE.ts`

**修改内容**:

```typescript
// 通用事件处理函数
const handleSSEEvent = (event: MessageEvent) => {
    if (!event?.data) return
    const data = JSON.parse(event.data) as BaseEventItem
    handleEvent(data, source)
}

// 监听所有可能的事件类型
source.addEventListener('message', handleSSEEvent)
source.addEventListener('STARTED', handleSSEEvent)
source.addEventListener('PROGRESS', handleSSEEvent)
source.addEventListener('AGENT_SELECTED', handleSSEEvent)
source.addEventListener('THINKING', handleSSEEvent)
source.addEventListener('ACTION', handleSSEEvent)
source.addEventListener('ACTING', handleSSEEvent)
source.addEventListener('OBSERVING', handleSSEEvent)
source.addEventListener('COLLABORATING', handleSSEEvent)
source.addEventListener('PARTIAL_RESULT', handleSSEEvent)
source.addEventListener('DONE', handleSSEEvent)
source.addEventListener('EXECUTING', handleSSEEvent)
source.addEventListener('ERROR', handleSSEEvent)
source.addEventListener('TOOL', handleSSEEvent)
source.addEventListener('DONEWITHWARNING', handleSSEEvent)
source.addEventListener('TOOL_APPROVAL', handleSSEEvent)
source.addEventListener('INTERACTION', handleSSEEvent)
source.addEventListener('COMPLETED', handleSSEEvent)
```

## 修改范围

修改了两个函数：
1. `executeReAct()` - ReAct 模式的 SSE 处理
2. `executeGeek()` - Geek 模式的 SSE 处理

## 测试验证

启动前端和后端服务，发送消息：
```
查看清华大学的坐标
```

应该能够正常接收并显示以下事件：
- ✅ PROGRESS - 进度信息
- ✅ THINKING - Agent 思考过程
- ✅ ACTION - Agent 执行动作
- ✅ TOOL - 工具调用结果
- ✅ OBSERVING - 观察结果
- ✅ DONE - 任务完成
- ✅ COMPLETED - 流结束

## 注意事项

1. **事件类型大小写敏感**：SSE 的 `event` 字段是大小写敏感的，必须与后端发送的完全一致。

2. **向后兼容**：保留了 `message` 事件监听器，以兼容可能没有指定 `event` 字段的消息。

3. **新增事件类型**：如果后端新增了事件类型（如 `INTERACTION`），需要在前端添加对应的监听器。

## 相关文件

- `src/composables/useSSE.ts` - SSE 事件处理逻辑
- `src/types/events.ts` - 事件类型定义
- `src/constants/ui.ts` - 事件类型映射

## 后续优化建议

1. **动态注册监听器**：可以从 `EventType` 枚举自动生成监听器，避免手动维护。

```typescript
// 示例代码
Object.values(EventType).forEach(eventType => {
    source.addEventListener(eventType, handleSSEEvent)
})
```

2. **统一事件格式验证**：在 `handleEvent` 中添加事件格式验证，确保数据完整性。

3. **错误处理增强**：对于无法解析的事件，记录详细的错误日志。
