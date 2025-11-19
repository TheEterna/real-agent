# 🖌️ 水墨晕染动效系统

## 📅 创建时间：2025-01-14

---

## 🎯 设计理念

**"墨点涟漪，晕染乾坤"**

灵感源自中国传统水墨画的晕染技法，当墨滴坠入水面，激起层层涟漪，墨色如云雾般扩散，时而浓如乌云蔽日，时而淡若轻烟拂柳，在水天一色间绘出千变万化的图案。

---

## 📐 系统架构

### 核心组件

```
src/components/
├── InkModeButton.vue      # 水墨风格模式按钮
└── InkTransition.vue      # 全屏水墨晕染动效层
```

### 技术栈

- **GSAP** - 专业动画时序控制
- **Canvas API** - 水墨粒子系统绘制
- **Vue 3 Composition API** - 响应式状态管理
- **TypeScript** - 类型安全

---

## 🎨 组件详解

### 1️⃣ InkModeButton - 水墨模式按钮

#### 设计特点

**视觉设计**：
- **形状**：圆形（直径 52px），如满月般温润
- **颜色**：深墨色渐变（#2a3a3a → #0f1818）
- **纹理**：细腻的宣纸纹理效果
- **边框**：淡灰色描边（rgba(180, 190, 190, 0.25)）
- **文字**："墨韵"二字，楷体，letter-spacing: 2px

**辅助元素**：
- 4片竹叶剪影，环绕按钮四周
- 竹叶颜色：rgba(60, 80, 80, 0.15)
- 自然飘动动画（2-4秒循环）

#### 交互动效

**悬停效果**（Hover）：
```typescript
gsap.to(button, {
  scale: 1.08,
  filter: 'brightness(0.85)',
  duration: 0.4,
  ease: 'power2.out'
})

// 竹叶颤动
bambooLeaves.forEach((leaf, index) => {
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
```

**激活状态**（Active）：
- 背景亮度提升
- 汉白玉边框光晕出现（青龙旋转效果）
- 持续的 `jadeRotate` 动画（6秒一圈）

#### Props

| 属性 | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| `mode` | `string` | - | 模式标识（如 'command'） |
| `active` | `boolean` | `false` | 是否激活状态 |

#### Events

| 事件 | 参数 | 说明 |
|-----|------|------|
| `click` | `(mode: string, event: MouseEvent)` | 按钮点击事件 |

---

### 2️⃣ InkTransition - 全屏水墨晕染动效

#### 四阶段动画

**阶段1：墨滴初落（0-300ms）**

```typescript
// 创建墨滴
const gradient = ctx.createRadialGradient(x, y, 0, x, y, dropRadius)
gradient.addColorStop(0, 'rgba(20, 30, 30, 1)')
gradient.addColorStop(0.7, 'rgba(30, 40, 40, 0.9)')
gradient.addColorStop(1, 'rgba(40, 50, 50, 0)')

gsap.fromTo({ r: 0, opacity: 0 }, {
  r: 12,
  opacity: 1,
  duration: 0.3,
  ease: 'power2.out'
})
```

**特点**：
- 墨珠从透明渐变为深墨
- 半径从 0 扩展到 12px
- 径向渐变模拟墨汁质感

---

**阶段2：涟漪扩散（300ms-1.5s）**

```typescript
// 创建5圈涟漪
for (let i = 0; i < 5; i++) {
  const ripple = {
    x, y,
    radius: 0,
    maxRadius: 300 + i * 80,
    opacity: 0.6 - i * 0.1,
    thickness: 3 - i * 0.4
  }
  
  gsap.to(ripple, {
    radius: ripple.maxRadius,
    opacity: 0,
    duration: 1.2 + i * 0.3,
    ease: 'power2.out'
  })
}
```

**特点**：
- 5圈涟漪依次扩散（间隔 120ms）
- 每圈不透明度递减
- 涟漪线宽递减（3px → 1px）
- 添加毛边效果（24个随机点）

---

**阶段3：墨色晕染（1.5s-3s）**

```typescript
// 创建150个墨粒子
const particleCount = 150

for (let i = 0; i < particleCount; i++) {
  const angle = (Math.PI * 2 * i) / particleCount + Math.random() * 0.5
  const velocity = 0.5 + Math.random() * 1.5
  
  const particle = {
    x, y,
    vx: Math.cos(angle) * velocity,
    vy: Math.sin(angle) * velocity,
    radius: 2 + Math.random() * 4,
    opacity: 0.4 + Math.random() * 0.3,
    life: 2000 + Math.random() * 2000
  }
  
  particles.push(particle)
}
```

**特点**：
- 150个粒子向四周扩散
- 每个粒子独立的速度和生命周期
- 使用 `requestAnimationFrame` 持续更新
- 添加"飞白"效果（70%概率）

**粒子更新循环**：
```typescript
const updateParticles = () => {
  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i]
    
    // 更新位置
    p.x += p.vx
    p.y += p.vy
    
    // 减速（模拟阻力）
    p.vx *= 0.98
    p.vy *= 0.98
    
    // 生命周期衰减
    p.life -= 16
    const lifeRatio = p.life / p.maxLife
    const currentOpacity = p.opacity * lifeRatio
    
    // 绘制粒子
    ctx.fillStyle = p.color + currentOpacity + ')'
    ctx.arc(p.x, p.y, p.radius * lifeRatio, 0, Math.PI * 2)
    ctx.fill()
  }
}
```

---

**阶段4：全屏覆盖（3s-5s）**

```typescript
const maxRadius = Math.sqrt(centerX ** 2 + centerY ** 2) * 2

gsap.to(inkCircle, {
  radius: maxRadius,
  opacity: 0.95,
  duration: 2,
  ease: 'power1.inOut',
  onUpdate: () => {
    // 绘制扩散的水墨圆
    const gradient = ctx.createRadialGradient(x, y, 0, x, y, inkCircle.radius)
    gradient.addColorStop(0, `rgba(18, 25, 25, ${inkCircle.opacity})`)
    gradient.addColorStop(0.3, `rgba(25, 35, 35, ${inkCircle.opacity * 0.95})`)
    gradient.addColorStop(0.6, `rgba(30, 40, 40, ${inkCircle.opacity * 0.9})`)
    gradient.addColorStop(0.85, `rgba(35, 45, 45, ${inkCircle.opacity * 0.8})`)
    gradient.addColorStop(1, `rgba(40, 50, 50, ${inkCircle.opacity * 0.7})`)
    
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    
    // 当覆盖70%时，绘制山水剪影
    if (inkCircle.radius > maxRadius * 0.7) {
      drawMountainSilhouette()
    }
  }
})
```

**山水剪影**：
- 远山轮廓（随机起伏的曲线）
- 飞鸟剪影（3只，分布在画面上部）

**篆字浮现**（延迟 3.5s）：
```typescript
gsap.fromTo(textRef.value, {
  opacity: 0,
  scale: 0.8,
  filter: 'blur(10px)'
}, {
  opacity: 1,
  scale: 1,
  filter: 'blur(0px)',
  duration: 1.5,
  delay: 3.5,
  ease: 'power2.out'
})
```


---

#### Props

| 属性 | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| `trigger` | `boolean` | `false` | 触发动画开关 |
| `originX` | `number` | `0` | 水墨扩散起点X坐标 |
| `originY` | `number` | `0` | 水墨扩散起点Y坐标 |

#### Events

| 事件 | 参数 | 说明 |
|-----|------|------|
| `complete` | `()` | 动画完成回调（6秒后） |

---

## 🔧 集成使用

### 在 ReActPlus.vue 中的集成

#### 1. 导入组件

```vue
<script setup>
import InkModeButton from '@/components/InkModeButton.vue'
import InkTransition from '@/components/InkTransition.vue'
</script>
```

#### 2. 状态管理

```typescript
// 水墨模式状态
const currentMode = ref<string>('command')
const inkTransitionTrigger = ref(false)
const inkOrigin = ref({ x: 0, y: 0 })
```

#### 3. 事件处理

```typescript
// 水墨模式切换
const handleInkModeClick = (mode: string, event: MouseEvent) => {
  // 记录点击位置
  inkOrigin.value = {
    x: event.clientX,
    y: event.clientY
  }
  
  // 触发水墨晕染动效
  inkTransitionTrigger.value = true
  
  // 3.5秒后切换模式
  setTimeout(() => {
    currentMode.value = mode
    
    notification.success({
      message: '墨韵模式',
      description: `已切换至「${mode === 'command' ? '命令' : mode}」模式`,
      placement: 'topRight',
      duration: 2
    })
  }, 3500)
}

// 动效完成
const handleInkTransitionComplete = () => {
  inkTransitionTrigger.value = false
}
```

#### 4. 模板使用

```vue
<template>
  <div class="react-plus-app">
    <!-- 顶部状态栏 -->
    <div class="top-status-bar">
      <div class="status-left">
        <!-- 水墨模式按钮 -->
        <InkModeButton
          mode="command"
          :active="currentMode === 'command'"
          @click="handleInkModeClick"
        />
        <StatusIndicator :status="taskStatus.value" />
      </div>
    </div>

    <!-- 页面内容 -->
    <!-- ... -->

    <!-- 水墨晕染全屏动效层 -->
    <InkTransition
      :trigger="inkTransitionTrigger"
      :origin-x="inkOrigin.x"
      :origin-y="inkOrigin.y"
      @complete="handleInkTransitionComplete"
    />
  </div>
</template>
```

---

## 🎭 动画参数配置

### 关键时间点

| 阶段 | 时间范围 | 持续时间 | 关键动作 |
|-----|---------|----------|----------|
| 墨滴初落 | 0-300ms | 300ms | 墨珠出现并变大 |
| 涟漪扩散 | 300ms-1.5s | 1.2s | 5圈涟漪依次扩散 |
| 墨色晕染 | 1.5s-3s | 1.5s | 150个粒子自由扩散 |
| 全屏覆盖 | 3s-5s | 2s | 墨色覆盖全屏 + 山水浮现 |
| **总时长** | **0-6s** | **6s** | 完整动画周期 |

### 性能优化

**Canvas 优化**：
```typescript
// 使用 DPR 确保高清显示
const dpr = window.devicePixelRatio || 1
canvas.width = window.innerWidth * dpr
canvas.height = window.innerHeight * dpr
ctx.scale(dpr, dpr)
```

**动画优化**：
```typescript
// 使用 requestAnimationFrame
let animationFrameId: number | null = null

const animate = () => {
  drawRipples()
  updateParticles()
  animationFrameId = requestAnimationFrame(animate)
}

// 清理动画
onComplete: () => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  particles.length = 0
  ripples.length = 0
}
```

**内存管理**：
- 粒子生命周期自动清理
- 动画完成后释放 Canvas 资源
- 使用对象池优化（未来可优化）

---

## 🌟 视觉效果细节

### 色彩系统

**墨色渐变**：
```scss
// 深墨色 → 浅墨色
rgba(18, 25, 25, 0.95)  // 焦墨
rgba(25, 35, 35, 0.90)  // 浓墨
rgba(30, 40, 40, 0.85)  // 重墨
rgba(35, 45, 45, 0.75)  // 淡墨
rgba(40, 50, 50, 0.65)  // 清墨
```

### 纹理效果

**宣纸纹理**：
```scss
background-image: 
  radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.03) 0%, transparent 50%),
  radial-gradient(circle at 70% 60%, rgba(255, 255, 255, 0.02) 0%, transparent 50%),
  radial-gradient(circle at 40% 80%, rgba(255, 255, 255, 0.025) 0%, transparent 50%);
opacity: 0.8;
mix-blend-mode: overlay;
```

**飞白效果**：
```typescript
// 70%概率随机出现
if (Math.random() > 0.7) {
  ctx.fillStyle = p.color + (currentOpacity * 0.3) + ')'
  ctx.fillRect(p.x, p.y, p.radius * 0.5, p.radius * 0.5)
}
```

**毛边效果**：
```typescript
// 涟漪边缘的不规则点
const edgeCount = 24
for (let i = 0; i < edgeCount; i++) {
  const angle = (Math.PI * 2 * i) / edgeCount
  const offset = Math.random() * 3  // 随机偏移
  const px = ripple.x + Math.cos(angle) * (ripple.radius + offset)
  const py = ripple.y + Math.sin(angle) * (ripple.radius + offset)
  
  ctx.fillStyle = `rgba(40, 50, 50, ${ripple.opacity * 0.3})`
  ctx.arc(px, py, 1, 0, Math.PI * 2)
  ctx.fill()
}
```

---

## 📱 响应式设计

### 屏幕适配

```typescript
// Canvas 自动适配窗口大小
window.addEventListener('resize', initCanvas)

// 计算最大覆盖半径
const centerX = canvas.width / dpr / 2
const centerY = canvas.height / dpr / 2
const maxRadius = Math.sqrt(centerX ** 2 + centerY ** 2) * 2
```

### 移动设备优化

- 触摸事件支持（自动获取 `event.clientX/Y`）
- 粒子数量可根据设备性能动态调整（未来优化）
- Canvas 分辨率根据 DPR 自适应

---

## 🎯 未来扩展

### 多模式支持

```typescript
// 未来可添加更多模式
const modes = [
  { key: 'command', label: '墨韵', color: '#2a3a3a' },
  { key: 'creative', label: '丹青', color: '#8b4513' },
  { key: 'analyze', label: '竹韵', color: '#4a7c59' }
]
```

### 动效定制

- 可配置粒子数量
- 可自定义墨色渐变
- 可调整动画速度
- 可添加音效（滴水声、毛笔声）

### 性能监控

```typescript
// 添加 FPS 监控
const fps = ref(60)
const frameTime = ref(16.67)

// 自适应降低粒子数
if (fps.value < 30) {
  particleCount = Math.floor(particleCount * 0.5)
}
```

---

## 🎨 文化内涵

**"墨分五色"**：
- **焦墨**：最浓，如夜空
- **浓墨**：浓重有力
- **重墨**：层次分明
- **淡墨**：轻盈飘逸
- **清墨**：若有若无

**"道法自然"**：
- 取法自然，师法造化
- 水墨扩散如云雾流动，遵循自然规律
- 技法虽繁，意境须简，追求天人合一

**诗意引用**：
> "空山新雨后，天气晚来秋。  
> 明月松间照，清泉石上流。"  
> —— 王维《山居秋暝》

---

## ✅ 总结

### 技术亮点

1. ✅ **GSAP 精确时序控制**：四阶段动画无缝衔接
2. ✅ **Canvas 粒子系统**：150个粒子模拟墨色晕染
3. ✅ **性能优化**：requestAnimationFrame + DPR 适配
4. ✅ **响应式设计**：自动适配不同屏幕尺寸
5. ✅ **文化内涵**：融入中国传统水墨美学

### 视觉效果

- 🎨 深墨色按钮 + 竹叶剪影
- 💧 墨滴→涟漪→晕染→覆盖 完整过程
- 🏔️ 山水剪影 + 飞鸟点缀
- 📜 篆字"道法自然"浮现

### 交互体验

- 悬停时竹叶颤动，按钮微微放大
- 点击后触发 6 秒完整动画
- 3.5秒显示模式切换通知
- 动画完成自动清理资源

---

**创建时间**：2025-01-14  
**版本**：v1.0  
**状态**：✅ 已完成并集成

🖌️✨🐉
