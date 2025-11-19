<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  class?: HTMLAttributes['class']
}

const props = defineProps<Props>()
</script>

<template>
  <div
    :class="
      cn(
        'is-user:dark flex w-fit flex-col gap-2 overflow-hidden',
        'group-[.is-user]:ml-auto group-[.is-user]:rounded-lg group-[.is-user]:bg-secondary group-[.is-user]:px-4 group-[.is-user]:py-3 group-[.is-user]:text-foreground',
        'group-[.is-assistant]:text-foreground message-content',
        props.class,
      )
    "
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>
<style lang="scss" scoped>
.message-content {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI Variable Display", "Segoe UI", Helvetica, "Apple Color Emoji", Arial, sans-serif, "Segoe UI Emoji", "Segoe UI Symbol";
  font-size: 16px;

}
</style>