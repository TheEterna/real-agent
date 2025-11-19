<script setup lang="ts">
import { computed } from 'vue'
import type { SimpleCommand } from '@/stores/terminalStore'

interface Props {
  suggestions: SimpleCommand[]
  selectedIndex?: number
  maxDisplay?: number
}

const props = withDefaults(defineProps<Props>(), {
  selectedIndex: -1,
  maxDisplay: 5
})

interface Emits {
  select: [index: number]
}

const emit = defineEmits<Emits>()

// 计算属性：限制显示数量
const displaySuggestions = computed(() => {
  return props.suggestions.slice(0, props.maxDisplay)
})

// 处理点击选择
const handleSelect = (index: number) => {
  emit('select', index)
}
</script>

<template>
  <div class="command-suggestions">
    <div
      v-for="(suggestion, index) in displaySuggestions"
      :key="suggestion.name"
      :class="['suggestion-item', { selected: index === selectedIndex }]"
      @click="handleSelect(index)"
    >
      <div class="suggestion-main">
        <span class="command-name">{{ suggestion.name }}</span>
        <span v-if="suggestion.aliases" class="aliases">({{ suggestion.aliases.join(', ') }})</span>
      </div>
      <div class="suggestion-desc">{{ suggestion.description }}</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.command-suggestions {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.95);
  border: 1px solid rgba(0, 255, 0, 0.3);
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
  z-index: 1000;
  backdrop-filter: blur(8px);
}

.suggestion-item {
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid rgba(0, 255, 0, 0.1);
  color: #00ff00;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  transition: all 0.2s ease;

  &:hover,
  &.selected {
    background: rgba(0, 255, 0, 0.1);
    border-left: 3px solid #00ff00;
  }

  &:last-child {
    border-bottom: none;
  }
}

.suggestion-main {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.command-name {
  font-weight: bold;
  color: #40ff40;
}

.aliases {
  font-size: 10px;
  color: rgba(0, 255, 0, 0.7);
}

.suggestion-desc {
  font-size: 10px;
  color: rgba(0, 255, 0, 0.8);
  margin-left: 8px;
}
</style>