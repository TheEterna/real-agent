<template>
  <div class="audio-bar">
    <a-space>
      <a-button type="primary" :disabled="mode !== '语音'" @click="start">开始录音</a-button>
      <a-button :disabled="!recording" @click="stop">停止</a-button>
      <span v-if="recording" class="rec">录音中...</span>
    </a-space>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{ mode: '文本' | '语音' }>()
const recording = ref(false)

watch(() => props.mode, () => { recording.value = false })

function start(){ if (props.mode !== '语音') return; recording.value = true }
function stop(){ recording.value = false }
</script>

<style scoped lang="scss">
.audio-bar { display:flex; align-items:center; }
.rec { color:#d9534f; font-weight:600; margin-left:8px; }
</style>
