<script setup lang="ts">
import { nextTick, ref, watch, watchEffect, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import { fetchSessionMessages, fetchUserSessions, createSession } from '@/services/roleplay'
import { DEFAULT_ROLEPLAY_USER_ID } from '@/constants/roleplay'
import type { SessionMessage, SessionDetail } from '@/types/roleplay'

interface Message { id: string; role: 'user' | 'agent'; content: string }

function newMsgId() {
  return `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function selectSession(s: SessionDetail) {
  if (!s || !s.sessionCode) return
  if (s.sessionCode === props.sessionId) return
  // 切换前关闭当前流
  if (sseAbort) {
    try { sseAbort.abort() } catch {}
    sseAbort = null
  }
  activeSession.value = s.sessionCode
  emit('update:sessionId', s.sessionCode)
  emit('sessionSelected', s)
}

interface RecordTestState {
  active: boolean
  lastCheckAt: string
  duration: number
  audioUrl: string | null
  chunkCount: number
  totalSamples: number
  totalBytes: number
}

const props = defineProps<{
  sessionId: string
  roleId?: number
  recordTest?: RecordTestState
  onStartTest?: () => void
  onStopTest?: () => void
}>()

const emit = defineEmits<{
  (e: 'update:sessionId', value: string): void
  (e: 'sessionSelected', value: SessionDetail): void
}>()

const messages = ref<Message[]>([])
const draft = ref('')
const messagesRef = ref<HTMLElement | null>(null)
const loading = ref(false)
let sseAbort: AbortController | null = null
// 会话列表
const sessions = ref<SessionDetail[]>([])
const sessionsLoading = ref(false)
const sessionsActiveOnly = ref(false)
const activeSession = ref("")
function formatTime(t?: string | null) {
  if (!t) return ''
  try { return new Date(t).toLocaleString() } catch { return String(t) }
}

async function loadSessions() {
  sessionsLoading.value = true
  try {
    const resp = await fetchUserSessions(1, sessionsActiveOnly.value)
    const arr = Array.isArray(resp) ? resp : (resp as any)
    const list: SessionDetail[] = (arr || [])
        .map((it: any) => it?.data)
        .filter((x: any) => !!x)
        .sort((a: SessionDetail, b: SessionDetail) => {
          const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0
          const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0
          return tb - ta
        })
    sessions.value = list
  } catch (e) {
    console.error('[ChatPanel] 加载会话列表失败:', e)
  } finally {
    sessionsLoading.value = false
  }
}

// 加载会话消息历史
async function loadMessages() {
  if (!props.sessionId) return

  try {
    const response = await fetchSessionMessages(props.sessionId, { limit: 100 })
    const sessionMessages = response.data

    // 转换为组件所需的消息格式（后端角色枚举为大写）
    messages.value = sessionMessages.map((msg: SessionMessage) => ({
      id: String(msg.id),
      role: msg.role === 'USER' ? 'user' : 'agent',
      content: msg.content || ''
    }))

  } catch (error) {
    console.error('[ChatPanel] 加载消息失败:', error)
  }
}

watchEffect(() => {
  // 当 sessionId 变化时，重新加载消息历史
  if (props.sessionId) {
    messages.value = [] // 清空当前消息
    loadMessages()
  }
})

watch(messages, () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
})

// 发送消息
async function send() {
  if (!draft.value.trim() || loading.value) return

  const userContent = draft.value.trim()
  draft.value = ''

  messages.value.push({ id: newMsgId(), role: 'user', content: userContent })

  loading.value = true

  try {
    // 若当前无会话，则在首次发送时创建文本会话
    let sessionCode = props.sessionId
    if (!sessionCode) {
      if (!props.roleId || props.roleId <= 0) {
        message.error('角色无效，无法创建会话')
        loading.value = false
        return
      }
      const respCreate = await createSession({
        userId: DEFAULT_ROLEPLAY_USER_ID,
        roleId: props.roleId as number,
        // 文本模式固定为 text
        ...( { mode: 'text' } as any )
      })
      sessionCode = respCreate.data.sessionCode
      emit('update:sessionId', sessionCode)
      // 刷新会话列表
      loadSessions().catch(() => {})
    }

    // 若存在未结束的流，先关闭
    if (sseAbort) {
      try { sseAbort.abort() } catch {}
      sseAbort = null
    }

    sseAbort = new AbortController()

    const resp = await fetch(`/api/sessions/${encodeURIComponent(sessionCode)}/messages/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify({
        messageType: 'USER_TEXT',
        role: 'USER',
        content: userContent
      }),
      signal: sseAbort.signal
    })

    if (!resp.ok || !resp.body) {
      throw new Error(`SSE 请求失败: ${resp.status} ${resp.statusText}`)
    }

    // 为助手创建一个占位消息，后续增量填充
    messages.value.push({ id: newMsgId(), role: 'agent', content: '' })
    const agentIndex = messages.value.length - 1

    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    const flushEvents = () => {
      let sepIndex = buffer.indexOf('\n\n')
      while (sepIndex !== -1) {
        const raw = buffer.slice(0, sepIndex)
        buffer = buffer.slice(sepIndex + 2)
        const lines = raw.split('\n')
        let evt = 'message'
        const dataLines: string[] = []
        for (const line of lines) {
          if (line.startsWith('event:')) evt = line.slice(6).trim()
          else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
        }
        const data = dataLines.join('\n')

        if (evt === 'ack') {
          // 用户消息已保存
        } else if (evt === 'delta') {
          messages.value[agentIndex].content += data
        } else if (evt === 'done') {
          // 结束：不主动 abort，避免 BodyStreamBuffer was aborted
          loading.value = false
        } else if (evt === 'error') {
          message.error(data || '生成出错')
          loading.value = false
        }

        sepIndex = buffer.indexOf('\n\n')
      }
    }

    // 读取并解析 SSE 数据
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      flushEvents()
    }

    // 流自然结束后，若还有残留事件块，尝试解析
    flushEvents()
    loading.value = false

  } catch (error: any) {
    // 忽略因切换会话或新一轮发送而触发的中止异常
    const msg = String(error?.message || error || '')
    if (error?.name === 'AbortError' || /aborted|BodyStreamBuffer was aborted/i.test(msg)) {
      console.warn('[ChatPanel] SSE 读流被中止(预期):', msg)
    } else {
      console.error('[ChatPanel] 发送消息失败:', error)
      message.error('发送失败，请重试')
    }
    loading.value = false
  }
}

const onStart = () => {
  if (props.onStartTest) props.onStartTest()
}

const onStop = () => {
  if (props.onStopTest) props.onStopTest()
}

// 组件初始化
onMounted(() => {
  if (props.sessionId) {
    loadMessages()
  }
  loadSessions()
})

onBeforeUnmount(() => {
  if (sseAbort) {
    try { sseAbort.abort() } catch {}
    sseAbort = null
  }
})

watch(sessionsActiveOnly, () => {
  loadSessions()
})
</script>
<template>
  <div class="chat-layout">
    <section class="chat-panel">
      <div class="chat-shell">
        <header class="chat-header">
          <div class="chat-title">文本对话</div>
          <div class="chat-subtitle">实时记录你与角色之间的每一句交流</div>
        </header>

        <div class="messages" ref="messagesRef">
          <div class="empty" v-if="!messages.length">暂无消息，开始对话吧～</div>
          <div
            v-for="m in messages"
            :key="m.id"
            class="msg"
            :class="m.role"
          >
            <div class="msg-meta">
              <span class="sender">{{ m.role === 'user' ? '你' : 'AI' }}</span>
            </div>
            <div class="bubble" :class="m.role">
              <div class="bubble-text">{{ m.content }}</div>
            </div>
          </div>
        </div>

        <footer class="composer">
          <a-input-search
            v-model:value="draft"
            :enter-button="'发送'"
            :loading="loading"
            @search="send"
            placeholder="来说点什么..."
          />
        </footer>
      </div>
    </section>

    <aside class="chat-side">
      <a-card class="session-card" bordered="false">
        <div class="session-header">
          <div class="session-title">会话列表</div>
          <div class="session-actions">
            <a-tooltip title="仅显示进行中会话">
              <a-switch v-model:checked="sessionsActiveOnly" size="small" />
            </a-tooltip>
            <a-button size="small" :loading="sessionsLoading" @click="loadSessions">刷新</a-button>
          </div>
        </div>

        <div v-if="sessionsLoading" class="session-skeleton">
          <a-skeleton active :title="false" :paragraph="{ rows: 6 }" />
        </div>

        <div class="session-body" v-else-if="sessions.length">
          <a-list class="session-list">
            <a-list-item
              v-for="s in sessions"
              :key="s.sessionCode"
              class="session-item"
              :class="{ current: s.sessionCode === props.sessionId }"
              @click="selectSession(s)"
            >
              <div class="session-item-inner">
                <div class="session-dot" :class="{ active: activeSession === s.sessionCode }"></div>
                <div class="session-main">
                  <div class="session-row">
                    <span class="code" :title="s.sessionCode">{{ s.sessionCode }}</span>
                  </div>
                  <div class="session-summary" :title="s.summary || '暂无摘要'">{{ s.summary || '暂无摘要' }}</div>
                  <div class="session-meta">
                    <span>开始：{{ formatTime(s.createdAt) }}</span>
                    <span v-if="s.endedAt">结束：{{ formatTime(s.endedAt) }}</span>
                  </div>
                </div>
              </div>
            </a-list-item>
          </a-list>
        </div>
        <div v-else class="session-empty">暂无会话</div>
      </a-card>
      <a-card class="record-card" bordered="false">
        <div class="record-header">
          <div class="record-title">
            <span>录音调试</span>
            <a-tag v-if="recordTest?.active" color="#5b77ff">进行中</a-tag>
          </div>
          <div class="record-hint">确保麦克风状态可用，录制链路通畅。</div>
        </div>

        <div v-if="recordTest" class="record-body">
          <div class="record-status" :class="{ active: recordTest.active }">
            <span class="status-light"></span>
            <div class="status-text">
              <span v-if="recordTest.active">{{ `录音测试进行中 · ${recordTest.duration.toFixed(1)}s` }}</span>
              <span v-else-if="recordTest.lastCheckAt">最近一次测试：{{ recordTest.lastCheckAt }}</span>
              <span v-else>尚未进行录音测试</span>
            </div>
          </div>

          <a-space direction="vertical" style="width:100%">
            <a-button type="primary" block :disabled="recordTest.active" @click="onStart">
              开始 5 秒录音测试
            </a-button>
            <a-button block danger :disabled="!recordTest.active" @click="onStop">
              手动停止
            </a-button>
          </a-space>

          <div class="record-preview" v-if="recordTest.audioUrl">
            <audio :src="recordTest.audioUrl" controls style="width: 100%;"></audio>
            <div class="record-info">
              <div>音频块：{{ recordTest.chunkCount }} 个</div>
              <div>总样本数：{{ recordTest.totalSamples }}</div>
              <div>总字节数：{{ recordTest.totalBytes }}</div>
            </div>
          </div>

          <a-alert
              v-else-if="!recordTest.active && recordTest.lastCheckAt"
              type="warning"
              show-icon
              message="未捕获到任何音频数据，请检查麦克风权限或输入设备。"
          />
          <div class="record-preview-placeholder" v-else>
            <div class="placeholder-content">
              <div class="placeholder-icon">
                <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="24" cy="24" r="20" fill="#F0F6FF"/>
                  <path d="M20 16L20 32" stroke="#8AA4FF" stroke-width="2" stroke-linecap="round"/>
                  <path d="M28 16L28 32" stroke="#8AA4FF" stroke-width="2" stroke-linecap="round"/>
                </svg>
              </div>
              <div class="placeholder-text">
                <h3 v-if="recordTest.active">正在录音...</h3>
                <h3 v-else>等待音频数据</h3>
                <p>音频数据将在此处播放</p>
              </div>
            </div>
          </div>

        </div>

        <div v-else class="record-placeholder">
          <div class="placeholder-content">
            <div class="placeholder-icon">
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="24" cy="24" r="20" fill="#F0F6FF"/>
                <path d="M24 34C29.5228 34 34 29.5228 34 24C34 18.4772 29.5228 14 24 14C18.4772 14 14 18.4772 14 24C14 29.5228 18.4772 34 24 34Z" stroke="#8AA4FF" stroke-width="2"/>
                <path d="M20 24L23 27L28 21" stroke="#8AA4FF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="placeholder-text">
              <h3>录音测试功能</h3>
              <p>加载中...</p>
            </div>
          </div>
        </div>
      </a-card>
    </aside>
  </div>
</template>


<style scoped lang="scss">
.chat-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: stretch;
  height: 100%;
}

.chat-panel {
  border-radius: 20px;
  box-shadow: 0 26px 40px rgba(24, 52, 133, 0.12);
  overflow: hidden;
  background: linear-gradient(180deg, rgba(240, 246, 255, 0.9), #ffffff 60%);
  border: 1px solid rgba(174, 191, 242, 0.24);
}

.chat-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 520px;
  background: rgba(255, 255, 255, 0.82);
}

.chat-header {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chat-title {
  font-size: 18px;
  font-weight: 700;
  color: #1c2358;
}

.chat-subtitle {
  font-size: 13px;
  color: #5a6592;
}

.messages {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 24px 8px;
  scrollbar-width: thin;
}

.empty {
  color: #8893c2;
  font-size: 14px;
  text-align: center;
  padding: 80px 0;
}

.msg {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 78%;
}

.msg.user {
  align-self: flex-end;
  text-align: right;
}

.msg.agent {
  align-self: flex-start;
}

.msg.user .msg-meta {
  justify-content: flex-end;
}

.msg-meta {
  display: flex;
  gap: 6px;
  font-size: 12px;
  color: #7a86be;
}

.sender {
  font-weight: 600;
  letter-spacing: .2px;
}

.bubble {
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.55;
  box-shadow: 0 16px 30px rgba(35, 63, 148, 0.08);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(142, 160, 215, 0.2);
  color: #1f2558;
  background: rgba(255, 255, 255, 0.75);
}

.bubble.user {
  background: linear-gradient(135deg, rgba(129, 211, 255, 0.26), rgba(99, 162, 255, 0.24));
  color: #124b73;
  border-color: rgba(99, 162, 255, 0.28);
}

.bubble.agent {
  background: linear-gradient(135deg, rgba(142, 233, 208, 0.24), rgba(97, 204, 169, 0.18));
  color: #0b5d4d;
  border-color: rgba(97, 204, 169, 0.22);
}

.composer {
  padding: 12px;
  display: flex;
}

.composer :deep(.ant-input) {
  padding: 12px 16px;
  font-size: 14px;
}

.composer :deep(.ant-btn) {
  border-radius: 0 14px 14px 0;
  padding: 0 18px;
  height: 48px;
}

.composer :deep(.ant-input-search-button) {
  height: 48px;
}

.chat-side {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  max-height: 100%;
  gap: 12px;
}
.session-card {
  border-radius: 18px;
  height: 25%;
  overflow: auto;
}
 .session-card :deep(.ant-card-body) {
   padding: 16px;
 }

 .session-header {
   display: flex;
   align-items: center;
   justify-content: space-between;
   margin-bottom: 8px;

 }
 .session-title {
   font-size: 15px;
   font-weight: 600;
   color: #1f2558;
 }
 .session-actions {
   display: flex;
   align-items: center;
   gap: 10px;
 }
 .session-skeleton {
   padding: 8px 2px 2px 2px;
 }
 .session-list {
   display: flex;
   flex-direction: column;
   gap: 8px;
 }
 .session-item {
  padding: 8px 6px !important;
  border-radius: 12px;
  transition: background 0.2s ease;
  cursor: pointer;
 }
 .session-item.current {
   background: rgba(92, 114, 205, 0.08);
 }
 .session-item:hover {
   background: rgba(92, 114, 205, 0.06);
 }
 .session-item-inner {
   display: flex;
   gap: 10px;
   align-items: flex-start;
 }
 .session-dot {
   margin-top: 6px;
   width: 10px;
   height: 10px;
   border-radius: 50%;
   background: #c9cfe8;
 }
 .session-dot.active {
   background: #52c41a;
   box-shadow: 0 0 0 4px rgba(82, 196, 26, 0.12);
 }
 .session-main {
   flex: 1;
   min-width: 0;
   display: flex;
   flex-direction: column;
   gap: 6px;
 }
 .session-row {
   display: flex;
   align-items: center;
   gap: 8px;
 }
 .session-row .code {
   max-width: 160px;
   font-weight: 600;
   color: #1e2559;
   white-space: nowrap;
   overflow: hidden;
   text-overflow: ellipsis;
 }
 .session-summary {
   font-size: 12px;
   color: #5e6aa0;
   line-height: 1.4;
   display: -webkit-box;
   -webkit-line-clamp: 2;
   -webkit-box-orient: vertical;
   overflow: hidden;
 }
 .session-meta {
   display: flex;
   gap: 12px;
   font-size: 12px;
   color: #7b86b9;
 }
.record-card {
  border-radius: 18px;
  box-shadow: 0 20px 36px rgba(24, 52, 133, 0.12);
  padding: 20px;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(75% - 12px)

}

.record-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.record-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2558;
}

.record-hint {
  font-size: 13px;
  color: #6b78a9;
}

.record-body {
  display: flex;
  flex-direction: column;

  gap: 16px;
}

.record-status {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(92, 114, 205, 0.08);
  border: 1px solid rgba(92, 114, 205, 0.18);
}

.record-status.active {
  background: rgba(92, 114, 205, 0.14);
  border-color: rgba(92, 114, 205, 0.28);
}

.status-light {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #8aa4ff;
  box-shadow: 0 0 0 4px rgba(138, 164, 255, 0.22);
}

.status-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #4a5586;
}

.record-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(237, 243, 255, 0.6);
}

.record-info {
  display: grid;
  gap: 4px;
  font-size: 12px;
  color: #566296;
}

.record-preview-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px 12px;
  border-radius: 12px;
  background: rgba(237, 243, 255, 0.6);
  min-height: 120px;
}

.record-preview-placeholder .placeholder-content {
  text-align: center;
  color: #8893c2;
}

.record-preview-placeholder .placeholder-icon {
  margin-bottom: 16px;
}

.record-preview-placeholder .placeholder-text h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #5a6592;
}

.record-preview-placeholder .placeholder-text p {
  margin: 0;
  font-size: 14px;
  color: #8893c2;
}

@media (max-width: 1024px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .chat-side {
    order: -1;
  }
}
</style>
