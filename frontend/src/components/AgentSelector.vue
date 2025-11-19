<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { AgentType } from '@/types/session'
import gsap from 'gsap'

interface AgentOption {
  type: AgentType
  label: string
  description: string
  icon: string
  color: string
  disabled?: boolean
}

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  select: [agentType: AgentType]
  close: []
}>()

const agentOptions: AgentOption[] = [
  {
    type: AgentType.ReAct,
    label: 'ReAct',
    description: '推理-行动-观察框架，适合复杂推理任务',
    icon: '🧠',
    color: '#1677ff',
  },
  {
    type: AgentType.ReAct_Plus,
    label: 'ReAct+',
    description: '增强版ReAct，下一代主力通用Agent',
    icon: '⚡',
    color: '#52c41a',
  },
  {
    type: AgentType.Coding,
    label: '代码编写',
    description: '专业代码生成助手（开发中）',
    icon: '💻',
    color: '#fa8c16',
    disabled: true,
  },
]

const overlayRef = ref<HTMLElement>()
const cardsRef = ref<HTMLElement[]>([])

const handleSelect = (agent: AgentOption) => {
  if (agent.disabled) return

  // 退出动画
  const tl = gsap.timeline({
    onComplete: () => {
      emit('select', agent.type)
    }
  })

  tl.to(cardsRef.value, {
    scale: 0.8,
    opacity: 0,
    duration: 0.3,
    stagger: 0.05,
    ease: 'back.in(1.7)'
  })
  .to(overlayRef.value, {
    opacity: 0,
    duration: 0.2
  }, '-=0.1')
}

const handleClose = () => {
  // 退出动画
  const tl = gsap.timeline({
    onComplete: () => {
      emit('close')
    }
  })

  tl.to(cardsRef.value, {
    scale: 0.8,
    opacity: 0,
    y: 50,
    duration: 0.3,
    stagger: 0.05,
    ease: 'power2.in'
  })
  .to(overlayRef.value, {
    opacity: 0,
    duration: 0.2
  }, '-=0.1')
}

// 入场动画
const playEnterAnimation = () => {
  nextTick(() => {
    if (!overlayRef.value || cardsRef.value.length === 0) return

    const tl = gsap.timeline()

    // 背景淡入
    gsap.set(overlayRef.value, { opacity: 0 })
    tl.to(overlayRef.value, {
      opacity: 1,
      duration: 0.3,
      ease: 'power2.out'
    })

    // 卡片入场动画
    gsap.set(cardsRef.value, {
      scale: 0,
      opacity: 0,
      rotationY: -180,
    })

    tl.to(cardsRef.value, {
      scale: 1,
      opacity: 1,
      rotationY: 0,
      duration: 0.6,
      stagger: {
        each: 0.15,
        from: 'center'
      },
      ease: 'back.out(1.7)'
    }, '-=0.2')

    // 悬浮动画
    tl.to(cardsRef.value, {
      y: -10,
      duration: 1.5,
      stagger: 0.1,
      ease: 'sine.inOut',
      repeat: -1,
      yoyo: true
    })
  })
}

// 监听visible变化
onMounted(() => {
  if (props.visible) {
    playEnterAnimation()
  }
})

// 当visible变为true时播放入场动画
const handleVisibleChange = (newVal: boolean) => {
  if (newVal) {
    playEnterAnimation()
  }
}

// 监听props.visible变化
import { watch } from 'vue'
watch(() => props.visible, handleVisibleChange)
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      ref="overlayRef"
      class="agent-selector-overlay"
      @click.self="handleClose"
    >
      <div class="agent-selector-container">
        <div class="selector-header">
          <h2 class="selector-title">选择你的 Agent</h2>
          <p class="selector-subtitle">开始一段全新的智能对话旅程</p>
        </div>

        <div class="agent-cards">
          <div
            v-for="(agent, index) in agentOptions"
            :key="agent.type"
            :ref="el => cardsRef[index] = el as HTMLElement"
            class="agent-card"
            :class="{ disabled: agent.disabled }"
            :style="{ '--agent-color': agent.color }"
            @click="handleSelect(agent)"
          >
            <div class="card-glow"></div>
            <div class="card-content">
              <div class="agent-icon">{{ agent.icon }}</div>
              <h3 class="agent-label">{{ agent.label }}</h3>
              <p class="agent-description">{{ agent.description }}</p>
              <div v-if="agent.disabled" class="disabled-badge">敬请期待</div>
            </div>
          </div>
        </div>

        <button class="close-btn" @click="handleClose">
          <span>✕</span>
        </button>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.agent-selector-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 40px;
}

.agent-selector-container {
  position: relative;
  max-width: 1000px;
  width: 100%;
}

.selector-header {
  text-align: center;
  margin-bottom: 60px;
  perspective: 1000px;
}

.selector-title {
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 12px;
  letter-spacing: -1px;
}

.selector-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 300;
}

.agent-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 30px;
  perspective: 1000px;
}

.agent-card {
  position: relative;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 40px 30px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  backdrop-filter: blur(10px);
  transform-style: preserve-3d;
}

.agent-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--agent-color);
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 0;
}

.agent-card:hover::before {
  opacity: 0.1;
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, var(--agent-color) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.agent-card:hover .card-glow {
  opacity: 0.3;
}

.agent-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: var(--agent-color);
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.3),
    0 0 40px var(--agent-color);
}

.agent-card.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.agent-card.disabled:hover {
  transform: none;
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: none;
}

.card-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.agent-icon {
  font-size: 64px;
  margin-bottom: 20px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.3));
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.agent-label {
  font-size: 28px;
  font-weight: 700;
  color: white;
  margin-bottom: 12px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
}

.agent-description {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  margin: 0;
}

.disabled-badge {
  margin-top: 16px;
  display: inline-block;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.close-btn {
  position: absolute;
  top: -60px;
  right: 0;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  color: white;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.5);
  transform: rotate(90deg);
}

/* 响应式 */
@media (max-width: 768px) {
  .selector-title {
    font-size: 32px;
  }

  .agent-cards {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}
</style>
