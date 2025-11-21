# Real Agent UI/UX 设计系统文档

> **对标目标**:Gemini、ChatGPT、Manus 极简洁、极精美、高效空间利用、协调主题色

---

## 一、设计哲学 (Design Philosophy)

### 1.1 核心原则

#### 🎯 **极简主义 (Minimalism)**
- **去除视觉噪音**: 仅保留必要元素,每个组件有明确目的
- **呼吸空间**: 善用留白,不强塞内容
- **渐进展示**: 默认简洁,按需展开细节

#### ✨ **精美视觉 (Visual Excellence)**
- **精致微交互**: 动画柔和流畅 (0.2-0.4s easing)
- **高品质渐变**: 微妙的渐变背景,不过度使用
- **细腻阴影层次**: 3级阴影系统 (sm/md/lg)
- **圆角一致性**: 卡片 8-12px, 按钮 6-8px,小组件 4-6px

#### 📐 **空间效率 (Space Efficiency)**
- **组件压缩技术**:
  - Collapse/Expand 面板
  - Tabs/Segmented Control 切换视图
  - Tooltip/Popover 悬浮显示细节
  - Modal/Drawer 临时层级
- **垂直节奏**: 8px grid system (4/8/12/16/24/32)
- **响应式密度**: 根据屏幕宽度动态调整信息密度

#### 🎨 **协调主题色 (Harmonious Theme)**
- **主色调**: Teal/Cyan (青绿色系) - 平静、智能、科技
- **辅助色**: 青蓝色系 - 信息提示、交互反馈
- **功能色**: 
  - Success: #52c41a (绿色)
  - Warning: #fa8c16 (橙色)
  - Error: #ff4d4f (红色)
  - Info: #00bac4 (青蓝)

---

## 二、色彩系统 (Color System)

### 2.1 主色调 (Primary Colors)

```scss
// 青绿色系 (Teal) - 主品牌色
$primary-color-50:  #f0f7f7   // 背景 - 极浅
$primary-color-100: #d8ebea   // 背景 - 浅
$primary-color-200: #b8ddd9   // 边框 - hover
$primary-color-300: #98cfc8   // 边框 - default
$primary-color-500: #6b9a98   // 主色调 - 核心
$primary-color-700: #4a6d6c   // 深色 - 文字
$primary-color-900: #2a3f3f   // 最深 - 强调
```

**使用场景**:
- 50-100: 页面背景、卡片背景、侧边栏背景
- 200-300: 边框、分割线、禁用状态
- 500: 主按钮、链接、图标、活动状态
- 700-900: 标题文字、重要文本

### 2.2 辅助色 (Secondary Colors)

```scss
// 青蓝色系 (Cyan-Blue) - 辅助色/强调色
$secondary-color-50:  #e0fafa   // 信息提示背景
$secondary-color-100: #99e6e6   // 浅青蓝
$secondary-color-300: #33cfd2   // 适中青蓝 - 图标/徽章
$secondary-color-500: #009aa7   // 核心辅助色
$secondary-color-700: #00606b   // 深青蓝 - 重要信息
```

**使用场景**:
- 信息徽章 (badges)
- 辅助按钮
- 强调区域边框
- 通知/提示组件

### 2.3 Jelly 色彩系统 (UI 组件专用)

**设计理念**: 柔和、果冻质感、低饱和度,适用于状态指示、标签、卡片

#### 粉色系 (Pink) - 温馨、标签
```scss
$jelly-pink-lightest: #fffafb
$jelly-pink-light:    #ffebee
$jelly-pink:          #fce4ec
$jelly-pink-deep:     #f3bddb
```

#### 蓝色系 (Blue) - 信息、状态
```scss
$jelly-blue-light: #e3f2fd
$jelly-blue:       #bbdefb
$jelly-blue-deep:  #90caf9
```

#### 绿色系 (Green) - 成功、完成
```scss
$jelly-green-light: #e8f5e9
$jelly-green:       #c8e6c9
$jelly-green-deep:  #a5d6a7
```

#### 黄色系 (Yellow) - 警告、待处理
```scss
$jelly-yellow-lightest: #fbf9f2
$jelly-yellow-light:    #fff8e1
$jelly-yellow:          #ffeeba
$jelly-yellow-deep:     #ffe082
```

#### 红色系 (Red) - 错误、失败
```scss
$jelly-red-lightest: #fff0f2
$jelly-red-light:    #ffccd5
$jelly-red:          #ff9eaf
$jelly-red-deep:     #ff6b8e
```

### 2.4 文本色彩 (Text Colors)

```scss
// 深色文本层次
$text-primary:    rgba(51, 65, 85, 0.9)    // 主标题
$text-secondary:  rgba(51, 65, 85, 0.75)   // 副文本
$text-tertiary:   rgba(51, 65, 85, 0.5)    // 辅助信息
$text-quaternary: rgba(51, 65, 85, 0.3)    // 占位符/禁用
```

**使用规则**:
- **Primary**: 页面标题、重要内容
- **Secondary**: 正文、描述
- **Tertiary**: 提示文字、时间戳
- **Quaternary**: 占位符、禁用状态

---

## 三、排版系统 (Typography)

### 3.1 字体栈 (Font Stack)

```css
font-family: -apple-system, BlinkMacSystemFont, 
             "Segoe UI Variable Display", "Segoe UI", 
             Helvetica, Arial, sans-serif,
             "Apple Color Emoji", "Segoe UI Emoji";
```

**优先级**: 系统字体 > Segoe UI > Helvetica > Arial

### 3.2 字号系统 (Font Sizes)

| 用途 | 字号 | 行高 | 字重 |
|------|------|------|------|
| **超大标题** | 28px | 1.3 | 600-700 |
| **大标题** | 20-24px | 1.4 | 600 |
| **小标题** | 16-18px | 1.4 | 600 |
| **正文** | 14px | 1.5 | 400-500 |
| **辅助文字** | 12-13px | 1.4 | 400-500 |
| **小字** | 10-11px | 1.3 | 400-500 |

### 3.3 排版规则

1. **标题与正文对比**: 标题字号至少比正文大 4px
2. **行高**: 正文 1.5-1.6,标题 1.3-1.4
3. **字间距**: 标题略紧 (-0.01em),正文正常
4. **段落间距**: 12-16px

---

## 四、间距系统 (Spacing System)

### 4.1 基础单位 (Base Units)

```scss
$space-xs: 4px    // 微小间距
$space-sm: 8px    // 小间距
$space-md: 12px   // 中间距
$space-lg: 16px   // 大间距
$space-xl: 24px   // 超大间距
$space-2xl: 32px  // 巨大间距
```

### 4.2 组件内部间距 (Component Padding)

| 组件类型 | Padding |
|---------|---------|
| **卡片 (Card)** | 16px (lg) |
| **按钮 (Button)** | 8px 16px |
| **输入框 (Input)** | 8px 12px |
| **Modal** | 24px (xl) |
| **侧边栏** | 16-18px |
| **列表项** | 12px 16px |

### 4.3 间距应用原则

1. **垂直节奏**: 使用 8px grid
2. **组件间距**: 12-16px (md-lg)
3. **章节间距**: 24-32px (xl-2xl)
4. **紧凑模式**: 减少 50% 间距

---

## 五、组件设计规范 (Component Guidelines)

### 5.1 按钮 (Buttons)

#### 主按钮 (Primary)
```scss
background: linear-gradient(135deg, #6b9a98, #50c8b7);
padding: 8px 16px;
border-radius: 6-8px;
box-shadow: 0 2px 8px rgba(107, 154, 152, 0.2);
transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

&:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(107, 154, 152, 0.3);
}
```

#### 次要按钮 (Secondary)
```scss
background: transparent;
border: 1px solid $primary-color-300;
color: $primary-color-700;

&:hover {
  background: $primary-color-50;
  border-color: $primary-color-500;
}
```

### 5.2 卡片 (Cards)

```scss
background: linear-gradient(135deg, 
            rgba(255,255,255,0.95), 
            rgba(248,250,252,0.95));
border-radius: 12px;
border: 1px solid rgba(0,0,0,0.05);
padding: 16px;
box-shadow: 0 2px 8px rgba(0,0,0,0.08);

&:hover {
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}
```

### 5.3 输入框 (Inputs)

```scss
border: 1px solid $primary-color-200;
border-radius: 6px;
padding: 8px 12px;
font-size: 14px;
transition: all 0.3s;

&:focus {
  border-color: $primary-color-500;
  box-shadow: 0 0 0 3px rgba(107, 154, 152, 0.1);
  outline: none;
}
```

### 5.4 徽章/标签 (Badges/Tags)

```scss
// 状态徽章
padding: 2px 8px;
border-radius: 12px;  // 全圆角
font-size: 11px;
font-weight: 500;

// Todo状态
background: $jelly-yellow-light;
color: #ad6800;

// Running状态
background: $jelly-green;
color: #237804;
animation: pulse 2s infinite;

// Completed状态
background: $jelly-green-deep;
color: white;
```

### 5.5 导航菜单 (Navigation)

```scss
// 侧边栏导航项
padding: 8px 12px;
border-radius: 16px;  // 较大圆角,柔和
transition: all 0.3s;

// 活动状态
background: $primary-color-100;
color: $primary-color-700;
border-left: 3px solid $primary-color-500;

// Hover状态
background: rgba($primary-color-900, 0.08);
```

---

## 六、动画与过渡 (Animation & Transitions)

### 6.1 缓动函数 (Easing Functions)

```scss
$ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);   // 默认
$ease-out: cubic-bezier(0, 0, 0.2, 1);        // 快入慢出
$ease-in: cubic-bezier(0.4, 0, 1, 1);         // 慢入快出
$ease-bounce: cubic-bezier(0.68, -0.55, 0.265, 1.55); // 弹性
```

### 6.2 时长标准 (Duration Standards)

| 动画类型 | 时长 |
|---------|------|
| **微交互** (按钮) | 0.2s |
| **组件过渡** | 0.3s |
| **页面切换** | 0.4s |
| **复杂动画** | 0.5-0.8s |

### 6.3 常用动画模式

#### Fade In/Out
```scss
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s $ease-in-out;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
```

#### Slide Up
```scss
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
```

#### Pulse (强调)
```scss
@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(107, 154, 152, 0.7);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(107, 154, 152, 0);
  }
}
```

---

## 七、响应式设计 (Responsive Design)

### 7.1 断点系统 (Breakpoints)

```scss
$breakpoint-xs: 480px;   // 手机
$breakpoint-sm: 768px;   // 平板
$breakpoint-md: 1024px;  // 小屏笔记本
$breakpoint-lg: 1280px;  // 桌面
$breakpoint-xl: 1920px;  // 大屏
```

### 7.2 布局策略

- **< 768px**: 单列,侧边栏收起,全屏模态
- **768-1024px**: 双列,侧边栏可切换
- **> 1024px**: 多列,侧边栏常驻

### 7.3 字号缩放

```scss
// 移动端字号略小
@media (max-width: $breakpoint-sm) {
  html { font-size: 14px; }
}

// 桌面端标准
@media (min-width: $breakpoint-md) {
  html { font-size: 16px; }
}
```

---

## 八、交互模式 (Interaction Patterns)

### 8.1 反馈机制

| 交互 | 反馈方式 |
|-----|---------|
| **点击** | 0.2s 缩放 (scale: 0.98) |
| **悬停** | 提升 2px, 增强阴影 |
| **加载** | 骨架屏/加载动画 |
| **成功** | ✓ 图标 + 绿色提示 |
| **错误** | ⚠ 图标 + 红色提示 + 震动 |

### 8.2 空间压缩技巧

#### Collapse 折叠面板
```vue
<a-collapse ghost :bordered="false">
  <a-collapse-panel header="详细信息">
    <!-- 详情内容 -->
  </a-collapse-panel>
</a-collapse>
```

#### Tooltip 悬浮提示
```vue
<a-tooltip placement="top">
  <template #title>详细说明</template>
  <InfoCircleOutlined/>
</a-tooltip>
```

#### Drawer 抽屉
```vue
<a-drawer 
  v-model:open="visible" 
  width="400" 
  placement="right">
  <!-- 侧边详情 -->
</a-drawer>
```

### 8.3 滚动优化

```scss
// 自定义滚动条
@mixin pretty-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: $primary-color-200 transparent;
  
  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba($primary-color-200, 0.8);
    border-radius: 6px;
  }
}
```

---

## 九、对标分析 (Benchmark Analysis)

### 9.1 Gemini 特点
- ✅ **极简输入框**: 中央大输入框,无干扰
- ✅ **渐变背景**: 微妙的背景渐变
- ✅ **卡片式消息**: 圆角卡片,清晰层次
- ✅ **动画流畅**: 消息逐字显示,过渡柔和

### 9.2 ChatGPT 特点
- ✅ **侧边栏历史**: 紧凑的对话列表
- ✅ **简洁导航**: 左侧极简图标导航
- ✅ **Markdown 渲染**: 代码高亮,表格支持
- ✅ **暗色模式**: 护眼的深色主题

### 9.3 Manus 特点
- ✅ **空间高效**: 多面板布局,信息密度高
- ✅ **图标系统**: 统一的图标语言
- ✅ **状态指示**: 清晰的任务状态可视化
- ✅ **快捷操作**: 悬浮操作按钮

### 9.4 Real Agent 应坚持的优势
- ✨ **青花瓷过渡动画**: 独特的文化标识
- ✨ **Plan 可视化**: 直观的计划展示
- ✨ **Jelly 色彩系统**: 温和友好的色调
- ✨ **多模式切换**: Geek/Multimodal 模式

---

## 十、设计检查清单 (Design Checklist)

### 新组件设计时必须考虑:

#### ✅ 视觉层面
- [ ] 使用主题色系 (Primary/Secondary)
- [ ] 遵循间距系统 (8px grid)
- [ ] 圆角统一 (卡片 12px, 按钮 6-8px)
- [ ] 阴影层次合理 (sm/md/lg)
- [ ] 文字层次清晰 (Primary/Secondary/Tertiary)

#### ✅ 交互层面
- [ ] Hover 状态有反馈
- [ ] 点击有视觉反馈 (0.2s)
- [ ] 加载状态有指示
- [ ] 错误提示清晰
- [ ] 过渡动画流畅 (0.3s)

#### ✅ 空间效率
- [ ] 能否折叠/展开?
- [ ] 能否用 Tooltip 替代?
- [ ] 能否用 Tab 切换?
- [ ] 能否用 Drawer 隐藏?
- [ ] 响应式适配?

#### ✅ 可访问性
- [ ] 颜色对比度 > 4.5:1
- [ ] 可键盘导航
- [ ] 有明确的焦点指示
- [ ] 支持屏幕阅读器

---

## 十一、实现建议 (Implementation Recommendations)

### 11.1 优先级
1. **P0 - 核心体验**: 输入框、消息列表、导航
2. **P1 - 关键功能**: 按钮、卡片、表单
3. **P2 - 增强体验**: 动画、过渡、微交互

### 11.2 技术栈选择
- **CSS 预处理器**: SCSS (已使用)
- **动画库**: GSAP (已集成,流畅强大)
- **组件库**: Ant Design Vue (已集成,需定制主题)
- **图标**: Ant Design Icons (统一性)

### 11.3 渐进式改进
1. **Phase 1**: 统一主题色,调整间距
2. **Phase 2**: 优化关键组件 (按钮、卡片、输入框)
3. **Phase 3**: 添加动画过渡
4. **Phase 4**: 空间优化 (Collapse, Drawer)
5. **Phase 5**: 暗色模式支持

---

## 十二、禁止事项 (Don'ts)

### ❌ 视觉禁忌
- **过度渐变**: 避免使用超过 2 个色阶的渐变
- **色彩混乱**: 一个界面不超过 3 种主色
- **阴影过重**: box-shadow 不超过 0.15 透明度
- **圆角不一致**: 同类组件圆角必须一致

### ❌ 交互禁忌
- **过慢动画**: 动画时长不超过 0.5s
- **突兀出现**: 所有元素都应有入场动画
- **无反馈**: 点击、悬停必须有反馈
- **加载黑洞**: 超过 1s 的操作必须显示加载状态

### ❌ 空间禁忌
- **信息过载**: 一屏不超过 3 个关键信息区
- **间距不均**: 必须遵循 8px grid
- **挤压内容**: 留白不足 8px 视为过密

---

## 附录 A: 常用 SCSS Mixins

```scss
// 卡片基础样式
@mixin plan-card-base {
  background: linear-gradient(135deg, 
              rgba(255,255,255,0.95), 
              rgba(248,250,252,0.95));
  border-radius: 12px;
  border: 1px solid rgba(0,0,0,0.05);
  padding: $space-lg;
  transition: all 0.3s ease;
}

// 漂亮的滚动条
@mixin pretty-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: $primary-color-200 transparent;
  &::-webkit-scrollbar { width: 8px; height: 8px; }
  &::-webkit-scrollbar-thumb {
    background: rgba($primary-color-200, 0.8);
    border-radius: 6px;
  }
}

// 状态样式
@mixin plan-status-style($color, $bg-color) {
  border-left: 4px solid $color;
  background: linear-gradient(135deg, 
              $bg-color 0%, 
              rgba($bg-color, 0.5) 100%);
}
```

---

**文档版本**: v1.0  
**最后更新**: 2025-11-22  
**维护者**: Real Agent Team
