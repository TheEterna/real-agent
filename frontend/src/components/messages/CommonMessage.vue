<script setup lang="ts">
import {computed, ref} from 'vue'
import type { UIMessage } from '@/types/events'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { getRandomGlassColor } from '@/utils/colorUtils'
import {
  Message,
  MessageAction,
  MessageActions,
  MessageAttachment,
  MessageAttachments,
  MessageBranch,
  MessageBranchContent,
  MessageBranchNext,
  MessageBranchPage,
  MessageBranchPrevious,
  MessageBranchSelector,
  MessageContent,
  MessageResponse,
  MessageToolbar,
} from '@/components/ai-elements/message'
const props = defineProps<{ message: UIMessage }>()

interface Attachment {
  type: 'file'
  url: string
  mediaType?: string
  filename?: string
}

interface Version {
  id: string
  content: string
}

interface MessageType {
  key: string
  from: 'user' | 'assistant'
  versions?: Version[]
  content?: string
  attachments?: Attachment[]
}

// 判断是否来自用户（根据项目实际 sender 命名，备用：'USER' | 'ASSISTANT'）
const isUser = computed(() => {
  const s: any = (props.message as any)?.sender
  if (typeof s === 'string') return s.toLowerCase() === 'user'
  // 兼容对象或枚举，尽量降噪
  return s === 1 || s === 'USER'
})

// 渲染 markdown 为安全的 HTML
const htmlContent = computed(() => {
  const raw = props.message?.message ?? ''
  const md = marked.parse(raw || '') as string
  return DOMPurify.sanitize(md)
})
// 头像圆形背景（玻璃浅色）
const avatarBg = getRandomGlassColor()


const liked = ref<Record<string, boolean>>({})
const disliked = ref<Record<string, boolean>>({})

function handleCopy(content: string) {
  navigator.clipboard.writeText(content)
}

function handleRetry() {
  console.log('Retrying...')
}

function toggleLike(key: string) {
  liked.value = {
    ...liked.value,
    [key]: !liked.value[key],
  }
}

function toggleDislike(key: string) {
  disliked.value = {
    ...disliked.value,
    [key]: !disliked.value[key],
  }
}

function hasMultipleVersions(message: MessageType) {
  return message.versions && message.versions.length > 1
}

function handleBranchChange(index: number) {
  console.log('Branch changed to:', index)
}
</script>

<template>

  <Message
      from='assistant'
  >


    <!-- Single version without branch selector -->

      <MessageContent>
        <MessageResponse :content="props.message.message" />
      </MessageContent>

  </Message>

</template>

<style scoped>
</style>
