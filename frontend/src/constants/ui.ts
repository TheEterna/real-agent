import { EventType, EventType } from '../types/events'



// Map EventType => MessageType used by UI renderer
export const MessageTypeMap: Record<string, EventType> = {
  [EventType.STARTED]: EventType.SYSTEM,
  [EventType.PROGRESS]: EventType.SYSTEM,
  [EventType.AGENT_SELECTED]: EventType.SYSTEM,
  [EventType.THINKING]: EventType.ASSISTANT, // rendered as thinking
  [EventType.ACTION]: EventType.ASSISTANT,   // rendered as action in UI component
  [EventType.ACTING]: EventType.ASSISTANT,
  [EventType.OBSERVING]: EventType.ASSISTANT,
  [EventType.DONE]: EventType.SYSTEM,
  [EventType.EXECUTING]: EventType.ASSISTANT,
  [EventType.ERROR]: EventType.ERROR,
  [EventType.TOOL]: EventType.TOOL,
  [EventType.DONEWITHWARNING]: EventType.SYSTEM,
  [EventType.TOOL_APPROVAL]: EventType.TOOL_APPROVAL,
  [EventType.INTERACTION]: EventType.TOOL_APPROVAL, // 通用交互请求，使用与工具审批相同的UI
  [EventType.COMPLETED]: EventType.SYSTEM,

  // ReActPlus 专属事件类型
  [EventType.TASK_ANALYSIS]: EventType.ASSISTANT,  // 任务分析阶段
  [EventType.THOUGHT]: EventType.ASSISTANT,        // 思维链生成
  [EventType.INIT_PLAN]: EventType.ASSISTANT,      // 初始化计划
  [EventType.UPDATE_PLAN]: EventType.ASSISTANT,    // 更新计划
  [EventType.ADVANCE_PLAN]: EventType.ASSISTANT,   // 推进计划
}

