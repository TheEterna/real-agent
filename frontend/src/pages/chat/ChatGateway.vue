<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick, shallowRef } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chatStore'
import { AgentType } from '@/types/session'
import CeladonVideoLoading from '@/components/loading/CeladonVideoLoading.vue'

// 动态导入Agent组件
import ReAct from './ReAct.vue'
import Index from './reactplus/Index.vue'

const router = useRouter()
const route = useRoute()
const chat = useChatStore()

// 使用shallowRef优化性能
const currentComponent = shallowRef<any>(null)
const previousComponent = shallowRef<any>(null)

// 青花瓷过渡动画相关
const isTransitioning = ref(false)
const showVideoTransition = ref(false)
const transitionAudioIndex = ref(0)

// 过渡文案
const transitionTitle = ref('切换中')
const transitionSubtitle = ref('...')

// Agent组件映射
const agentComponentMap: Record<AgentType, any> = {
  [AgentType.ReAct]: ReAct,
  [AgentType.ReAct_Plus]: Index,
  [AgentType.Coding]: ReAct, // todo 暂时使用ReAct作为placeholder
}

// Agent名称映射
const agentNameMap: Record<AgentType, string> = {
  [AgentType.ReAct]: 'ReAct',
  [AgentType.ReAct_Plus]: 'ReAct+',
  [AgentType.Coding]: 'Coding',
}


// 获取当前应该渲染的组件
const getComponentForAgent = (agentType: AgentType) => {
  return agentComponentMap[agentType] || ReAct
}

// 青花瓷过渡动画
const playVideoTransition = async (targetAgent?: AgentType) => {
  if (isTransitioning.value) return

  isTransitioning.value = true

  // 设置过渡文案
  if (targetAgent) {
    transitionTitle.value = `切换至 ${agentNameMap[targetAgent]}`
    transitionSubtitle.value = '切换中...'
  } else {
    transitionTitle.value = '切换中'
    transitionSubtitle.value = '...'
  }

  // 随机选择音效
  transitionAudioIndex.value = Math.floor(Math.random() * 3)

  // 显示视频过渡
  showVideoTransition.value = true
}

// 视频过渡事件处理
const onTransitionStarted = () => {

}

const onTransitionEnded = () => {
  showVideoTransition.value = false
  isTransitioning.value = false
}

const onTransitionError = (error: string) => {
  showVideoTransition.value = false
  isTransitioning.value = false
}


watch(() => route.query.sessionId as string | undefined, (urlSessionId) => {
  if (urlSessionId && urlSessionId !== chat.sessionId) {
    // URL 中的 sessionId 存在且与当前不同，切换会话
    const sessionExists = chat.sessions.find(s => s.id === urlSessionId)
    if (sessionExists) {
      console.log('🔗 从 URL 恢复会话:', urlSessionId)
      chat.switchConversation(urlSessionId)
    } else {
      console.warn('⚠️ URL 中的 sessionId 不存在:', urlSessionId)
      // URL 中的会话不存在，使用默认会话并更新 URL
      const defaultSessionId = chat.sessions[0]?.id
      if (defaultSessionId) {
        chat.switchConversation(defaultSessionId)
      }
    }
  } else if (!urlSessionId && chat.sessionId) {
    // URL 中没有 sessionId，但 store 中有当前会话，同步到 URL
    router.replace({ 
      query: { ...route.query, sessionId: chat.sessionId } 
    })
  }
}, { immediate: true })

// 监听session变化，处理组件切换和过渡动画
watch(() => chat.sessionId, async (newSessionId, oldSessionId) => {
  console.log('会话切换检测:', { newSessionId, oldSessionId })

  if (oldSessionId && newSessionId !== oldSessionId) {
    const session = chat.getCurrentSession()

    // 1. 先播放青花瓷过渡动画，阻止组件切换
    if (session) {
      await playVideoTransition(session.type)
    }

    // 2. 等待过渡动画开始后再更新组件
    setTimeout(() => {
      const currentSession = chat.getCurrentSession()
      if (currentSession) {
        previousComponent.value = currentComponent.value
        currentComponent.value = getComponentForAgent(currentSession.type)
      }
    }, 100) // 100ms后更新，确保过渡动画已开始显示
  } else {
    // 初始加载或没有切换，直接更新组件
    const session = chat.getCurrentSession()
    if (session) {
      previousComponent.value = currentComponent.value
      currentComponent.value = getComponentForAgent(session.type)
    }
  }
})

// 初始化
onMounted(() => {
  const session = chat.getCurrentSession()
  if (session) {
    currentComponent.value = getComponentForAgent(session.type)
  }
})
</script>

<template>
  <div class="chat-gateway">
    <!-- 过渡效果 -->
    <CeladonVideoLoading
      :visible="showVideoTransition"
      :title="transitionTitle"
      :subtitle="transitionSubtitle"
      :audio-index="transitionAudioIndex"
      @started="onTransitionStarted"
      @ended="onTransitionEnded"
      @error="onTransitionError"
    />

    <!-- 动态渲染Agent组件 -->
    <Transition name="fade" mode="out-in">
      <component
        v-if="currentComponent"
        :is="currentComponent"
        :key="chat.sessionId"
        class="agent-view"
      />
      <div v-else class="empty-state">
        <div class="empty-icon">💬</div>
        <h3>欢迎使用 Real Agent</h3>
        <p>请从左侧选择或创建一个会话</p>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.chat-gateway {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.agent-view {
  width: 100%;
  height: 100%;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 24px;
}

.empty-state h3 {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.empty-state p {
  font-size: 16px;
  color: #666;
}

/* 淡入淡出过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}


.fade-leave-to {
  opacity: 0;
  transform: scale(3) translateY(-20px);
}
</style>
