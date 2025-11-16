<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'
import { computed, useSlots } from 'vue'
import MarkdownRender from 'vue-renderer-markdown'
import 'vue-renderer-markdown/index.css'

interface Props {
  content?: string
  class?: HTMLAttributes['class']
}
const props = defineProps<Props>()

const slots = useSlots()
const slotContent = computed<string | undefined>(() => {
  const nodes = slots.default?.() || []
  let text = ''
  for (const node of nodes) {
    if (typeof node.children === 'string')
      text += node.children
  }
  return text || undefined
})

const md = computed(() => (slotContent.value ?? props.content ?? '') as string)
</script>

<template>
  <MarkdownRender
      :class="
        cn(
          'size-full [&>*:first-child]:mt-0 [&>*:last-child]:mb-0',
          props.class,
        )
      "
      :content="md"
      v-bind="$attrs"
  />
</template>
