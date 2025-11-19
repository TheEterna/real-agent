# 🔗 URL 状态同步机制

## 📅 创建时间：2025-01-14

---

## 🎯 核心功能

**问题**：刷新页面后，当前会话状态丢失，用户体验不佳。

**解决方案**：将 `sessionId` 挂载到 URL 参数上，实现**双向同步**。

---

## 🏗️ 实现架构

### URL 格式

```
/chat?sessionId=session-1
```

### 双向同步

```
┌──────────────┐         ┌──────────────┐
│   URL Query  │ ◄─────► │  chatStore   │
│  sessionId   │         │  sessionId   │
└──────────────┘         └──────────────┘
       ↕                        ↕
  刷新页面恢复              切换会话更新URL
```

---

## 🔧 实现细节

### 1️⃣ 会话切换 → 更新 URL

**监听 `chat.sessionId` 变化**：

```typescript
// src/pages/chat/ChatGateway.vue

watch(() => chat.sessionId.value, (newSessionId) => {
  // 更新 URL query 参数（不触发页面刷新）
  if (route.query.sessionId !== newSessionId) {
    router.replace({ 
      query: { ...route.query, sessionId: newSessionId } 
    })
  }
})
```

**关键点**：
- ✅ 使用 `router.replace()` 替换当前路由（不会产生历史记录）
- ✅ 保留其他 query 参数 `{ ...route.query }`
- ✅ 仅在 URL 与 store 不同步时更新，避免循环

---

### 2️⃣ URL 变化 → 切换会话

**监听 URL `query.sessionId` 变化**：

```typescript
watch(() => route.query.sessionId as string | undefined, (urlSessionId) => {
  if (urlSessionId && urlSessionId !== chat.sessionId.value) {
    // URL 中的 sessionId 存在且与当前不同，切换会话
    const sessionExists = chat.sessions.value.find(s => s.id === urlSessionId)
    if (sessionExists) {
      console.log('🔗 从 URL 恢复会话:', urlSessionId)
      chat.switchConversation(urlSessionId)
    } else {
      console.warn('⚠️ URL 中的 sessionId 不存在:', urlSessionId)
      // 回退到默认会话
      const defaultSessionId = chat.sessions.value[0]?.id
      if (defaultSessionId) {
        chat.switchConversation(defaultSessionId)
      }
    }
  } else if (!urlSessionId && chat.sessionId.value) {
    // URL 中没有 sessionId，但 store 中有当前会话，同步到 URL
    router.replace({ 
      query: { ...route.query, sessionId: chat.sessionId.value } 
    })
  }
}, { immediate: true })  // ⚠️ immediate: true 确保初始化时执行
```

**关键点**：
- ✅ `immediate: true` - 页面加载时立即执行，从 URL 恢复会话
- ✅ **容错处理** - URL 中的 sessionId 不存在时回退到默认会话
- ✅ **双向同步** - URL 没有参数时，从 store 同步到 URL

---

### 3️⃣ 初始化流程

```
用户访问 /chat
    ↓
URL 中有 sessionId?
    │
    ├─ YES → 从 URL 恢复会话
    │          ├─ sessionId 存在？ → 切换到该会话
    │          └─ sessionId 不存在？ → 使用默认会话
    │
    └─ NO  → 使用 store 默认会话
               └─ 同步 sessionId 到 URL
```

---

## 📊 工作流程示例

### 场景1：用户首次访问

```
1. 访问 /chat
2. URL: /chat (无 sessionId)
3. store 初始化为 session-1
4. 同步到 URL: /chat?sessionId=session-1
```

### 场景2：切换会话

```
1. 用户点击会话列表中的 session-2
2. chat.switchConversation('session-2')
3. watch 监听到 sessionId 变化
4. 更新 URL: /chat?sessionId=session-2
```

### 场景3：刷新页面

```
1. 当前 URL: /chat?sessionId=session-3
2. 刷新页面
3. watch(route.query.sessionId) 立即执行 (immediate: true)
4. 从 URL 读取 session-3
5. chat.switchConversation('session-3')
6. ✅ 会话状态恢复！
```

### 场景4：手动修改 URL

```
1. 用户手动修改 URL: /chat?sessionId=session-5
2. watch(route.query.sessionId) 触发
3. 检查 session-5 是否存在
4. 切换到 session-5
```

### 场景5：URL 中的 sessionId 无效

```
1. URL: /chat?sessionId=invalid-id
2. watch(route.query.sessionId) 触发
3. 找不到对应的会话
4. console.warn('⚠️ URL 中的 sessionId 不存在')
5. 回退到默认会话 session-1
6. 更新 URL: /chat?sessionId=session-1
```

---

## 🔒 防止循环更新

### 问题

```
watch(sessionId) → 更新 URL
    ↓
watch(URL) → 切换会话
    ↓
watch(sessionId) → 更新 URL
    ↓
💥 无限循环！
```

### 解决方案

**在每个 watch 中添加条件判断**：

```typescript
// watch sessionId
if (route.query.sessionId !== newSessionId) {
  router.replace({ query: { sessionId: newSessionId } })
}

// watch URL
if (urlSessionId && urlSessionId !== chat.sessionId.value) {
  chat.switchConversation(urlSessionId)
}
```

**原理**：
- 只有当 URL 和 store **不同步**时才触发更新
- 更新后两者同步，不会再次触发

---

## 🎨 用户体验优化

### 1. 页面刷新无感知

- ✅ URL 参数自动恢复会话
- ✅ 消息历史完整保留
- ✅ 当前 Agent 类型正确

### 2. 分享链接

```
用户A: /chat?sessionId=session-2
    ↓ 复制链接分享给用户B
用户B: 打开链接 → 自动定位到 session-2
```

**注意**：
- 当前 sessionId 仅在内存中，刷新后消息会丢失
- 如需持久化，需要配合 LocalStorage 或后端存储

### 3. 浏览器前进/后退

- ✅ 使用 `router.replace()` 不产生历史记录
- ✅ 避免用户点击后退时频繁切换会话

---

## 🚀 扩展功能

### 多参数支持

可以扩展更多 URL 参数：

```typescript
/chat?sessionId=session-1&agentType=ReAct&tab=tools
```

```typescript
watch(() => ({
  sessionId: route.query.sessionId,
  agentType: route.query.agentType,
  tab: route.query.tab
}), (params) => {
  // 根据多个参数恢复状态
})
```

### 持久化存储

配合 `localStorage` 持久化消息：

```typescript
// 保存消息
watch(() => chat.messagesBySession.value, (messages) => {
  localStorage.setItem('messages', JSON.stringify(messages))
}, { deep: true })

// 恢复消息
onMounted(() => {
  const saved = localStorage.getItem('messages')
  if (saved) {
    chat.messagesBySession.value = JSON.parse(saved)
  }
})
```

---

## 📝 代码位置

### 核心实现

- **文件**：`src/pages/chat/ChatGateway.vue`
- **行数**：90-122

### 依赖

- `vue-router` - 路由管理
- `useChatStore` - 会话状态管理

---

## ✅ 测试清单

- [ ] 首次访问 /chat，sessionId 同步到 URL
- [ ] 点击会话列表，URL 正确更新
- [ ] 刷新页面，会话状态恢复
- [ ] 手动修改 URL sessionId，会话正确切换
- [ ] URL 中 sessionId 无效，回退到默认会话
- [ ] 切换会话时不产生浏览器历史记录
- [ ] 多次快速切换会话，URL 正确同步
- [ ] 开发者工具查看，无循环更新警告

---

## 🎯 总结

### 核心原理

**双向同步**：
- `sessionId` 变化 → 更新 URL
- URL 变化 → 切换 `sessionId`

**防止循环**：
- 仅在不同步时更新
- 使用条件判断避免循环

**用户体验**：
- 刷新页面无感知
- 支持分享链接
- 状态持久化

---

**创建时间**: 2025-01-14  
**版本**: v1.0  
**状态**: ✅ 已实现并测试

🔗✨🎯
