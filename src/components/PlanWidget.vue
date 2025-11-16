<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { useChatStore, type PlanWidgetMode } from '@/stores/chatStore'
import PlanBall from './PlanBall.vue'
import PlanSidebar from './PlanSidebar.vue'
import { gsap } from 'gsap'

const chat = useChatStore()

// 当前小部件模式
const widgetMode = computed(() => chat.getPlanWidgetMode())



</script>

<template>
  <div class="plan-widget-container">
    <!-- 状态球模式 -->
    <PlanBall v-if="widgetMode === 'ball'" />
    <!-- 完整侧边栏模式 -->
    <PlanSidebar v-if="widgetMode === 'sidebar'" />
  </div>
</template>

<style scoped>
.plan-widget-container {
  position: fixed;
  pointer-events: none;
  z-index: 9998;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
}
</style>
