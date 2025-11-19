<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'
import MarkdownRender from 'vue-renderer-markdown'
import 'vue-renderer-markdown/index.css'

interface Props {
  content: string
  class?: HTMLAttributes['class']
}
const props = defineProps<Props>()

</script>

<template>
  <MarkdownRender
      :class="
        cn(
          'size-full [&>*:first-child]:mt-0 [&>*:last-child]:mb-0',
          props.class,
        )
      "
      :content="props.content"
      v-bind="$attrs"
  />
</template>
