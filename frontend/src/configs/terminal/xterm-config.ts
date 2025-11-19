// xterm.js 基础配置
// Real Agent 极客模式终端配置

import type {TerminalConfig, TerminalTheme} from '@/types/terminal'
import type {GeekModeTheme} from '@/types/terminal/themes'

// 默认终端配置
export const DEFAULT_TERMINAL_CONFIG: TerminalConfig = {
    // 基础配置
    scrollback: 1000,
    fontSize: 16,
    fontWeight: 'normal',
    lineHeight: 1.2,  // 调整为1.2，1.0太紧凑可能导致选择框计算问题
    letterSpacing: 0,

    // 光标配置
    cursorBlink: true,
    cursorStyle: 'bar',
    cursorWidth: 1,

    // 渲染配置
    allowTransparency: true,
    drawBoldTextInBrightColors: true,
    logLevel: 'info',

    // 交互配置
    convertEol: true,
    disableStdin: false,
    macOptionIsMeta: false,
    rightClickSelectsWord: false,  // ⚠️ 改为false，避免与我们的右键菜单冲突

    // 滚动配置
    fastScrollModifier: 'alt',
    fastScrollSensitivity: 5,
    scrollSensitivity: 1,

    // 选择配置
    wordSeparator: ' ()[]{}\'"`',

    // 扩展配置
    enableFit: true,
    enableWebLinks: true,
    enableSearch: true,
    autoFocus: true,

    // 极客模式特殊配置
    geekMode: {
        matrixEffect: true,
        scanlineEffect: true,
        glitchEffect: false,
        customPrompt: '$ '
    }
}

// 极客模式默认主题
export const GEEK_DEFAULT_THEME: GeekModeTheme = {
    // 基础信息
    id: 'geek-default',
    name: 'Geek Matrix',
    category: 'dark',
    description: 'Default geek mode theme with matrix effects',
    author: 'Real Agent',
    version: '1.0.0',

    // 基础颜色 (xterm.js ITheme 兼容)
    foreground: '#00ff00',
    background: '#000000',
    // 光标配置
    cursor: {
        style: 'block',
        blink: true,
        color: '#00ff00',
        width: 1
    },
    cursorAccent: '#000000',
    selection: 'rgba(0, 255, 0, 0.3)',

    // 标准16色
    black: '#000000',
    red: '#ff0000',
    green: '#00ff00',
    yellow: '#ffff00',
    blue: '#0000ff',
    magenta: '#ff00ff',
    cyan: '#00ffff',
    white: '#ffffff',
    brightBlack: '#404040',
    brightRed: '#ff4040',
    brightGreen: '#40ff40',
    brightYellow: '#ffff40',
    brightBlue: '#4040ff',
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
        fontFamily: 'Courier New, JetBrains Mono, Consolas, Monaco, monospace',
        fontSize: 14,
        fontWeight: 'normal',
        lineHeight: 1.2,
        letterSpacing: 0
    },


    // UI元素颜色
    ui: {
        prompt: '#00ff00',
        command: '#40ff40',
        output: '#00ff00',
        error: '#ff4040',
        success: '#40ff40',
        warning: '#ffff40',
        info: '#40ffff',
        link: '#4040ff',
        highlight: 'rgba(0, 255, 0, 0.3)'
    },

    // 矩阵效果配置
    matrix: {
        enabled: true,
        characters: ['0', '1', 'A', 'B', 'C', 'D', 'E', 'F', '!', '@', '#', '$', '%', '^', '&', '*'],
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

    // 音效配置
    audio: {
        enabled: false,
        keyPress: '',
        startup: '',
        error: '',
        success: '',
        volume: 0.5
    }
}

// 性能优化配置
export const PERFORMANCE_CONFIG = {
    // 渲染优化
    renderer: {
        type: 'canvas' as const,
        antialias: false,
        powerPreference: 'high-performance' as const
    },

    // 滚动优化
    scrolling: {
        buffer: 1000,
        smooth: false,
        throttle: 16 // 60fps
    },

    // 内存优化
    memory: {
        gcThreshold: 5000,
        maxHistory: 10000,
        cleanupInterval: 30000 // 30秒
    }
}

// 快捷键配置
export const KEYBINDINGS = {
    copy: ['Ctrl+C', 'Ctrl+Shift+C'],
    paste: ['Ctrl+V', 'Ctrl+Shift+V'],
    clear: ['Ctrl+L'],
    interrupt: ['Ctrl+C'],
    selectAll: ['Ctrl+A'],
    find: ['Ctrl+F'],
    findNext: ['F3', 'Ctrl+G'],
    findPrevious: ['Shift+F3', 'Ctrl+Shift+G'],

    // 极客模式专用
    toggleMatrix: ['Ctrl+M'],
    toggleScanlines: ['Ctrl+S'],
    toggleFullscreen: ['F11'],
    switchTheme: ['Ctrl+T']
}

// 辅助函数：合并配置
export function mergeTerminalConfig(
    base: Partial<TerminalConfig>,
    override: Partial<TerminalConfig> = {}
): TerminalConfig {
    return {
        ...DEFAULT_TERMINAL_CONFIG,
        ...base,
        ...override,
        geekMode: {
            ...DEFAULT_TERMINAL_CONFIG.geekMode,
            ...base.geekMode,
            ...override.geekMode
        }
    }
}

// 辅助函数：合并主题
export function mergeTheme(
    base: Partial<GeekModeTheme>,
    override: Partial<GeekModeTheme> = {}
): GeekModeTheme {
    return {
        ...GEEK_DEFAULT_THEME,
        ...base,
        ...override,
        effects: {
            ...GEEK_DEFAULT_THEME.effects,
            ...base.effects,
            ...override.effects
        },
        typography: {
            ...GEEK_DEFAULT_THEME.typography,
            ...base.typography,
            ...override.typography
        },
        ui: {
            ...GEEK_DEFAULT_THEME.ui,
            ...base.ui,
            ...override.ui
        },
        matrix: {
            ...GEEK_DEFAULT_THEME.matrix,
            ...base.matrix,
            ...override.matrix
        },
        scanlines: {
            ...GEEK_DEFAULT_THEME.scanlines,
            ...base.scanlines,
            ...override.scanlines
        }
    }
}

// 验证配置
export function validateTerminalConfig(config: Partial<TerminalConfig>): boolean {
    try {

        if (config.fontSize && (config.fontSize < 6 || config.fontSize > 72)) {
            console.warn('Invalid fontSize value:', config.fontSize)
            return false
        }

        return true
    } catch (error) {
        console.error('Error validating terminal config:', error)
        return false
    }
}