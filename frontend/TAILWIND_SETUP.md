# Tailwind CSS 安装指南

## 快速开始

### 1. 安装依赖包

在项目根目录 (`real-agent-font`) 下执行以下命令:

```bash
npm install -D tailwindcss @tailwindcss/vite postcss autoprefixer
```

或者使用 yarn:

```bash
yarn add -D tailwindcss @tailwindcss/vite postcss autoprefixer
```

### 2. 验证安装

安装完成后,检查 `package.json` 的 `devDependencies` 中是否包含:

```json
{
  "devDependencies": {
    "@tailwindcss/vite": "^4.x.x",
    "autoprefixer": "^10.x.x",
    "postcss": "^8.x.x",
    "tailwindcss": "^4.x.x"
  }
}
```

### 3. 启动开发服务器

```bash
npm run dev
```

### 4. 验证 Tailwind 是否生效

创建一个测试组件或在现有组件中添加 Tailwind 类:

```vue
<template>
  <div class="flex items-center justify-center h-screen bg-gradient-to-r from-blue-500 to-purple-600">
    <h1 class="text-4xl font-bold text-white">
      Tailwind CSS 已成功安装! 🎉
    </h1>
  </div>
</template>
```

如果样式生效,说明 Tailwind CSS 已正确配置。

---

## 已完成的配置

以下配置文件已经为您准备好,无需手动修改:

### ✅ `vite.config.ts`
已添加 Tailwind CSS Vite 插件

### ✅ `tailwind.config.js`
已创建 Tailwind 配置文件,包含:
- 内容扫描路径配置
- 与 Ant Design Vue 兼容的主题色
- 自定义间距、圆角、字体等扩展

### ✅ `src/styles/index.scss`
已在主样式文件中引入 Tailwind CSS

### ✅ `docs/TAILWIND_CSS_GUIDE.md`
详细的 Tailwind CSS 使用规范文档

---

## 故障排除

### 问题 1: PowerShell 执行策略错误

如果在 Windows PowerShell 中遇到执行策略错误:

```
无法加载文件 C:\...\npm.ps1,因为在此系统上禁止运行脚本
```

**解决方案:**

1. 以管理员身份运行 PowerShell
2. 执行: `Set-ExecutionPolicy RemoteSigned -Scope CurrentUser`
3. 或者使用 CMD 代替 PowerShell 执行 npm 命令

### 问题 2: 样式不生效

1. 确认已执行 `npm install`
2. 重启开发服务器
3. 清除浏览器缓存
4. 检查浏览器控制台是否有错误信息

### 问题 3: 与 Ant Design Vue 样式冲突

如果遇到样式冲突,可以在 `tailwind.config.js` 中禁用 Tailwind 的基础样式重置:

```javascript
export default {
  corePlugins: {
    preflight: false, // 禁用 Tailwind 的样式重置
  },
}
```

---

## 下一步

1. 阅读 `docs/TAILWIND_CSS_GUIDE.md` 了解详细使用规范
2. 查看 [Tailwind CSS 官方文档](https://tailwindcss.com/docs)
3. 开始在项目中使用 Tailwind CSS 构建界面

---

**提示**: 如果您在安装过程中遇到任何问题,请参考上述故障排除部分或查阅官方文档。
