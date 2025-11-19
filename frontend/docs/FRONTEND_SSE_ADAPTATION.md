# 前端 SSE 事件处理适配文档

## 修改概述

为了适配后端新的 SSE 消息格式，对前端进行了以下修改：

## 修改文件列表

### 1. `src/composables/useSSE.ts`
**修改内容**：为所有事件类型添加监听器

**修改位置**：
- `executeReAct()` 函数（第 286-311 行）
- `executeGeek()` 函数（第 214-239 行）

**修改前**：
```typescript
source.addEventListener('message', (event: MessageEvent) => {
    if (!event?.data) return
    const data = JSON.parse(event.data) as BaseEventItem
    handleEvent(data, source)
})
```

**修改后**：
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

### 2. `src/types/events.ts`
**修改内容**：添加缺失的事件类型

**修改前**：
```typescript
export enum EventType {
  STARTED = 'STARTED',
  EXECUTING = 'EXECUTING',
  THINKING = 'THINKING',
  ACTION = 'ACTION',
  OBSERVING = 'OBSERVING',
  TOOL = 'TOOL',
  TOOL_APPROVAL = 'TOOL_APPROVAL',
  PROGRESS = 'PROGRESS',
  ERROR = 'ERROR',
  DONE = 'DONE',
  DONEWITHWARNING = 'DONEWITHWARNING',
  COMPLETED = 'COMPLETED'
}
```

**修改后**：
```typescript
export enum EventType {
  STARTED = 'STARTED',
  PROGRESS = 'PROGRESS',
  AGENT_SELECTED = 'AGENT_SELECTED',      // ✅ 新增
  THINKING = 'THINKING',
  ACTION = 'ACTION',
  ACTING = 'ACTING',                      // ✅ 新增
  OBSERVING = 'OBSERVING',
  COLLABORATING = 'COLLABORATING',        // ✅ 新增
  PARTIAL_RESULT = 'PARTIAL_RESULT',      // ✅ 新增
  DONE = 'DONE',
  EXECUTING = 'EXECUTING',
  ERROR = 'ERROR',
  TOOL = 'TOOL',
  DONEWITHWARNING = 'DONEWITHWARNING',
  TOOL_APPROVAL = 'TOOL_APPROVAL',
  INTERACTION = 'INTERACTION',            // ✅ 新增（通用交互请求）
  COMPLETED = 'COMPLETED'
}
```

### 3. `src/constants/ui.ts`
**修改内容**：为新增事件类型添加 UI 映射

**新增映射**：
```typescript
[EventType.AGENT_SELECTED]: MessageType.System,
[EventType.ACTING]: MessageType.Assistant,
[EventType.COLLABORATING]: MessageType.Assistant,
[EventType.PARTIAL_RESULT]: MessageType.Assistant,
[EventType.INTERACTION]: MessageType.ToolApproval,  // 通用交互请求，使用与工具审批相同的UI
```

## 后端 SSE 消息格式

### 标准格式
```
event: PROGRESS
data: {"sessionId":"session-1","type":"PROGRESS","message":"ReAct任务开始执行",...}
```

### 字段说明
- `event`: 事件类型（对应 `EventType` 枚举）
- `data`: JSON 格式的事件数据

## 前端处理流程

```
后端发送 SSE 消息
  ↓
event: PROGRESS
data: {...}
  ↓
sse.js 库解析
  ↓
触发 'PROGRESS' 事件监听器
  ↓
handleSSEEvent(event)
  ↓
JSON.parse(event.data)
  ↓
handleEvent(data, source)
  ↓
根据 eventType 处理
  ├─ PROGRESS → 更新进度条
  ├─ THINKING → 显示思考过程
  ├─ TOOL → 显示工具调用结果
  ├─ INTERACTION → 显示交互请求（工具审批/缺少信息等）
  └─ COMPLETED → 关闭连接
```

## 测试验证

### 测试步骤
1. 启动后端服务
2. 启动前端服务
3. 在聊天界面发送消息："查看清华大学的坐标"
4. 观察控制台和 UI

### 预期结果
✅ 能够正常接收所有事件类型
✅ 进度条正常显示
✅ 思考过程正常显示
✅ 工具调用结果正常显示
✅ 任务完成后连接正常关闭

### 实际测试消息示例
```
event:PROGRESS
data:{"type":"PROGRESS","message":"ReAct任务开始执行"}

event:THINKING
data:{"type":"THINKING","message":"用户想要查询清华大学的坐标..."}

event:TOOL
data:{"type":"TOOL","message":"map_geocode","data":{...}}

event:OBSERVING
data:{"type":"OBSERVING","message":"根据地理编码服务的结果..."}

event:DONE
data:{"type":"DONE","message":"清华大学的坐标是：经度 116.3334，纬度 40.0096。"}

event:COMPLETED
data:{"type":"COMPLETED"}
```

## 兼容性说明

### 向后兼容
- ✅ 保留了 `message` 事件监听器，兼容没有指定 `event` 字段的消息
- ✅ 所有现有功能保持不变

### 新功能支持
- ✅ 支持通用交互请求（`INTERACTION` 事件）
- ✅ 支持所有后端定义的事件类型

## 注意事项

1. **事件类型大小写**：SSE 的 `event` 字段是大小写敏感的，必须与后端完全一致。

2. **事件类型同步**：前端 `EventType` 枚举必须与后端保持同步。

3. **UI 映射**：新增事件类型时，需要在 `MessageTypeMap` 中添加对应的 UI 映射。

4. **监听器注册**：新增事件类型时，需要在 `useSSE.ts` 中添加对应的监听器。

## 后续优化建议

### 1. 动态注册监听器
```typescript
// 从 EventType 枚举自动生成监听器
Object.values(EventType).forEach(eventType => {
    source.addEventListener(eventType, handleSSEEvent)
})
```

### 2. 事件格式验证
```typescript
const handleSSEEvent = (event: MessageEvent) => {
    if (!event?.data) {
        console.warn('[SSE] 收到空数据事件')
        return
    }
    
    try {
        const data = JSON.parse(event.data) as BaseEventItem
        
        // 验证必需字段
        if (!data.type) {
            console.error('[SSE] 事件缺少 type 字段', data)
            return
        }
        
        handleEvent(data, source)
    } catch (e) {
        console.error('[SSE] 解析事件数据失败', e, event.data)
    }
}
```

### 3. 事件日志记录
```typescript
const handleSSEEvent = (event: MessageEvent) => {
    if (import.meta.env.DEV) {
        console.log('[SSE Event]', event.type, event.data)
    }
    // ... 处理逻辑
}
```

## 相关文档

- [SSE_EVENT_HANDLING_FIX.md](./SSE_EVENT_HANDLING_FIX.md) - SSE 事件处理修复详细说明
- [UNIVERSAL_INTERACTION_ARCHITECTURE.md](../../UNIVERSAL_INTERACTION_ARCHITECTURE.md) - 通用交互架构文档
