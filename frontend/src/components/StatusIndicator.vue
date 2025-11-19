<script setup lang="ts">
const props = defineProps<{
  status: 'idle' | 'running' | 'completed' | 'error'
}>()

const getStatusIcon = (status: string): string => {
  const iconMap: Record<string, string> = {
    running: 'icon-running',
    completed: 'icon-completed',
    error: 'icon-error',
    idle: ''
  }
  return iconMap[status] || ''
}

const getStatusText = (status: string): string => {
  const textMap: Record<string, string> = {
    running: 'ReAct任务执行中...',
    completed: '任务执行完成',
    error: '任务执行失败',
    idle: '等待中'
  }
  return textMap[status] || ''
}
</script>

<template>
  <div v-if="props.status !== 'idle'" class="status-indicator">
    <div :class="['status-badge', props.status]">
      <i :class="getStatusIcon(props.status)"></i>
      <span>{{ getStatusText(props.status) }}</span>
      <div v-if="props.status === 'running'" class="status-spinner"></div>
    </div>
  </div>
</template>

<style scoped>
.status-indicator { position: sticky; top: 0; z-index: 10; margin-bottom: 1rem; display: flex; justify-content: center; }
.status-badge { display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 1rem; border-radius: 20px; font-size: 0.9rem; font-weight: 500; box-shadow: 0 2px 8px rgba(0,0,0,0.1); backdrop-filter: blur(10px); }
.status-badge.running { background: rgba(107, 154, 152, 0.1); color: #6b9a98; border: 1px solid rgba(107, 154, 152, 0.3); }
.status-badge.completed { background: rgba(157, 181, 170, 0.1); color: #9db5aa; border: 1px solid rgba(157, 181, 170, 0.3); }
.status-badge.error { background: rgba(244, 67, 54, 0.1); color: #d32f2f; border: 1px solid rgba(244, 67, 54, 0.3); }
.status-spinner { width: 16px; height: 16px; border: 2px solid transparent; border-top: 2px solid currentColor; border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { 0% { transform: rotate(0deg);} 100% { transform: rotate(360deg);} }
.icon-running::before { content: '🔄'; }
.icon-completed::before { content: '✅'; }
.icon-error::before { content: '❌'; }
</style>
