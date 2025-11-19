<script setup lang="ts">
import { computed, ref } from 'vue'
import { defineAsyncComponent } from 'vue'

const CodeBlock = defineAsyncComponent(() => import('./CodeBlock.vue'))

interface ToolExecutionData {
  toolCallId?: string
  toolName: string
  status: 'pending' | 'executing' | 'success' | 'error'
  args: Record<string, any>
  result?: any
  executionTime?: number
  error?: string
  startTime?: Date
}

interface Props {
  data: ToolExecutionData
  collapsible?: boolean  // 是否可折叠，默认 true
  defaultOpen?: boolean  // 默认是否展开，默认根据状态决定
}

const props = withDefaults(defineProps<Props>(), {
  collapsible: true,
  defaultOpen: undefined
})

// 控制折叠状态
const isArgsOpen = ref(props.defaultOpen ?? (props.data.status === 'error'))
const isResultOpen = ref(props.defaultOpen ?? (props.data.status === 'success'))

// 状态图标
const statusIcon = computed(() => {
  switch (props.data.status) {
    case 'pending':
      return '⏳'
    case 'executing':
      return '⚙️'
    case 'success':
      return '✅'
    case 'error':
      return '❌'
    default:
      return '❓'
  }
})

// 状态文本
const statusText = computed(() => {
  switch (props.data.status) {
    case 'pending':
      return '准备中'
    case 'executing':
      return '执行中'
    case 'success':
      return '成功'
    case 'error':
      return '失败'
    default:
      return '未知'
  }
})

// 状态颜色类
const statusColorClass = computed(() => {
  return `status-${props.data.status}`
})

// 格式化时间
const formatTime = (ms?: number) => {
  if (!ms) return ''
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(2)}s`
}

// 格式化 JSON
const formatJson = (obj: any) => {
  try {
    return JSON.stringify(obj, null, 2)
  } catch {
    return String(obj)
  }
}

// 切换折叠状态
const toggleArgs = () => {
  if (props.collapsible) {
    isArgsOpen.value = !isArgsOpen.value
  }
}

const toggleResult = () => {
  if (props.collapsible) {
    isResultOpen.value = !isResultOpen.value
  }
}
</script>

<template>
  <div class="tool-execution-card" :class="statusColorClass">
    <!-- 工具头部 -->
    <div class="tool-header">
      <div class="tool-info">
        <span class="status-icon">{{ statusIcon }}</span>
        <span class="tool-name">{{ data.toolName }}</span>
        <span class="status-badge" :class="statusColorClass">
          {{ statusText }}
        </span>
      </div>

      <div class="tool-meta">
        <!-- 执行时间 -->
        <span v-if="data.executionTime" class="execution-time">
          <span class="icon">⏱️</span>
          {{ formatTime(data.executionTime) }}
        </span>

        <!-- 时间戳 -->
        <span v-if="data.startTime" class="startTime">
          {{ new Date(data.startTime).toLocaleTimeString('zh-CN') }}
        </span>
      </div>
    </div>

    <!-- 工具参数 -->
    <div class="tool-section">
      <div
        class="section-header"
        :class="{ clickable: collapsible }"
        @click="toggleArgs"
      >
        <span class="section-title">
          <span v-if="collapsible" class="collapse-icon">
            {{ isArgsOpen ? '▼' : '▶' }}
          </span>
          参数
        </span>
        <span class="section-count">{{ Object.keys(data.args).length }} 项</span>
      </div>

      <div v-show="isArgsOpen" class="section-content">
        <CodeBlock
          v-if="Object.keys(data.args).length > 0"
          :code="formatJson(data.args)"
          language="json"
          :show-line-numbers="false"
        />
        <div v-else class="empty-content">无参数</div>
      </div>
    </div>

    <!-- 工具结果 -->
    <div v-if="data.result || data.status === 'success'" class="tool-section">
      <div
        class="section-header"
        :class="{ clickable: collapsible }"
        @click="toggleResult"
      >
        <span class="section-title">
          <span v-if="collapsible" class="collapse-icon">
            {{ isResultOpen ? '▼' : '▶' }}
          </span>
          结果
        </span>
      </div>

      <div v-show="isResultOpen" class="section-content">
        <!-- 字符串结果 -->
        <div v-if="typeof data.result === 'string'" class="text-result">
          {{ data.result }}
        </div>

        <!-- 对象结果 -->
        <CodeBlock
          v-else-if="data.result"
          :code="formatJson(data.result)"
          language="json"
          :show-line-numbers="false"
        />

        <div v-else class="empty-content">暂无结果</div>
      </div>
    </div>

    <!-- 错误信息 -->
    <div v-if="data.error" class="tool-section error-section">
      <div class="section-header">
        <span class="section-title">
          <span class="error-icon">⚠️</span>
          错误信息
        </span>
      </div>
      <div class="section-content">
        <div class="error-message">{{ data.error }}</div>
      </div>
    </div>

    <!-- 执行中的加载动画 -->
    <div v-if="data.status === 'executing'" class="executing-indicator">
      <div class="loading-dots">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.tool-execution-card {
  border: 1px solid var(--border-color, #e8e8e8);
  border-radius: 8px;
  padding: 12px 16px;
  margin: 8px 0;
  background: var(--card-bg, #ffffff);
  transition: all 0.3s ease;

  &.status-pending {
    border-left: 4px solid #faad14;
  }

  &.status-executing {
    border-left: 4px solid #1890ff;
    background: linear-gradient(90deg, rgba(24, 144, 255, 0.05) 0%, transparent 100%);
  }

  &.status-success {
    border-left: 4px solid #52c41a;
  }

  &.status-error {
    border-left: 4px solid #ff4d4f;
    background: rgba(255, 77, 79, 0.03);
  }
}

.tool-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color, #f0f0f0);
}

.tool-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.status-icon {
  font-size: 18px;
}

.tool-name {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color, #333);
}

.status-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;

  &.status-pending {
    background: #fff7e6;
    color: #fa8c16;
  }

  &.status-executing {
    background: #e6f7ff;
    color: #1890ff;
  }

  &.status-success {
    background: #f6ffed;
    color: #52c41a;
  }

  &.status-error {
    background: #fff1f0;
    color: #ff4d4f;
  }
}

.tool-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary, #8c8c8c);
}

.execution-time {
  display: flex;
  align-items: center;
  gap: 4px;

  .icon {
    font-size: 14px;
  }
}

.tool-section {
  margin-top: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  user-select: none;

  &.clickable {
    cursor: pointer;

    &:hover {
      background: rgba(0, 0, 0, 0.02);
      border-radius: 4px;
      padding-left: 4px;
      padding-right: 4px;
    }
  }
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color, #595959);
  display: flex;
  align-items: center;
  gap: 6px;
}

.collapse-icon {
  display: inline-block;
  font-size: 10px;
  color: var(--text-secondary, #8c8c8c);
  transition: transform 0.2s ease;
}

.section-count {
  font-size: 12px;
  color: var(--text-secondary, #8c8c8c);
}

.section-content {
  margin-top: 8px;
  padding-left: 16px;
}

.text-result {
  padding: 8px 12px;
  background: var(--code-bg, #f5f5f5);
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.empty-content {
  padding: 8px 12px;
  color: var(--text-secondary, #8c8c8c);
  font-size: 13px;
  font-style: italic;
}

.error-section {
  .error-message {
    padding: 8px 12px;
    background: #fff1f0;
    border-left: 3px solid #ff4d4f;
    border-radius: 4px;
    color: #cf1322;
    font-size: 13px;
    line-height: 1.6;
  }
}

.executing-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color, #d9d9d9);
}

.loading-dots {
  display: flex;
  gap: 6px;

  span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #1890ff;
    animation: bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) {
      animation-delay: -0.32s;
    }

    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
