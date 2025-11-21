import { ref, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UIMessage, EventType, BaseEventItem, InitPlanEventData, UpdatePlanEventData, AdvancePlanEventData, PlanData } from '@/types/events'
import { ConnectionStatus, TaskStatus, ProgressInfo } from '@/types/status'
import { NotificationType } from '@/types/notification'
import { AgentType } from '@/types/session'
import { useChatStore } from '@/stores/chatStore'
import { useAuthStore } from '@/stores/authStore'
const ssePromise = import('sse.js')

// === SSE 相关接口定义 ===

/** SSE 连接源接口 */
interface SSESource {
    addEventListener(event: string, handler: (e: MessageEvent) => void): void
    close(): void
    stream(): void
}


/** 自定义事件处理器映射 */
interface SSEEventHandlers {
    onStarted?: (event: BaseEventItem) => void | boolean
    onThinking?: (event: BaseEventItem) => void | boolean
    onAction?: (event: BaseEventItem) => void | boolean
    onActing?: (event: BaseEventItem) => void | boolean
    onObserving?: (event: BaseEventItem) => void | boolean
    onExecuting?: (event: BaseEventItem) => void | boolean
    onTool?: (event: BaseEventItem) => void | boolean
    onToolApproval?: (event: BaseEventItem) => void | boolean
    onInteraction?: (event: BaseEventItem) => void | boolean
    onProgress?: (event: BaseEventItem) => void | boolean
    onDone?: (event: BaseEventItem) => void | boolean
    onDoneWithWarning?: (event: BaseEventItem) => void | boolean
    onError?: (event: BaseEventItem) => void | boolean
    onCompleted?: (event: BaseEventItem) => void | boolean
    onDefault?: (event: BaseEventItem) => void | boolean

    // ReActPlus 专属事件处理器
    onTaskAnalysis?: (event: BaseEventItem) => void | boolean
    onThought?: (event: BaseEventItem) => void | boolean
    onInitPlan?: (event: BaseEventItem) => void | boolean
    onUpdatePlan?: (event: BaseEventItem) => void | boolean
    onAdvancePlan?: (event: BaseEventItem) => void | boolean
}

/** Agent执行配置 */
interface AgentExecuteOptions {
    endpoint: string
    method?: 'POST' | 'GET'
    headers?: Record<string, string>
    payload?: Record<string, any>
}

/** useSSE 配置选项 */
interface SSEOptions {
    /** 自定义事件处理器 */
    handlers?: SSEEventHandlers
    /** 滚动到底部回调 */
    onScrollToBottom?: () => void
    /** 完成通知回调 */
    onDoneNotice?: (p: {
        text: string;
        startTime: Date;
        title: string;
        messageId?: string,
        type: NotificationType
    }) => void
}

export function useSSE(options: SSEOptions = {}) {
    // === 响应式状态 ===
    const messages = ref<UIMessage[]>([])
    const nodeIndex = ref<Record<string, number>>({})
    const connectionStatus = ref(new ConnectionStatus('disconnected'))
    const taskStatus = ref(new TaskStatus('idle'))
    const progress = ref<ProgressInfo | null>(null)
    const currentTaskTitle = ref<string>("")

    // === SSE 连接管理 ===
    const activeSource = ref<SSESource | null>(null)

    // === 工具函数 ===
    const closeSource = (source: SSESource | null) => {
        try {
            if (source && typeof source.close === 'function') source.close()
        } catch (e) {
            console.error('Failed to close SSE source:', e)
        }
    }

    /** 关闭当前活动的 SSE 连接 */
    const closeActiveSource = () => {
        if (activeSource.value) {
            closeSource(activeSource.value)
            activeSource.value = null
        }
    }

    /** 清理所有状态和资源 */
    const cleanup = () => {
        closeActiveSource()
        messages.value = []
        nodeIndex.value = {}
        progress.value = null
        taskStatus.value.set('idle')
        connectionStatus.value.set('disconnected')
        currentTaskTitle.value = ""
    }


    const scrollToBottom = async () => {
        await nextTick()
        options?.onScrollToBottom?.()
    }

    const getSenderByEventType = (event: BaseEventItem): string => {
        return event.agentId || "Agent"
    }

    // === 默认事件处理器实现（策略模式） ===


    /** 更新消息到消息列表（默认的消息聚合逻辑） */
    const updateMessage = (event: BaseEventItem): void => {
        // 安全的类型检查，避免强制断言
        const messageId = event.messageId
        if (!messageId) {
            console.warn('Event missing messageId, cannot aggregate messages', event)
            return
        }

        const sessionId = event.sessionId
        const turnId = event.turnId
        const type = event.type as EventType
        const message = (event.message || '').toString()
        const data = event.data
        const startTime = event.startTime || new Date()
        const endTime = event.endTime ?? new Date()

        const index = nodeIndex.value[messageId]

        if (type === EventType.TOOL) {
            // 工具事件作为独立消息插入
            const toolMsg: UIMessage = {
                messageId: messageId,
                sessionId: sessionId,
                turnId: turnId,
                type: type,
                sender: getSenderByEventType(event),
                message: message,
                data: data,
                startTime: startTime,
                endTime: endTime,
                meta: event.meta
            }
            messages.value.push(toolMsg)
        }
        else if (type === EventType.TOOL_APPROVAL) {

            const toolApprovalMsg: UIMessage = {
                messageId: messageId,
                sessionId: sessionId,
                turnId: turnId,
                type: type,
                sender: getSenderByEventType(event),
                message: message,
                data: data,
                startTime: startTime,
                endTime: endTime,
                meta: event.meta
            }
            messages.value.push(toolApprovalMsg)
        } else if (index !== undefined) {
            // messageId已存在，更新现有消息
            const node = messages.value[index]

            // 非工具事件：累积到message字段
            node.message = node.message ? `${node.message}${message}` : message
            node.events?.push?.(event)

        } else {

            // 非工具事件作为主消息创建并建立nodeIndex

            const firstNodeMessage: UIMessage = {
                messageId: messageId,
                sessionId: sessionId,
                turnId: turnId,
                type: type,
                sender: getSenderByEventType(event),
                message: message,
                data: data,
                startTime: startTime,
                endTime: endTime,
                meta: event.meta
            }
            messages.value.push(firstNodeMessage)
            // 立即建立nodeIndex映射
            nodeIndex.value[messageId] = messages.value.length - 1

        }
    }

    /** 更新思考消息（ReActPlus专用） */
    const updateThoughtMessage = (event: BaseEventItem): void => {
        const messageId = event.messageId
        if (!messageId) {
            console.warn('THOUGHT event missing messageId, falling back to default handling', event)
            updateMessage(event)
            return
        }

        const index = nodeIndex.value[messageId]
        const message = (event.message || '').toString()

        if (index !== undefined) {
            // messageId已存在，累积思考内容到现有消息
            // messageId已存在，累积思考内容到现有消息 - 使用响应式更新
            const node = messages.value[index]
            node.message = node.message ? `${node.message}${message}` : message
            node.events?.push?.(event)
        } else {
            // 创建新的思考消息节点
            const thoughtMessage: UIMessage = {
                messageId: messageId,
                sessionId: event.sessionId,
                turnId: event.turnId,
                type: 'assistant' as any, // 思考属于assistant类型
                sender: getSenderByEventType(event),
                message: message,
                data: event.data,
                startTime: event.startTime || new Date(),
                endTime: event.endTime,
                events: [event],
                meta: event.meta
            }
            messages.value.push(thoughtMessage)
            nodeIndex.value[messageId] = messages.value.length - 1
        }
    }

    /** 更新任务分析消息（ReActPlus专用） */
    const updateTaskAnalysisMessage = (event: BaseEventItem): void => {
        const messageId = event.messageId
        if (!messageId) {
            console.warn('TASK_ANALYSIS event missing messageId, falling back to default handling', event)
            updateMessage(event)
            return
        }

        const index = nodeIndex.value[messageId]
        const message = (event.message || '').toString()

        if (index !== undefined) {
            // messageId已存在，累积任务分析内容到现有消息 - 使用响应式更新
            const node = messages.value[index]
            node.message = node.message ? `${node.message}${message}` : message
            node.events?.push?.(event)
        } else {
            // 创建新的任务分析消息节点
            const analysisMessage: UIMessage = {
                messageId: messageId,
                sessionId: event.sessionId,
                turnId: event.turnId,
                type: 'assistant' as any, // 任务分析属于assistant类型
                sender: getSenderByEventType(event),
                message: message,
                data: event.data,
                startTime: event.startTime || new Date(),
                endTime: event.endTime,
                events: [event],
                meta: event.meta
            }
            messages.value.push(analysisMessage)
            nodeIndex.value[messageId] = messages.value.length - 1
        }
    }


    /** 默认事件处理器映射 */
    const defaultHandlers: Required<SSEEventHandlers> = {
        onStarted: (event: BaseEventItem) => {
            console.log("chat started：" + event.message)
            const startTime = event.startTime || new Date()
            progress.value = new ProgressInfo(event.message, startTime, event.agentId as any)

            // 懒加载模式：如果后端返回了 sessionId，更新前端状态和 URL
            if (event.sessionId && typeof event.sessionId === 'string') {
                const chatStore = useChatStore()
                const route = useRoute()
                const router = useRouter()

                // 如果当前 URL 中没有 sessionId，说明是新会话，需要更新
                if (!route.query.sessionId) {
                    console.log('[useSSE] 收到新 sessionId，更新 URL:', event.sessionId)

                    // 在 chatStore 中创建会话记录
                    chatStore.createSessionIfNotExists(event.sessionId, AgentType.ReAct_Plus)

                    // 更新 URL（使用 replace 避免产生历史记录）
                    router.replace({
                        query: { ...route.query, sessionId: event.sessionId }
                    })
                }
            }

        },
        onThinking: (event: BaseEventItem) => {
            updateMessage(event)
        },
        onAction: (event: BaseEventItem) => {
            updateMessage(event)
        },
        onActing: (event: BaseEventItem) => {
            updateMessage(event)
        },
        onObserving: (event: BaseEventItem) => {
            updateMessage(event)
        },
        onExecuting: (event: BaseEventItem) => {
            updateMessage(event)
        },
        onTool: (event: BaseEventItem) => {
            updateMessage(event)
        },
        onToolApproval: (event: BaseEventItem) => {
            updateMessage(event)
        },
        onInteraction: (event: BaseEventItem) => {
            updateMessage(event)
        },
        onProgress: (event: BaseEventItem) => {
            const message = (event.message || '').toString()
            const startTime = event.startTime || new Date()
            progress.value = new ProgressInfo(message, startTime, event.agentId as any)
        },
        onDone: (event: BaseEventItem) => {
            const message = (event.message || '').toString()
            const startTime = event.startTime || new Date()
            options?.onDoneNotice?.({
                text: message,
                startTime,
                title: currentTaskTitle.value || '',
                messageId: event.messageId || undefined,
                type: NotificationType.WARNING
            })
        },
        onDoneWithWarning: (event: BaseEventItem) => {
            const message = (event.message || '').toString()
            const startTime = event.startTime || new Date()
            progress.value = null
            options?.onDoneNotice?.({
                text: message,
                startTime,
                title: currentTaskTitle.value || '',
                messageId: event.messageId || undefined,
                type: NotificationType.WARNING
            })
        },
        onError: (event: BaseEventItem) => {
            const message = (event.message || '').toString()
            const startTime = event.startTime || new Date()

            // 1. 更新消息列表 - 确保错误信息在聊天记录中可见
            updateMessage(event)

            // 2. 更新状态 - 同步任务状态、进度和连接状态
            taskStatus.value.set('error')
            progress.value = null
            connectionStatus.value.set('disconnected')

            // 3. 发送错误通知
            options?.onDoneNotice?.({
                text: '[ERROR] ' + message,
                startTime,
                title: currentTaskTitle.value || '',
                messageId: event.messageId || undefined,
                type: NotificationType.ERROR
            })

            // 4. 关闭连接防止资源泄露
            closeActiveSource()
        },
        onCompleted: (event: BaseEventItem) => {
            // COMPLETED为流结束信号，不写入消息列表，但需要更新状态
            connectionStatus.value.set('disconnected')
            taskStatus.value.set('completed')
            progress.value = null // 清空进度信息
            closeActiveSource() // 使用新的安全关闭方法
        },
        onDefault: (event: BaseEventItem) => {
            updateMessage(event)
        },

        // ReActPlus 专属事件处理器的默认实现
        onTaskAnalysis: (event: BaseEventItem) => {
            // 任务分析阶段：累积消息到独立节点
            updateTaskAnalysisMessage(event)
        },
        onThought: (event: BaseEventItem) => {
            // 思维链生成：累积消息到独立节点
            updateThoughtMessage(event)
        },
        onInitPlan: (event: BaseEventItem) => {
            // 初始化计划：处理计划创建

            // 处理计划数据，集成到ChatStore
            if (event.data && event.sessionId) {
                const chatStore = useChatStore()
                try {
                    const planData = event.data as InitPlanEventData
                    if (planData.plan) {
                        chatStore.setSessionPlan(event.sessionId, planData.plan)
                        console.log('Plan initialized for session:', event.sessionId, planData.plan)
                    }
                } catch (error) {
                    console.error('Failed to process INIT_PLAN event:', error)
                }
            }
        },
        onUpdatePlan: (event: BaseEventItem) => {
            // 更新计划：处理计划修改

            // 更新ChatStore中的计划数据
            if (event.data && event.sessionId) {
                const chatStore = useChatStore()
                try {
                    const updateData = event.data as UpdatePlanEventData
                    if (updateData.updates) {
                        chatStore.updateSessionPlan(event.sessionId, updateData.updates)
                        console.log('Plan updated for session:', event.sessionId, updateData.updates)
                    }
                } catch (error) {
                    console.error('Failed to process UPDATE_PLAN event:', error)
                }
            }
        },
        onAdvancePlan: (event: BaseEventItem) => {
            // 推进计划：处理阶段推进

            // 处理阶段推进逻辑
            if (event.data && event.sessionId) {
                const chatStore = useChatStore()
                try {
                    const advanceData = event.data as AdvancePlanEventData
                    chatStore.advancePlanPhase(
                        event.sessionId,
                        advanceData.fromPhaseId,
                        advanceData.toPhaseId
                    )
                    console.log('Plan advanced for session:', event.sessionId,
                        'from:', advanceData.fromPhaseId, 'to:', advanceData.toPhaseId)
                } catch (error) {
                    console.error('Failed to process ADVANCE_PLAN event:', error)
                }
            }
        }
    }

    // === 核心事件处理函数（对外暴露） ===

    /**
     * 手动处理SSE事件
     * @param event SSE事件对象
     * @param source SSE连接源（可选）
     */
    const handleEvent = (event: BaseEventItem, source?: SSESource): void => {


        const eventType = event.type
        const customHandlers = options.handlers || {}

        // 获取事件类型对应的处理器名称
        const handlerName = getHandlerNameByEventType(eventType)

        // 优先使用自定义处理器
        const customHandler = customHandlers[handlerName]
        if (customHandler) {
            const result = customHandler(event)
            // 如果自定义处理器返回false，则跳过默认处理器
            if (result === false) return
        }

        // 执行默认处理
        console.log('default handle event: ', eventType)
        const defaultHandler = defaultHandlers[handlerName]
        defaultHandler(event)

    }

    /** 根据事件类型获取处理器方法名称 */
    const getHandlerNameByEventType = (eventType: string): keyof SSEEventHandlers => {
        const handlerMap: Record<string, keyof SSEEventHandlers> = {
            [EventType.STARTED]: 'onStarted',
            [EventType.THINKING]: 'onThinking',
            [EventType.ACTION]: 'onAction',
            [EventType.ACTING]: 'onActing',
            [EventType.OBSERVING]: 'onObserving',
            [EventType.EXECUTING]: 'onExecuting',
            [EventType.TOOL]: 'onTool',
            [EventType.TOOL_APPROVAL]: 'onToolApproval',
            [EventType.INTERACTION]: 'onInteraction',
            [EventType.PROGRESS]: 'onProgress',
            [EventType.DONE]: 'onDone',
            [EventType.DONEWITHWARNING]: 'onDoneWithWarning',
            [EventType.ERROR]: 'onError',
            [EventType.COMPLETED]: 'onCompleted',
            // ReActPlus 专属事件类型
            [EventType.TASK_ANALYSIS]: 'onTaskAnalysis',
            [EventType.THOUGHT]: 'onThought',
            [EventType.INIT_PLAN]: 'onInitPlan',
            [EventType.UPDATE_PLAN]: 'onUpdatePlan',
            [EventType.ADVANCE_PLAN]: 'onAdvancePlan'
        }

        return handlerMap[eventType] || 'onDefault'
    }

    // === 通用 Agent 执行器 ===


    /**
     * 通用的 Agent 执行方法
     * @param text 用户输入文本
     * @param sessionId 会话ID
     * @param agentOptions Agent配置选项
     */
    const executeAgent = async (
        text: string,
        sessionId: string,
        agentOptions: AgentExecuteOptions
    ): Promise<void> => {

        return new Promise<void>((resolve, reject) => {
            // 动态 import，避免在 SSR 或测试环境报错
            ssePromise.then(({ SSE }) => {
                // 启动新任务前先清理之前的连接
                closeActiveSource()

                currentTaskTitle.value = text || ''
                progress.value = null


                const defaultHeaders: Record<string, string> = {
                    'Content-Type': 'application/json',
                    Accept: 'text/event-stream',
                    'Cache-Control': 'no-cache',
                }

                // 从 authStore 获取 token 并添加到 headers
                const authStore = useAuthStore()
                const token = authStore.accessToken
                if (token) {
                    defaultHeaders['Authorization'] = `Bearer ${token}`
                }

                const source = new SSE(agentOptions.endpoint, {
                    method: agentOptions.method || 'POST',
                    headers: { ...defaultHeaders, ...(agentOptions.headers || {}) },
                    payload: agentOptions.payload ? JSON.stringify(agentOptions.payload) : undefined,
                }) as SSESource

                // 保存当前活动连接的引用
                activeSource.value = source

                source.addEventListener('open', () => {
                    connectionStatus.value.set('connected')
                    taskStatus.value.set('running')
                    scrollToBottom()
                })


                /** 创建事件监听器的工厂函数 */
                const createEventListener = (eventName: string) => (messageEvent: MessageEvent) => {
                    console.log(`[SSE] 收到事件: ${eventName}`, messageEvent.data)
                    if (!messageEvent?.data) return
                    try {
                        const event = JSON.parse(messageEvent.data) as BaseEventItem
                        console.log(`[SSE] 解析事件成功: ${eventName}`, event)
                        handleEvent(event, source)
                    } catch (e) {
                        console.error(`Failed to parse ${eventName} event:`, e)
                    }
                }

                // 定义所有需要监听的事件类型（避免重复注册）
                const EVENT_TYPES = [
                    // 基础事件
                    'STARTED', 'PROGRESS', 'AGENT_SELECTED', 'THINKING',
                    'ACTION', 'ACTING', 'OBSERVING', 'DONE', 'EXECUTING',
                    'ERROR', 'TOOL', 'DONEWITHWARNING', 'TOOL_APPROVAL',
                    'INTERACTION', 'COMPLETED',
                    // ReActPlus 专属事件
                    'TASK_ANALYSIS', 'THOUGHT', 'INIT_PLAN',
                    'UPDATE_PLAN', 'ADVANCE_PLAN'
                ] as const

                // 统一注册所有事件监听器
                EVENT_TYPES.forEach(eventType => {
                    source.addEventListener(eventType, createEventListener(eventType))
                })

                // 特别重要：监听默认的 'message' 事件
                source.addEventListener('message', createEventListener('message'))

                // 监听连接状态事件
                source.addEventListener('error', (event: Event) => {
                    console.error('[SSE] 连接错误:', event)
                    connectionStatus.value.set('error')
                    taskStatus.value.set('error')
                })

                source.addEventListener('close', () => {
                    console.log('[SSE] 连接关闭')
                    connectionStatus.value.set('disconnected')
                })

                try {
                    source.stream()
                } catch (e: any) {
                    reject(new Error('启动SSE流失败: ' + (e?.message || '未知错误')))
                }
            })
                .catch((e) => {
                    reject(new Error('未能加载 sse.js: ' + (e as Error).message))
                })
        })
    }

    /**
     * 执行ReAct智能体
     * @param text 用户输入文本
     * @param sessionId 会话ID
     */
    const executeReAct = async (text: string, sessionId: string): Promise<void> => {
        return executeAgent(text, sessionId, {
            endpoint: '/api/agent/chat/react/stream',
            method: 'POST',
            payload: {
                message: text,
                sessionId,
            }
        })
    }
    /**
     * 执行ReActPlus智能体
     * @param text 用户输入文本
     * @param sessionId 会话ID
     */
    const executeReActPlus = async (text: string, sessionId: string): Promise<void> => {
        return executeAgent(text, sessionId, {
            endpoint: '/api/agent/chat/react-plus/stream',
            method: 'POST',
            payload: {
                message: text,
                sessionId,
            }
        })
    }



    // === 返回接口 ===
    return {
        // 状态数据（保持不变，确保向后兼容）
        messages,
        nodeIndex,
        connectionStatus,
        taskStatus,
        progress,

        // 执行方法（重构后的新接口）
        executeReAct,          // 保留：向后兼容的快捷方法
        executeReActPlus,          // 保留：向后兼容的快捷方法

        // 连接管理（新增）
        cleanup,               // 新增：清理所有状态和连接
        closeActiveSource,     // 新增：关闭当前活动连接

    }
}
