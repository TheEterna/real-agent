# isLoading 状态管理修复文档

## 问题描述

当 Agent 任务完成（收到 `COMPLETED` 事件）后，前端页面仍然显示"AI 正在思考中"，输入框的 `disabled` 状态也没有取消，用户无法继续发送消息。

## 问题根源

### 原始实现
```typescript
const isLoading = ref(false)

const sendMessage = async () => {
  isLoading.value = true
  
  try {
    await executeReAct(currentMessage, sessionId.value)
  } finally {
    isLoading.value = false  // ❌ 只在 finally 块中重置
  }
}
```

### 问题分析

1. **异步时序问题**：
   - `executeReAct()` 是异步函数，返回 Promise
   - `COMPLETED` 事件到达时，`executeReAct()` 可能还没有完全结束
   - `isLoading` 只在 `finally` 块中重置，而不是在 `COMPLETED` 事件到达时重置

2. **状态不同步**：
   - `taskStatus` 在 `COMPLETED` 事件到达时已经设置为 `'completed'`
   - 但 `isLoading` 仍然是 `true`
   - 导致 UI 状态不一致

## 解决方案

### 方案一：使用 watch 监听（已废弃）
```typescript
const isLoading = ref(false)

watch(() => taskStatus.value.status, (newStatus) => {
  if (newStatus === 'completed' || newStatus === 'error' || newStatus === 'idle') {
    isLoading.value = false
  }
})
```

**缺点**：需要手动同步两个状态

### 方案二：使用 computed（最终方案）✅
```typescript
const isLoading = computed(() => taskStatus.value.status === 'running')
```

**优点**：
- ✅ 自动响应 `taskStatus` 的变化
- ✅ 单一数据源，避免状态不一致
- ✅ 代码更简洁
- ✅ 与 `Geek.vue` 的实现保持一致

## 修改内容

### 1. ReActPlus.vue

**修改前**：
```typescript
const isLoading = ref(false)

const sendMessage = async () => {
  isLoading.value = true
  
  try {
    await executeReAct(currentMessage, sessionId.value)
  } finally {
    isLoading.value = false
    connectionStatus.value.set('disconnected')
    if (taskStatus.value.is('running')) {
      taskStatus.value.set('error')
    }
  }
}
```

**修改后**：
```typescript
const isLoading = computed(() => taskStatus.value.status === 'running')

const sendMessage = async () => {
  // 不再手动设置 isLoading
  
  try {
    await executeReAct(currentMessage, sessionId.value)
  } catch (error) {
    // 出错时手动设置任务状态
    taskStatus.value.set('error')
  } finally {
    // 清空已发送的附件
    attachments.value = []
  }
}
```

### 2. ReAct.vue

**修改内容**：与 `ReActPlus.vue` 相同

### 3. Geek.vue

**无需修改**：已经使用了 `computed` 实现
```typescript
const isLoading = computed(() => taskStatus.value.status === 'running' || isSSEExecuting.value)
```

## 工作原理

### 状态流转

```
用户发送消息
  ↓
executeReAct() 开始
  ↓
taskStatus.set('running')
  ↓
isLoading = true (computed 自动计算)
  ↓
输入框 disabled = true
  ↓
【Agent 执行中...】
  ↓
收到 COMPLETED 事件
  ↓
taskStatus.set('completed')
  ↓
isLoading = false (computed 自动计算) ✅
  ↓
输入框 disabled = false ✅
  ↓
用户可以继续发送消息 ✅
```

### 关键代码

**useSSE.ts 中的 COMPLETED 事件处理**：
```typescript
else if (eventType === EventType.COMPLETED) {
  // COMPLETED 为流结束信号，不写入消息列表，但需要更新状态
  connectionStatus.value.set('disconnected')
  taskStatus.value.set('completed')  // ✅ 设置任务状态
  progress.value = null // 清空进度信息
  closeSource(source)
  return
}
```

**页面中的 isLoading 计算**：
```typescript
const isLoading = computed(() => taskStatus.value.status === 'running')
```

**输入框的 disabled 绑定**：
```vue
<a-textarea
  :disabled="isLoading"
  placeholder="请输入您的问题..."
/>
```

## 测试验证

### 测试步骤
1. 启动前端和后端服务
2. 发送消息："查看清华大学的坐标"
3. 观察任务执行过程
4. 等待任务完成（收到 COMPLETED 事件）
5. 检查输入框是否可用

### 预期结果
- ✅ 任务执行中：输入框 disabled，显示"AI 正在思考中"
- ✅ 任务完成：输入框恢复可用，可以继续发送消息
- ✅ 任务出错：输入框恢复可用，显示错误信息

## 相关文件

- `src/pages/chat/ReActPlus.vue` - ReAct Plus 聊天页面
- `src/pages/chat/ReAct.vue` - ReAct 聊天页面
- `src/pages/chat/Geek.vue` - Geek 模式页面（参考实现）
- `src/composables/useSSE.ts` - SSE 事件处理

## 最佳实践

### 1. 使用 computed 而不是 ref
当状态可以从其他状态计算得出时，优先使用 `computed`：

```typescript
// ❌ 不推荐：需要手动同步
const isLoading = ref(false)
watch(taskStatus, () => { isLoading.value = ... })

// ✅ 推荐：自动同步
const isLoading = computed(() => taskStatus.value.status === 'running')
```

### 2. 单一数据源
避免维护多个表示相同状态的变量：

```typescript
// ❌ 不推荐：两个状态可能不一致
const isLoading = ref(false)
const taskStatus = ref(new TaskStatus('idle'))

// ✅ 推荐：taskStatus 是唯一数据源
const taskStatus = ref(new TaskStatus('idle'))
const isLoading = computed(() => taskStatus.value.status === 'running')
```

### 3. 状态管理集中化
将状态管理逻辑集中在 `useSSE` composable 中：

```typescript
// useSSE.ts
const taskStatus = ref(new TaskStatus('idle'))

// 在 COMPLETED 事件处理中更新状态
if (eventType === EventType.COMPLETED) {
  taskStatus.value.set('completed')  // ✅ 集中管理
}
```

## 总结

通过将 `isLoading` 从 `ref` 改为 `computed`，我们实现了：
- ✅ 状态自动同步
- ✅ 代码更简洁
- ✅ 避免状态不一致
- ✅ 与 `Geek.vue` 的实现保持一致

这是一个典型的 Vue 3 响应式系统最佳实践案例。
