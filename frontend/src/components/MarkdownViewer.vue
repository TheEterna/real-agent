<script setup lang="ts">
// Lightweight markdown viewer component
// Keep dependencies local to this component to enable lazy loading
// @ts-ignore
import MarkdownIt from 'markdown-it'
// @ts-ignore
import DOMPurify from 'dompurify'


// @ts-ignore
import * as markdownItEmoji from 'markdown-it-emoji'
// @ts-ignore
import * as markdownItTaskLists from 'markdown-it-task-lists'
// @ts-ignore
import * as container from 'markdown-it-container'
// @ts-ignore
import * as markdownItAnchor from 'markdown-it-anchor'
// @ts-ignore
import * as mkatex from 'markdown-it-katex'

// @ts-ignore
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import * as anchor from "markdown-it-anchor"; // 使用 GitHub 风格的代码高亮

const props = defineProps<{ message: string | undefined }>()
// 渲染Markdown
// 兼容 Vite 对 CommonJS/ESM 插件的导入：有的为 default，有的为命名空间对象
const resolvePlugin = (p: any) => {
  if (!p) return p
  // 优先 default
  const cand = (p as any).default ?? p
  if (typeof cand === 'function') return cand
  // 若仍为对象，尝试在其键里找到函数导出
  for (const key of Object.keys(p)) {
    const v = (p as any)[key]
    if (typeof v === 'function') return v
  }
  return cand
}

// 配置 markdown-it 实例，启用所有已安装的插件
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  // 代码高亮配置
  highlight: (str: string, lang: string) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang, ignoreIllegals: true }).value}</code></pre>`
      } catch (e) {
        console.error('代码高亮失败:', e)
      }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})
  .use(resolvePlugin(markdownItEmoji)) // emoji 支持 :smile:
  .use(resolvePlugin(markdownItTaskLists), { enabled: true }) // 任务列表 - [ ] todo
  .use(resolvePlugin(markdownItAnchor)) // 标题锚点
  .use(resolvePlugin(mkatex))
  .use(resolvePlugin(container), 'info')
  .use(resolvePlugin(container), 'warning')
  .use(resolvePlugin(container), 'success')


const renderMarkdown = (message: string) => {
  const unsafe = md.render(message || '')
  return DOMPurify.sanitize(unsafe)
}
</script>

<template>
  <div class="markdown-message" v-html="renderMarkdown(props.message || '')"></div>
</template>

<style scoped>
.markdown-message {
  line-height: 1.6;
  color: #333;
}

/* 标题样式 */
.markdown-message :deep(h1),
.markdown-message :deep(h2),
.markdown-message :deep(h3),
.markdown-message :deep(h4),
.markdown-message :deep(h5),
.markdown-message :deep(h6) {
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  font-weight: 600;
  line-height: 1.3;
}

.markdown-message :deep(h1) { font-size: 2em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
.markdown-message :deep(h2) { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
.markdown-message :deep(h3) { font-size: 1.25em; }
.markdown-message :deep(h4) { font-size: 1em; }
.markdown-message :deep(h5) { font-size: 0.875em; }
.markdown-message :deep(h6) { font-size: 0.85em; color: #6a737d; }

/* 段落与文本 */
.markdown-message :deep(p) {
  margin-top: 0;
  margin-bottom: 1em;
}

.markdown-message :deep(strong) {
  font-weight: 600;
}

.markdown-message :deep(em) {
  font-style: italic;
}

/* 链接样式 */
.markdown-message :deep(a) {
  color: #0366d6;
  text-decoration: none;
}

.markdown-message :deep(a:hover) {
  text-decoration: underline;
}

/* 有序列表样式 - 关键修复 */
.markdown-message :deep(ol) {
  padding-left: 2em;
  margin-top: 0;
  margin-bottom: 1em;
  list-style-type: decimal; /* 显示 1. 2. 3. */
}

/* 无序列表样式 */
.markdown-message :deep(ul) {
  padding-left: 2em;
  margin-top: 0;
  margin-bottom: 1em;
  list-style-type: disc; /* 显示圆点 */
}

/* 嵌套列表 */
.markdown-message :deep(ul ul),
.markdown-message :deep(ol ol),
.markdown-message :deep(ul ol),
.markdown-message :deep(ol ul) {
  margin-top: 0;
  margin-bottom: 0;
}

.markdown-message :deep(li) {
  margin-bottom: 0.25em;
}

/* 任务列表样式 */
.markdown-message :deep(.task-list-item) {
  list-style-type: none;
}

.markdown-message :deep(.task-list-item-checkbox) {
  margin-right: 0.5em;
  margin-left: -1.5em;
}

/* 代码块样式 */
.markdown-message :deep(pre) {
  background: #f6f8fa;
  padding: 1rem;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 0.875rem;
  line-height: 1.45;
  margin-bottom: 1em;
}

.markdown-message :deep(code) {
  background: #f6f8fa;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-size: 0.9em;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.markdown-message :deep(pre code) {
  background: transparent;
  padding: 0;
  font-size: inherit;
}

/* 引用块样式 */
.markdown-message :deep(blockquote) {
  padding: 0 1em;
  color: #6a737d;
  border-left: 0.25em solid #dfe2e5;
  margin: 0 0 1em 0;
}

.markdown-message :deep(blockquote > :first-child) {
  margin-top: 0;
}

.markdown-message :deep(blockquote > :last-child) {
  margin-bottom: 0;
}

/* 表格样式 */
.markdown-message :deep(table) {
  border-collapse: collapse;
  border-spacing: 0;
  width: 100%;
  margin-bottom: 1em;
  overflow: auto;
}

.markdown-message :deep(table th),
.markdown-message :deep(table td) {
  padding: 6px 13px;
  border: 1px solid #dfe2e5;
}

.markdown-message :deep(table th) {
  font-weight: 600;
  background-color: #f6f8fa;
}

.markdown-message :deep(table tr) {
  background-color: #fff;
  border-top: 1px solid #c6cbd1;
}

.markdown-message :deep(table tr:nth-child(2n)) {
  background-color: #f6f8fa;
}

/* 水平线 */
.markdown-message :deep(hr) {
  height: 0.25em;
  padding: 0;
  margin: 24px 0;
  background-color: #e1e4e8;
  border: 0;
}

/* 图片样式 */
.markdown-message :deep(img) {
  max-width: 100%;
  box-sizing: border-box;
  border-radius: 4px;
}
</style>
