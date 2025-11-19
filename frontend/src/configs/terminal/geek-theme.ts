// 极客模式主题配置
// Real Agent 极客模式预定义主题集合

import type { GeekModeTheme } from '@/types/terminal/themes'
import { GEEK_THEMES } from '@/types/terminal/themes'

// 矩阵绿色主题
export const MATRIX_GREEN_THEME: GeekModeTheme = {
  id: 'geek-matrix-green',
  name: 'Matrix Green',
  category: 'dark',
  description: 'Classic Matrix movie inspired green terminal',
  author: 'Real Agent',
  version: '1.0.0',

  // 基础颜色
  foreground: '#00ff00',
  background: '#000000',
  cursorAccent: '#000000',
  selection: 'rgba(0, 255, 0, 0.3)',

  // 16色调色板
  black: '#000000',
  red: '#ff0000',
  green: '#00ff00',
  yellow: '#ffff00',
  blue: '#0080ff',
  magenta: '#ff00ff',
  cyan: '#00ffff',
  white: '#c0c0c0',
  brightBlack: '#404040',
  brightRed: '#ff4040',
  brightGreen: '#40ff40',
  brightYellow: '#ffff40',
  brightBlue: '#4080ff',
  brightMagenta: '#ff40ff',
  brightCyan: '#40ffff',
  brightWhite: '#ffffff',

  // 视觉效果
  effects: {
    animations: ['matrix-rain', 'scanlines'],
    transparency: 0.95,
    glow: true,
  },

  // 字体配置
  typography: {
    fontFamily: 'Courier New, JetBrains Mono, SF Mono, Consolas, monospace',
    fontSize: 14,
    fontWeight: 'normal',
    lineHeight: 1.2,
    letterSpacing: 0.5
  },

  // 光标配置
  cursor: {
    style: 'block',
    blink: true,
    color: '#00ff00',
    width: 1
  },

  // UI元素颜色
  ui: {
    prompt: '#00ff00',
    command: '#40ff40',
    output: '#00dd00',
    error: '#ff4040',
    success: '#40ff40',
    warning: '#ffff40',
    info: '#40ffff',
    link: '#0080ff',
    highlight: 'rgba(0, 255, 0, 0.2)'
  },

  // 矩阵效果
  matrix: {
    enabled: true,
    characters: ['0', '1', 'ア', 'イ', 'ウ', 'エ', 'オ', 'カ', 'キ', 'ク', 'ケ', 'コ'],
    speed: 50,
    density: 0.05,
    color: '#00ff00',
    fadeSpeed: 1000
  },

  // 扫描线效果
  scanlines: {
    enabled: true,
    opacity: 0.1,
    thickness: 1,
    speed: 2,
    color: '#00ff00'
  },

  // CRT效果
  crt: {
    enabled: false,
    curvature: 0,
    distortion: 0,
    noise: 0,
    flicker: 0
  },

  // 音效
  audio: {
    enabled: false,
    keyPress: '',
    startup: '',
    error: '',
    success: '',
    volume: 0.5
  }
}

// 赛博朋克主题
export const CYBERPUNK_THEME: GeekModeTheme = {
  id: 'geek-cyberpunk',
  name: 'Cyberpunk Neon',
  category: 'cyberpunk',
  description: 'Futuristic cyberpunk terminal with neon colors',
  author: 'Real Agent',
  version: '1.0.0',

  // 基础颜色
  foreground: '#ff00ff',
  background: '#0a0a0a',
  cursorAccent: '#0a0a0a',
  selection: 'rgba(255, 0, 255, 0.3)',

  // 16色调色板
  black: '#000000',
  red: '#ff0080',
  green: '#00ff80',
  yellow: '#ff8000',
  blue: '#8000ff',
  magenta: '#ff00ff',
  cyan: '#00ffff',
  white: '#e0e0e0',
  brightBlack: '#404040',
  brightRed: '#ff4080',
  brightGreen: '#40ff80',
  brightYellow: '#ff8040',
  brightBlue: '#8040ff',
  brightMagenta: '#ff40ff',
  brightCyan: '#40ffff',
  brightWhite: '#ffffff',

  // 视觉效果
  effects: {
    animations: ['scanlines', 'bloom', 'glitch'],
    transparency: 0.9,
    glow: true,
  },

  // 字体配置
  typography: {
    fontFamily: 'JetBrains Mono, SF Mono, Consolas, monospace',
    fontSize: 14,
    fontWeight: 'normal',
    lineHeight: 1.3,
    letterSpacing: 0.3
  },

  // 光标配置
  cursor: {
    style: 'bar',
    blink: true,
    color: '#ff00ff',
    width: 2
  },

  // UI元素颜色
  ui: {
    prompt: '#ff00ff',
    command: '#ff40ff',
    output: '#ff80ff',
    error: '#ff0080',
    success: '#00ff80',
    warning: '#ff8000',
    info: '#00ffff',
    link: '#8000ff',
    highlight: 'rgba(255, 0, 255, 0.2)'
  },

  // 矩阵效果
  matrix: {
    enabled: false,
    characters: [],
    speed: 0,
    density: 0,
    color: '#ff00ff',
    fadeSpeed: 0
  },

  // 扫描线效果
  scanlines: {
    enabled: true,
    opacity: 0.15,
    thickness: 2,
    speed: 3,
    color: '#ff00ff'
  },

  // CRT效果
  crt: {
    enabled: true,
    curvature: 0.05,
    distortion: 0.02,
    noise: 0.05,
    flicker: 0.01
  },

  // 音效
  audio: {
    enabled: false,
    keyPress: '',
    startup: '',
    error: '',
    success: '',
    volume: 0.5
  }
}

// 复古琥珀主题
export const RETRO_AMBER_THEME: GeekModeTheme = {
  id: 'geek-retro-amber',
  name: 'Retro Amber',
  category: 'retro',
  description: 'Classic amber terminal from the 80s',
  author: 'Real Agent',
  version: '1.0.0',

  // 基础颜色
  foreground: '#ffb000',
  background: '#2a1810',
  cursorAccent: '#2a1810',
  selection: 'rgba(255, 176, 0, 0.3)',

  // 16色调色板
  black: '#2a1810',
  red: '#ff8080',
  green: '#80ff80',
  yellow: '#ffff80',
  blue: '#8080ff',
  magenta: '#ff80ff',
  cyan: '#80ffff',
  white: '#ffb000',
  brightBlack: '#804020',
  brightRed: '#ffb080',
  brightGreen: '#b0ff80',
  brightYellow: '#ffffb0',
  brightBlue: '#b080ff',
  brightMagenta: '#ffb0ff',
  brightCyan: '#b0ffff',
  brightWhite: '#ffe080',

  // 视觉效果
  effects: {
    animations: ['crt-flicker'],
    transparency: 1,
    glow: true,
  },

  // 字体配置
  typography: {
    fontFamily: 'Courier New, monospace',
    fontSize: 16,
    fontWeight: 'normal',
    lineHeight: 1.1,
    letterSpacing: 0
  },

  // 光标配置
  cursor: {
    style: 'block',
    blink: true,
    color: '#ffb000',
    width: 1
  },

  // UI元素颜色
  ui: {
    prompt: '#ffb000',
    command: '#ffe080',
    output: '#ffb000',
    error: '#ff8080',
    success: '#80ff80',
    warning: '#ffff80',
    info: '#80ffff',
    link: '#8080ff',
    highlight: 'rgba(255, 176, 0, 0.2)'
  },

  // 矩阵效果
  matrix: {
    enabled: false,
    characters: [],
    speed: 0,
    density: 0,
    color: '#ffb000',
    fadeSpeed: 0
  },

  // 扫描线效果
  scanlines: {
    enabled: true,
    opacity: 0.05,
    thickness: 1,
    speed: 1,
    color: '#ffb000'
  },

  // CRT效果
  crt: {
    enabled: true,
    curvature: 0.1,
    distortion: 0.05,
    noise: 0.1,
    flicker: 0.02
  },

  // 音效
  audio: {
    enabled: false,
    keyPress: '',
    startup: '',
    error: '',
    success: '',
    volume: 0.5
  }
}

// 预定义主题映射
export const PREDEFINED_THEMES = {
  'matrix-green': MATRIX_GREEN_THEME,
  'cyberpunk': CYBERPUNK_THEME,
  'retro-amber': RETRO_AMBER_THEME
} as const

// 获取默认主题
export function getDefaultTheme(): GeekModeTheme {
  return MATRIX_GREEN_THEME
}

// 获取主题列表
export function getAvailableThemes(): GeekModeTheme[] {
  return Object.values(PREDEFINED_THEMES)
}

// 根据名称获取主题
export function getThemeByName(name: string): GeekModeTheme | null {
  return PREDEFINED_THEMES[name as keyof typeof PREDEFINED_THEMES] || null
}

// 验证主题配置
export function validateTheme(theme: Partial<GeekModeTheme>): boolean {
  try {
    // 检查必需字段
    if (!theme.name || !theme.foreground || !theme.background) {
      return false
    }

    // 检查颜色格式
    const colorRegex = /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$|^rgba?\(/
    if (!colorRegex.test(theme.foreground) || !colorRegex.test(theme.background)) {
      return false
    }

    return true
  } catch (error) {
    console.error('Error validating theme:', error)
    return false
  }
}