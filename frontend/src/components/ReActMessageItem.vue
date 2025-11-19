<script setup lang="ts">
import { computed } from 'vue'
import ToolBox from './ToolBox.vue'
import { defineAsyncComponent } from 'vue'
import { UIMessage, EventType } from '@/types/events'
import ErrorMessage from './ErrorMessage.vue'

const MarkdownViewer = defineAsyncComponent(() => import('./MarkdownViewer.vue'))
const ToolApprovalCard = defineAsyncComponent(() => import('./ToolApprovalCard.vue'))

const props = defineProps<{
  message: UIMessage
}>()


// 主题样式需要的语义类名：thinking/action/observing/tool/error/completed/system/user
const messageCssClass = computed(() => {
  const mes = props.message
  if (mes.type === EventType.TOOL) return 'tool'
  if (mes.type === EventType.TOOL_APPROVAL) return 'tool_approval'
  if (mes.type === EventType.PROGRESS) return 'progress'
  if (mes.type === EventType.DONEWITHWARNING) return 'warning'
  if (mes.type === EventType.ERROR) {
    return 'error'
  }
  if (mes.type === EventType.SYSTEM) return 'system'
  if (mes.type === EventType.USER) return 'user'
  switch (mes.type) {
    case EventType.THINKING: return 'thinking'
    case EventType.ACTION: return 'action'
    case EventType.OBSERVING: return 'observing'
    default: return 'assistant'
  }
  return String(mes.type || '').toLowerCase()
})

// 安全的时间格式化，兼容 undefined / Date / string
const formatTime = (ts?: Date | string) => {
  if (!ts) return ''
  const d = ts instanceof Date ? ts : new Date(ts)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}
</script>

<template>
  <div :class="['message', messageCssClass]">
    <!-- 错误消息使用专用组件 -->
    <ErrorMessage
      v-if="props.message.type === EventType.ERROR"
      :message="props.message"
    />

    <!-- 其他消息类型使用原有渲染逻辑 -->
    <template v-else>
      <div v-if="props.message.type !== EventType.USER" class="message-header">
        <span class="sender">{{ props.message.sender }}</span>
        <span class="startTime">{{ formatTime((props.message as any).startTime) }}</span>
      </div>

      <div class="message-body">

         <!-- 嵌入：若该消息节点包含 TOOL 事件，则在同一消息框内追加工具框列表 -->
        <div v-if="props.message.type === EventType.TOOL" class="embedded-tools">
          <ToolBox :message="props.message" />
        </div>

        <!-- 工具审批 -->
        <div v-else-if="props.message.type === EventType.TOOL_APPROVAL">
          <ToolApprovalCard :approval="(props.message as any).approval" />
        </div>

        <!-- 其他：默认按 Markdown 渲染正文 -->
        <div v-else class="normal-message">
          <div class="message-text">
            <MarkdownViewer
              :message="props.message.message"
            />
          </div>
        </div>
      </div>
    </template>

  </div>
</template>

<style scoped lang="scss">
/* 使用 CSS 变量实现主题化 */

/* 基础消息样式 - 使用变量 */
.message {
  margin-bottom: var(--message-spacing, 1rem);
  padding: var(--message-padding, 1.1rem);
  border-radius: var(--message-radius, 12px);
  background: var(--message-bg, white);
  box-shadow: 0 2px 8px var(--message-shadow, rgba(15, 23, 42, 0.06));
  color: var(--message-text, #333333);
  transition: all 0.3s ease;
}

/* 各种状态 - 使用主题变量 */
.message.thinking {
  background: var(--message-thinking-bg, #fff3e0);
  border-left: var(--message-border-width, 4px) solid var(--message-thinking-border, #ff9800);
  color: var(--message-thinking-text, #e65100);
}

.message.action {
  background: var(--message-action-bg, #e8f5e8);
  border-left: var(--message-border-width, 4px) solid var(--message-action-border, #4caf50);
  color: var(--message-action-text, #2e7d32);
}

.message.observing {
  background: var(--message-observing-bg, #f3e5f5);
  border-left: var(--message-border-width, 4px) solid var(--message-observing-border, #9c27b0);
  color: var(--message-observing-text, #6a1b9a);
}

.message.error {
  background: var(--message-error-bg, #ffebee);
  border-left: var(--message-border-width, 4px) solid var(--message-error-border, #f44336);
  color: var(--message-error-text, #c62828);
}

.message.completed {
  background: var(--message-action-bg, #e8f5e8);
  border-left: var(--message-border-width, 4px) solid var(--message-action-border, #4caf50);
  border-radius: var(--message-radius, 8px);
  box-shadow: 0 2px 8px var(--message-action-border, rgba(76, 175, 80, 0.2));
}

.message.progress {
  background: var(--message-progress-bg, #fffbea);
  border-left: var(--message-border-width, 4px) solid var(--message-progress-border, #f6c342);
  color: var(--message-progress-text, #f57f17);
}

.message.warning {
  background: var(--message-warning-bg, #fff8e1);
  border-left: var(--message-border-width, 4px) solid var(--message-warning-border, #ffb300);
  color: var(--message-warning-text, #ff6f00);
}

.message.tool {
  background: var(--message-tool-bg, #e3f2fd);
  border-left: var(--message-border-width, 4px) solid var(--message-tool-border, #42a5f5);
  color: var(--message-tool-text, #1565c0);
}

/* 用户消息：右侧简洁气泡样式 */
.message.user {
  background: var(--message-user-bg, #ffffff);
  border: 1px solid var(--message-user-border, #e5e7eb);
  margin-left: auto;
  max-width: 88%;
  width: fit-content;
  box-shadow: none;
  border-radius: var(--message-radius, 12px);
}

.message.user .message-header {
  flex-direction: row-reverse;
}

.message.user .sender {
  color: var(--message-user-text, #1e88e5);
}

/* 消息头部 */
.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  color: var(--message-header-text, #666);
}

.sender {
  font-weight: 500;
  color: var(--message-sender-text, #333333);
}

.message-message {
  line-height: 1.6;
}

/* 流式消息样式 */
.stream-message .thinking-message,
.stream-message .action-message,
.stream-message .observing-message,
.stream-message .completion-message {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

/* 嵌入工具 */
.embedded-tools {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}

.tool-data {
  margin-top: 10px;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr));
  gap: 10px;
}

@media (min-width: 768px) {
  .tool-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.tool-card {
  background: var(--message-bg, #fff);
  border: 1px solid var(--message-border, rgba(0,0,0,.06));
  border-radius: var(--message-radius, 12px);
  padding: 12px 14px;
  box-shadow: 0 2px 8px var(--message-shadow, rgba(0,0,0,.05));
}

.tool-card-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--message-text, #333);
}

.tool-card-row {
  font-size: 13px;
  color: var(--message-text, #444);
  line-height: 1.6;
}

.tool-json {
  background: #0f172a;
  color: #e2e8f0;
  padding: 10px 12px;
  border-radius: 10px;
  overflow: auto;
  font-size: 12px;
}

.message-text {
  flex: 1;
  line-height: 1.6;
  margin-bottom: -1em;
}

.formatted-message {
  background: transparent;
  padding: 0;
  margin: 0;
  font-family: inherit;
  white-space: pre-wrap;
  word-wrap: break-word;
  border: none;
}

.completion-text {
  font-weight: 500;
  color: var(--message-action-text, #2e7d32);
}

.completion-message {
  animation: slideInUp 0.3s ease-out;
}

/* 图标 */
.icon-thinking::before { content: '🤔'; }
.icon-action::before { content: '🛠️'; }
.icon-observing::before { content: '👀'; }
.icon-completed::before { content: '✅'; }
.icon-progress::before { content: '⏳'; }

/* 状态气泡（进度） */
.status-bubble {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--message-progress-bg, #fff8e1);
  border: 1px solid var(--message-progress-border, #ffe082);
}

.status-bubble.progress {
  background: var(--message-progress-bg, #fff8e1);
  border-color: var(--message-progress-border, #ffd54f);
}

.status-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.status-bubble.progress .status-icon {
  animation: spin 1s linear infinite;
}

.status-content {
  display: grid;
  gap: 2px;
}

.status-title {
  font-weight: 600;
  color: var(--message-warning-text, #8d6e63);
  font-size: 0.85rem;
}

.status-text {
  font-size: 0.95rem;
  color: var(--message-text, #5d4037);
  white-space: pre-wrap;
}

.status-meta {
  font-size: 0.75rem;
  color: var(--message-header-text, #8d6e63);
}

.icon-warning::before { content: '⚠️'; }

/* 动画 */
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}


</style>
