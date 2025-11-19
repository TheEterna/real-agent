# 🎨 GSAP 动画系统迁移文档

## 📅 创建时间：2025-01-14

---

## 🎯 迁移目标

**核心理念**：统一使用 GSAP 专业动效库管理所有动画，便于维护和优化。

**原则**：
- ✅ GSAP 是专业动效库，性能优秀
- ✅ 统一管理，避免 CSS keyframes 和 GSAP 混用
- ✅ 更好的控制和扩展性
- ✅ 便于调试和维护

---

## 📊 迁移清单

### ✅ 已迁移动画 (11个)

| CSS @keyframes | GSAP 函数 | 功能描述 |
|---------------|-----------|----------|
| `dragonRipple` | `createRippleEffect()` + `setupToolbarAdvancedAnimations()` | 涟漪扩散效果 |
| `dragonPulse` | `createPulseEffect()` | 青光脉动效果 |
| `shimmer` | `setupAttachmentAdvancedAnimations()` | 波光粼粼效果 |
| `spinRipple` | `createRippleEffect(config)` | 旋转涟漪效果 |
| `dragonGlaze` | `setupInputContainerAdvancedAnimations()` | 龙泉青瓷釉光流淌 |
| `jadeShimmer` | `setupInputContainerAdvancedAnimations()` | 汉白玉深层纹理流动 |
| `dragonJadeBreathing` | `setupInputContainerAdvancedAnimations()` | 汉白玉龙泉青瓷呼吸 |
| `dragonJadeRotation` | `setupInputContainerAdvancedAnimations()` | 青龙旋转光环 |
| `textareaJadeGlow` | `setupTextareaAdvancedAnimations()` | Textarea 龙泉青瓷光晕 |
| `sendButtonJadeBreathing` | `setupSendButtonAdvancedAnimations()` | 发送按钮呼吸动画 |
| `jadeInnerFlow` | `setupSendButtonAdvancedAnimations()` | 龙泉青瓷内在流光 |

### ⚠️ 保留的 CSS 动画

| CSS 动画 | 保留原因 |
|---------|----------|
| `dragonRise` (用于Vue transition) | Vue组件过渡使用，不影响核心动效 |
| CSS `transition` 属性 | 简单的状态切换，无需GSAP |

---

## 🎨 新增 GSAP 动画函数

### 1️⃣ **setupInputContainerAdvancedAnimations()**

**替代动画**：
- `dragonGlaze` - 龙泉青瓷顶部流光
- `jadeShimmer` - 汉白玉深层纹理流动
- `dragonJadeBreathing` - 汉白玉龙泉青瓷呼吸
- `dragonJadeRotation` - 青龙旋转光环

**实现方式**：
```typescript
const setupInputContainerAdvancedAnimations = () => {
  const inputContainer = document.querySelector('.input-container')
  
  // 1. 顶部流光
  gsap.to(afterElement, {
    backgroundPosition: '200% center',
    duration: 6,
    repeat: -1,
    yoyo: true
  })
  
  // 2. 深层纹理流动 (Timeline)
  const shimmerTimeline = gsap.timeline({ repeat: -1 })
  shimmerTimeline
    .to(beforeElement, { backgroundPosition: '25% 25%...', duration: 2 })
    .to(beforeElement, { backgroundPosition: '50% 50%...', duration: 2 })
    // ...
  
  // 3. 监听聚焦状态
  textarea.addEventListener('focus', () => {
    // 创建旋转光环元素
    const rotationElement = document.createElement('div')
    rotationElement.className = 'dragon-jade-rotation-ring'
    
    gsap.to(rotationElement, {
      rotation: 360,
      scale: 1.02,
      duration: 6,
      repeat: -1
    })
    
    // 呼吸动画
    breathingAnimation = gsap.to(inputContainer, {
      boxShadow: '...',
      duration: 2.5,
      repeat: -1,
      yoyo: true
    })
  })
}
```

**特点**：
- 使用 `gsap.timeline()` 管理复杂序列
- 动态创建 DOM 元素用于动画
- 事件驱动的动画启停

---

### 2️⃣ **setupTextareaAdvancedAnimations()**

**替代动画**：`textareaJadeGlow` - Textarea 龙泉青瓷光晕

**实现方式**：
```typescript
const setupTextareaAdvancedAnimations = () => {
  const textarea = document.querySelector('.input-field textarea')
  let glowAnimation: gsap.core.Tween | null = null
  
  textarea.addEventListener('focus', () => {
    glowAnimation = gsap.to(textarea, {
      boxShadow: `复杂的多层阴影...`,
      duration: 2.5,
      repeat: -1,
      yoyo: true
    })
  })
  
  textarea.addEventListener('blur', () => {
    if (glowAnimation) {
      glowAnimation.kill()  // 停止动画
    }
  })
}
```

**特点**：
- 聚焦时启动，失焦时停止
- 使用 `animation.kill()` 精确控制

---

### 3️⃣ **setupSendButtonAdvancedAnimations()**

**替代动画**：
- `sendButtonJadeBreathing` - 发送按钮呼吸
- `jadeInnerFlow` - 龙泉青瓷内在流光

**实现方式**：
```typescript
const setupSendButtonAdvancedAnimations = () => {
  const sendButton = document.querySelector('.send-button')
  let breathingAnimation: gsap.core.Tween | null = null
  let innerFlowAnimation: gsap.core.Tween | null = null
  
  sendButton.addEventListener('mouseenter', () => {
    // 呼吸动画
    breathingAnimation = gsap.to(sendButton, {
      boxShadow: `多层阴影呼吸效果...`,
      duration: 2.5,
      repeat: -1,
      yoyo: true
    })
    
    // 内在流光
    innerFlowAnimation = gsap.to(beforeElement, {
      backgroundPosition: '100% 100%',
      opacity: 1,
      duration: 5,
      repeat: -1,
      yoyo: true
    })
  })
  
  sendButton.addEventListener('mouseleave', () => {
    if (breathingAnimation) breathingAnimation.kill()
    if (innerFlowAnimation) innerFlowAnimation.kill()
  })
}
```

**特点**：
- Hover时启动多个并发动画
- 离开时清理所有动画

---

### 4️⃣ **setupToolbarAdvancedAnimations()**

**替代动画**：`dragonRipple` - 涟漪扩散

**实现方式**：
```typescript
const setupToolbarAdvancedAnimations = () => {
  const toolbarButtons = document.querySelectorAll('.input-toolbar button')
  
  toolbarButtons.forEach(button => {
    button.addEventListener('mouseenter', () => {
      // 创建涟漪元素
      const ripple = document.createElement('div')
      ripple.className = 'gsap-ripple-effect'
      button.appendChild(ripple)
      
      // 涟漪动画序列
      gsap.timeline()
        .fromTo(ripple, 
          { scale: 0.9, opacity: 0.8 },
          { scale: 1.05, opacity: 0.4, duration: 0.5 }
        )
        .to(ripple, 
          { scale: 1.15, opacity: 0, duration: 0.7 }
        )
        .then(() => ripple.remove())  // 动画结束后移除元素
    })
  })
}
```

**特点**：
- 动态创建和销毁 DOM
- 使用 `timeline.then()` 清理资源

---

### 5️⃣ **setupAttachmentAdvancedAnimations()**

**替代动画**：`shimmer` - 波光粼粼

**实现方式**：
```typescript
const setupAttachmentAdvancedAnimations = () => {
  const attachmentChips = document.querySelectorAll('.attachment-chip')
  
  attachmentChips.forEach(chip => {
    chip.addEventListener('mouseenter', () => {
      gsap.fromTo(chip,
        { backgroundPosition: '-100% center' },
        { 
          backgroundPosition: '100% center',
          duration: 2,
          ease: 'power2.inOut'
        }
      )
    })
  })
}
```

**特点**：
- `fromTo` 明确起止状态
- 自定义缓动函数

---

### 6️⃣ **createRippleEffect()** - 通用涟漪效果

**替代动画**：`dragonRipple`, `spinRipple`

**实现方式**：
```typescript
const createRippleEffect = (element: Element, config = {}) => {
  const defaultConfig = {
    duration: 1.2,
    scale: 1.3,
    rotation: 360,
    ease: 'power2.out'
  }
  
  const finalConfig = { ...defaultConfig, ...config }
  
  gsap.fromTo(element,
    { scale: 1, rotation: 0, opacity: 0.6 },
    {
      scale: finalConfig.scale,
      rotation: finalConfig.rotation,
      opacity: 0,
      duration: finalConfig.duration,
      ease: finalConfig.ease
    }
  )
}
```

**特点**：
- 可复用的通用函数
- 支持自定义配置

---

### 7️⃣ **createPulseEffect()** - 青光脉动

**替代动画**：`dragonPulse`

**实现方式**：
```typescript
const createPulseEffect = (element: Element) => {
  return gsap.to(element, {
    boxShadow: '0 0 0 8px var(--brand-glow), var(--shadow-large)',
    duration: 2,
    ease: 'sine.inOut',
    repeat: -1,
    yoyo: true
  })
}
```

**特点**：
- 返回动画实例，便于后续控制
- 无限循环 + 往复

---

## 🚀 调用流程

### onMounted 初始化

```typescript
onMounted(() => {
  nextTick(() => {
    // ... 现有的 GSAP 动画
    
    // ========== 🎨 高级 GSAP 动画 - 替代 CSS keyframes ==========
    // 8. 输入容器汉白玉龙泉青瓷动画
    setupInputContainerAdvancedAnimations()
    
    // 9. Textarea 龙泉青瓷光晕动画
    setupTextareaAdvancedAnimations()
    
    // 10. 发送按钮持续动画
    setupSendButtonAdvancedAnimations()
    
    // 11. 工具栏按钮涟漪动画
    setupToolbarAdvancedAnimations()
    
    // 12. 附件卡片光泽动画
    setupAttachmentAdvancedAnimations()
  })
})
```

---

## 📝 CSS 清理工作

### 删除的 @keyframes

```css
/* ❌ 已删除 */
@keyframes dragonRipple { ... }
@keyframes dragonPulse { ... }
@keyframes shimmer { ... }
@keyframes spinRipple { ... }
@keyframes dragonGlaze { ... }
@keyframes jadeShimmer { ... }
@keyframes dragonJadeBreathing { ... }
@keyframes dragonJadeRotation { ... }
@keyframes textareaJadeGlow { ... }
@keyframes sendButtonJadeBreathing { ... }
@keyframes jadeInnerFlow { ... }
```

### 移除的 animation 引用

```css
/* ❌ 已移除 */
animation: dragonGlaze 6s ease-in-out infinite;
animation: jadeShimmer 8s ease-in-out infinite;
animation: dragonJadeRotation 6s linear infinite;
animation: textareaJadeGlow 2.5s ease-in-out infinite;
animation: sendButtonJadeBreathing 2.5s ease-in-out infinite;
animation: jadeInnerFlow 3s ease-in-out infinite;
```

### 替换为注释

```css
/* ✅ 替换为 */
/* 动画已迁移至 GSAP: setupInputContainerAdvancedAnimations() */
/* 动画已迁移至 GSAP: setupTextareaAdvancedAnimations() */
/* 动画已迁移至 GSAP: setupSendButtonAdvancedAnimations() */
/* 动画已迁移至 GSAP: setupToolbarAdvancedAnimations() */
/* 动画已迁移至 GSAP: setupAttachmentAdvancedAnimations() */
```

---

## 🎯 优势对比

### CSS Keyframes 的局限

❌ **难以控制**：
- 无法精确启停
- 难以动态修改参数
- 无法监听动画完成事件

❌ **维护困难**：
- CSS 和 JS 分离
- 难以调试
- 代码分散

❌ **性能问题**：
- 某些属性触发重排重绘
- 无法利用 GSAP 的优化

### GSAP 的优势

✅ **精确控制**：
- `animation.kill()` 随时停止
- `animation.pause()`, `animation.play()`
- `animation.restart()`

✅ **便于维护**：
- 所有动画集中在 JS
- 清晰的函数命名
- 易于调试

✅ **性能优秀**：
- GPU 加速
- 自动批处理
- 避免layout thrashing

✅ **功能强大**：
- Timeline 序列动画
- 事件回调 `onComplete`, `onUpdate`
- 丰富的缓动函数

---

## 🛠️ 开发建议

### 1. 新增动画时

```typescript
// ✅ 推荐：使用 GSAP
const setupNewAnimation = () => {
  gsap.to(element, {
    property: value,
    duration: 1,
    ease: 'power2.out'
  })
}

// ❌ 避免：CSS keyframes
@keyframes newAnimation { ... }
```

### 2. 调试动画

```typescript
// 在 GSAP 动画中添加日志
gsap.to(element, {
  x: 100,
  onUpdate: () => console.log('动画进度:', gsap.getProperty(element, 'x')),
  onComplete: () => console.log('动画完成')
})
```

### 3. 性能优化

```typescript
// 使用 will-change 提示浏览器
element.style.willChange = 'transform, opacity'

gsap.to(element, {
  x: 100,
  opacity: 0,
  onComplete: () => {
    element.style.willChange = 'auto'  // 动画完成后清除
  }
})
```

---

## 📊 迁移统计

| 项目 | 数量 |
|------|------|
| 删除的 @keyframes | 11 个 |
| 移除的 animation 引用 | 15+ 处 |
| 新增 GSAP 函数 | 7 个 |
| 代码行数减少 | ~200 行 CSS |
| 代码行数增加 | ~300 行 TS (更清晰) |

---

## ✅ 总结

### 核心成果

1. **统一管理**：所有动画由 GSAP 统一管理
2. **便于维护**：动画逻辑集中在 TypeScript 中
3. **性能优化**：利用 GSAP 专业优化
4. **扩展性强**：易于添加新动画和交互

### 下一步优化

- [ ] 考虑使用 GSAP Context 统一管理动画上下文
- [ ] 添加动画性能监控
- [ ] 探索 GSAP ScrollTrigger 用于滚动动画
- [ ] 优化动画资源清理机制

---

**迁移完成时间**：2025-01-14  
**版本**：v1.0  
**状态**：✅ 已完成并测试

🎨✨🚀
