<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import { useRouter, useRoute } from 'vue-router'
import { AgentType } from '@/types/session'
import type { Ref } from 'vue'
import { getRandomGlassColor, getRandomTooltipColor } from '@/utils/colorUtils'
import AgentSelector from '@/components/AgentSelector.vue'

const chat = useChatStore()
const router = useRouter()
const route = useRoute()

// 响应式的当前会话ID
const currentSessionId = computed(() => chat.sessionId)

// Agent选择弹窗显示状态
const showAgentSelector = ref(false)

// Agent标签配置（用于显示和提示）
const agentTags = [
  {
    label: 'ReAct',
    value: AgentType.ReAct,
    description: '推理-行动-观察框架',
    disabled: false,
    tipColor: getRandomTooltipColor()
  },
  {
    label: 'ReAct+',
    value: AgentType.ReAct_Plus,
    description: '本系统下一代主力通用Agent',
    disabled: false,
    tipColor: getRandomTooltipColor()
  },
  {
    label: '代码编写',
    value: AgentType.Coding,
    description: '专业代码生成助手',
    disabled: true,
    tipColor: getRandomTooltipColor()
  }
]

const isActive = (id: string) => {
  return currentSessionId.value === id
}

// 打开新建会话弹窗
const handleNewConversation = () => {
  showAgentSelector.value = true
}

// 选择Agent并创建会话
const handleAgentSelect = (agentType: AgentType) => {
  chat.newConversation(agentType)
  showAgentSelector.value = false

  // 确保路由是/chat
  if (route.path !== '/chat') {
    router.push('/chat')
  }
}

// 关闭Agent选择器
const handleCloseSelector = () => {
  showAgentSelector.value = false
}

// 点击会话时直接切换，不需要路由跳转
const handleSessionClick = (sessionId: string) => {
  chat.switchConversation(sessionId)

  // 确保路由是/chat
  if (route.path !== '/chat') {
    router.push('/chat')
  }
}

// Get agent label by type
const getAgentLabel = (agentType: AgentType): string => {
  const tag = agentTags.find(t => t.value === agentType)
  return tag?.label || agentType
}

// Get agent color by type
const getAgentColor = (agentType: AgentType): string => {
  const colors = {
    [AgentType.ReAct]: '#1677ff',
    [AgentType.ReAct_Plus]: '#52c41a',
    [AgentType.Coding]: '#fa8c16',
  }
  return colors[agentType] || '#666'
}

// Format date safely
const formatDate = (date: Date | string | undefined): string => {
  if (!date) return ''
  try {
    const d = date instanceof Date ? date : new Date(date)
    if (isNaN(d.getTime())) return '无效日期'
    return d.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (error) {
    return '无效日期'
  }
}

onMounted(() => {
  // 初始化时确保在/chat路由
  if (route.path !== '/chat' && route.path.startsWith('/chat')) {
    router.replace('/chat')
  }
})
</script>

<template>
  <div>
    <!-- Agent选择弹窗 -->
    <AgentSelector
      :visible="showAgentSelector"
      @select="handleAgentSelect"
      @close="handleCloseSelector"
    />

    <div class="sl-section">
      <div class="sl-title">会话</div>
      <div class="sl-conv-list">
        <div
          v-for="c in chat.sessions"
          :key="c.id"
          class="sl-conv-item"
          :class="{ active: isActive(c.id) }"
          @click="handleSessionClick(c.id)"
        >
          <div class="sl-conv-header">
            <div class="sl-conv-title">{{ c.title }}</div>
            <div
              v-if="c.agentType"
              class="sl-agent-tag"
              :style="{ backgroundColor: getAgentColor(c.agentType) }"
            >
              {{ getAgentLabel(c.agentType) }}
            </div>
          </div>
          <div class="sl-conv-meta">
            {{ c.id }} • {{ formatDate(c.updatedAt) }}
          </div>
        </div>
      </div>
      <a-button
        class="new-session-btn text-shadow-cyan-900"
        :style='{ background: `linear-gradient(135deg, ${getRandomGlassColor()} 0%, ${getRandomGlassColor()} 100%)`}'
        type="primary"
        size="large"
        @click="handleNewConversation"
      >
        ✨ 新建会话
      </a-button>
    </div>
  </div>
</template>

<style scoped>

.sl-section { padding: 12px 14px; border-bottom: 1px solid #f5f7fb; }
.sl-title { font-weight: 600; font-size: 13px; color: #445; margin-bottom: 8px; }
.sl-conv-list { display: grid; gap: 6px; margin-bottom: 12px; }

.sl-conv-item {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  cursor: pointer;
  background: linear-gradient(135deg, rgba(255,255,255,0.9) 0%, rgba(248,250,252,0.9) 100%);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.sl-conv-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #1677ff, #52c41a, #fa8c16);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.sl-conv-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.1);
  border-color: rgba(22, 119, 255, 0.3);
}

.sl-conv-item:hover::before {
  opacity: 1;
}

.sl-conv-item.active {
  border-color: #1677ff;
  background: linear-gradient(135deg, rgba(245,250,255,0.95) 0%, rgba(240,248,255,0.95) 100%);
  box-shadow: 0 4px 20px rgba(22, 119, 255, 0.15);
}

.sl-conv-item.active::before {
  opacity: 1;
}

.sl-conv-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
  gap: 8px;
}

.sl-conv-title {
  font-size: 14px;
  color: #222;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  line-height: 1.4;
}

.sl-agent-tag {
  font-size: 11px;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  letter-spacing: 0.3px;
}

.sl-conv-meta {
  font-size: 11px;
  color: #888;
  line-height: 1.3;
}

/* 新建会话按钮样式 */
.new-session-btn {
  color: rgba(0, 0, 0, 0.5);
  width: 100%;
  height: 44px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.new-session-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.new-session-btn:active {
  transform: translateY(0);
}
</style>
