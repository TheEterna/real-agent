# 🖌️ 水墨晕染系统 - 快速入门

## 📍 按钮位置

**顶部状态栏左侧**：紧邻 StatusIndicator，方便模式切换。

```
┌─────────────────────────────────────────┐
│ [墨韵] [●状态]          [⚙️设置]     │  ← 顶部状态栏
├─────────────────────────────────────────┤
│                                         │
│          对话消息区域                    │
│                                         │
```

---

## 🎬 使用流程

### 1. 用户点击"墨韵"按钮

### 2. 触发水墨晕染动效（6秒）

**阶段时间线**：
```
0ms ────────── 墨滴初落
300ms ──────── 涟漪扩散
1500ms ─────── 墨色晕染（150个粒子）
3000ms ─────── 全屏覆盖 + 山水浮现
3500ms ─────── 篆字"道法自然"浮现 + 模式切换通知
6000ms ─────── 动画完成，资源清理
```

### 3. 模式切换完成

---

## 🎨 视觉效果预览

### 按钮样式

```
    🍃                     🍃
         ┌─────────┐
    🍃   │         │   🍃
         │  墨韵   │
         │         │
         └─────────┘
```

- **尺寸**：52px × 52px（圆形）
- **颜色**：深墨色渐变
- **文字**：楷体"墨韵"
- **辅助**：4片竹叶剪影

### 悬停效果

- 按钮放大 1.08x
- 墨色加深（brightness 0.85）
- 竹叶轻微颤动

### 激活状态

- 背景亮度提升
- 青龙旋转光环（6秒一圈）
- 持续呼吸效果

---

## 💻 代码示例

### 最简集成

```vue
<script setup>
import InkModeButton from '@/components/InkModeButton.vue'
import InkTransition from '@/components/InkTransition.vue'

const currentMode = ref('command')
const inkTransitionTrigger = ref(false)
const inkOrigin = ref({ x: 0, y: 0 })

const handleInkModeClick = (mode: string, event: MouseEvent) => {
  inkOrigin.value = { x: event.clientX, y: event.clientY }
  inkTransitionTrigger.value = true
  
  setTimeout(() => {
    currentMode.value = mode
    notification.success({
      message: '墨韵模式',
      description: `已切换至「${mode}」模式`
    })
  }, 3500)
}

const handleInkTransitionComplete = () => {
  inkTransitionTrigger.value = false
}
</script>

<template>
  <div>
    <!-- 按钮 -->
    <InkModeButton
      mode="command"
      :active="currentMode === 'command'"
      @click="handleInkModeClick"
    />
    
    <!-- 全屏动效层 -->
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

## 🔧 配置选项

### InkModeButton Props

| 属性 | 类型 | 说明 | 示例 |
|-----|------|------|------|
| `mode` | `string` | 模式标识 | `"command"` |
| `active` | `boolean` | 是否激活 | `true` |

### InkTransition Props

| 属性 | 类型 | 说明 | 示例 |
|-----|------|------|------|
| `trigger` | `boolean` | 触发开关 | `true` |
| `originX` | `number` | 起点X坐标 | `500` |
| `originY` | `number` | 起点Y坐标 | `300` |

---

## 🎯 多模式扩展

### 添加新模式

```typescript
// 1. 添加按钮
<InkModeButton
  mode="creative"
  :active="currentMode === 'creative'"
  @click="handleInkModeClick"
/>

// 2. 更新文字映射
const modeLabels = {
  command: '墨韵',
  creative: '丹青',
  analyze: '竹韵'
}

// 3. 在按钮组件中自定义文字
<span class="ink-text">
  {{ modeLabels[mode] || mode }}
</span>
```

---

## 🐛 常见问题

### Q1: 动画卡顿怎么办？

**A**: 检查以下几点：
- Canvas DPR 是否正确设置
- 粒子数量是否过多（降低到 100）
- 是否有其他高耗能动画同时运行

### Q2: 点击位置不准确？

**A**: 确保获取的是 `event.clientX/Y`，而非 `pageX/Y`。

### Q3: 竹叶不动？

**A**: 检查 `.bamboo-leaf` 类名是否正确，GSAP 是否成功加载。

### Q4: 篆字不显示？

**A**: 检查系统是否安装了仿宋字体，或替换为其他中文字体。

---

## 📊 性能指标

| 指标 | 目标值 | 说明 |
|-----|--------|------|
| FPS | ≥ 50 | 动画流畅度 |
| 粒子数 | 150 | 墨色晕染粒子 |
| 内存占用 | < 50MB | Canvas + 粒子系统 |
| 动画时长 | 6s | 完整周期 |
| 响应时间 | < 100ms | 按钮点击到动效开始 |

---

## 🎨 自定义建议

### 修改墨色

```typescript
// 在 InkTransition.vue 中
const inkColors = {
  black: 'rgba(20, 30, 30, 1)',    // 墨黑
  red: 'rgba(139, 69, 19, 1)',     // 丹青红
  green: 'rgba(74, 124, 89, 1)'    // 竹韵绿
}
```

### 调整动画速度

```typescript
// 加快动画（4秒）
setTimeout(() => currentMode.value = mode }, 2500)  // 原3500ms

// 在 InkTransition.vue 中调整各阶段duration
```

### 添加音效

```typescript
const playSound = (soundFile: string) => {
  const audio = new Audio(`/sounds/${soundFile}.mp3`)
  audio.volume = 0.5
  audio.play()
}

// 在动画各阶段调用
playSound('water-drop')  // 墨滴初落
playSound('ripple')      // 涟漪扩散
```

---

## ✅ 检查清单

部署前确认：

- [ ] GSAP 已正确安装（`gsap: ^3.13.0`）
- [ ] 两个组件已放置在 `src/components/`
- [ ] 已在主页面导入并注册组件
- [ ] 状态管理变量已声明
- [ ] 事件处理函数已实现
- [ ] Canvas DPR 适配已设置
- [ ] 动画完成后资源清理
- [ ] 响应式布局测试通过
- [ ] 多设备兼容性测试

---

## 📚 相关文档

- [完整技术文档](./INK_TRANSITION_SYSTEM.md)
- [GSAP 动画迁移文档](./GSAP_ANIMATION_MIGRATION.md)
- [青花瓷主题文档](./CELADON_DRAGON_THEME.md)

---

**快速开始，诗意编程！** 🖌️✨

*"一点墨色起，万千气象生"*
