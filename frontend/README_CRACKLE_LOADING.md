# 🏺 青花瓷冰裂纹 Loading 快速开始

## 🎯 效果预览

**访问演示页面**：`http://localhost:5173/demo/crackle`

---

## ⚡ 5分钟快速集成

### 1️⃣ **引入组件**

```vue
<script setup lang="ts">
import CeladonCrackleLoading from '@/components/loading/CeladonCrackleLoading.vue'
import { ref } from 'vue'

const isLoading = ref(false)
</script>
```

### 2️⃣ **添加到模板**

```vue
<template>
  <div>
    <!-- 你的内容 -->
    <YourContent />
    
    <!-- 冰裂纹 Loading -->
    <CeladonCrackleLoading
      :visible="isLoading"
      title="切换中"
      subtitle="..."
    />
  </div>
</template>
```

### 3️⃣ **触发动画**

```typescript
// Agent 切换时
const switchAgent = async () => {
  isLoading.value = true
  
  try {
    await performAgentSwitch()
  } finally {
    // 4.5秒后自动关闭
    setTimeout(() => {
      isLoading.value = false
    }, 4500)
  }
}
```

---

## 🎨 动画效果说明

### 总时长：**4.5 秒**

| 阶段 | 时间 | 效果 | 音效 |
|------|------|------|------|
| 1️⃣ | 0-0.2s | 中心点闪现 | - |
| 2️⃣ | 0.2-1s | 主裂纹扩散（8条） | 🔊 破裂声1 |
| 3️⃣ | 0.8-1.5s | 次级裂纹分支（16条） | 🔊 破裂声2 |
| 4️⃣ | 1.2-2s | 细微裂纹网状（24条） | 🔊 破裂声3 |
| 5️⃣ | 1.5-2.5s | 闪光点闪烁（20个） | - |
| 6️⃣ | 2.5-2.8s | 整体颤动 | 🔊 爆裂声 |
| 7️⃣ | 2.8-4s | 碎片四散（120个） | - |
| 8️⃣ | 3.5-4.5s | 整体淡出 | - |

---

## 📝 API 文档

### Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `visible` | `boolean` | `false` | 是否显示 Loading |
| `title` | `string` | `'切换中'` | 主标题文字 |
| `subtitle` | `string` | `'...'` | 副标题文字 |

### 示例

```vue
<!-- 基础使用 -->
<CeladonCrackleLoading :visible="true" />

<!-- 自定义文字 -->
<CeladonCrackleLoading
  :visible="true"
  title="凤凰涅槃"
  subtitle="浴火重生，破茧成蝶..."
/>
```

---

## 🎯 使用场景

### ✅ 推荐

- **Agent 切换** - AI 模型切换
- **系统重启** - 应用重新初始化
- **重要转场** - 关键页面过渡
- **任务完成** - 大任务结束

### ❌ 不推荐

- 频繁操作（如滚动加载）
- 快速切换（如标签切换）
- 简单加载（如按钮 loading）

---

## 🔧 常见问题

### Q1: 如何修改动画时长？

**A**: 修改 `CeladonCrackleLoading.vue` 中的 GSAP timeline:

```javascript
// 加快主裂纹扩散
tl.fromTo('.main-crack', {...}, {
  duration: 0.3,  // 原本 0.6s → 改为 0.3s
  ...
})
```

### Q2: 如何修改颜色？

**A**: 修改组件的 CSS 变量:

```scss
.main-crack {
  stroke: rgba(255, 0, 0, 0.6) !important;  // 改为红色
}
```

### Q3: 如何减少碎片数量（优化性能）？

**A**: 修改 `generateFragments()` 函数:

```typescript
// 从 120 个减少到 60 个
for (let i = 0; i < 60; i++) {
  fragments.value.push({...})
}
```

### Q4: 音效太响/太小？

**A**: 修改 `playcrackSound()` 的音量参数:

```typescript
playcrackSound(0.3)  // 0.3 = 30% 音量
playcrackSound(0.7)  // 0.7 = 70% 音量
```

---

## 📊 性能建议

### 设备适配

```typescript
// 根据设备性能调整碎片数量
const isMobile = /iPhone|iPad|Android/i.test(navigator.userAgent)
const fragmentCount = isMobile ? 40 : 120

for (let i = 0; i < fragmentCount; i++) {
  fragments.value.push({...})
}
```

### 优化级别

| 设备 | 碎片数 | 裂纹密度 | 动画时长 |
|------|--------|----------|----------|
| 高端 | 120 | 8+16+24 | 4.5s |
| 中端 | 80 | 6+12+16 | 3.5s |
| 低端 | 40 | 4+8+8 | 2.5s |

---

## 📚 相关文档

- **详细文档**: `docs/CELADON_CRACKLE_LOADING.md`
- **演示页面**: `http://localhost:5173/demo/crackle`
- **源码**: `src/components/loading/CeladonCrackleLoading.vue`

---

## 🐉 诗意表达

**"银瓶乍破水浆迸，碎玉落叠冰壶裂"**

- 表面：青花瓷的素雅
- 内里：青龙的力量
- 破碎：重生的希望

🏺✨🐉

---

**创建时间**: 2025-01-14  
**创建人**: 李大飞  
**状态**: ✅ 可用
