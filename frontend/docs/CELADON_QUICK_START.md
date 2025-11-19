# 🐉 青花瓷主题快速指南

## 🎯 一句话总结

**表面宁静如碧水，交互有力如青龙** - 每个按钮、每个切换、每个操作都蕴含青龙之力。

---

## ✨ 核心动效一览

### 🔵 按钮交互

| 动作 | 效果 | 视觉隐喻 |
|------|------|----------|
| **Hover** | 缩放+光晕+阴影 | 青龙气息显现 |
| **Active** | 快速缩小 | 龙爪按下 |
| **Focus** | 旋转光环 | 青龙盘旋 |

### 🎨 输入框状态

| 状态 | 动效 | 描述 |
|------|------|------|
| **静止** | 淡青边框 | 碧池平静 |
| **悬停** | 釉光扫过 | 水面涟漪 |
| **聚焦** | 四层叠加 | 青龙入池 |

### 💬 内容动画

| 元素 | 入场方式 | 耗时 |
|------|----------|------|
| 消息 | 柔和升起 (dragonRise) | 500ms |
| 附件 | 弹性缩放 (dragonScale) | 600ms |
| 快捷按钮 | 分批入场 | 0/100/200ms |

---

## 🎨 配色速查

```css
/* 主色调 */
青瓷色: #5B8A8A
深青色: #3A5F5F
淡青光: #D8E8E8

/* 背景 */
米白: #F8F9FA
纯白: #FEFEFE
淡青: #F0F4F4

/* 文字 */
主墨: #2C3E3E
次墨: #5B7373
淡墨: #8B9D9D
```

---

## ⚡ 动效关键帧

### 常用动画

```css
dragonRise      /* 柔和升起 - 元素入场 */
dragonScale     /* 弹性缩放 - 弹出效果 */
dragonPulse     /* 青光脉动 - 呼吸律动 */
dragonRipple    /* 涟漪扩散 - 波纹效果 */
shimmer         /* 波光粼粼 - 光泽流动 */
spinRipple      /* 旋转涟漪 - 光环旋转 */
```

### 使用示例

```css
.my-button:hover {
  /* 方式1: 单个动画 */
  animation: dragonRise 500ms cubic-bezier(0.34, 1.56, 0.64, 1);
  
  /* 方式2: 组合动效 */
  animation: dragonPulse 2s infinite, dragonRise 500ms;
  
  /* 方式3: 配合过渡 */
  transition: all var(--transition-spring);
  transform: scale(1.05);
}
```

---

## 🎯 最佳实践

### ✅ 推荐做法

```css
/* 1. 使用 CSS Variables */
color: var(--brand-primary);
box-shadow: var(--shadow-glow);

/* 2. 多层动效叠加 */
.button::before { /* 外圈 */ }
.button::after { /* 内部 */ }
.button:hover { /* 整体 */ }

/* 3. 语义化过渡时长 */
transition: all var(--transition-normal);
animation: dragonRise var(--transition-slow);

/* 4. 使用 GPU 加速属性 */
transform: translateY(-2px);  /* ✅ 好 */
top: -2px;                     /* ❌ 差 */
```

### ❌ 避免做法

```css
/* 1. 硬编码颜色 */
color: #5B8A8A;  /* ❌ 应使用 var(--brand-primary) */

/* 2. 过度动效 */
.button {
  animation: rotate 0.3s, scale 0.3s, fade 0.3s;  /* ❌ 太多 */
}

/* 3. 触发 layout */
margin-top: 10px;  /* ❌ 使用 transform 代替 */
```

---

## 🔧 调试技巧

### 查看所有伪元素

```css
*::before, *::after {
  outline: 1px solid rgba(255, 0, 0, 0.5) !important;
}
```

### 放慢动画10倍

```css
* {
  animation-duration: calc(var(--duration) * 10) !important;
  transition-duration: calc(var(--duration) * 10) !important;
}
```

### 禁用某个动画

```css
.element {
  animation: none !important;
}
```

---

## 📱 响应式适配

主题已自动适配：

- ✅ **桌面端**：完整动效
- ✅ **平板端**：保持动效
- ✅ **移动端**：简化但不失去
- ✅ **无障碍**：`prefers-reduced-motion`

---

## 🎨 自定义扩展

### 添加新按钮样式

```css
.my-button {
  /* 1. 继承基础样式 */
  position: relative;
  background: var(--bg-secondary);
  border: 2px solid var(--border-light);
  border-radius: var(--radius-md);
  transition: all var(--transition-normal);
  overflow: hidden;
  
  /* 2. 添加伪元素效果 */
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: var(--brand-glow);
    opacity: 0;
    transition: opacity var(--transition-normal);
  }
  
  /* 3. 定义 hover 状态 */
  &:hover {
    border-color: var(--brand-primary);
    transform: translateY(-2px);
    box-shadow: var(--shadow-medium), var(--shadow-glow);
  }
  
  &:hover::before {
    opacity: 1;
    animation: dragonRipple 1.5s infinite;
  }
}
```

### 添加新动画

```css
@keyframes myDragonAnimation {
  0% {
    /* 起始状态 */
    opacity: 0;
    transform: scale(0.9);
  }
  50% {
    /* 中间状态 */
    opacity: 1;
    transform: scale(1.1);
  }
  100% {
    /* 结束状态 */
    opacity: 1;
    transform: scale(1);
  }
}

/* 使用 */
.element {
  animation: myDragonAnimation var(--transition-slow) cubic-bezier(0.34, 1.56, 0.64, 1);
}
```

---

## 🎯 性能优化建议

### 1. 优先使用 transform 和 opacity

```css
/* ✅ GPU 加速 */
transform: translateY(-2px) scale(1.02);
opacity: 0.8;

/* ❌ 触发重排 */
margin-top: -2px;
width: 102%;
```

### 2. 使用 will-change 提示

```css
.button:hover {
  will-change: transform, box-shadow;
  /* 动画完成后移除 */
}
```

### 3. 复杂动效用 requestAnimationFrame

```javascript
// 对于复杂的 JS 动画
function animate() {
  // 动画逻辑
  requestAnimationFrame(animate);
}
```

---

## 🐛 常见问题

### Q: 动效不流畅？

```css
/* A: 添加 GPU 加速 */
.element {
  transform: translateZ(0);
  backface-visibility: hidden;
}
```

### Q: 伪元素不显示？

```css
/* A: 确保有以下属性 */
&::before {
  content: '';  /* ⚠️ 必需 */
  position: absolute;  /* ⚠️ 必需 */
  /* ... */
}
```

### Q: Z-index 层级问题？

```css
/* A: 创建新的堆叠上下文 */
.parent {
  position: relative;
  z-index: 1;
}

.child::before {
  z-index: -1;  /* 相对于父元素 */
}
```

---

## 📚 进阶阅读

- 📖 [完整设计文档](./CELADON_DRAGON_THEME.md)
- 🎨 [配色系统详解](./CELADON_DRAGON_THEME.md#配色系统)
- ⚡ [动效系统详解](./CELADON_DRAGON_THEME.md#青龙动效系统)
- 🎯 [实现细节](./CELADON_DRAGON_THEME.md#实现细节)

---

## 🎨 效果预览

### 发送按钮 - 最强动效

```
静止态:   [发送]
           ↓
悬停态:   [发送] ← 青光环绕 + 内部涟漪 + 脉动阴影
           ↓
按下态:   [发送] ← 快速缩小
```

### 输入框 - 青龙入池

```
静止态:   ┌─────────────┐
          │ 输入框      │
          └─────────────┘
           ↓
聚焦态:   ┏━━━━━━━━━━━━━┓ ← 4层动效叠加
          ┃ 输入框   🐉┃    - 青光圈
          ┗━━━━━━━━━━━━━┛    - 旋转光环
                            - 脉动阴影
                            - 立体抬升
```

---

## ⚡ 一键切换

如果需要切换回简单主题：

```css
/* 禁用所有青龙动效 */
.react-plus-app * {
  animation: none !important;
  transition: all 200ms ease !important;
}

/* 或者导入其他主题 */
@import './theme-simple.css';
```

---

**快速上手时间**: 5分钟  
**完全掌握时间**: 30分钟  
**青龙之力**: 无穷 🐉

---

> "静水流深，青龙在渊。表面平静，内蕴无穷。"
