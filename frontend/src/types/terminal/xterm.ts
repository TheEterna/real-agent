// xterm.js 扩展类型定义
// 为 Real Agent 极客模式定制的 xterm.js 类型扩展

import type { Terminal } from '@xterm/xterm'

// 扩展 xterm Terminal 类型
export interface ExtendedTerminal extends Terminal {
  // 自定义属性
  _realAgentId?: string
  _sessionId?: string
  _isGeekMode?: boolean

  // 自定义方法
  writeMessage?: (message: string, type?: 'success' | 'error' | 'warning' | 'info') => void
  writeTyping?: (text: string, speed?: number) => Promise<void>
  clearScreen?: () => void
  showPrompt?: () => void
  hidePrompt?: () => void

  // 极客模式特殊方法
  enableMatrixEffect?: () => void
  disableMatrixEffect?: () => void
  addScanlineEffect?: () => void
  removeScanlineEffect?: () => void
}

// xterm.js 插件接口扩展
export interface XTermAddonConfig {
  fit?: {
    proposeDimensions?: () => { cols: number; rows: number }
  }
  webLinks?: {
    urlRegex?: RegExp
    handler?: (event: MouseEvent, uri: string) => void
  }
  search?: {
    caseSensitive?: boolean
    regex?: boolean
    wholeWord?: boolean
    incremental?: boolean
  }
}

// 终端适配器接口
export interface TerminalAdapter {
  terminal: ExtendedTerminal
  container: HTMLElement

  // 生命周期方法
  mount: () => Promise<void>
  unmount: () => void
  resize: () => void
  focus: () => void
  blur: () => void

  // 内容方法
  write: (data: string) => void
  writeln: (data: string) => void
  clear: () => void

  // 事件方法
  onData: (handler: (data: string) => void) => void
  onKey: (handler: (event: { key: string; domEvent: KeyboardEvent }) => void) => void
  onResize: (handler: (data: { cols: number; rows: number }) => void) => void

  // 特殊功能
  enableGeekMode: () => void
  disableGeekMode: () => void
}

// 终端渲染选项
export interface TerminalRenderOptions {
  fontSize: number
  fontFamily: string
  lineHeight: number
  letterSpacing: number
  cursorBlink: boolean
  cursorStyle: 'block' | 'underline' | 'bar'
  cursorWidth: number
  allowTransparency: boolean
  drawBoldTextInBrightColors: boolean
}

// 极客模式视觉效果配置
export interface GeekModeVisualConfig {
  matrixRain: {
    enabled: boolean
    speed: number
    density: number
    color: string
  }
  scanlines: {
    enabled: boolean
    opacity: number
    thickness: number
    speed: number
  }
  glitch: {
    enabled: boolean
    intensity: number
    frequency: number
  }
  bloom: {
    enabled: boolean
    intensity: number
    threshold: number
  }
}

// 终端快捷键配置
export interface TerminalKeyBindings {
  copy: string[]
  paste: string[]
  clear: string[]
  interrupt: string[]
  selectAll: string[]
  find: string[]
  findNext: string[]
  findPrevious: string[]
}

// 终端性能配置
export interface TerminalPerformanceConfig {
  fastScrollModifier: 'alt' | 'ctrl' | 'shift'
  fastScrollSensitivity: number
  scrollSensitivity: number
  scrollback: number
  windowsMode: boolean
  rendererType: 'dom' | 'canvas'
}

// 导出 xterm.js 相关类型
export type {
  ITerminalOptions,
  ITerminalAddon,
  IBuffer,
  IBufferLine,
  IBufferCell,
  IViewportRange,
  ITheme
} from '@xterm/xterm'