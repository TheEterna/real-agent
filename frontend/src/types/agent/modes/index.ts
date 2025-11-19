// Agent模式类型定义
// 参考：docs/front/agent/models/architecture.md

import type { ComponentType } from 'vue'

export type AgentModeType = 'default' | 'geek' | 'multimodal' | 'coding'

export interface AgentModeConfig {
  id: AgentModeType
  name: string
  description: string
  icon: ComponentType
  renderer: ComponentType        // UI渲染器
  processor: MessageProcessor   // 消息处理器
  apiAdapter: ApiAdapter       // API适配器
  commandSupport: boolean      // 是否支持命令
  theme: string               // 主题样式
  inputPlaceholder: string    // 输入框提示
}

export interface AgentModeContext {
  currentMode: Ref<AgentModeType>
  modeConfig: ComputedRef<AgentModeConfig>
  availableModes: ComputedRef<AgentModeConfig[]>
  switchMode: (mode: AgentModeType) => Promise<void>
  isSupported: (feature: string) => boolean
}

// 消息处理器接口
export interface MessageProcessor {
  processMessage(message: string, context: AgentModeContext): Promise<any>
}

// API适配器接口
export interface ApiAdapter {
  processMessage(message: string, context: AgentModeContext): Promise<any>
}

// 渲染器类型
export type RendererComponent = ComponentType

import type { Ref, ComputedRef } from 'vue'