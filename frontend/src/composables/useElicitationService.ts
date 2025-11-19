import { ref, onUnmounted } from 'vue'

// 类型定义
export interface ElicitationNotification {
  elicitationId: string
  message: string
  schema?: any
  startTime: number
}

export interface ElicitationService {
  isConnected: boolean
  connect: () => void
  disconnect: () => void
  submitResponse: (elicitationId: string, data: Record<string, any>) => Promise<boolean>
}

/**
 * Elicitation 服务 - 处理前端的 elicitation 功能
 *
 * 功能：
 * 1. 通过 SSE 接收来自服务端的 elicitation 请求
 * 2. 提供 API 接口向服务端提交用户填写的数据
 * 3. 管理连接状态和错误处理
 *
 * @author 李大飞
 */
export function useElicitationService() {
  // 状态管理
  const isConnected = ref(false)
  const currentElicitation = ref<ElicitationNotification | null>(null)
  const error = ref<string | null>(null)

  let eventSource: EventSource | null = null
  let reconnectTimer: number | null = null
  let reconnectAttempts = 0
  const maxReconnectAttempts = 5
  const reconnectDelay = 3000 // 3秒

  /**
   * 建立 SSE 连接
   */
  function connect() {
    if (eventSource?.readyState === EventSource.OPEN) {
      console.log('Elicitation SSE already connected')
      return
    }

    try {
      console.log('Connecting to elicitation SSE...')
      eventSource = new EventSource('/api/elicitation/stream')

      eventSource.onopen = () => {
        console.log('Elicitation SSE connected successfully')
        isConnected.value = true
        error.value = null
        reconnectAttempts = 0

        // 清除重连计时器
        if (reconnectTimer) {
          clearTimeout(reconnectTimer)
          reconnectTimer = null
        }
      }

      eventSource.onmessage = (event) => {
        try {
          const notification: ElicitationNotification = JSON.parse(event.data)
          console.log('Received elicitation notification:', notification)

          // 更新当前的 elicitation 请求
          currentElicitation.value = notification

          // 可以在这里触发其他事件，比如显示模态框
          handleElicitationReceived(notification)

        } catch (err) {
          console.error('Failed to parse elicitation notification:', err)
          error.value = '解析 elicitation 通知失败'
        }
      }

      eventSource.onerror = (event) => {
        console.error('Elicitation SSE error:', event)
        isConnected.value = false
        error.value = 'SSE 连接错误'

        // 尝试重连
        if (reconnectAttempts < maxReconnectAttempts) {
          scheduleReconnect()
        } else {
          console.error('Max reconnection attempts reached for elicitation SSE')
          error.value = '超过最大重连次数，请刷新页面重试'
        }
      }

    } catch (err) {
      console.error('Failed to create elicitation SSE connection:', err)
      error.value = '创建 SSE 连接失败'
    }
  }

  /**
   * 断开 SSE 连接
   */
  function disconnect() {
    if (eventSource) {
      console.log('Disconnecting elicitation SSE...')
      eventSource.close()
      eventSource = null
    }

    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    isConnected.value = false
    reconnectAttempts = 0
  }

  /**
   * 计划重连
   */
  function scheduleReconnect() {
    if (reconnectTimer) return

    reconnectAttempts++
    console.log(`Scheduling elicitation SSE reconnection attempt ${reconnectAttempts}/${maxReconnectAttempts}`)

    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = null
      connect()
    }, reconnectDelay)
  }

  /**
   * 提交用户填写的数据
   */
  async function submitResponse(elicitationId: string, data: Record<string, any>): Promise<boolean> {
    try {
      console.log('Submitting elicitation response:', { elicitationId, data })

      const response = await fetch(`/api/elicitation/${elicitationId}/response`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ data })
      })

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const result = await response.json()
      console.log('Elicitation response submitted successfully:', result)

      // 清除当前的 elicitation 请求
      if (currentElicitation.value?.elicitationId === elicitationId) {
        currentElicitation.value = null
      }

      return result.success || false

    } catch (err: any) {
      console.error('Failed to submit elicitation response:', err)
      error.value = '提交数据失败: ' + (err?.message || String(err))
      return false
    }
  }

  /**
   * 取消 elicitation 请求
   */
  function cancelElicitation(elicitationId: string) {
    console.log('Cancelling elicitation:', elicitationId)

    // 清除当前的 elicitation 请求
    if (currentElicitation.value?.elicitationId === elicitationId) {
      currentElicitation.value = null
    }

    // 这里可以发送取消请求到服务端（如果需要的话）
    // 目前只是本地清除状态
  }

  /**
   * 处理接收到的 elicitation 通知
   * 可以在这里触发全局事件或状态更新
   */
  function handleElicitationReceived(notification: ElicitationNotification) {
    // 可以在这里添加全局通知、音效提示等
    console.log('New elicitation request received:', notification.message)

    // 如果需要，可以发送浏览器通知
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification('需要用户输入', {
        body: notification.message,
        icon: '/favicon.ico'
      })
    }
  }

  /**
   * 获取待处理的 elicitation 列表
   * 用于页面刷新后恢复状态
   */
  async function getPendingElicitations() {
    try {
      const response = await fetch('/api/elicitation/pending')
      if (response.ok) {
        return await response.json()
      }
    } catch (err) {
      console.error('Failed to get pending elicitations:', err)
    }
    return null
  }

  // 组件卸载时自动断开连接
  onUnmounted(() => {
    disconnect()
  })

  return {
    // 状态
    isConnected,
    currentElicitation,
    error,

    // 方法
    connect,
    disconnect,
    submitResponse,
    cancelElicitation,
    getPendingElicitations
  }
}