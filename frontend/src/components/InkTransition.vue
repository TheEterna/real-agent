<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { gsap } from 'gsap'

interface Props {
  trigger: boolean
  originX?: number
  originY?: number
}

const props = withDefaults(defineProps<Props>(), {
  trigger: false,
  originX: 0,
  originY: 0
})

const emit = defineEmits<{
  complete: []
}>()

const canvasRef = ref<HTMLCanvasElement>()
const overlayRef = ref<HTMLElement>()
const textRef = ref<HTMLElement>()
const isAnimating = ref(false)

let ctx: CanvasRenderingContext2D | null = null
let animationFrameId: number | null = null

// 粒子系统 - 用于水墨扩散
interface InkParticle {
  x: number
  y: number
  vx: number
  vy: number
  radius: number
  opacity: number
  life: number
  maxLife: number
  color: string
}

const particles: InkParticle[] = []

// 涟漪系统
interface Ripple {
  x: number
  y: number
  radius: number
  maxRadius: number
  opacity: number
  thickness: number
}

const ripples: Ripple[] = []

// 初始化 Canvas
const initCanvas = () => {
  if (!canvasRef.value) return

  const canvas = canvasRef.value
  const dpr = window.devicePixelRatio || 1

  canvas.width = window.innerWidth * dpr
  canvas.height = window.innerHeight * dpr
  canvas.style.width = `${window.innerWidth}px`
  canvas.style.height = `${window.innerHeight}px`

  ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.scale(dpr, dpr)
  }
}

// 创建墨滴
const createInkDrop = (x: number, y: number) => {
  if (!ctx) return

  const dropRadius = 12

  // 绘制墨滴
  const gradient = ctx.createRadialGradient(x, y, 0, x, y, dropRadius)
  gradient.addColorStop(0, 'rgba(20, 30, 30, 1)')
  gradient.addColorStop(0.7, 'rgba(30, 40, 40, 0.9)')
  gradient.addColorStop(1, 'rgba(40, 50, 50, 0)')

  ctx.fillStyle = gradient
  ctx.beginPath()
  ctx.arc(x, y, dropRadius, 0, Math.PI * 2)
  ctx.fill()

  // 墨滴动画
  gsap.fromTo(
    { r: 0, opacity: 0 },
    {
      r: dropRadius,
      opacity: 1,
      duration: 0.3,
      ease: 'power2.out',
      onUpdate: function() {
        if (!ctx) return
        ctx.clearRect(x - 20, y - 20, 40, 40)

        const grad = ctx.createRadialGradient(x, y, 0, x, y, this.targets()[0].r)
        grad.addColorStop(0, `rgba(20, 30, 30, ${this.targets()[0].opacity})`)
        grad.addColorStop(0.7, `rgba(30, 40, 40, ${this.targets()[0].opacity * 0.9})`)
        grad.addColorStop(1, `rgba(40, 50, 50, 0)`)

        ctx.fillStyle = grad
        ctx.beginPath()
        ctx.arc(x, y, this.targets()[0].r, 0, Math.PI * 2)
        ctx.fill()
      }
    }
  )
}

// 创建涟漪
const createRipples = (x: number, y: number) => {
  const rippleCount = 5

  for (let i = 0; i < rippleCount; i++) {
    setTimeout(() => {
      const ripple: Ripple = {
        x,
        y,
        radius: 0,
        maxRadius: 300 + i * 80,
        opacity: 0.6 - i * 0.1,
        thickness: 3 - i * 0.4
      }
      ripples.push(ripple)

      // 涟漪扩散动画
      gsap.to(ripple, {
        radius: ripple.maxRadius,
        opacity: 0,
        duration: 1.2 + i * 0.3,
        ease: 'power2.out',
        onComplete: () => {
          const index = ripples.indexOf(ripple)
          if (index > -1) ripples.splice(index, 1)
        }
      })
    }, i * 120)
  }
}

// 创建水墨粒子
const createInkParticles = (x: number, y: number) => {
  const particleCount = 150

  for (let i = 0; i < particleCount; i++) {
    const angle = (Math.PI * 2 * i) / particleCount + Math.random() * 0.5
    const velocity = 0.5 + Math.random() * 1.5
    const life = 2000 + Math.random() * 2000

    const particle: InkParticle = {
      x,
      y,
      vx: Math.cos(angle) * velocity,
      vy: Math.sin(angle) * velocity,
      radius: 2 + Math.random() * 4,
      opacity: 0.4 + Math.random() * 0.3,
      life,
      maxLife: life,
      color: `rgba(${20 + Math.random() * 30}, ${30 + Math.random() * 30}, ${30 + Math.random() * 30}, `
    }

    particles.push(particle)
  }
}

// 绘制涟漪
const drawRipples = () => {
  if (!ctx) return

  ripples.forEach(ripple => {
    ctx!.strokeStyle = `rgba(40, 50, 50, ${ripple.opacity})`
    ctx!.lineWidth = ripple.thickness
    ctx!.beginPath()
    ctx!.arc(ripple.x, ripple.y, ripple.radius, 0, Math.PI * 2)
    ctx!.stroke()

    // 添加毛边效果
    const edgeCount = 24
    for (let i = 0; i < edgeCount; i++) {
      const angle = (Math.PI * 2 * i) / edgeCount
      const offset = Math.random() * 3
      const px = ripple.x + Math.cos(angle) * (ripple.radius + offset)
      const py = ripple.y + Math.sin(angle) * (ripple.radius + offset)

      ctx!.fillStyle = `rgba(40, 50, 50, ${ripple.opacity * 0.3})`
      ctx!.beginPath()
      ctx!.arc(px, py, 1, 0, Math.PI * 2)
      ctx!.fill()
    }
  })
}

// 更新和绘制粒子
const updateParticles = () => {
  if (!ctx) return

  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i]

    // 更新位置
    p.x += p.vx
    p.y += p.vy

    // 减速
    p.vx *= 0.98
    p.vy *= 0.98

    // 生命周期
    p.life -= 16
    if (p.life <= 0) {
      particles.splice(i, 1)
      continue
    }

    // 绘制粒子
    const lifeRatio = p.life / p.maxLife
    const currentOpacity = p.opacity * lifeRatio

    ctx.fillStyle = p.color + currentOpacity + ')'
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.radius * lifeRatio, 0, Math.PI * 2)
    ctx.fill()

    // 添加飞白效果
    if (Math.random() > 0.7) {
      ctx.fillStyle = p.color + (currentOpacity * 0.3) + ')'
      ctx.fillRect(p.x, p.y, p.radius * 0.5, p.radius * 0.5)
    }
  }
}

// 全屏水墨覆盖
const fullScreenInk = (x: number, y: number) => {
  if (!ctx || !canvasRef.value) return

  const canvas = canvasRef.value
  const centerX = canvas.width / (window.devicePixelRatio || 1) / 2
  const centerY = canvas.height / (window.devicePixelRatio || 1) / 2
  const maxRadius = Math.sqrt(centerX ** 2 + centerY ** 2) * 2

  const inkCircle = { radius: 0, opacity: 0.85 }

  gsap.to(inkCircle, {
    radius: maxRadius,
    opacity: 0.95,
    duration: 2,
    ease: 'power1.inOut',
    onUpdate: () => {
      if (!ctx) return

      // 清空画布
      ctx.clearRect(0, 0, canvas.width, canvas.height)

      // 绘制扩散的水墨
      const gradient = ctx.createRadialGradient(x, y, 0, x, y, inkCircle.radius)
      gradient.addColorStop(0, `rgba(18, 25, 25, ${inkCircle.opacity})`)
      gradient.addColorStop(0.3, `rgba(25, 35, 35, ${inkCircle.opacity * 0.95})`)
      gradient.addColorStop(0.6, `rgba(30, 40, 40, ${inkCircle.opacity * 0.9})`)
      gradient.addColorStop(0.85, `rgba(35, 45, 45, ${inkCircle.opacity * 0.8})`)
      gradient.addColorStop(1, `rgba(40, 50, 50, ${inkCircle.opacity * 0.7})`)

      ctx.fillStyle = gradient
      ctx.fillRect(0, 0, canvas.width, canvas.height)

      // 添加山水纹理
      if (inkCircle.radius > maxRadius * 0.7) {
        drawMountainSilhouette()
      }
    }
  })
}

// 绘制山水剪影
const drawMountainSilhouette = () => {
  if (!ctx || !canvasRef.value) return

  const canvas = canvasRef.value
  const width = canvas.width / (window.devicePixelRatio || 1)
  const height = canvas.height / (window.devicePixelRatio || 1)

  ctx.fillStyle = 'rgba(10, 15, 15, 0.3)'
  ctx.beginPath()
  ctx.moveTo(0, height * 0.7)

  for (let x = 0; x < width; x += 20) {
    const y = height * 0.7 - Math.random() * 80 - Math.sin(x / 100) * 40
    ctx.lineTo(x, y)
  }

  ctx.lineTo(width, height)
  ctx.lineTo(0, height)
  ctx.closePath()
  ctx.fill()

  // 飞鸟剪影
  for (let i = 0; i < 3; i++) {
    const birdX = width * (0.3 + i * 0.2)
    const birdY = height * 0.3 + Math.random() * 100
    drawBird(birdX, birdY)
  }
}

// 绘制飞鸟
const drawBird = (x: number, y: number) => {
  if (!ctx) return

  ctx.strokeStyle = 'rgba(0, 0, 0, 0.4)'
  ctx.lineWidth = 1.5
  ctx.lineCap = 'round'

  // 左翼
  ctx.beginPath()
  ctx.moveTo(x, y)
  ctx.quadraticCurveTo(x - 8, y - 5, x - 12, y - 3)
  ctx.stroke()

  // 右翼
  ctx.beginPath()
  ctx.moveTo(x, y)
  ctx.quadraticCurveTo(x + 8, y - 5, x + 12, y - 3)
  ctx.stroke()
}

// 动画循环
const animate = () => {
  if (!ctx || !canvasRef.value) return

  drawRipples()
  updateParticles()

  animationFrameId = requestAnimationFrame(animate)
}

// 显示篆字
const showSealText = () => {
  if (!textRef.value) return

  gsap.fromTo(
    textRef.value,
    {
      opacity: 0,
      scale: 0.8,
      filter: 'blur(10px)'
    },
    {
      opacity: 1,
      scale: 1,
      filter: 'blur(0px)',
      duration: 1.5,
      delay: 3.5,
      ease: 'power2.out'
    }
  )
}

// 主动画流程
const playInkAnimation = () => {
  if (isAnimating.value) return
  isAnimating.value = true

  if (!overlayRef.value) return

  // 显示覆盖层
  overlayRef.value.style.display = 'block'

  initCanvas()

  const originX = props.originX || window.innerWidth / 2
  const originY = props.originY || window.innerHeight / 2

  // 阶段1: 墨滴初落 (0-300ms)
  setTimeout(() => {
    createInkDrop(originX, originY)
  }, 0)

  // 阶段2: 涟漪扩散 (300ms-1.5s)
  setTimeout(() => {
    createRipples(originX, originY)
    animate()
  }, 300)

  // 阶段3: 墨色晕染 (1.5s-3s)
  setTimeout(() => {
    createInkParticles(originX, originY)
  }, 1500)

  // 阶段4: 全屏覆盖 (3s-5s)
  setTimeout(() => {
    fullScreenInk(originX, originY)
    showSealText()
  }, 3000)

  // 动画完成
  setTimeout(() => {
    isAnimating.value = false
    emit('complete')

    // 清理
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId)
    }
    particles.length = 0
    ripples.length = 0
  }, 6000)
}

// 监听触发
watch(() => props.trigger, (newVal) => {
  if (newVal) {
    playInkAnimation()
  }
})

onMounted(() => {
  window.addEventListener('resize', initCanvas)
})
</script>

<template>
  <div ref="overlayRef" class="ink-transition-overlay">
    <!-- Canvas 水墨画布 -->
    <canvas ref="canvasRef" class="ink-canvas"></canvas>

  </div>
</template>

<style scoped lang="scss">
.ink-transition-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: none;
  background: transparent;
  pointer-events: none;
}

.ink-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.seal-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  gap: 24px;
  opacity: 0;

  .seal-char {
    font-size: 72px;
    font-weight: 700;
    color: rgba(255, 255, 255, 0.85);
    font-family: 'STZhongsong', 'FangSong', '仿宋', serif;
    text-shadow:
      0 2px 8px rgba(0, 0, 0, 0.8),
      0 0 24px rgba(255, 255, 255, 0.2);
    letter-spacing: 12px;
  }
}
</style>
