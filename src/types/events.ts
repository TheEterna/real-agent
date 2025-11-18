// Strong types for SSE events and UI messages


export enum EventType {
  STARTED = 'STARTED',
  PROGRESS = 'PROGRESS',
  AGENT_SELECTED = 'AGENT_SELECTED',
  THINKING = 'THINKING',
  ACTION = 'ACTION',
  ACTING = 'ACTING',
  OBSERVING = 'OBSERVING',
  DONE = 'DONE',
  EXECUTING = 'EXECUTING',
  ERROR = 'ERROR',
  DONEWITHWARNING = 'DONEWITHWARNING',
  INTERACTION = 'INTERACTION',
  COMPLETED = 'COMPLETED',

  SYSTEM = 'SYSTEM',
  USER = 'USER',
  ASSISTANT = 'ASSISTANT',
  TOOL = 'TOOL',
  TOOL_APPROVAL = 'TOOL_APPROVAL',

  // ReActPlus 专属事件类型,
  TASK_ANALYSIS = 'TASK_ANALYSIS',  // 任务分析阶段
  THOUGHT = 'THOUGHT',              // 思维链生成
  INIT_PLAN = 'INIT_PLAN',          // 初始化计划
  UPDATE_PLAN = 'UPDATE_PLAN',      // 更新计划
  ADVANCE_PLAN = 'ADVANCE_PLAN',    // 推进计划

}

export interface BaseEventItem {
  sessionId?: string
  turnId?: string
  startTime: Date
  endTime?: Date
  spanId?: string
  nodeId?: string
  agentId: string
  type: EventType
  message: string
  data?: any
  meta?: object
}

export interface ToolResponseData {
  toolCallId?: string
  toolName?: string
  responseData?: any
}

export type ToolEventData = ToolResponseData | Record<string, any> | any

// ReActPlus 计划相关的数据结构定义
export interface PlanPhase {
  id?: string                  // 唯一标识，前端生成
  title: string               // 阶段标题
  description: string         // 阶段详细描述
  isParallel: boolean        // 是否并行执行
  status?: PlanPhaseStatus   // 阶段执行状态
  index?: number             // 阶段在计划中的顺序
}

export enum PlanPhaseStatus {
  TODO = 'TODO',                 // 待执行
  RUNNING = 'RUNNING',           // 执行中
  COMPLETED = 'COMPLETED',       // 已完成
  PAUSED = 'PAUSED',             // 已暂停
  FAILED = 'FAILED'              // 执行失败
}
export interface PlanData {
  goal: string                   // 任务总体目标
  phases: PlanPhase[]           // 计划阶段列表
  currentPhaseId?: string       // 当前执行的阶段ID
  status?: PlanStatus           // 整体计划状态
  createdAt?: Date             // 计划创建时间
  updatedAt?: Date             // 计划最后更新时间
}

export enum PlanStatus {
  PLANNING = 'PLANNING',         // 规划中
  EXECUTING = 'EXECUTING',       // 执行中
  COMPLETED = 'COMPLETED',       // 已完成
  PAUSED = 'PAUSED',             // 已暂停
  FAILED = 'FAILED'              // 执行失败
}

// Plan事件的数据载荷
export interface InitPlanEventData {
  plan: PlanData                // 初始化的计划数据
  sessionId: string             // 会话ID
  turnId?: string               // 轮次ID
}

export interface UpdatePlanEventData {
  planId?: string               // 计划ID（如果需要）
  updates: Partial<PlanData>    // 需要更新的计划字段
  reason?: string               // 更新原因
  sessionId: string             // 会话ID
}

export interface AdvancePlanEventData {
  planId?: string               // 计划ID
  fromPhaseId?: string          // 源阶段ID
  toPhaseId: string             // 目标阶段ID
  completedPhase?: PlanPhase    // 已完成的阶段信息
  sessionId: string             // 会话ID
}

export interface UIMessage {
  // identity & tracing
  nodeId?: string
  sessionId?: string
  turnId?: string

  // categorization
  type: EventType

  sender: string

  // text payload
  message: string
  data?: any

  // time
  startTime?: Date
  endTime?: Date

  events?: BaseEventItem[],
  meta?: any;

}
