<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { SSE } from 'sse.js'

const messages = ref<string[]>([])
const isConnected = ref(false)
const testMessage = ref('Hello SSE Test')

// 测试GET方式的SSE连接
const testGetSse = () => {
  messages.value.push('开始测试GET SSE连接...')
  
  const source = new EventSource('/api/test/sse')
  
  source.onopen = () => {
    isConnected.value = true
    messages.value.push('✅ GET SSE连接已建立')
  }
  
  source.onmessage = (event) => {
    messages.value.push(`📨 收到消息: ${event.data}`)
  }
  
  source.onerror = (error) => {
    isConnected.value = false
    messages.value.push(`❌ GET SSE连接错误: ${error}`)
    source.close()
  }
  
  // 10秒后自动关闭
  setTimeout(() => {
    source.close()
    isConnected.value = false
    messages.value.push('GET SSE连接已关闭')
  }, 12000)
}

// 测试POST方式的SSE连接
const testPostSse = () => {
  messages.value.push('开始测试POST SSE连接...')
  
  const source = new SSE('/api/test/sse', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    },
    payload: JSON.stringify(testMessage.value)
  })
  
  source.addEventListener('open', () => {
    isConnected.value = true
    messages.value.push('✅ POST SSE连接已建立')
  })
  
  source.addEventListener('message', (event: MessageEvent) => {
    messages.value.push(`📨 收到消息: ${event.data}`)
  })
  
  source.addEventListener('error', (error: any) => {
    isConnected.value = false
    messages.value.push(`❌ POST SSE连接错误: ${JSON.stringify(error)}`)
    try { (source as any).close() } catch {}
  })
  
  // 启动连接
  ;(source as any).stream()
  
  // 6秒后自动关闭
  setTimeout(() => {
    try { (source as any).close() } catch {}
    isConnected.value = false
    messages.value.push('POST SSE连接已关闭')
  }, 7000)
}

// 测试ReAct SSE连接
const testReActSse = () => {
  messages.value.push('开始测试ReAct SSE连接...')
  
  const source = new SSE('/api/agent/chat/react/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    },
    payload: JSON.stringify({
      message: testMessage.value,
      userId: 'test-user',
      sessionId: `test-session-${Date.now()}`,
      agentType: 'ReAct'
    })
  })
  
  source.addEventListener('open', () => {
    isConnected.value = true
    messages.value.push('✅ ReAct SSE连接已建立')
  })
  
  source.addEventListener('message', (event: MessageEvent) => {
    messages.value.push(`📨 ReAct消息: ${event.data}`)
  })
  
  source.addEventListener('error', (error: any) => {
    isConnected.value = false
    messages.value.push(`❌ ReAct SSE连接错误: ${JSON.stringify(error)}`)
    try { (source as any).close() } catch {}
  })
  
  // 启动连接
  ;(source as any).stream()
  
  // 30秒后自动关闭
  setTimeout(() => {
    try { (source as any).close() } catch {}
    isConnected.value = false
    messages.value.push('ReAct SSE连接已关闭')
  }, 30000)
}

const clearMessages = () => {
  messages.value = []
}
</script>

<template>
  <div class="sse-test-container">
    <div class="test-header">
      <h2>SSE连接测试工具</h2>
      <div class="connection-status" :class="{ connected: isConnected }">
        {{ isConnected ? '🟢 已连接' : '🔴 未连接' }}
      </div>
    </div>

    <div class="test-controls">
      <div class="input-group">
        <label>测试消息:</label>
        <input v-model="testMessage" placeholder="输入测试消息" />
      </div>
      
      <div class="button-group">
        <button @click="testGetSse" :disabled="isConnected">测试GET SSE</button>
        <button @click="testPostSse" :disabled="isConnected">测试POST SSE</button>
        <button @click="testReActSse" :disabled="isConnected">测试ReAct SSE</button>
        <button @click="clearMessages">清空日志</button>
      </div>
    </div>

    <div class="messages-area">
      <h3>连接日志:</h3>
      <div class="messages-list">
        <div 
          v-for="(message, index) in messages" 
          :key="index"
          class="message-item"
        >
          <span class="startTime">{{ new Date().toLocaleTimeString() }}</span>
          <span class="message-text">{{ message }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sse-test-container {
  padding: 2rem;
  max-width: 1000px;
  margin: 0 auto;
}

.test-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #eee;
}

.test-header h2 {
  margin: 0;
  color: #333;
}

.connection-status {
  padding: 0.5rem 1rem;
  border-radius: 20px;
  background: #ffebee;
  color: #c62828;
  font-weight: bold;
}

.connection-status.connected {
  background: #e8f5e8;
  color: #2e7d32;
}

.test-controls {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-bottom: 2rem;
}

.input-group {
  margin-bottom: 1rem;
}

.input-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #555;
}

.input-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.button-group {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.button-group button {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.3s;
}

.button-group button:not(:last-child) {
  background: #007bff;
  color: white;
}

.button-group button:last-child {
  background: #6c757d;
  color: white;
}

.button-group button:hover:not(:disabled) {
  opacity: 0.8;
  transform: translateY(-1px);
}

.button-group button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.messages-area {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  overflow: hidden;
}

.messages-area h3 {
  margin: 0;
  padding: 1rem 1.5rem;
  background: #f8f9fa;
  border-bottom: 1px solid #eee;
  color: #333;
}

.messages-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 1rem;
}

.message-item {
  display: flex;
  gap: 1rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f0f0f0;
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
}

.message-item:last-child {
  border-bottom: none;
}

.startTime {
  color: #666;
  min-width: 80px;
}

.message-text {
  flex: 1;
  word-break: break-all;
}
</style>
