import { ref } from 'vue'
import { defineStore } from 'pinia'
import { AgentType } from '@/types/session'
import type { UIMessage } from '@/types/events'
import type { Session } from '@/types/session'
import { PlanData, PlanStatus, PlanPhase, PlanPhaseStatus } from '@/types/events'
import { nanoid } from 'nanoid'

// 使用 Pinia 包装：保持对外 API 不变

// Create 5 fixed test sessions
const fixedSessions: Session[] = [
  { id: 'session-1', title: 'ReAct 智能推理', agentType: AgentType.ReAct, updatedAt: new Date(2024, 0, 15, 10, 30, 0) },
  { id: 'session-2', title: 'ReAct+ 增强版', agentType: AgentType.ReAct_Plus, updatedAt: new Date(2024, 0, 14, 16, 45, 0) },
  { id: 'session-3', title: '代码开发助手', agentType: AgentType.ReAct, updatedAt: new Date(2024, 0, 13, 9, 20, 0) },
  { id: 'session-4', title: 'ReAct 数据分析', agentType: AgentType.ReAct, updatedAt: new Date(2024, 0, 12, 14, 15, 0) },
  { id: 'session-5', title: 'ReAct+ 深度学习', agentType: AgentType.ReAct_Plus, updatedAt: new Date(2024, 0, 11, 11, 0, 0) }
]

const sessionId = ref<string>(fixedSessions[0].id)
const selectedTag = ref<AgentType>(fixedSessions[0].agentType)
const sessions = ref<Session[]>(fixedSessions)
// Initialize message arrays for all sessions
const initialMessages: Record<string, UIMessage[]> = {}
fixedSessions.forEach(session => {
  initialMessages[session.id] = []
})
const messagesBySession = ref<Record<string, UIMessage[]>>(initialMessages)

// Plan状态管理
const plansBySession = ref<Record<string, PlanData | null>>({})

// Plan小部件状态管理
export type PlanWidgetMode = 'hidden' | 'ball' | 'mini' | 'sidebar'

interface PlanWidgetState {
  mode: PlanWidgetMode
  position: { x: number; y: number }
  size: { width: number; height: number }
}

// Plan小部件状态（由 Pinia 持久化插件自动管理）
const planWidgetState = ref<PlanWidgetState>({
  mode: 'hidden',
  position: { x: 100, y: 100 },
  size: { width: 380, height: 600 }
})

const switchConversation = (id: string) => {
  if (sessionId.value === id) return
  sessionId.value = id
  // Update selectedTag based on session's agentType
  const session = sessions.value.find(s => s.id === id)
  if (session) {
    selectedTag.value = session.agentType
  }
}

const newConversation = (agentType: AgentType = AgentType.ReAct) => {
  const id = `session-${Date.now()}`
  const newSession: Session = {
    id,
    title: '新对话',
    agentType,
    updatedAt: new Date()
  }
  sessions.value.unshift(newSession)
  switchConversation(id)
  messagesBySession.value[id] = []
}

const selectTag = (tag: AgentType) => {
  selectedTag.value = tag
}

const store = {
  sessionId,
  selectedTag,
  sessions,
  messagesBySession,
  plansBySession,
  planWidgetState,
  switchConversation,
  newConversation,
  selectTag,
  getSessionMessages(id: string): UIMessage[] { return messagesBySession.value[id] ? [...messagesBySession.value[id]] : [] },
  setSessionMessages(id: string, msgs: UIMessage[]) { messagesBySession.value[id] = [...msgs] },
  touchSession(id: string) {
    const idx = sessions.value.findIndex(s => s.id === id)
    if (idx >= 0) {
      
      sessions.value[idx] = { ...sessions.value[idx], updatedAt: new Date() }
    }
  },

  // Plan 管理方法
  getCurrentPlan(): PlanData | null {
    return plansBySession.value[sessionId.value] || null
  },

  getSessionPlan(id: string): PlanData | null {
    return plansBySession.value[id] || null
  },

  setSessionPlan(id: string, plan: PlanData) {
    plansBySession.value[id] = {
      ...plan,
      phases: plan.phases.map((phase, index) => ({
        ...phase,
        id: phase.id || nanoid(8),
        index: index,
        status: phase.status || PlanPhaseStatus.TODO
      })),
      status: plan.status || PlanStatus.PLANNING,
      createdAt: plan.createdAt || new Date(),
      updatedAt: new Date()
    }
    store.touchSession(id)
  },

  updateSessionPlan(id: string, updates: Partial<PlanData>) {
    const existingPlan = plansBySession.value[id]
    if (!existingPlan) return

    plansBySession.value[id] = {
      ...existingPlan,
      ...updates,
      phases: updates.phases ?
        updates.phases.map((phase, index) => ({
          ...phase,
          id: phase.id || nanoid(8),
          index: index,
          status: phase.status || PlanPhaseStatus.TODO
        })) : existingPlan.phases,
      updatedAt: new Date()
    }
    this.touchSession(id)
  },

  advancePlanPhase(id: string, fromPhaseId?: string, toPhaseId?: string) {
    const plan = plansBySession.value[id]
    if (!plan) return

    const phases = plan.phases.map(phase => {
      if (fromPhaseId && phase.id === fromPhaseId) {
        return { ...phase, status: PlanPhaseStatus.COMPLETED }
      }
      if (toPhaseId && phase.id === toPhaseId) {
        return { ...phase, status: PlanPhaseStatus.RUNNING }
      }
      return phase
    })

    store.updateSessionPlan(id, {
      phases,
      currentPhaseId: toPhaseId,
      status: PlanStatus.EXECUTING
    })
  },

  updatePhase(id: string, phaseId: string, updates: Partial<PlanPhase>) {
    const plan = plansBySession.value[id]
    if (!plan) return

    const phases = plan.phases.map(phase =>
      phase.id === phaseId ? { ...phase, ...updates } : phase
    )

    store.updateSessionPlan(id, { phases })
  },

  clearSessionPlan(id: string) {
    delete plansBySession.value[id]
  },



  // Plan小部件状态管理方法
  getPlanWidgetMode(): PlanWidgetMode {
    return planWidgetState.value.mode
  },

  setPlanWidgetMode(mode: PlanWidgetMode) {
    planWidgetState.value.mode = mode
  },

  getPlanWidgetPosition(): { x: number; y: number } {
    return { ...planWidgetState.value.position }
  },

  setPlanWidgetPosition(position: { x: number; y: number }) {
    planWidgetState.value.position = position
  },

  getPlanWidgetSize(): { width: number; height: number } {
    return { ...planWidgetState.value.size }
  },

  setPlanWidgetSize(size: { width: number; height: number }) {
    planWidgetState.value.size = size
  },

  updatePlanWidgetState(updates: Partial<PlanWidgetState>) {
    planWidgetState.value = {
      ...planWidgetState.value,
      ...updates
    }
  },

  // New methods for Agent-Session binding
  getCurrentSession(): Session | undefined {
    return sessions.value.find(s => s.id === sessionId.value)
  },
  getSessionsByAgent(agentType: AgentType): Session[] {
    return sessions.value.filter(s => s.agentType === agentType)
  },
  findOrCreateSessionForAgent(agentType: AgentType): string {
    // Try to find an existing session with this agent type
    const existingSessions = store.getSessionsByAgent(agentType)
    if (existingSessions.length > 0) {
      // Switch to the most recently updated session
      const mostRecentSession = existingSessions.sort((a, b) => b.updatedAt.getTime() - a.updatedAt.getTime())[0]
      store.switchConversation(mostRecentSession.id)
      return mostRecentSession.id
    } else {
      // Create new session for this agent
      store.newConversation(agentType)
      return sessionId.value
    }
  }


}

// 导出 Pinia 版本的 useChatStore（保持原 API 不变）
export const useChatStore = defineStore('chat', () => store, {
  persist: true
})

/**
 * Append a message into a session and touch its updated time.
 * Exported for convenience of simple views/components.
 */
export function appendMessage(id: string, msg: UIMessage) {
  const arr = messagesBySession.value[id] ?? []
  messagesBySession.value[id] = [...arr, msg]
  const idx = sessions.value.findIndex(s => s.id === id)
  if (idx >= 0) {
    sessions.value[idx] = { ...sessions.value[idx], updatedAt: new Date() }
  } else {
    // if session not exists, create a lightweight entry with default ReAct agent
    const newSession: Session = {
      id,
      title: '新对话',
      agentType: AgentType.ReAct,
      updatedAt: new Date()
    }
    sessions.value.unshift(newSession)
  }
}
