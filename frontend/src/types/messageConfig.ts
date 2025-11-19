/**
 * 消息配置类型定义
 * 用于控制消息的行为、布局和交互
 */

export interface CollapsibleConfig {
  /** 是否启用折叠 */
  enabled: boolean
  /** 默认是否折叠 */
  defaultCollapsed?: boolean
  /** 超过多少行才折叠 */
  collapseThreshold?: number
  /** 折叠时是否显示预览 */
  showPreview?: boolean
  /** 预览行数 */
  previewLines?: number
}

export interface InteractionConfig {
  /** 可复制 */
  copyable?: boolean
  /** 可展开 */
  expandable?: boolean
  /** 悬停效果 */
  hoverable?: boolean
  /** 可拖拽 */
  draggable?: boolean
}

export interface AnimationConfig {
  /** 入场动画类型 */
  entrance?: 'fade' | 'slide' | 'zoom' | 'none'
  /** 动画持续时间（毫秒） */
  duration?: number
  /** 缓动函数 */
  easing?: string
}

export interface LayoutConfig {
  /** 布局变体 */
  variant?: 'default' | 'compact' | 'card' | 'minimal'
  /** 对齐方式 */
  alignment?: 'left' | 'right' | 'center'
  /** 最大宽度 */
  maxWidth?: string
}

/**
 * 单个消息的配置
 */
export interface MessageConfig {
  /** 折叠配置 */
  collapsible?: CollapsibleConfig
  /** 交互配置 */
  interaction?: InteractionConfig
  /** 动画配置 */
  animation?: AnimationConfig
  /** 布局配置 */
  layout?: LayoutConfig
  /** 自定义渲染组件名称 */
  customComponent?: string
}

/**
 * Agent级别的消息配置
 * 针对不同事件类型设置不同配置
 */
export interface AgentMessageConfig {
  /** Thinking 事件配置 */
  thinking?: MessageConfig
  /** Action 事件配置 */
  action?: MessageConfig
  /** Observing 事件配置 */
  observing?: MessageConfig
  /** Tool 事件配置 */
  tool?: MessageConfig
  /** Error 事件配置 */
  error?: MessageConfig
  /** User 消息配置 */
  user?: MessageConfig
  /** Assistant 消息配置 */
  assistant?: MessageConfig
  /** 默认配置（fallback） */
  default?: MessageConfig
}

/**
 * 预设风格常量
 */
export enum MessageStyle {
  /** 默认风格 */
  DEFAULT = 'default',
  /** ChatGPT 风格 */
  CHATGPT = 'chatgpt',
  /** 豆包风格 */
  DOUBAO = 'doubao',
  /** Claude 风格 */
  CLAUDE = 'claude',
  /** 紧凑风格 */
  COMPACT = 'compact',
}
