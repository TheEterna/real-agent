# 🏺 青花瓷冰裂纹 Loading - "银瓶乍破"效果

## 📅 创建时间：2025-01-14

---

## 🎯 设计灵感

> **"银瓶乍破水浆迸，铁骑突出刀枪鸣"** —— 白居易《琵琶行》

这个 Loading 效果灵感来源于：
- **青花瓷釉面开片** - 自然形成的冰裂纹理
- **冰裂纹美学** - 中国传统瓷器的独特美感
- **破碎之美** - 如碎玉落叠，如冰壶炸裂

---

## 🎨 视觉效果

### 阶段分解

```
阶段1 (0-0.2s)    → 中心点闪现
阶段2 (0.2-1s)    → 主裂纹快速扩散（8条放射线）
阶段3 (0.8-1.5s)  → 次级裂纹分支（16条分支）
阶段4 (1.2-2s)    → 细微裂纹网状扩展（24条网纹）
阶段5 (1.5-2.5s)  → 闪光点依次闪烁（20个光点）
阶段6 (2.5-2.8s)  → 整体颤动（震动5次）
阶段7 (2.8-4s)    → 碎片爆裂四散（120个碎片）
阶段8 (3.5-4.5s)  → 整体淡出
```

### 视觉特征

| 元素 | 描述 | 颜色 | 动画 |
|------|------|------|------|
| **青瓷底纹** | 径向渐变圆形背景 | 青花瓷色 | 静态 |
| **主裂纹** | 8条放射状主干 | 青色 60% 透明 | strokeDashoffset |
| **次级裂纹** | 16条分支裂纹 | 青色 40% 透明 | 随机扩展 |
| **细微裂纹** | 24条网状连接 | 青色 25% 透明 | 细密填充 |
| **闪光点** | 20个光点 | 白色 90% 透明 | 闪烁 + 缩放 |
| **碎片** | 120个不规则碎片 | 青色半透明 | 爆裂四散 |

---

## 🔧 技术实现

### 核心技术栈

1. **SVG 路径绘制** - 绘制自然的裂纹路径
2. **GSAP Timeline** - 精确编排 8 个动画阶段
3. **Web Audio API** - 生成破裂音效
4. **Vue 3 Composition API** - 响应式状态管理

### 关键算法

#### 1️⃣ **裂纹路径生成算法**

```typescript
const generateCrackPath = (
  startX: number,      // 起始点X
  startY: number,      // 起始点Y
  angle: number,       // 初始角度
  length: number,      // 总长度
  segments: number,    // 分段数
  chaos: number        // 混乱度 (0-1)
): string => {
  let path = `M ${startX} ${startY}`
  let currentX = startX
  let currentY = startY
  let currentAngle = angle

  const segmentLength = length / segments

  for (let i = 0; i < segments; i++) {
    // 🔥 关键：添加随机偏移，模拟自然裂纹
    const angleOffset = (Math.random() - 0.5) * chaos
    currentAngle += angleOffset

    // 计算下一个点
    const nextX = currentX + Math.cos(currentAngle) * segmentLength
    const nextY = currentY + Math.sin(currentAngle) * segmentLength

    // 🔥 使用贝塞尔曲线连接，更自然
    const controlX = currentX + Math.cos(currentAngle - 0.2) * segmentLength * 0.5
    const controlY = currentY + Math.sin(currentAngle - 0.2) * segmentLength * 0.5

    path += ` Q ${controlX} ${controlY} ${nextX} ${nextY}`

    currentX = nextX
    currentY = nextY
  }

  return path
}
```

**参数说明**：
- `chaos` 越大，裂纹越不规则
- `segments` 越多，裂纹越平滑
- 使用二次贝塞尔曲线 `Q` 命令，让裂纹更自然

**效果**：
```
chaos = 0.4  → 主裂纹（较直）
chaos = 0.6  → 次级裂纹（略弯）
chaos = 0.8  → 细微裂纹（很弯）
```

---

#### 2️⃣ **GSAP Timeline 编排**

```javascript
const playShatterAnimation = () => {
  const tl = gsap.timeline()

  // 阶段1: 中心点
  tl.fromTo('.crack-origin', { scale: 0 }, { scale: 1, duration: 0.2 })

  // 阶段2: 主裂纹（strokeDashoffset 动画）
  tl.fromTo('.main-crack',
    { 
      strokeDashoffset: function(index, target) {
        return target.getTotalLength()  // 🔥 获取路径总长度
      }
    },
    {
      strokeDashoffset: 0,  // 🔥 绘制到 0，完整显示
      duration: 0.6,
      stagger: 0.05,        // 🔥 依次出现
      onStart: () => playcrackSound(0.3)
    },
    '+=0.1'
  )

  // ... 其他阶段
}
```

**关键技术**：
- `getTotalLength()` - 获取 SVG 路径长度
- `strokeDashoffset` - 控制路径显示部分
- `stagger` - 交错动画，营造"依次扩散"效果
- Timeline 相对定位 (`+=`, `-=`) - 精确控制时序

---

#### 3️⃣ **Web Audio API 音效生成**

```typescript
const generateCrackSound = (volume: number) => {
  const audioContext = new AudioContext()
  
  // 1. 创建白噪声缓冲区
  const bufferSize = audioContext.sampleRate * 0.1 // 100ms
  const buffer = audioContext.createBuffer(1, bufferSize, audioContext.sampleRate)
  const output = buffer.getChannelData(0)
  
  // 2. 填充随机噪声
  for (let i = 0; i < bufferSize; i++) {
    output[i] = Math.random() * 2 - 1
  }
  
  // 3. 创建高通滤波器（模拟高频破裂声）
  const filter = audioContext.createBiquadFilter()
  filter.type = 'highpass'
  filter.frequency.value = 2000  // 🔥 高频
  
  // 4. 音量衰减
  const gainNode = audioContext.createGain()
  gainNode.gain.value = volume * 0.3
  gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.1)
  
  // 5. 连接并播放
  source.connect(filter).connect(gainNode).connect(audioContext.destination)
  source.start(0)
}
```

**效果**：
- 白噪声 → 模拟破裂的杂音
- 高通滤波器 → 突出高频"脆响"
- 指数衰减 → 模拟声音快速消失

---

## 📦 使用方法

### 1. 基础使用

```vue
<template>
  <div>
    <!-- 你的页面内容 -->
    <YourContent />
    
    <!-- 冰裂纹 Loading -->
    <CeladonCrackleLoading
      :visible="isLoading"
      title="银瓶乍破"
      subtitle="..."
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CeladonCrackleLoading from '@/components/loading/CeladonCrackleLoading.vue'

const isLoading = ref(false)

// Agent 切换时显示
const switchAgent = async () => {
  isLoading.value = true
  
  // 模拟 Agent 切换
  await new Promise(resolve => setTimeout(resolve, 4500))
  
  isLoading.value = false
}
</script>
```

---

### 2. 自定义文本

```vue
<CeladonCrackleLoading
  :visible="isLoading"
  title="青龙觉醒"
  subtitle="碎瓷重生，凤凰涅槃..."
/>
```

---

### 3. 完整示例（Agent 切换场景）

```vue
<template>
  <div class="agent-switch-demo">
    <button @click="handleSwitchAgent">切换 Agent</button>
    
    <CeladonCrackleLoading
      :visible="isSwitching"
      :title="loadingTitle"
      :subtitle="loadingSubtitle"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import CeladonCrackleLoading from '@/components/loading/CeladonCrackleLoading.vue'

const currentAgent = ref('ReAct+')
const nextAgent = ref('DeepSeek')
const isSwitching = ref(false)

const loadingTitle = computed(() => `${currentAgent.value} → ${nextAgent.value}`)
const loadingSubtitle = computed(() => '银瓶乍破，青龙转世...')

const handleSwitchAgent = async () => {
  isSwitching.value = true
  
  try {
    // 1. 播放冰裂纹动画（4.5s）
    await new Promise(resolve => setTimeout(resolve, 4500))
    
    // 2. 切换 Agent
    currentAgent.value = nextAgent.value
    
    // 3. 初始化新 Agent
    await initializeAgent(nextAgent.value)
    
  } finally {
    isSwitching.value = false
  }
}
</script>
```

---

## 🎭 动画细节

### SVG 路径动画原理

```svg
<!-- 初始状态：路径不可见 -->
<path
  d="M 500 500 Q 520 480 550 450"
  stroke-dasharray="100 100"    <!-- 虚线：100px 实线 + 100px 间隔 -->
  stroke-dashoffset="100"       <!-- 偏移 100px，完全不可见 -->
/>

<!-- 动画后：路径完全显示 -->
<path
  d="M 500 500 Q 520 480 550 450"
  stroke-dasharray="100 100"
  stroke-dashoffset="0"         <!-- 偏移 0px，完全可见 -->
/>
```

**GSAP 实现**：
```javascript
gsap.fromTo('.crack-line',
  { strokeDashoffset: 100 },  // 从不可见
  { strokeDashoffset: 0 }     // 到完全可见
)
```

---

### 交错动画 (Stagger)

```javascript
// 依次出现，间隔 50ms
stagger: 0.05

// 从中心开始
stagger: { each: 0.05, from: 'center' }

// 随机顺序
stagger: { each: 0.05, from: 'random' }

// 从边缘到中心
stagger: { each: 0.05, from: 'edges' }
```

---

### 碎片爆裂效果

```javascript
gsap.to('.fragment', {
  // 🔥 使用 GSAP utils.random 为每个碎片生成随机值
  x: gsap.utils.random(-500, 500),      // 随机 X 偏移
  y: gsap.utils.random(-500, 500),      // 随机 Y 偏移
  rotation: gsap.utils.random(-180, 180), // 随机旋转
  scale: gsap.utils.random(0.5, 1.5),   // 随机缩放
  opacity: 0,                           // 淡出
  duration: 1.2,
  stagger: {
    each: 0.01,
    from: 'center'  // 从中心向外爆裂
  }
})
```

---

## 🎨 自定义样式

### 修改颜色主题

```scss
// 修改裂纹颜色
.main-crack {
  stroke: rgba(255, 0, 0, 0.6) !important;  // 改为红色
}

// 修改背景
.celadon-surface {
  background: radial-gradient(circle at center,
    rgba(255, 240, 240, 1) 0%,    // 改为红色系
    rgba(255, 220, 220, 0.95) 50%,
    rgba(255, 200, 200, 0.9) 100%
  );
}
```

---

### 修改动画时长

```javascript
const playShatterAnimation = () => {
  const tl = gsap.timeline()

  // 加快主裂纹扩散
  tl.fromTo('.main-crack', {...}, {
    duration: 0.3,  // 原本 0.6s，改为 0.3s
    ...
  })
}
```

---

### 修改裂纹密度

```javascript
// 生成更多主裂纹
for (let i = 0; i < 16; i++) {  // 原本 8 条，改为 16 条
  const angle = (Math.PI * 2 * i) / 16
  // ...
}

// 生成更多碎片
for (let i = 0; i < 300; i++) {  // 原本 120 个，改为 300 个
  fragments.value.push({...})
}
```

---

## 🎵 音效说明

### 当前实现

使用 **Web Audio API** 实时生成破裂音效：
- **白噪声** - 模拟破裂的杂音
- **高通滤波器** (2000Hz) - 突出高频"脆响"
- **指数衰减** - 模拟声音快速消失

### 使用真实音频

```vue
<audio ref="crackSound" preload="auto">
  <source src="/sounds/ice-crack-1.mp3" type="audio/mpeg">
  <source src="/sounds/ice-crack-1.ogg" type="audio/ogg">
</audio>
```

**推荐音效资源**：
- [Freesound.org](https://freesound.org/search/?q=glass+break+ice+crack)
- [Zapsplat](https://www.zapsplat.com/)
- 关键词：`glass break`, `ice crack`, `porcelain shatter`

---

## 📊 性能优化

### 1. **will-change 优化**

```scss
.fragment {
  will-change: transform, opacity;  // 🔥 提前告知浏览器
}
```

### 2. **GPU 加速**

GSAP 自动使用 `transform` 和 `opacity`，触发 GPU 加速：
```javascript
// ✅ 好：使用 transform
gsap.to('.element', { x: 100, y: 100 })

// ❌ 差：使用 left/top
gsap.to('.element', { left: 100, top: 100 })
```

### 3. **减少 DOM 元素**

```javascript
// 如果性能不足，减少碎片数量
for (let i = 0; i < 60; i++) {  // 从 120 改为 60
  fragments.value.push({...})
}
```

### 4. **防抖处理**

```javascript
// 防止快速切换时重复触发
let isAnimating = false

watch(() => props.visible, (newVal) => {
  if (newVal && !isAnimating) {
    isAnimating = true
    playShatterAnimation().then(() => {
      isAnimating = false
    })
  }
})
```

---

## 🎯 最佳实践

### ✅ 推荐场景

- **Agent 切换** - 不同 AI 模型切换
- **页面转场** - 重要页面的过渡
- **任务完成** - 大型任务结束的视觉反馈
- **错误恢复** - 系统重启或重置

### ❌ 不推荐场景

- **频繁操作** - 如列表滚动加载
- **快速切换** - 如标签页切换（太慢）
- **后台任务** - 不需要用户等待的操作

### ⚡ 性能建议

| 设备类型 | 碎片数量 | 裂纹密度 | 动画时长 |
|---------|----------|----------|----------|
| **高端设备** | 120 | 高（8+16+24） | 4.5s |
| **中端设备** | 80 | 中（6+12+16） | 3.5s |
| **低端设备** | 40 | 低（4+8+8） | 2.5s |

```javascript
// 根据设备性能调整
const isMobile = /iPhone|iPad|Android/i.test(navigator.userAgent)
const fragmentCount = isMobile ? 40 : 120
```

---

## 🐉 诗意解读

### 视觉隐喻

| 阶段 | 诗意描述 | 视觉效果 |
|------|----------|----------|
| 1️⃣ | 初心萌动 | 中心点闪现 |
| 2️⃣ | 银瓶乍破 | 主裂纹扩散 |
| 3️⃣ | 玉碎声起 | 分支裂纹 |
| 4️⃣ | 冰壶炸裂 | 网状细纹 |
| 5️⃣ | 碎玉落叠 | 闪光点闪烁 |
| 6️⃣ | 余音绕梁 | 整体颤动 |
| 7️⃣ | 凤凰涅槃 | 碎片四散 |
| 8️⃣ | 重归于无 | 淡出消失 |

### 美学特征

- **静态之美** - 青花瓷底纹，素雅宁静
- **动态之美** - 裂纹扩散，如龙觉醒
- **破碎之美** - 冰裂纹理，残缺之美
- **重生之美** - 碎片飞散，凤凰涅槃

---

## 📝 总结

### 技术亮点

- ✅ **算法生成裂纹** - 每次都不同，自然真实
- ✅ **GSAP Timeline** - 8 个阶段精确编排
- ✅ **Web Audio API** - 实时生成破裂音效
- ✅ **性能优化** - GPU 加速 + will-change
- ✅ **响应式设计** - 支持移动端

### 视觉效果

- 🎨 **青花瓷美学** - 素雅色调
- 🎨 **冰裂纹理** - 自然不规则
- 🎨 **破碎之美** - 碎片飞散
- 🎨 **音画同步** - 视听结合

### 诗意表达

**"银瓶乍破水浆迸，碎玉落叠冰壶裂"**

表面是青花瓷的优雅，内里是青龙的力量，破碎中蕴含着重生的希望！

---

**创建时间**：2025-01-14  
**创建人**：李大飞  
**状态**：✅ 完整实现

🏺✨🐉
