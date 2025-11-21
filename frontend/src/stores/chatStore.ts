import {ref} from 'vue'
import {defineStore} from 'pinia'
import {AgentType} from '@/types/session'
import type {UIMessage} from '@/types/events'
import type {Session} from '@/types/session'
import {PlanData, PlanStatus, PlanPhase, PlanPhaseStatus} from '@/types/events'
import {nanoid} from 'nanoid'

// 使用 Pinia 包装：保持对外 API 不变

const sessionId = ref<string>('')
const selectedTag = ref<AgentType>(AgentType.ReAct_Plus)
const sessions = ref<Session[]>([])
// Initialize message arrays for all sessions


const messagesBySession = ref<Record<string, UIMessage[]>>({})
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
    position: {x: 100, y: 100},
    size: {width: 380, height: 600}
})

const switchConversation = (id: string) => {
    if (sessionId.value === id) return
    sessionId.value = id
    // Update selectedTag based on session's agentType
    const session = sessions.value.find(s => s.id === id)
    if (session) {
        selectedTag.value = session.type
    }
}

const newConversation = (type: AgentType = AgentType.ReAct_Plus) => {
    // 创建临时会话（仅前端存储），使用临时 ID
    // 当用户发送第一条消息时，后端会创建真实的 sessionId，并通过 STARTED 事件返回
    const tempId = `temp-${Date.now()}` // 临时 ID
    const newSession: Session = {
        id: tempId,
        title: '新对话',
        type,
        createdTime: new Date(),
        updatedTime: new Date(),
        isTemp: true
    }

    // 添加到会话列表（前端临时存储）
    sessions.value.unshift(newSession)
    switchConversation(tempId)
    messagesBySession.value[tempId] = []
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
    getSessionMessages(id: string): UIMessage[] {
        return messagesBySession.value[id] ? [...messagesBySession.value[id]] : []
    },
    setSessionMessages(id: string, msgs: UIMessage[]) {
        messagesBySession.value[id] = [...msgs]
    },
    touchSession(id: string) {
        const idx = sessions.value.findIndex(s => s.id === id)
        if (idx >= 0) {

            sessions.value[idx] = {...sessions.value[idx], updatedTime: new Date()}
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
            updatedTime: new Date()
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
            updatedTime: new Date()
        }
        this.touchSession(id)
    },

    advancePlanPhase(id: string, fromPhaseId?: string, toPhaseId?: string) {
        const plan = plansBySession.value[id]
        if (!plan) return

        const phases = plan.phases.map(phase => {
            if (fromPhaseId && phase.id === fromPhaseId) {
                return {...phase, status: PlanPhaseStatus.COMPLETED}
            }
            if (toPhaseId && phase.id === toPhaseId) {
                return {...phase, status: PlanPhaseStatus.RUNNING}
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
            phase.id === phaseId ? {...phase, ...updates} : phase
        )

        store.updateSessionPlan(id, {phases})
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
        return {...planWidgetState.value.position}
    },

    setPlanWidgetPosition(position: { x: number; y: number }) {
        planWidgetState.value.position = position
    },

    getPlanWidgetSize(): { width: number; height: number } {
        return {...planWidgetState.value.size}
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
        return sessions.value.filter(s => s.type === agentType)
    },
    findOrCreateSessionForAgent(agentType: AgentType): string {
        // Try to find an existing session with this agent type
        const existingSessions = store.getSessionsByAgent(agentType)
        if (existingSessions.length > 0) {
            // Switch to the most recently updated session
            const mostRecentSession = existingSessions.sort((a, b) => b.updatedTime.getTime() - a.updatedTime.getTime())[0]
            store.switchConversation(mostRecentSession.id)
            return mostRecentSession.id
        } else {
            // Create new session for this agent
            store.newConversation(agentType)
            return sessionId.value
        }
    },

    createSessionIfNotExists(id: string, type: AgentType = AgentType.ReAct_Plus, title: string = '新对话') {
        // 查找是否存在临时会话（ID 以 temp- 开头）
        const tempSession = sessions.value.find(s => s.id.startsWith('temp-'))

        if (tempSession) {
            // 如果存在临时会话,先保存旧的临时 ID
            const oldTempId = tempSession.id

            // 替换会话 ID 和信息
            tempSession.id = id
            tempSession.title = title
            tempSession.updatedTime = new Date()

            // 将临时消息迁移到新的 sessionId
            const tempMessages = messagesBySession.value[oldTempId]
            if (tempMessages) {
                messagesBySession.value[id] = tempMessages
                delete messagesBySession.value[oldTempId]
            } else {
                messagesBySession.value[id] = []
            }
        } else {
            // 如果不存在临时会话，检查是否已有此 ID 的会话
            const existing = sessions.value.find(s => s.id === id)
            if (!existing) {
                const newSession: Session = {
                    id,
                    title,
                    type,
                    createdTime: new Date(),
                    updatedTime: new Date(),
                    isTemp: false
                }
                sessions.value.unshift(newSession)
                messagesBySession.value[id] = []
            }
        }

        sessionId.value = id
    },

    async fetchReActPlusSessionMessages(sessionId: string) {
        try {
            const http = (await import('@/services/http')).default
            const response: any = await http.get(`/agent/chat/react-plus/${sessionId}/messages`)
            if (response.code === 200 && Array.isArray(response.data)) {
                const messages = response.data.map((msg: any) => ({
                    messageId: msg.id,
                    type: msg.type,
                    sender: msg.type === 'USER' ? '用户' : 'Agent',
                    message: msg.message,
                    startTime: new Date(msg.startTime),
                    endTime: msg.endTime ? new Date(msg.endTime) : undefined,
                    data: msg.data
                }))
                store.setSessionMessages(sessionId, messages)
            }
        } catch (error) {
            console.error('Failed to fetch ReActPlus session messages:', error)
        }
    }


}

// 导出 Pinia 版本的 useChatStore（保持原 API 不变）
export const useChatStore = defineStore('chat', () => store, {
    // persist: true
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
        sessions.value[idx] = {...sessions.value[idx], updatedTime: new Date()}
    } else {
        // if session not exists, create a lightweight entry with default ReAct agent
        const newSession: Session = {
            id,
            title: '新对话',
            type: AgentType.ReAct,
            createdTime: new Date(),
            updatedTime: new Date(),
            isTemp: false
        }
        sessions.value.unshift(newSession)
    }
}
