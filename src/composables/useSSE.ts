import {ref, nextTick} from 'vue'
import {UIMessage, EventType, BaseEventItem, InitPlanEventData, UpdatePlanEventData, AdvancePlanEventData, PlanData} from '@/types/events'
import {ConnectionStatus, TaskStatus, ProgressInfo} from '@/types/status'
import {NotificationType} from '@/types/notification'
import { useChatStore } from '@/stores/chatStore'
const ssePromise = import('sse.js')
// === 新增接口定义 ===


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
    /** 是否启用默认处理器，默认为true */
    enableDefaultHandlers?: boolean
    /** 滚动到底部回调 */
    onScrollToBottom?: () => void
    /** 完成通知回调 */
    onDoneNotice?: (p: {
        text: string;
        startTime: Date;
        title: string;
        nodeId?: string,
        type: NotificationType
    }) => void
}

function updateThoughtMessage(event: BaseEventItem) {
    
}

function updateTaskAnalysisMessage(event: BaseEventItem) {
    
}

export function useSSE(options: SSEOptions = {}) {
    // === 响应式状态 ===
    const messages = ref<UIMessage[]>([])
    const nodeIndex = ref<Record<string, number>>({})
    const connectionStatus = ref(new ConnectionStatus('disconnected'))
    const taskStatus = ref(new TaskStatus('idle'))
    const progress = ref<ProgressInfo | null>(null)
    const currentTaskTitle = ref<string>("")

    // === 工具函数 ===
    const closeSource = (source: any) => {
        try {
            if (source && typeof source.close === 'function') source.close()
        } catch (e) {
            console.error(e)
        }
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
        const nodeId: string = event.nodeId as string
        const sessionId: string | undefined = event.sessionId
        const turnId: string | undefined = event.turnId
        const type: EventType = event.type
        const message = (event.message || '').toString()
        const data = event.data
        const startTime = event.startTime || new Date()
        const endTime = event.endTime ?? new Date()



        const index = nodeIndex.value[nodeId]

        if (type === EventType.TOOL) {
            // 工具事件作为独立消息插入
            const toolMsg: UIMessage = {
                nodeId: nodeId,
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
                nodeId: nodeId,
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
            // nodeId已存在，更新现有消息
            const node = messages.value[index]

            // 非工具事件：累积到message字段
            node.message = node.message ? `${node.message}${message}` : message
            node.events?.push?.(event)

        } else {

            // 非工具事件作为主消息创建并建立nodeIndex

            const firstNodeMessage: UIMessage = {
                nodeId: nodeId,
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
            nodeIndex.value[nodeId] = messages.value.length - 1
        }
    }


    /** 默认事件处理器映射 */
    const defaultHandlers: Required<SSEEventHandlers> = {
        onStarted: (event: BaseEventItem) => {
            updateMessage(event)
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
                nodeId: (event.nodeId as string) || undefined,
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
                nodeId: (event.nodeId as string) || undefined,
                type: NotificationType.WARNING
            })
        },
        onError: (event: BaseEventItem) => {
            const message = (event.message || '').toString()
            const startTime = event.startTime || new Date()
            options?.onDoneNotice?.({
                text: '[ERROR] ' + message,
                startTime,
                title: currentTaskTitle.value || '',
                nodeId: (event.nodeId as string) || undefined,
                type: NotificationType.ERROR
            })
        },
        onCompleted: (event: BaseEventItem) => {
            // COMPLETED为流结束信号，不写入消息列表，但需要更新状态
            connectionStatus.value.set('disconnected')
            taskStatus.value.set('completed')
            progress.value = null // 清空进度信息
            closeSource(source)
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
                        chatStore.setPlanVisibility(true) // 自动显示计划侧边栏
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
    const handleEvent = (event: BaseEventItem, source?: any): void => {
        if (!event?.type) return

        const eventType = event.type
        const customHandlers = options.handlers || {}
        const enableDefault = options.enableDefaultHandlers !== false

        // 获取事件类型对应的处理器名称
        const handlerName = getHandlerNameByEventType(eventType)

        // 优先使用自定义处理器
        const customHandler = customHandlers[handlerName]
        if (customHandler) {
            const result = customHandler(event)
            // 如果自定义处理器返回false，则跳过默认处理器
            if (result === false) return
        }

        // 如果启用默认处理器且没有被自定义处理器阻止，则执行默认处理
        if (enableDefault) {
            const defaultHandler = defaultHandlers[handlerName]
            defaultHandler(event)
        }
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
            ssePromise.then(({SSE}) => {
                    currentTaskTitle.value = text || ''
                    // 启动新任务时清理进度
                    progress.value = null

                    const defaultHeaders = {
                        'Content-Type': 'application/json',
                        Accept: 'text/event-stream',
                        'Cache-Control': 'no-cache',
                    }

                    const source = new SSE(agentOptions.endpoint, {
                        method: agentOptions.method || 'POST',
                        headers: { ...defaultHeaders, ...(agentOptions.headers || {}) },
                        payload: agentOptions.payload ? JSON.stringify(agentOptions.payload) : undefined,
                    })

                    const closeAndResolve = () => {
                        closeSource(source)
                        resolve()
                    }

                    source.addEventListener('open', () => {
                        connectionStatus.value.set('connected')
                        taskStatus.value.set('running')
                        scrollToBottom()
                    })


                    /** 创建事件监听器的工厂函数 */
                    const createEventListener = (eventName: string) => (messageEvent: MessageEvent) => {
                        if (!messageEvent?.data) return
                        try {
                            const event = JSON.parse(messageEvent.data) as BaseEventItem
                            handleEvent(event, source)
                        } catch (e) {
                            console.error(`Failed to parse ${eventName} event:`, e)
                        }
                    }

                    // 为每个事件类型创建专属的监听器函数

                    const onStarted = createEventListener('STARTED')
                    const onProgress = createEventListener('PROGRESS')
                    const onAgentSelected = createEventListener('AGENT_SELECTED')
                    const onThinking = createEventListener('THINKING')
                    const onAction = createEventListener('ACTION')
                    const onActing = createEventListener('ACTING')
                    const onObserving = createEventListener('OBSERVING')
                    const onDone = createEventListener('DONE')
                    const onExecuting = createEventListener('EXECUTING')
                    const onError = createEventListener('ERROR')
                    const onTool = createEventListener('TOOL')
                    const onDoneWithWarning = createEventListener('DONEWITHWARNING')
                    const onToolApproval = createEventListener('TOOL_APPROVAL')
                    const onInteraction = createEventListener('INTERACTION')
                    const onCompleted = createEventListener('COMPLETED')

                    // ReActPlus 专属事件监听器
                    const onTaskAnalysis = createEventListener('TASK_ANALYSIS')
                    const onThought = createEventListener('THOUGHT')
                    const onInitPlan = createEventListener('INIT_PLAN')
                    const onUpdatePlan = createEventListener('UPDATE_PLAN')
                    const onAdvancePlan = createEventListener('ADVANCE_PLAN')

                    // 监听所有可能的事件类型，每个事件使用专属的监听器
                    source.addEventListener('message', onExecuting)
                    source.addEventListener('STARTED', onStarted)
                    source.addEventListener('PROGRESS', onProgress)
                    source.addEventListener('AGENT_SELECTED', onAgentSelected)
                    source.addEventListener('THINKING', onThinking)
                    source.addEventListener('ACTION', onAction)
                    source.addEventListener('ACTING', onActing)
                    source.addEventListener('OBSERVING', onObserving)
                    source.addEventListener('DONE', onDone)
                    source.addEventListener('EXECUTING', onExecuting)
                    source.addEventListener('ERROR', onError)
                    source.addEventListener('TOOL', onTool)
                    source.addEventListener('DONEWITHWARNING', onDoneWithWarning)
                    source.addEventListener('TOOL_APPROVAL', onToolApproval)
                    source.addEventListener('INTERACTION', onInteraction)
                    source.addEventListener('COMPLETED', onCompleted)

                    // 监听 ReActPlus 专属事件
                    source.addEventListener('TASK_ANALYSIS', onTaskAnalysis)
                    source.addEventListener('THOUGHT', onThought)
                    source.addEventListener('INIT_PLAN', onInitPlan)
                    source.addEventListener('UPDATE_PLAN', onUpdatePlan)
                    source.addEventListener('ADVANCE_PLAN', onAdvancePlan)

                    source.addEventListener('ERROR', onError)

                    try {
                        (source as any).stream()
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
                userId: 'user-001',
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
                userId: 'user-001',
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

        // 事件处理（新增，修复ReActPlus的handleEvent问题）
        handleEvent,           // 新增：手动处理SSE事件的方法
        updateMessage,         // 新增：手动更新消息的方法

    }
}
