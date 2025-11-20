## 开发规范文档 / 项目提示词

**严格遵守本文档所有规范，确保代码与现有架构完美融合**

### 项目架构简介

Real Agent Console：Vue 3 + TypeScript 智能体交互前端，采用 **Agent 注册表模式** + **SSE 流式通信**。

**核心技术**: Vue 3 (Composition API), TypeScript (严格模式), Ant Design Vue, GSAP (必须用于动效), Pinia, Vue Router 4

**架构特点**: 模块化、类型安全、流式交互、主题化

### 📋 强制架构规范

#### 🔴 Agent 开发规范 (严格遵守)

**核心原则**: 所有 Agent 必须通过注册表模式集成，禁止直接修改核心文件。

##### 1. Agent 注册流程 (必须按顺序执行)

**步骤 1**: 在 `src/types/agents.ts` 添加类型
```typescript
export enum AgentType {
  YourAgent = 'your-agent-name'  // ⚠️ 使用 kebab-case
}
```

**步骤 2**: 在 `src/agent-ui/registry.ts` 注册配置
```typescript
[AgentType.YourAgent]: {
  type: AgentType.YourAgent,
  title: 'Agent 显示名称',
  themeClass: 'theme-your-agent',     // ⚠️ 必须以 theme- 开头
  renderer: 'default',
  interactions: {
    sendFnName: 'executeYourAgent',   // ⚠️ 函数名必须以 execute 开头
  },
}
```

**步骤 3**: 创建主题样式 `src/styles/agents/your-agent.css`
```css
.theme-your-agent {
  /* ⚠️ 必须定义完整的主题变量 */
}
```

**步骤 4**: 在 `src/composables/useSSE.ts` 实现执行函数
```typescript
const executeYourAgent = async (text: string, sessionId: string) => {
  // ⚠️ 必须遵守 SSE 事件规范，见下文
}
```

**步骤 5**: 创建页面组件 `src/pages/chat/YourAgent.vue`
- ⚠️ 必须继承自 `ReAct.vue` 的模式
- ⚠️ 必须实现错误处理和加载状态

**步骤 6**: 添加路由 `src/router/index.ts`
```typescript
{
  path: '/chat/your-agent',
  components: {
    default: () => import('@/pages/chat/YourAgent.vue'),
    sider: () => import('@/pages/chat/ChatSider.vue')  // ⚠️ 侧边栏必须复用
  }
}
```

**步骤 7**: 在 `src/pages/chat/ChatSider.vue` 添加导航

##### ❌ 禁止行为
- 直接修改 `registry.ts` 已有配置
- 绕过注册机制直接创建路由
- 修改 `useSSE.ts` 的核心逻辑
- 创建不符合命名规范的文件

#### 🔴 SSE 事件流规范 (严格遵守)

**核心原则**: 所有 Agent 必须遵守统一的事件类型和处理流程。

##### 2. 强制事件类型 (`src/types/events.ts`)
```typescript
export enum EventType {
  STARTED = 'STARTED',              // ⚠️ 任务开始 - 必须发送
  EXECUTING = 'EXECUTING',          // 执行中
  THINKING = 'THINKING',            // 思考中
  ACTION = 'ACTION',                // 动作执行
  OBSERVING = 'OBSERVING',          // 观察结果
  TOOL = 'TOOL',                    // 工具调用 - 独立消息
  TOOL_APPROVAL = 'TOOL_APPROVAL',  // 工具审批
  PROGRESS = 'PROGRESS',            // ⚠️ 进度更新 - 不进消息列表
  ERROR = 'ERROR',                  // ⚠️ 错误 - 必须处理
  DONE = 'DONE',                    // 完成（普通）
  DONEWITHWARNING = 'DONEWITHWARNING', // 完成（警告）
  COMPLETED = 'COMPLETED'           // ⚠️ 流结束 - 必须发送
}
```

##### 3. 强制消息聚合规则
- ✅ **相同 `messageId`**: 自动合并为同一条消息
- ✅ **TOOL 事件**: 独立消息插入，视觉归属父节点
- ✅ **PROGRESS 事件**: 仅更新全局状态，不生成消息
- ✅ **COMPLETED 事件**: 关闭连接，不生成消息

##### 4. SSE 函数实现模板 (必须遵守)
```typescript
const executeYourAgent = async (text: string, sessionId: string) => {
  try {
    // ⚠️ 1. 必须发送 STARTED 事件
    handleEvent({
      sessionId,
      agentId: 'your-agent',
      type: EventType.STARTED,
      message: '开始处理...',
      messageId: generateMessageId(), // ⚠️ 必须生成唯一messageId
      startTime: new Date()
    });

    // ⚠️ 2. 业务逻辑处理，发送对应事件类型

    // ⚠️ 3. 必须发送 COMPLETED 事件
    handleEvent({
      sessionId,
      agentId: 'your-agent',
      type: EventType.COMPLETED,
      endTime: new Date()
    });
  } catch (error) {
    // ⚠️ 4. 错误处理 - 必须实现
    handleEvent({
      sessionId,
      agentId: 'your-agent',
      type: EventType.ERROR,
      message: error.message,
      endTime: new Date()
    });
  }
}
```

##### ❌ SSE 开发禁止行为
- 修改 `useSSE.ts` 核心事件处理逻辑
- 不发送 STARTED 或 COMPLETED 事件
- 忽略错误处理
- 使用未定义的事件类型
- 破坏 messageId 聚合机制

#### 🔴 状态管理规范 (严格遵守)

**核心原则**: 使用 Pinia Store 统一管理状态，禁止组件间直接状态传递。

##### 5. 会话管理必须遵守的接口 (`src/stores/chatStore.ts`)
```typescript
// ⚠️ 只能调用以下方法，禁止直接修改 store 状态
- switchConversation(id: string)    // 切换会话
- newConversation()                 // 创建新会话
- selectTag(tag: AgentType)         // 选择 Agent
- getSessionMessages(id: string)    // 获取会话消息
- setSessionMessages(id, messages)  // 保存会话消息
- touchSession(id: string)          // 更新时间戳
```

##### 6. 路由规范 (强制使用命名视图)
```typescript
// ⚠️ 必须按此格式添加路由，不得直接修改
{
  path: '/chat/your-agent',
  components: {
    default: () => import('@/pages/chat/YourAgent.vue'),
    sider: () => import('@/pages/chat/ChatSider.vue')  // ⚠️ 侧边栏强制复用
  }
}
```

### 💡 开发规范与最佳实践

#### 🔴 代码质量要求 (不可违反)

##### TypeScript 严格规范
- ✅ **严格模式**: `strict: true` 强制启用
- ❌ **禁用 any**: 必要时添加 `// @ts-ignore` 注释说明
- ✅ **类型定义**: 所有接口和类型必须完整定义
- ✅ **组件类型化**: Props 和 Emits 必须类型化

##### Vue 组件开发规范
```vue
<!-- ⚠️ 组件开发强制模板 -->
<script setup lang="ts">
// 1. 类型导入
import type { PropType } from 'vue'

// 2. Props 定义 (必须类型化)
interface Props {
  data: SomeType
}
const props = defineProps<Props>()

// 3. Emits 定义 (必须类型化)
interface Emits {
  change: [value: string]
}
const emit = defineEmits<Emits>()

// 4. 组合式函数使用
const { messages } = useSSE()
</script>

<style scoped>
/* ⚠️ 组件样式必须 scoped */
</style>
```

##### 样式开发规范
- ✅ **主题样式**: 放置于 `src/styles/agents/`
- ✅ **组件样式**: 必须使用 `scoped`
- ✅ **动画实现**: 强制使用 GSAP（禁用 CSS transition），除非GSAP 无法实现，或 过于复杂
- ✅ **颜色工具**: 使用 `src/utils/colorUtils.ts` 生成颜色

##### 命名规范 (严格执行)
```typescript
// ✅ 正确命名
Component: PascalCase     // ReActMessageItem.vue
File: kebab-case         // color-utils.ts
Variable: camelCase      // sessionId
Constant: UPPER_CASE     // EVENT_TYPE
AgentType: kebab-case    // 'react-plus'
ThemeClass: kebab-case   // 'theme-react-plus'
Function: camelCase      // executeReact
```

### 🚀 快速开发指南

#### 5分钟添加新 Agent

```bash
# 1. 创建 Agent 目录结构
mkdir -p src/pages/chat/NewAgent
mkdir -p src/styles/agents

# 2. 复制模板文件
cp src/pages/chat/ReAct.vue src/pages/chat/NewAgent.vue
cp src/styles/agents/react.css src/styles/agents/new-agent.css
```

**步骤清单** (⚠️ 按顺序执行)：
1. ✅ `src/types/agents.ts` → 添加 `NewAgent = 'new-agent'`
2. ✅ `src/agent-ui/registry.ts` → 注册配置
3. ✅ `src/styles/agents/new-agent.css` → 定义主题
4. ✅ `src/composables/useSSE.ts` → 实现 `executeNewAgent`
5. ✅ `src/pages/chat/NewAgent.vue` → 修改组件
6. ✅ `src/router/index.ts` → 添加路由
7. ✅ `src/pages/chat/ChatSider.vue` → 添加导航

#### 核心文件速查

```
📁 核心架构文件 (⚠️ 修改需谨慎)
├── src/agent-ui/registry.ts         # Agent 注册表
├── src/composables/useSSE.ts        # SSE 流处理
├── src/stores/chatStore.ts          # 会话状态管理
├── src/types/events.ts              # 事件类型定义
└── src/types/agents.ts              # Agent 类型定义

📁 开发主要文件
├── src/pages/chat/ReAct.vue         # Agent 页面模板
├── src/utils/colorUtils.ts          # 颜色工具
├── src/router/index.ts              # 路由配置
└── src/pages/chat/ChatSider.vue     # 侧边栏导航

📁 样式文件
├── src/styles/agents/               # Agent 主题样式
└── src/components/                  # 通用组件
```

### ⚠️ 开发禁区

#### 🚫 绝对不能修改的文件
- `src/composables/useSSE.ts` 核心逻辑
- `src/stores/chatStore.ts` 状态管理逻辑
- `src/agent-ui/registry.ts` 已有配置
- `App.vue` 路由视图结构

#### 🚫 绝对不能违反的规则
1. **不得绕过注册机制** 直接创建 Agent
2. **不得破坏 SSE 事件流** 自定义事件类型
3. **不得违反命名规范** 使用错误的命名方式
4. **不得忽略错误处理** 在 SSE 函数中
5. **不得使用 CSS transition** 强制使用 GSAP

#### 🚫 架构升级审批制
当遇到以下情况必须先获得用户批准：
- 现有架构无法支持新功能
- 需要修改核心文件结构
- 性能瓶颈需要重构解决
- 新需求与现有设计冲突

### 🛠️ 颜色工具速用

```typescript
import { getRandomGlassColor, getRandomTooltipColor } from '@/utils/colorUtils'

// 玻璃效果背景 (浅色、半透明)
const cardBg = getRandomGlassColor()

// Tooltip 背景 (深色、可读性好)
const tooltipBg = getRandomTooltipColor()

// GSAP 动画应用
gsap.to(element, { backgroundColor: cardBg, duration: 0.3 })
```

### 📝 开发检查清单

**新 Agent 开发完成检查**：
- [ ] Agent 类型已添加到 `agents.ts`
- [ ] 注册表配置正确且完整
- [ ] 主题样式文件已创建
- [ ] SSE 函数实现 STARTED/COMPLETED 事件
- [ ] 页面组件继承 ReAct 模式
- [ ] 路由使用命名视图格式
- [ ] 侧边栏导航已添加
- [ ] 错误处理已实现
- [ ] TypeScript 严格模式通过
- [ ] 所有动画使用 GSAP

**代码质量检查**：
- [ ] 无 TypeScript 错误
- [ ] Props/Emits 已类型化
- [ ] 组件样式使用 scoped
- [ ] 遵循命名规范
- [ ] 无 eslint 警告

---

**🎯 目标**: 让每个开发者都能在严格遵守架构规范的前提下，快速、正确地扩展项目功能。
