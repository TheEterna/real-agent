<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { gsap } from 'gsap'

interface Props {
  active?: boolean
  icon?: any
  label?: string
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  active: false,
  label: 'label',
  disabled: false
})

interface Emits {
  click: []
}

const emit = defineEmits<Emits>()

const buttonRef = ref<HTMLButtonElement>()
const matrixRef = ref<HTMLDivElement>()
const scanlineRef = ref<HTMLDivElement>()
const codeCharsRef = ref<HTMLDivElement>()
const terminalCursorRef = ref<HTMLDivElement>()

let hoverTimeline: gsap.core.Timeline | null = null
let breathingAnimation: gsap.core.Tween | null = null
let matrixAnimation: gsap.core.Tween | null = null
let scanlineAnimation: gsap.core.Tween | null = null
let cursorAnimation: gsap.core.Tween | null = null
let codeAnimations: gsap.core.Timeline[] = []

const codeChars = ['{', '}', '<', '>', '/', '\\', '=', ';', '(', ')', '[', ']', '$', '#', '@', '&', '*', '+', '-', '~', '`', '|', '0', '1']
const hackTexts = ['sudo', 'npm', 'git', 'vim', 'cd', 'ls', 'cat', 'grep', 'ssh', 'curl', '>', '$', '#']

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

const createFlyingChars = () => {
  if (!codeCharsRef.value) return

  codeCharsRef.value.innerHTML = ''
  codeAnimations.forEach(anim => anim.kill())
  codeAnimations = []

  for (let i = 0; i < 8; i++) {
    const char = document.createElement('div')
    char.className = 'code-char'
    char.textContent = codeChars[Math.floor(Math.random() * codeChars.length)]
    codeCharsRef.value.appendChild(char)

    const angle = (i * 45) + Math.random() * 20 - 10
    const radius = 35 + Math.random() * 15
    const x = Math.cos(angle * Math.PI / 180) * radius
    const y = Math.sin(angle * Math.PI / 180) * radius

    gsap.set(char, {
      x: 0,
      y: 0,
      scale: 0,
      opacity: 0,
      rotation: 0
    })

    const charAnimation = gsap.timeline({ repeat: -1 })
    charAnimation.to(char, {
      x,
      y,
      scale: 0.8 + Math.random() * 0.4,
      opacity: 0.7,
      rotation: Math.random() * 360,
      duration: 1.5 + Math.random() * 1,
      ease: "power2.out"
    })
    .to(char, {
      x: x * 1.5,
      y: y * 1.5,
      scale: 0,
      opacity: 0,
      rotation: (Math.random() * 360) + 180,
      duration: 1 + Math.random() * 0.5,
      ease: "power2.in"
    })

    codeAnimations.push(charAnimation)
  }
}

const setupMatrixEffect = () => {
  if (!matrixRef.value) return

  matrixAnimation = gsap.to(matrixRef.value, {
    backgroundPosition: '0% 100%',
    duration: 8,
    ease: 'none',
    repeat: -1
  })
}

const setupScanlineEffect = () => {
  if (!scanlineRef.value) return

  scanlineAnimation = gsap.to(scanlineRef.value, {
    y: '100%',
    duration: 2,
    ease: 'none',
    repeat: -1
  })
}

const setupCursorBlink = () => {
  if (!terminalCursorRef.value) return

  cursorAnimation = gsap.to(terminalCursorRef.value, {
    opacity: 0,
    duration: 0.5,
    ease: 'none',
    repeat: -1,
    yoyo: true
  })
}

const setupHoverEffects = () => {
  if (!buttonRef.value) return

  const button = buttonRef.value

  button.addEventListener('mouseenter', () => {
    if (props.disabled) return

    hoverTimeline = gsap.timeline()

    hoverTimeline.to(button, {
      y: -2,
      scale: 1.02,
      duration: 0.3,
      ease: "back.out(1.5)"
    }, 0)

    createFlyingChars()

    if (scanlineRef.value) {
      gsap.to(scanlineRef.value, {
        opacity: 0.6,
        duration: 0.3
      })
    }
  })

  button.addEventListener('mouseleave', () => {
    if (props.disabled) return

    if (hoverTimeline) {
      hoverTimeline.kill()
    }

    gsap.to(button, {
      y: 0,
      scale: 1,
      duration: 0.3,
      ease: "power2.out"
    })

    codeAnimations.forEach(anim => anim.kill())
    codeAnimations = []
    if (codeCharsRef.value) {
      codeCharsRef.value.innerHTML = ''
    }

    if (scanlineRef.value) {
      gsap.to(scanlineRef.value, {
        opacity: props.active ? 0.3 : 0.1,
        duration: 0.3
      })
    }
  })
}

const setupBreathingEffect = () => {
  if (!props.active || !buttonRef.value) return

  const button = buttonRef.value
  breathingAnimation = gsap.to(button, {
    boxShadow: '0 0 20px rgba(0, 255, 0, 0.4), inset 0 0 20px rgba(0, 255, 0, 0.1)',
    duration: 2,
    ease: "sine.inOut",
    repeat: -1,
    yoyo: true
  })
}

onMounted(() => {
  setupHoverEffects()
  setupMatrixEffect()
  setupScanlineEffect()
  setupCursorBlink()

  if (props.active) {
    setupBreathingEffect()
  }
})

onUnmounted(() => {
  if (hoverTimeline) hoverTimeline.kill()
  if (breathingAnimation) breathingAnimation.kill()
  if (matrixAnimation) matrixAnimation.kill()
  if (scanlineAnimation) scanlineAnimation.kill()
  if (cursorAnimation) cursorAnimation.kill()
  codeAnimations.forEach(anim => anim.kill())
})

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
      'geek-mode-button',
      {
        'geek-mode-button--active': active,
        'geek-mode-button--disabled': disabled
      }
    ]"
    :disabled="disabled"
    @click="handleClick"
  >
    <div ref="matrixRef" class="matrix-bg"></div>

    <div ref="scanlineRef" class="scanline"></div>

    <div ref="codeCharsRef" class="code-chars-container"></div>

    <div class="button-content">
      <component v-if="icon" :is="icon" class="button-icon" />
      <span v-if="label" class="button-label">
        {{ label }}
        <span ref="terminalCursorRef" class="terminal-cursor">_</span>
      </span>
    </div>

    <div class="terminal-border"></div>
  </button>
</template>

<style scoped lang="scss">
.geek-mode-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  min-height: 44px;
  border: none;
  border-radius: 0.375rem;
  background: transparent;
  cursor: pointer;
  font-family: 'Courier New', 'JetBrains Mono', monospace;
  font-size: 0.875rem;
  font-weight: 600;
  color: #00ff00;
  transition: none;
  overflow: hidden;
  outline: none;
  text-transform: uppercase;
  letter-spacing: 1px;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(145deg,
      rgba(0, 0, 0, 0.9) 0%,
      rgba(10, 20, 10, 0.85) 50%,
      rgba(0, 0, 0, 0.9) 100%
    );
    border-radius: inherit;
    z-index: 1;
  }

  .matrix-bg {
    position: absolute;
    inset: 0;
    background: linear-gradient(
      0deg,
      transparent 0%,
      rgba(0, 255, 0, 0.04) 20%,
      rgba(0, 255, 0, 0.06) 40%,
      rgba(0, 255, 0, 0.04) 60%,
      transparent 100%
    );
    background-size: 100% 200%;
    opacity: 0.5;
    z-index: 2;
    transition: opacity 0.3s ease;
  }

  .scanline {
    position: absolute;
    inset: 0;
    background: linear-gradient(
      180deg,
      transparent 0%,
      transparent 45%,
      rgba(0, 255, 0, 0.7) 50%,
      transparent 55%,
      transparent 100%
    );
    opacity: 0.2;
    z-index: 3;
    pointer-events: none;
    transition: opacity 0.3s ease;
  }

  .code-chars-container {
    position: absolute;
    inset: 0;
    pointer-events: none;
    z-index: 2;
    overflow: visible;

    .code-char {
      position: absolute;
      top: 50%;
      left: 50%;
      font-family: 'Courier New', monospace;
      font-size: 12px;
      font-weight: bold;
      color: #00ff00;
      text-shadow: 0 0 6px #00ff00;
      pointer-events: none;
    }
  }

  .button-content {
    position: relative;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    z-index: 4;
  }

  .button-icon {
    font-size: 1rem;
    filter: drop-shadow(0 0 4px currentColor);
  }

  .button-label {
    white-space: nowrap;
    position: relative;
    text-shadow: 0 0 8px currentColor;
  }

  .terminal-cursor {
    animation: none;
    margin-left: 2px;
    color: #00ff00;
  }

  .terminal-border {
    position: absolute;
    inset: 1px;
    border: 1px solid rgba(0, 255, 0, 0.25);
    border-radius: 2px;
    z-index: 2;
    pointer-events: none;
    transition: all 0.3s ease;
    box-shadow: 0 0 4px rgba(0, 255, 0, 0.1);

    &::before {
      content: '';
      position: absolute;
      top: -1px;
      left: -1px;
      right: -1px;
      bottom: -1px;
      border: 1px solid rgba(0, 255, 0, 0.08);
      border-radius: 3px;
      transition: all 0.3s ease;
    }

    &::after {
      content: '';
      position: absolute;
      top: 2px;
      left: 2px;
      width: 6px;
      height: 6px;
      border-left: 1px solid rgba(0, 255, 0, 0.3);
      border-top: 1px solid rgba(0, 255, 0, 0.3);
      transition: all 0.3s ease;
    }
  }

  &:hover:not(&--disabled) {
    .terminal-border {
      border-color: rgba(0, 255, 0, 0.5);
      box-shadow: 0 0 12px rgba(0, 255, 0, 0.2);

      &::before {
        border-color: rgba(0, 255, 0, 0.15);
      }

      &::after {
        border-left-color: rgba(0, 255, 0, 0.7);
        border-top-color: rgba(0, 255, 0, 0.7);
        box-shadow: 0 0 6px rgba(0, 255, 0, 0.4);
      }
    }

    .matrix-bg {
      opacity: 0.8;
    }

    .scanline {
      opacity: 0.4;
    }
  }

  &--active {
    color: #00ff00;
    text-shadow: 0 0 12px #00ff00;

    /* 增强的活跃状态背景 */
    &::before {
      background: linear-gradient(145deg,
        rgba(0, 50, 0, 0.98) 0%,
        rgba(10, 80, 10, 0.95) 50%,
        rgba(0, 50, 0, 0.98) 100%
      );
      box-shadow:
        inset 0 0 20px rgba(0, 255, 0, 0.15),
        0 0 20px rgba(0, 255, 0, 0.2);
    }

    .terminal-border {
      border-color: rgba(0, 255, 0, 0.9);
      box-shadow:
        0 0 20px rgba(0, 255, 0, 0.4),
        inset 0 0 12px rgba(0, 255, 0, 0.15);

      &::before {
        border-color: rgba(0, 255, 0, 0.4);
      }

      &::after {
        border-left-color: rgba(0, 255, 0, 0.9);
        border-top-color: rgba(0, 255, 0, 0.9);
        box-shadow: 0 0 12px rgba(0, 255, 0, 0.7);
        width: 10px;
        height: 10px;
      }
    }

    .matrix-bg {
      opacity: 1;
      background: linear-gradient(
        0deg,
        transparent 0%,
        rgba(0, 255, 0, 0.12) 20%,
        rgba(0, 255, 0, 0.18) 40%,
        rgba(0, 255, 0, 0.12) 60%,
        transparent 100%
      );
    }

    .scanline {
      opacity: 0.7;
    }

    /* 增强的发光外边框 */
    &::after {
      content: '';
      position: absolute;
      inset: -3px;
      border-radius: inherit;
      background: linear-gradient(45deg,
        rgba(0, 255, 0, 0.2) 0%,
        transparent 25%,
        rgba(0, 255, 0, 0.15) 50%,
        transparent 75%,
        rgba(0, 255, 0, 0.2) 100%
      );
      background-size: 200% 200%;
      animation: terminalActiveGlow 2s ease-in-out infinite;
      z-index: 0;
      pointer-events: none;
    }
  }

  &--disabled {
    cursor: not-allowed;
    color: rgba(0, 100, 0, 0.3);

    &::before {
      background: linear-gradient(145deg,
        rgba(20, 20, 20, 0.6) 0%,
        rgba(30, 30, 30, 0.5) 50%,
        rgba(20, 20, 20, 0.6) 100%
      );
    }

    .terminal-border {
      border-color: rgba(0, 100, 0, 0.1);
    }

    .matrix-bg,
    .scanline {
      opacity: 0.05;
    }
  }

  .terminal-flash {
    position: absolute;
    inset: 0;
    background: rgba(0, 255, 0, 0.3);
    z-index: 5;
    pointer-events: none;
  }

  @media (max-width: 768px) {
    padding: 0.625rem 1.25rem;
    min-height: 40px;
    font-size: 0.8125rem;
  }

  @media (max-width: 480px) {
    .button-label {
      display: none;
    }

    .terminal-cursor {
      display: none;
    }
  }
}

@keyframes terminalActiveGlow {
  0% {
    background-position: 0% 0%;
    opacity: 0.3;
  }
  50% {
    background-position: 100% 100%;
    opacity: 0.6;
  }
  100% {
    background-position: 0% 0%;
    opacity: 0.3;
  }
}
</style>
