<script setup lang="ts">
import { computed } from 'vue'
import type { UIMessage } from '@/types/events'
import { UserOutlined } from '@ant-design/icons-vue'
import { getRandomGlassColor } from '@/utils/colorUtils'
import { format, formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'


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

import { ExclamationOutlined } from '@ant-design/icons-vue';
// 格式化时间戳

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
    <div class="w-full relative gap-2 flex flex-row-reverse items-center">


      <!-- 用户消息气泡 -->
      <div
        class="peer self-end user-message-bubble px-4 py-2.5 rounded-2xl
          shadow-[2px_1px_6px_1px_theme('--color-primary-100')]
          transition-all duration-200 ease-in-out
          relative"
        :class="statusClass"
      >{{ props.message.message }}

      </div>
      <!-- 时间戳 (可选) -->
      <p class="absolute opacity-0 peer-hover:!opacity-100 transition text-xs text-slate-400 -bottom-8 right-1">{{ formatDistanceToNow(props.message.startTime ?? new Date(), { locale: zhCN }) }}</p>

      <a-button
          v-if="status === 'failed'"
          danger
          size="small"
          type="primary"
          shape="circle"
        @click="$emit('retry')"
      >
        <template #icon>
          <ExclamationOutlined />
        </template>
      </a-button>
      <div
          v-if="showAvatar"
          class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center shadow-sm select-none"
          :style="{ backgroundColor: avatarBg }"
      >
        <UserOutlined class="text-slate-600" />
      </div>

    </div>

    <!-- 用户头像 (可选,在右侧) -->
</template>

<style scoped>
/* 针对用户消息的自定义样式优化 */
.user-message-bubble {
  word-break: break-word;
  overflow-wrap: break-word;
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
