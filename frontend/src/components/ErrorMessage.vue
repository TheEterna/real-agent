<script setup lang="ts">
import { computed, ref } from 'vue'
import { UIMessage } from '@/types/events'

interface Props {
  message: UIMessage
  collapsible?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  collapsible: true
})

const isExpanded = ref(false)

// 从错误消息中提取错误类型
const errorType = computed(() => {
  const msg = props.message.message || ''

  if (msg.includes('超时') || msg.includes('timeout') || msg.includes('timed out')) {
    return {
      type: 'timeout',
      icon: '⏱️',
      title: '请求超时',
      severity: 'warning'
    }
  }
  else if (msg.includes('rate limit') || msg.includes('频率限制') || msg.includes('quota')) {
    return {
      type: 'rate_limit',
      icon: '🚦',
      title: 'API 调用限制',
      severity: 'warning'
    }
  }
  else if (msg.includes('authentication') || msg.includes('认证失败') || msg.includes('unauthorized')) {
    return {
      type: 'auth',
      icon: '🔑',
      title: '认证失败',
      severity: 'error'
    }
  }
  else if (msg.includes('connection') || msg.includes('连接失败') || msg.includes('connect')) {
    return {
      type: 'network',
      icon: '📡',
      title: '网络连接失败',
      severity: 'error'
    }
  }
  else if (msg.includes('工具') || msg.includes('tool')) {
    return {
      type: 'tool',
      icon: '🔧',
      title: '工具执行错误',
      severity: 'error'
    }
  }
  else if (msg.includes('LLM')) {
    return {
      type: 'llm',
      icon: '🤖',
      title: 'AI 模型错误',
      severity: 'error'
    }
  }
  else {
    return {
      type: 'unknown',
      icon: '❌',
      title: '执行错误',
      severity: 'error'
    }
  }
})

// 生成建议操作
const suggestions = computed(() => {
  const type = errorType.value.type
  const suggestions: string[] = []

  switch (type) {
    case 'timeout':
      suggestions.push('检查网络连接是否稳定')
      suggestions.push('稍后重试')
      suggestions.push('考虑增加超时时间设置')
      break
    case 'rate_limit':
      suggestions.push('等待几分钟后重试')
      suggestions.push('检查 API 配额使用情况')
      suggestions.push('考虑升级 API 套餐')
      break
    case 'auth':
      suggestions.push('检查 API Key 是否正确配置')
      suggestions.push('验证认证凭据是否过期')
      suggestions.push('确认服务端配置文件')
      break
    case 'network':
      suggestions.push('检查网络连接')
      suggestions.push('验证服务地址是否正确')
      suggestions.push('检查防火墙设置')
      break
    case 'tool':
      suggestions.push('确认工具参数是否正确')
      suggestions.push('检查工具是否已正确注册')
      suggestions.push('查看后端日志获取详细信息')
      break
    case 'llm':
      suggestions.push('检查模型服务是否正常')
      suggestions.push('验证 API 配置')
      suggestions.push('查看后端日志')
      break
    default:
      suggestions.push('查看详细错误信息')
      suggestions.push('检查后端日志')
      suggestions.push('联系技术支持')
  }

  return suggestions
})

const toggleExpand = () => {
  if (props.collapsible) {
    isExpanded.value = !isExpanded.value
  }
}

// 格式化时间
const formatTime = (ts?: Date | string) => {
  if (!ts) return ''
  const d = ts instanceof Date ? ts : new Date(ts)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}
</script>

<template>
  <div :class="['error-message', `error-${errorType.severity}`]">
    <div class="error-header" @click="toggleExpand">
      <div class="error-title-row">
        <span class="error-icon">{{ errorType.icon }}</span>
        <span class="error-title">{{ errorType.title }}</span>
        <span v-if="collapsible" class="expand-icon">
          {{ isExpanded ? '▼' : '▶' }}
        </span>
      </div>
      <span class="error-startTime">{{ formatTime(message.startTime) }}</span>
    </div>

    <div class="error-body">
      <!-- 错误消息摘要 -->
      <div class="error-summary">
        <p class="error-text">{{ message.message }}</p>
      </div>

      <!-- 展开的详细信息 -->
      <div v-if="isExpanded || !collapsible" class="error-details">
        <!-- 建议操作 -->
        <div v-if="suggestions.length > 0" class="error-suggestions">
          <h4 class="suggestions-title">💡 建议操作</h4>
          <ul class="suggestions-list">
            <li v-for="(suggestion, index) in suggestions" :key="index">
              {{ suggestion }}
            </li>
          </ul>
        </div>

        <!-- 技术详情（如果有） -->
        <div v-if="message.data" class="error-technical">
          <h4 class="technical-title">🔍 技术详情</h4>
          <pre class="technical-data">{{ JSON.stringify(message.data, null, 2) }}</pre>
        </div>

        <!-- Agent 信息 -->
        <div v-if="message.sender" class="error-meta">
          <span class="meta-label">来源:</span>
          <span class="meta-value">{{ message.sender }}</span>
          <span v-if="message.nodeId" class="meta-label">节点ID:</span>
          <span v-if="message.nodeId" class="meta-value">{{ message.nodeId }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.error-message {
  margin-bottom: 1rem;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
}

.error-warning {
  background: linear-gradient(135deg, #fff8e1 0%, #ffecb3 100%);
  border: 2px solid #ffb300;
}

.error-error {
  background: linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%);
  border: 2px solid #f44336;
}

.error-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s ease;

  &:hover {
    background: rgba(0, 0, 0, 0.03);
  }
}

.error-title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
}

.error-icon {
  font-size: 1.5rem;
  line-height: 1;
}

.error-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #d32f2f;

  .error-warning & {
    color: #f57c00;
  }
}

.expand-icon {
  font-size: 0.75rem;
  color: #666;
  transition: transform 0.3s ease;
  margin-left: auto;
}

.error-startTime {
  font-size: 0.85rem;
  color: #666;
  margin-left: 1rem;
}

.error-body {
  padding: 0 1.25rem 1.25rem;
}

.error-summary {
  margin-bottom: 1rem;
}

.error-text {
  font-size: 0.95rem;
  line-height: 1.6;
  color: #c62828;
  margin: 0;
  word-break: break-word;

  .error-warning & {
    color: #e65100;
  }
}

.error-details {
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  padding-top: 1rem;
  animation: slideDown 0.3s ease;
}

.error-suggestions {
  margin-bottom: 1rem;
}

.suggestions-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1976d2;
  margin: 0 0 0.5rem 0;
}

.suggestions-list {
  margin: 0;
  padding-left: 1.5rem;

  li {
    font-size: 0.9rem;
    line-height: 1.8;
    color: #424242;

    &::marker {
      color: #1976d2;
    }
  }
}

.error-technical {
  margin-bottom: 1rem;
}

.technical-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: #6a1b9a;
  margin: 0 0 0.5rem 0;
}

.technical-data {
  background: #0f172a;
  color: #e2e8f0;
  padding: 0.75rem;
  border-radius: 8px;
  font-size: 0.85rem;
  line-height: 1.4;
  overflow-x: auto;
  margin: 0;
  font-family: 'Monaco', 'Consolas', monospace;
}

.error-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  font-size: 0.85rem;
  color: #666;
}

.meta-label {
  font-weight: 600;
  color: #424242;
}

.meta-value {
  color: #666;
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 500px;
  }
}
</style>
