# 使用示例文档

## 📋 快速开始

### 1. 基础使用：在 ReActPlus 中启用 ChatGPT 风格

```vue
<script setup lang="ts">
import { ref } from 'vue'
import MessageItem from '@/components/ReActMessageItem.vue'
import CollapsibleThinking from '@/components/messages/ThinkingMessage.vue'
import { useMessageConfig } from '@/composables/useMessageConfig'
import { MessageStyle } from '@/types/messageConfig'
import { EventType } from '@/types/events'

// 使用 ChatGPT 风格
const { getMessageConfig, shouldCollapse } = useMessageConfig(MessageStyle.CHATGPT)

// 判断是否使用折叠组件
function shouldUseCollapsible(message: UIMessage): boolean {
  return message.eventType === EventType.THINKING && shouldCollapse(message)
}
</script>

<template>
  <div class="react-plus-app theme-react-plus">
    <div v-for="msg in messages" :key="msg.id">
      <!-- Thinking 消息：使用折叠组件 -->
      <CollapsibleThinking 
        v-if="shouldUseCollapsible(msg)"
        :content="msg.message"
        :sender="msg.sender"
        :startTime="msg.startTime"
      />
      
      <!-- 其他消息：使用标准组件 -->
      <MessageItem 
        v-else
        :message="msg"
        :config="getMessageConfig(msg)"
      />
    </div>
  </div>
</template>
```

### 2. 风格切换功能

```vue
<script setup lang="ts">
import { useMessageConfig } from '@/composables/useMessageConfig'
import { MessageStyle } from '@/types/messageConfig'

const { currentStyle, setStyle, getMessageConfig } = useMessageConfig()

// 风格选项
const styleOptions = [
  { label: 'ChatGPT 风格', value: MessageStyle.CHATGPT },
  { label: '豆包风格', value: MessageStyle.DOUBAO },
  { label: 'Claude 风格', value: MessageStyle.CLAUDE },
  { label: '紧凑风格', value: MessageStyle.COMPACT },
  { label: '默认风格', value: MessageStyle.DEFAULT },
]
</script>

<template>
  <div class="chat-page">
    <!-- 风格切换器 -->
    <div class="style-switcher">
      <label>消息风格：</label>
      <select v-model="currentStyle" @change="setStyle(currentStyle)">
        <option 
          v-for="option in styleOptions" 
          :key="option.value"
          :value="option.value"
        >
          {{ option.label }}
        </option>
      </select>
    </div>
    
    <!-- 消息列表 -->
    <div class="messages">
      <MessageItem 
        v-for="msg in messages"
        :key="msg.id"
        :message="msg"
        :config="getMessageConfig(msg)"
      />
    </div>
  </div>
</template>

<style scoped>
.style-switcher {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-light);
}

.style-switcher label {
  font-weight: 500;
  color: var(--text-primary);
}

.style-switcher select {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-light);
  background: var(--bg-primary);
  color: var(--text-primary);
  cursor: pointer;
}
</style>
```

### 3. 自定义配置

如果预设风格不满足需求，可以自定义配置：

```typescript
import { AgentMessageConfig } from '@/types/messageConfig'

// 自定义配置
const myCustomConfig: AgentMessageConfig = {
  thinking: {
    collapsible: {
      enabled: true,
      defaultCollapsed: true,
      collapseThreshold: 5,
      showPreview: true,
      previewLines: 3,
    },
    layout: {
      variant: 'card',
      maxWidth: '800px',
    },
    animation: {
      entrance: 'zoom',
      duration: 400,
      easing: 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
    },
    interaction: {
      copyable: true,
      hoverable: true,
      draggable: false,
    },
  },
  
  user: {
    layout: {
      variant: 'minimal',
      alignment: 'right',
      maxWidth: '70%',
    },
    animation: {
      entrance: 'slide',
      duration: 250,
    },
  },
}
```

使用自定义配置：

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { myCustomConfig } from '@/configs/customConfig'

const config = ref(myCustomConfig)

function getMessageConfig(message: UIMessage) {
  if (message.eventType === EventType.THINKING) {
    return config.value.thinking || {}
  }
  // ... 其他逻辑
}
</script>
```

## 🎨 预设风格对比

### ChatGPT 风格
**特点**：
- ✅ Thinking 默认折叠
- ✅ 紧凑布局
- ✅ 流畅动画
- ✅ 专业简洁

**适用场景**：日常对话、问答系统

**效果预览**：
```
┌─────────────────────────────────────┐
│ 🔄 思考过程        [展开查看 ▼]     │
├─────────────────────────────────────┤
│ 用户的回复...                        │
└─────────────────────────────────────┘
```

### 豆包风格
**特点**：
- ✅ Thinking 折叠 + 预览
- ✅ 卡片布局
- ✅ 圆润风格
- ✅ 亲和力强

**适用场景**：休闲对话、客服系统

**效果预览**：
```
┌─────────────────────────────────────┐
│ 🤔 思考中...                         │
│ 让我想想如何回答这个问题...          │
│                      [查看详情 ▼]   │
└─────────────────────────────────────┘
```

### Claude 风格
**特点**：
- ✅ Thinking 默认展开
- ✅ 宽松布局
- ✅ 专业严谨
- ✅ 内容为主

**适用场景**：深度分析、学术讨论

**效果预览**：
```
┌─────────────────────────────────────┐
│ 💭 思考过程                          │
│                                     │
│ 首先，我需要分析...                  │
│ 然后，考虑到...                      │
│ 最后，综合判断...                    │
│                                     │
│                      [收起 ▲]       │
└─────────────────────────────────────┘
```

### 紧凑风格
**特点**：
- ✅ 所有消息紧凑
- ✅ 快速动画
- ✅ 信息密集
- ✅ 节省空间

**适用场景**：监控面板、日志查看

**效果预览**：
```
┌──────────────────────┐
│ 🔄 [思考] ▼          │
├──────────────────────┤
│ 🛠️ [操作]            │
├──────────────────────┤
│ 👤 用户消息          │
└──────────────────────┘
```

## 🚀 高级用法

### 1. 动态切换风格

根据消息内容动态选择风格：

```typescript
function getStyleForMessage(message: UIMessage): MessageStyle {
  // 长消息使用 ChatGPT 风格（折叠）
  if (message.message.length > 500) {
    return MessageStyle.CHATGPT
  }
  
  // 代码消息使用 Claude 风格（展开）
  if (message.message.includes('```')) {
    return MessageStyle.CLAUDE
  }
  
  // 默认使用豆包风格
  return MessageStyle.DOUBAO
}
```

### 2. 组合使用

不同消息类型使用不同组件：

```vue
<template>
  <div class="messages">
    <component 
      :is="getComponentForMessage(msg)"
      v-for="msg in messages"
      :key="msg.id"
      :message="msg"
      :config="getMessageConfig(msg)"
    />
  </div>
</template>

<script setup lang="ts">
function getComponentForMessage(message: UIMessage) {
  // Thinking 使用折叠组件
  if (message.eventType === EventType.THINKING) {
    return CollapsibleThinking
  }
  
  // Tool 使用工具卡片
  if (message.type === MessageType.Tool) {
    return ToolCard
  }
  
  // 默认使用标准组件
  return MessageItem
}
</script>
```

### 3. 主题联动

根据主题自动选择风格：

```typescript
import { watch } from 'vue'

const isDarkMode = ref(false)
const { setStyle } = useMessageConfig()

watch(isDarkMode, (dark) => {
  if (dark) {
    setStyle(MessageStyle.COMPACT)  // 暗色模式用紧凑风格
  } else {
    setStyle(MessageStyle.CHATGPT)  // 亮色模式用 ChatGPT 风格
  }
})
```

## 📊 性能优化

### 1. 虚拟滚动

对于大量消息，使用虚拟滚动：

```vue
<script setup lang="ts">
import { useVirtualList } from '@vueuse/core'

const { list, containerProps, wrapperProps } = useVirtualList(
  messages,
  {
    itemHeight: 80,  // 估计高度
  }
)
</script>

<template>
  <div v-bind="containerProps" class="messages-container">
    <div v-bind="wrapperProps">
      <MessageItem 
        v-for="item in list"
        :key="item.data.id"
        :message="item.data"
        :config="getMessageConfig(item.data)"
      />
    </div>
  </div>
</template>
```

### 2. 懒加载

延迟加载折叠内容：

```vue
<script setup lang="ts">
const contentLoaded = ref(false)

const loadContent = () => {
  if (!contentLoaded.value) {
    contentLoaded.value = true
    // 加载内容...
  }
}
</script>

<template>
  <CollapsibleThinking 
    :content="contentLoaded ? fullContent : ''"
    @expand="loadContent"
  />
</template>
```

## 🎯 最佳实践

### 1. 选择合适的风格

| 场景 | 推荐风格 | 原因 |
|------|---------|------|
| 日常对话 | ChatGPT | 简洁高效 |
| 客服系统 | 豆包 | 亲和友好 |
| 技术分析 | Claude | 内容完整 |
| 监控日志 | 紧凑 | 信息密集 |

### 2. 统一风格

同一个应用保持风格统一，避免混乱。

### 3. 渐进增强

先使用默认配置，根据用户反馈逐步优化。

### 4. 用户偏好

提供设置选项，让用户自选风格：

```vue
<template>
  <div class="settings">
    <h3>消息显示偏好</h3>
    
    <label>
      <input type="checkbox" v-model="autoCollapseThinking" />
      自动折叠思考过程
    </label>
    
    <label>
      消息布局：
      <select v-model="preferredLayout">
        <option value="default">默认</option>
        <option value="compact">紧凑</option>
        <option value="card">卡片</option>
      </select>
    </label>
  </div>
</template>
```

## 🔧 故障排除

### Q: 折叠组件不工作？

**A**: 检查以下几点：
1. 是否正确引入了 `CollapsibleThinking` 组件
2. 消息是否满足折叠条件（`collapseThreshold`）
3. 配置中 `collapsible.enabled` 是否为 `true`

### Q: 样式不生效？

**A**: 确保：
1. 主题 CSS 已引入（`theme-react.css` 或 `theme-react-plus.css`）
2. 根元素有正确的 theme 类名
3. CSS 变量定义正确

### Q: 性能问题？

**A**: 优化建议：
1. 使用虚拟滚动
2. 减少动画时长
3. 禁用不必要的交互功能
4. 使用 `v-memo` 优化渲染

---

**更新日期**：2025年  
**文档版本**：v1.0
