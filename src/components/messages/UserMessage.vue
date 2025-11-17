<script setup lang="ts">
import { computed } from 'vue'
import type { UIMessage } from '@/types/events'
import { UserOutlined } from '@ant-design/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { getRandomGlassColor } from '@/utils/colorUtils'

interface Props {
  message: UIMessage
  showAvatar?: boolean      // 是否显示头像,默认不显示
  showTimestamp?: boolean   // 是否显示时间戳
  status?: 'sending' | 'sent' | 'failed'  // 消息状态
}

const props = withDefaults(defineProps<Props>(), {
  showAvatar: false,
  showTimestamp: false,
  status: 'sent'
})

// 渲染 Markdown 为安全的 HTML
const htmlContent = computed(() => {
  const raw = props.message?.message ?? ''
  const md = marked.parse(raw || '') as string
  return DOMPurify.sanitize(md)
})

// 格式化时间戳
const formattedTime = computed(() => {
  if (!props.message?.startTime) return ''
  const date = new Date(props.message.startTime)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
})

// 头像背景色 (玻璃浅色)
const avatarBg = getRandomGlassColor()

// 根据状态显示不同样式
const statusClass = computed(() => {
  switch (props.status) {
    case 'sending':
      return 'opacity-60'
    case 'failed':
      return 'border-red-300 bg-red-50'
    default:
      return ''
  }
})
</script>

<template>
  <div class="user-message-wrapper flex justify-end items-start gap-2.5 py-2">
    <!-- 消息内容区域 (右对齐) -->
    <div class="flex flex-col items-end max-w-[75%] gap-1">
      <!-- 时间戳 (可选) -->
      <span
        v-if="showTimestamp && formattedTime"
        class="text-xs text-slate-400 px-2"
      >
        {{ formattedTime }}
      </span>

      <!-- 用户消息气泡 -->
      <div
        class="user-message-bubble px-4 py-2.5 rounded-2xl
          bg-blue-50 hover:bg-blue-100
          border border-blue-100
          shadow-sm hover:shadow-md
          transition-all duration-200 ease-in-out
          relative group"
        :class="statusClass"
      >
        <!-- Markdown 内容渲染 -->
        <div
          class="prose prose-sm max-w-none
            prose-slate
            prose-headings:text-slate-800
            prose-p:text-slate-700 prose-p:leading-relaxed
            prose-a:text-blue-600 prose-a:no-underline hover:prose-a:underline
            prose-strong:text-slate-800 prose-strong:font-semibold
            prose-code:text-blue-700 prose-code:bg-blue-100 prose-code:px-1.5 prose-code:py-0.5 prose-code:rounded
            prose-pre:bg-slate-800 prose-pre:text-slate-100
            prose-ul:text-slate-700 prose-ol:text-slate-700
            prose-li:text-slate-700
            prose-blockquote:border-blue-300 prose-blockquote:text-slate-600
            text-[13px]"
          v-html="htmlContent"
        ></div>

        <!-- 发送状态指示器 -->
        <div
          v-if="status === 'sending'"
          class="absolute -bottom-1 -right-1 w-3 h-3"
        >
          <div class="w-full h-full border-2 border-blue-400 border-t-transparent rounded-full animate-spin"></div>
        </div>

        <!-- 失败状态指示 -->
        <div
          v-if="status === 'failed'"
          class="absolute -bottom-1 -right-1 w-4 h-4 bg-red-500 text-white rounded-full
            flex items-center justify-center text-xs"
          title="发送失败"
        >
          !
        </div>
      </div>

      <!-- 失败提示文本 -->
      <span
        v-if="status === 'failed'"
        class="text-xs text-red-500 px-2 flex items-center gap-1"
      >
        <span>消息发送失败</span>
        <button
          class="text-blue-500 hover:text-blue-600 underline"
          @click="$emit('retry')"
        >
          重试
        </button>
      </span>
    </div>

    <!-- 用户头像 (可选,在右侧) -->
    <div
      v-if="showAvatar"
      class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center shadow-sm select-none"
      :style="{ backgroundColor: avatarBg }"
    >
      <UserOutlined class="text-slate-600" />
    </div>
  </div>
</template>

<style scoped>
/* 针对用户消息的自定义样式优化 */
.user-message-bubble {
  word-break: break-word;
  overflow-wrap: break-word;
}

/* 确保代码块不会破坏布局 */
.user-message-bubble :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  border-radius: 6px;
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}

/* 优化行内代码样式 */
.user-message-bubble :deep(code:not(pre code)) {
  font-size: 0.9em;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

/* 优化列表间距 */
.user-message-bubble :deep(ul),
.user-message-bubble :deep(ol) {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  padding-left: 1.5rem;
}

.user-message-bubble :deep(li) {
  margin-top: 0.25rem;
  margin-bottom: 0.25rem;
}

/* 优化段落间距 */
.user-message-bubble :deep(p) {
  margin-top: 0;
  margin-bottom: 0.75rem;
}

.user-message-bubble :deep(p:last-child) {
  margin-bottom: 0;
}

/* 优化引用块样式 */
.user-message-bubble :deep(blockquote) {
  margin-top: 0.75rem;
  margin-bottom: 0.75rem;
  padding-left: 1rem;
  border-left-width: 3px;
}

/* 链接悬浮效果 */
.user-message-bubble :deep(a) {
  transition: color 0.15s ease-in-out;
}

/* 表格优化 (如果有) */
.user-message-bubble :deep(table) {
  font-size: 0.875rem;
  margin-top: 0.75rem;
  margin-bottom: 0.75rem;
}

/* 响应式优化 */
@media (max-width: 640px) {
  .user-message-wrapper {
    max-width: 100%;
  }

  .user-message-wrapper > div {
    max-width: 85%;
  }
}
</style>
