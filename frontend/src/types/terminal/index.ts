// 终端相关类型定义
// 基于 xterm.js 的 Real Agent 极客模式类型系统

import type { ITerminalOptions, ITerminalAddon } from '@xterm/xterm'

// 终端实例接口
export interface TerminalInstance {
  id: string
  element: HTMLElement | null
  terminal: any // xterm Terminal 实例
  addons: ITerminalAddon[]
  isReady: boolean
  theme: TerminalTheme
}

// 终端配置选项
export interface TerminalConfig extends ITerminalOptions {
  // 扩展的配置项
  container?: HTMLElement
  enableFit?: boolean
  enableWebLinks?: boolean
  enableSearch?: boolean
  autoFocus?: boolean

  // 极客模式特殊配置
  geekMode?: {
    matrixEffect?: boolean
    scanlineEffect?: boolean
    glitchEffect?: boolean
    customPrompt?: string
  }
}

// 终端主题配置
export interface TerminalTheme {
  name: string
  foreground: string
  background: string
  cursor: string
  cursorAccent: string
  selection: string
  black: string
  red: string
  green: string
  yellow: string
  blue: string
  magenta: string
  cyan: string
  white: string
  brightBlack: string
  brightRed: string
  brightGreen: string
  brightYellow: string
  brightBlue: string
  brightMagenta: string
  brightCyan: string
  brightWhite: string
}

// 终端消息类型
export interface TerminalMessage {
  id: string
  type: 'input' | 'output' | 'error' | 'system' | 'command'
  content: string
  startTime: Date
  sessionId?: string
  nodeId?: string
  metadata?: Record<string, any>
}

// 终端命令接口
export interface TerminalCommand {
  name: string
  description: string
  usage: string
  examples: string[]
  handler: (args: string[], options?: any) => Promise<string | void>
}

// 终端状态
export interface TerminalState {
  isConnected: boolean
  isLoading: boolean
  currentDirectory: string
  prompt: string
  history: string[]
  historyIndex: number
}

// 终端事件类型
export type TerminalEventType =
  | 'ready'
  | 'data'
  | 'key'
  | 'paste'
  | 'selection'
  | 'resize'
  | 'focus'
  | 'blur'
  | 'dispose'

// 终端事件处理器
export interface TerminalEventHandler {
  type: TerminalEventType
  handler: (...args: any[]) => void
}

// 流式输出处理器
export interface StreamProcessor {
  process: (chunk: string) => void
  complete: () => void
  error: (error: Error) => void
}

// 极客模式特殊功能
export interface GeekModeFeatures {
  commandHistory: boolean
  autoCompletion: boolean
  syntaxHighlighting: boolean
  asciiArt: boolean
  matrixRain: boolean
  glitchEffects: boolean
  soundEffects: boolean
}

// 导出所有类型
export type {
  ITerminalOptions,
  ITerminalAddon
} from '@xterm/xterm'