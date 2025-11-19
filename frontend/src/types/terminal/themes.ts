// 终端主题类型定义
// Real Agent 极客模式终端主题系统

import type { TerminalTheme } from './index'

// 预定义主题名称
export type ThemeName =
  | 'geek-matrix'
  | 'geek-cyberpunk'
  | 'geek-neon'
  | 'geek-retro'
  | 'geek-minimal'
  | 'geek-hacker'

// 主题类别
export type ThemeCategory =
  | 'dark'
  | 'light'
  | 'high-contrast'
  | 'cyberpunk'
  | 'retro'

// 动画效果类型
export type AnimationEffect =
  | 'matrix-rain'
  | 'scanlines'
  | 'crt-flicker'
  | 'glitch'
  | 'bloom'
  | 'typewriter'
  | 'pulse'

// 扩展的主题配置
export interface ExtendedTerminalTheme extends TerminalTheme {
  // 基础信息
  id: string
  name: string
  category: ThemeCategory
  description: string
  author: string
  version: string

  // 视觉效果
  effects: {
    animations: AnimationEffect[]
    transparency: number
    glow: boolean
  }

  // 字体配置
  typography: {
    fontFamily: string
    fontSize: number
    fontWeight: string
    lineHeight: number
    letterSpacing: number
  }

  // 光标配置
  cursor: {
    style: 'block' | 'underline' | 'bar'
    blink: boolean
    color: string
    width: number
  }

  // 特殊元素颜色
  ui: {
    prompt: string
    command: string
    output: string
    error: string
    success: string
    warning: string
    info: string
    link: string
    highlight: string
  }
}

// 极客模式专用主题
export interface GeekModeTheme extends ExtendedTerminalTheme {
  // 矩阵效果配置
  matrix: {
    enabled: boolean
    characters: string[]
    speed: number
    density: number
    color: string
    fadeSpeed: number
  }

  // 扫描线效果
  scanlines: {
    enabled: boolean
    opacity: number
    thickness: number
    speed: number
    color: string
  }

  // CRT效果
  crt: {
    enabled: boolean
    curvature: number
    distortion: number
    noise: number
    flicker: number
  }

  // 音效配置
  audio: {
    enabled: boolean
    keyPress: string
    startup: string
    error: string
    success: string
    volume: number
  }
}

// 主题变量映射
export interface ThemeVariables {
  '--terminal-bg': string
  '--terminal-fg': string
  '--terminal-cursor': string
  '--terminal-selection': string
  '--terminal-border': string
  '--terminal-shadow': string
  '--terminal-prompt': string
  '--terminal-command': string
  '--terminal-output': string
  '--terminal-error': string
  '--terminal-success': string
  '--terminal-warning': string
  '--terminal-info': string
}

// 主题配置构建器
export interface ThemeBuilder {
  setBase: (colors: Partial<TerminalTheme>) => ThemeBuilder
  setTypography: (typography: Partial<ExtendedTerminalTheme['typography']>) => ThemeBuilder
  setEffects: (effects: Partial<ExtendedTerminalTheme['effects']>) => ThemeBuilder
  setUI: (ui: Partial<ExtendedTerminalTheme['ui']>) => ThemeBuilder
  setGeekMode: (geek: Partial<GeekModeTheme['matrix'] & GeekModeTheme['scanlines']>) => ThemeBuilder
  build: () => GeekModeTheme
}

// 主题管理器接口
export interface ThemeManager {
  // 主题操作
  loadTheme: (name: ThemeName) => Promise<GeekModeTheme>
  applyTheme: (theme: GeekModeTheme) => void
  getCurrentTheme: () => GeekModeTheme | null
  listThemes: () => GeekModeTheme[]

  // 自定义主题
  createCustomTheme: (base: ThemeName, overrides: Partial<GeekModeTheme>) => GeekModeTheme
  saveCustomTheme: (theme: GeekModeTheme) => Promise<void>
  deleteCustomTheme: (id: string) => Promise<void>

  // 主题切换
  switchTheme: (name: ThemeName) => Promise<void>
  toggleDarkMode: () => void

  // 效果控制
  enableEffect: (effect: AnimationEffect) => void
  disableEffect: (effect: AnimationEffect) => void
  toggleEffect: (effect: AnimationEffect) => void
}

// 预定义的极客模式主题配置
export const GEEK_THEMES: Record<ThemeName, Partial<GeekModeTheme>> = {
  'geek-matrix': {
    name: 'Matrix Green',
    category: 'dark',
    foreground: '#00ff00',
    background: '#000000',
    matrix: {
      enabled: true,
      characters: ['0', '1', 'A', 'B', 'C', '!', '@', '#'],
      speed: 50,
      density: 0.05,
      color: '#00ff00',
      fadeSpeed: 1000
    }
  },

  'geek-cyberpunk': {
    name: 'Cyberpunk Neon',
    category: 'cyberpunk',
    foreground: '#ff00ff',
    background: '#0a0a0a',
    effects: {
      animations: ['scanlines', 'bloom', 'glitch'],
      glow: true,
      transparency: 0.9
    }
  },

  'geek-neon': {
    name: 'Neon Blue',
    category: 'dark',
    foreground: '#00ffff',
    background: '#001122',
    effects: {
      animations: ['scanlines', 'bloom'],
      glow: true,
      transparency: 0.95
    }
  },

  'geek-retro': {
    name: 'Retro Amber',
    category: 'retro',
    foreground: '#ffaa00',
    background: '#2a1810',
    crt: {
      enabled: true,
      curvature: 0.1,
      distortion: 0.05,
      noise: 0.1,
      flicker: 0.02
    }
  },

  'geek-minimal': {
    name: 'Minimal Dark',
    category: 'dark',
    foreground: '#ffffff',
    background: '#1a1a1a',
    effects: {
      animations: [],
      glow: false,
      transparency: 1
    }
  },

  'geek-hacker': {
    name: 'Elite Hacker',
    category: 'dark',
    foreground: '#00ff41',
    background: '#000000',
    matrix: {
      enabled: true,
      characters: ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'F'],
      speed: 30,
      density: 0.08,
      color: '#00ff41',
      fadeSpeed: 800
    }
  }
}