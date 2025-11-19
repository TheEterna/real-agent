<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { gsap } from 'gsap'

interface Props {
  mode: string
  active?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  active: false
})

const emit = defineEmits<{
  click: [mode: string, event: MouseEvent]
}>()

const buttonRef = ref<HTMLElement>()

// 悬停动画
const handleMouseEnter = () => {
  if (!buttonRef.value) return

  // 按钮放大 + 墨色加深
  gsap.to(buttonRef.value, {
    scale: 1.08,
    filter: 'brightness(0.85)',
    duration: 0.4,
    ease: 'power2.out'
  })

  // 竹叶颤动
  const leaves = document.querySelectorAll('.bamboo-leaf')
  leaves.forEach((leaf, index) => {
    gsap.to(leaf, {
      x: Math.sin(index) * 3,
      y: Math.cos(index) * 2,
      rotation: (index % 2 === 0 ? 1 : -1) * 5,
      duration: 0.8,
      ease: 'sine.inOut',
      repeat: 2,
      yoyo: true
    })
  })
}

const handleMouseLeave = () => {
  if (!buttonRef.value) return

  gsap.to(buttonRef.value, {
    scale: 1,
    filter: 'brightness(1)',
    duration: 0.4,
    ease: 'power2.out'
  })

  // 竹叶归位
  const leaves = document.querySelectorAll('.bamboo-leaf')
  leaves.forEach((leaf) => {
    gsap.to(leaf, {
      x: 0,
      y: 0,
      rotation: 0,
      duration: 0.5,
      ease: 'power2.out'
    })
  })
}

const handleClick = (event: MouseEvent) => {
  emit('click', props.mode, event)
}

onMounted(() => {
  // 竹叶自然飘动
  const leaves = document.querySelectorAll('.bamboo-leaf')
  leaves.forEach((leaf, index) => {
    gsap.to(leaf, {
      y: `+=${Math.random() * 4 - 2}`,
      rotation: `+=${Math.random() * 10 - 5}`,
      duration: 2 + Math.random() * 2,
      ease: 'sine.inOut',
      repeat: -1,
      yoyo: true,
      delay: index * 0.3
    })
  })
})
</script>

<template>
  <div class="ink-mode-button-wrapper">


    <!-- 主按钮 -->
    <button
      ref="buttonRef"
      class="ink-button"
      :class="{ active }"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
      @click="handleClick($event)"
    >
      <!-- 宣纸纹理层 -->
      <div class="paper-texture"></div>


      <!-- 汉白玉边框光晕 -->
      <div class="jade-border"></div>
    </button>
  </div>
</template>


<style scoped lang="scss">

.bamboo-leaves {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;

  .bamboo-leaf {
    fill: rgba(60, 80, 80, 0.15);
    filter: blur(0.5px);
    opacity: 0.6;
    transition: all 0.3s ease;
  }
}

.ink-button {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(
    135deg,
    #2a3a3a 0%,
    #1a2525 50%,
    #0f1818 100%
  );
  color: rgba(255, 255, 255, 0.92);
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;

  /* 淡灰色描边 */
  box-shadow:
    0 0 0 1.5px rgba(180, 190, 190, 0.25),
    0 4px 12px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);

  /* 定位到包裹器中心 */
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);

  &:hover {
    box-shadow:
      0 0 0 2px rgba(180, 190, 190, 0.4),
      0 6px 20px rgba(0, 0, 0, 0.4),
      inset 0 1px 0 rgba(255, 255, 255, 0.15);
  }

  &.active {
    background: linear-gradient(
      135deg,
      #3a4a4a 0%,
      #2a3535 50%,
      #1f2828 100%
    );
    box-shadow:
      0 0 0 2px rgba(107, 154, 152, 0.5),
      0 0 16px rgba(107, 154, 152, 0.3),
      0 6px 20px rgba(0, 0, 0, 0.4),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);
  }

  &:active {
    transform: translate(-50%, -50%) scale(0.95);
  }
}

/* 宣纸纹理 */
.paper-texture {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 70% 60%, rgba(255, 255, 255, 0.02) 0%, transparent 50%),
    radial-gradient(circle at 40% 80%, rgba(255, 255, 255, 0.025) 0%, transparent 50%);
  opacity: 0.8;
  pointer-events: none;
  mix-blend-mode: overlay;
}

/* 行书文字 */
.ink-text {
  position: relative;
  z-index: 2;
  font-family: 'KaiTi', 'STKaiti', '楷体', serif;
  letter-spacing: 2px;
  text-shadow:
    0 1px 2px rgba(0, 0, 0, 0.5),
    0 0 8px rgba(255, 255, 255, 0.1);
}

/* 汉白玉边框光晕 */
.jade-border {
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  opacity: 0;
  background: conic-gradient(
    from 0deg,
    rgba(107, 154, 152, 0.4) 0deg,
    rgba(91, 138, 138, 0.2) 90deg,
    rgba(255, 255, 255, 0.3) 180deg,
    rgba(91, 138, 138, 0.2) 270deg,
    rgba(107, 154, 152, 0.4) 360deg
  );
  transition: opacity 0.4s ease;
  z-index: -1;

  .ink-button.active & {
    opacity: 0.8;
    animation: jadeRotate 6s linear infinite;
  }
}

@keyframes jadeRotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
