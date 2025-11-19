<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { gsap } from 'gsap'

// Props 定义
interface Props {
  active?: boolean
  icon?: any
  label?: string
  disabled?: boolean
  variant?: 'geek' | 'multimodal' | 'default'
}

const props = withDefaults(defineProps<Props>(), {
  active: false,
  label: '模式',
  disabled: false,
  variant: 'default'
})

// Emits 定义
interface Emits {
  click: []
}

const emit = defineEmits<Emits>()

// 组件引用
const buttonRef = ref<HTMLButtonElement>()
const glowRef = ref<HTMLDivElement>()
const ringRef = ref<HTMLDivElement>()
const particlesRef = ref<HTMLDivElement>()

// 动画状态
let hoverTimeline: gsap.core.Timeline | null = null
let activeTimeline: gsap.core.Timeline | null = null
let breathingAnimation: gsap.core.Tween | null = null
let particleAnimations: gsap.core.Tween[] = []

// 点击处理
const handleClick = () => {
  if (props.disabled) return

  // 点击波纹效果
  if (buttonRef.value) {
    gsap.fromTo(buttonRef.value,
      { scale: 1 },
      {
        scale: 0.95,
        duration: 0.1,
        ease: "power2.in",
        onComplete: () => {
          gsap.to(buttonRef.value || null, {
            scale: 1,
            duration: 0.2,
            ease: "back.out(1.7)"
          })
        }
      }
    )
  }

  emit('click')
}

// 创建粒子效果
const createParticles = () => {
  if (!particlesRef.value) return

  // 清除现有粒子
  particlesRef.value.innerHTML = ''
  particleAnimations.forEach(anim => anim.kill())
  particleAnimations = []

  // 创建6个粒子
  for (let i = 0; i < 6; i++) {
    const particle = document.createElement('div')
    particle.className = 'particle'
    particlesRef.value.appendChild(particle)

    // 随机位置
    const angle = (i * 60) + Math.random() * 30 - 15
    const radius = 40 + Math.random() * 20
    const x = Math.cos(angle * Math.PI / 180) * radius
    const y = Math.sin(angle * Math.PI / 180) * radius

    gsap.set(particle, {
      x: 0,
      y: 0,
      scale: 0,
      opacity: 0
    })

    // 粒子动画
    const particleAnimation = gsap.to(particle, {
      x,
      y,
      scale: Math.random() * 0.5 + 0.3,
      opacity: 0.6,
      duration: 2 + Math.random() * 2,
      ease: "power2.out",
      repeat: -1,
      yoyo: true,
      delay: Math.random() * 2
    })

    particleAnimations.push(particleAnimation)
  }
}

// 悬浮动效
const setupHoverEffects = () => {
  if (!buttonRef.value || !glowRef.value || !ringRef.value) return

  const button = buttonRef.value
  const glow = glowRef.value
  const ring = ringRef.value

  button.addEventListener('mouseenter', () => {
    if (props.disabled) return

    // 创建悬浮时间线
    hoverTimeline = gsap.timeline()

    // 按钮主体
    hoverTimeline.to(button, {
      y: -2,
      scale: 1.02,
      duration: 0.3,
      ease: "back.out(1.5)"
    }, 0)

    // 光晕效果
    hoverTimeline.to(glow, {
      scale: 1.1,
      opacity: 0.8,
      duration: 0.3,
      ease: "power2.out"
    }, 0)

    // 外圈旋转
    hoverTimeline.to(ring, {
      rotation: 180,
      scale: 1.05,
      opacity: 0.6,
      duration: 0.8,
      ease: "power2.out"
    }, 0)

    // 创建粒子效果
    createParticles()
  })

  button.addEventListener('mouseleave', () => {
    if (props.disabled) return

    // 停止悬浮动画
    if (hoverTimeline) {
      hoverTimeline.kill()
    }

    // 恢复原状
    gsap.to(button, {
      y: 0,
      scale: 1,
      duration: 0.3,
      ease: "power2.out"
    })

    gsap.to(glow, {
      scale: 1,
      opacity: props.active ? 0.8 : 0.4, // 激活状态下更明显
      duration: 0.3,
      ease: "power2.out"
    })

    gsap.to(ring, {
      rotation: 0,
      scale: 1,
      opacity: props.active ? 0.6 : 0.3, // 激活状态下更明显
      duration: 0.5,
      ease: "power2.out"
    })

    // 清除粒子
    particleAnimations.forEach(anim => anim.kill())
    particleAnimations = []
    if (particlesRef.value) {
      particlesRef.value.innerHTML = ''
    }
  })
}

// 激活状态呼吸效果
const setupBreathingEffect = () => {
  if (!props.active || !glowRef.value) return

  breathingAnimation = gsap.to(glowRef.value, {
    scale: 1.1,
    opacity: 0.8,
    duration: 2,
    ease: "sine.inOut",
    repeat: -1,
    yoyo: true
  })
}

// 组件挂载
onMounted(() => {
  setupHoverEffects()

  if (props.active) {
    setupBreathingEffect()
  }
})

// 组件卸载时清理动画
onUnmounted(() => {
  if (hoverTimeline) hoverTimeline.kill()
  if (activeTimeline) activeTimeline.kill()
  if (breathingAnimation) breathingAnimation.kill()
  particleAnimations.forEach(anim => anim.kill())
})

// 监听 active 状态变化
watch(() => props.active, (newVal) => {
  if (breathingAnimation) {
    breathingAnimation.kill()
    breathingAnimation = null
  }

  if (newVal) {
    setupBreathingEffect()
  }
})
</script>

<template>
  <button
    ref="buttonRef"
    :class="[
      'neon-mode-button',
      `neon-mode-button--${variant}`,
      {
        'neon-mode-button--active': active,
        'neon-mode-button--disabled': disabled
      }
    ]"
    :disabled="disabled"
    @click="handleClick"
  >
    <!-- 外圈光环 -->
    <div ref="ringRef" class="neon-ring"></div>

    <!-- 主光晕 -->
    <div ref="glowRef" class="neon-glow"></div>

    <!-- 粒子容器 -->
    <div ref="particlesRef" class="particles-container"></div>

    <!-- 按钮内容 -->
    <div class="button-content">
      <component v-if="icon" :is="icon" class="button-icon" />
      <span v-if="label" class="button-label">{{ label }}</span>
    </div>

    <!-- 内部高光 -->
    <div class="inner-highlight"></div>
  </button>
</template>

<style scoped lang="scss">
.neon-mode-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  min-height: 44px;
  border: none;
  border-radius: 0.875rem;
  background: transparent;
  cursor: pointer;
  font-family: var(--font-sans);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
  transition: none; // GSAP 接管所有动画
  overflow: hidden;
  outline: none;

  // 基础背景
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(145deg,
      rgba(255, 255, 255, 0.9) 0%,
      rgba(248, 254, 254, 0.85) 50%,
      rgba(255, 255, 255, 0.9) 100%
    );
    border-radius: inherit;
    z-index: 1;
  }

  // 外圈光环
  .neon-ring {
    position: absolute;
    inset: -3px;
    border-radius: inherit;
    background: conic-gradient(
      from 0deg,
      rgba(91, 138, 138, 0.3) 0deg,
      rgba(107, 154, 152, 0.2) 120deg,
      rgba(255, 255, 255, 0.4) 180deg,
      rgba(107, 154, 152, 0.25) 240deg,
      rgba(91, 138, 138, 0.3) 360deg
    );
    opacity: 0.2;
    z-index: 0;
  }

  // 主光晕
  .neon-glow {
    position: absolute;
    inset: -8px;
    border-radius: inherit;
    background: radial-gradient(
      circle at center,
      rgba(91, 138, 138, 0.15) 0%,
      rgba(107, 154, 152, 0.08) 40%,
      transparent 70%
    );
    opacity: 0.3;
    z-index: 0;
    filter: blur(2px);
  }

  // 粒子容器
  .particles-container {
    position: absolute;
    inset: 0;
    pointer-events: none;
    z-index: 0;
    overflow: visible;

    .particle {
      position: absolute;
      top: 50%;
      left: 50%;
      width: 4px;
      height: 4px;
      background: radial-gradient(
        circle,
        rgba(91, 138, 138, 0.8) 0%,
        rgba(107, 154, 152, 0.4) 50%,
        transparent 100%
      );
      border-radius: 50%;
      filter: blur(0.5px);
    }
  }

  // 按钮内容
  .button-content {
    position: relative;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    z-index: 3;
  }

  .button-icon {
    font-size: 1rem;
    transition: none;
  }

  .button-label {
    white-space: nowrap;
    letter-spacing: 0.3px;
  }

  // 内部高光
  .inner-highlight {
    position: absolute;
    top: 1px;
    left: 1px;
    right: 1px;
    height: 40%;
    background: linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.5) 0%,
      rgba(255, 255, 255, 0.1) 50%,
      transparent 100%
    );
    border-radius: 0.75rem 0.75rem 2rem 2rem;
    z-index: 2;
    pointer-events: none;
  }

  // 变体样式 - 增强默认可见性
  &--geek {
    .neon-ring {
      background: conic-gradient(
        from 0deg,
        rgba(0, 255, 127, 0.6) 0deg,
        rgba(0, 255, 0, 0.5) 120deg,
        rgba(127, 255, 0, 0.6) 180deg,
        rgba(0, 255, 127, 0.55) 240deg,
        rgba(0, 255, 127, 0.6) 360deg
      );
      opacity: 0.8; // 增强默认可见性
    }

    .neon-glow {
      background: radial-gradient(
        circle at center,
        rgba(0, 255, 127, 0.35) 0%,
        rgba(0, 255, 0, 0.25) 40%,
        transparent 10%
      );
      opacity: 0.78; // 增强默认可见性
    }

    .particle {
      background: radial-gradient(
        circle,
        rgba(0, 255, 127, 0.95) 0%,
        rgba(0, 255, 0, 0.55) 50%,
        transparent 100%
      );
    }
  }

  &--multimodal {
    .neon-ring {
      background: conic-gradient(
        from 0deg,
        rgba(255, 107, 107, 0.9) 0deg,
        rgba(255, 193, 7, 0.85) 60deg,
        rgba(76, 175, 80, 0.9) 120deg,
        rgba(33, 150, 243, 0.85) 180deg,
        rgba(156, 39, 176, 0.9) 240deg,
        rgba(255, 87, 34, 0.85) 300deg,
        rgba(255, 107, 107, 0.9) 360deg
      );
      opacity: 0.85; // 进一步增强静止状态可见性
    }

    .neon-glow {
      background: radial-gradient(
        circle at center,
        rgba(255, 107, 107, 0.45) 0%,
        rgba(156, 39, 176, 0.33) 30%,
        rgba(33, 150, 243, 0.37) 50%,
        rgba(76, 175, 80, 0.30) 70%,
        rgba(255, 193, 7, 0.20) 85%,
        transparent 95%
      );
      opacity: 0.95; // 进一步增强静止状态光晕
    }

    .particle {
      background: radial-gradient(
        circle,
        rgba(255, 193, 7, 0.95) 0%,
        rgba(76, 175, 80, 0.55) 50%,
        transparent 100%
      );
    }
  }

  // 激活状态
  &--active {
    color: rgba(255, 255, 255, 0.95);

    .neon-ring {
      opacity: 0.6; // 激活时增强光环
    }

    .neon-glow {
      opacity: 0.8; // 激活时增强光晕
    }

    // 默认激活背景（青花瓷）
    &::before {
      background: linear-gradient(145deg,
        rgba(91, 138, 138, 0.95) 0%,
        rgba(107, 154, 152, 0.9) 50%,
        rgba(91, 138, 138, 0.95) 100%
      );
    }

    // 极客模式激活背景
    &.neon-mode-button--geek {
      &::before {
        background: linear-gradient(145deg,
          rgba(0, 255, 127, 0.9) 0%,
          rgba(0, 255, 0, 0.85) 50%,
          rgba(0, 255, 127, 0.9) 100%
        );
      }
    }

    // 多模态模式激活背景 - 彩虹渐变
    &.neon-mode-button--multimodal {
      &::before {
        background: linear-gradient(145deg,
          rgba(255, 107, 107, 0.9) 0%,
          rgba(255, 193, 7, 0.85) 20%,
          rgba(76, 175, 80, 0.9) 40%,
          rgba(33, 150, 243, 0.85) 60%,
          rgba(156, 39, 176, 0.9) 80%,
          rgba(255, 87, 34, 0.85) 100%
        );
      }
    }
  }

  // 禁用状态
  &--disabled {
    cursor: not-allowed;
    color: rgba(139, 157, 157, 0.5);

    &::before {
      background: linear-gradient(145deg,
        rgba(248, 252, 252, 0.6) 0%,
        rgba(240, 244, 244, 0.5) 50%,
        rgba(248, 252, 252, 0.6) 100%
      );
    }

    .neon-ring,
    .neon-glow {
      opacity: 0.1;
    }
  }

  // 悬浮状态（由 GSAP 控制，这里只做备用）
  &:hover:not(&--disabled) {
    .button-icon {
      color: var(--brand-primary);
    }
  }

  // 响应式
  @media (max-width: 768px) {
    padding: 0.625rem 1.25rem;
    min-height: 40px;
    font-size: 0.8125rem;
  }

  @media (max-width: 480px) {
    .button-label {
      display: none;
    }
  }
}
</style>
