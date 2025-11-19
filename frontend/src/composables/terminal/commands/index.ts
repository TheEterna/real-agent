// 终端命令执行器和注册表
// 管理所有可用的终端命令

import { useCommandParser } from '../useCommandParser'
import type { CommandDefinition } from '../useCommandParser'

// 命令执行上下文
export interface CommandContext {
  terminal?: any
  sessionId?: string
  switchTheme?: (themeName: string) => void
  xtermVersion?: string
  commandHistory?: string[]
  [key: string]: any
}

// 获取命令解析器实例
const { registerCommand, getCommandDefinition, isValidCommand } = useCommandParser()

// 基础命令：帮助
const helpCommand: CommandDefinition = {
  name: 'help',
  description: '显示帮助信息',
  usage: 'help [命令名]',
  examples: [
    'help',
    'help clear',
    'help theme'
  ],
  aliases: ['h', '?'],
  handler: async (args: string[], options: Record<string, any>, context?: CommandContext) => {
    const { formatCommandHelp, formatAllCommandsHelp } = useCommandParser()

    if (args.length === 0) {
      return formatAllCommandsHelp()
    }

    const commandName = args[0]
    return formatCommandHelp(commandName)
  }
}

// 清屏命令
const clearCommand: CommandDefinition = {
  name: 'clear',
  description: '清空终端屏幕',
  usage: 'clear',
  examples: ['clear'],
  aliases: ['cls', 'c'],
  handler: async (args: string[], options: Record<string, any>, context?: CommandContext) => {
    // 返回特殊标识符，告诉终端需要清屏
    return '__CLEAR_TERMINAL__'
  }
}

// 主题切换命令
const themeCommand: CommandDefinition = {
  name: 'theme',
  description: '切换终端主题',
  usage: 'theme [主题名]',
  examples: [
    'theme',
    'theme matrix-green',
    'theme cyberpunk',
    'theme hacker-gold'
  ],
  aliases: ['t'],
  handler: async (args: string[], options: Record<string, any>, context?: CommandContext) => {
    const availableThemes = [
      'matrix-green',
      'matrix-blue',
      'cyberpunk',
      'hacker-gold',
      'terminal-classic'
    ]

    if (args.length === 0) {
      return `可用主题: ${availableThemes.join(', ')}\n使用: /theme <主题名>`
    }

    const themeName = args[0]
    if (!availableThemes.includes(themeName)) {
      return `未知主题: ${themeName}\n可用主题: ${availableThemes.join(', ')}`
    }

    if (context?.switchTheme) {
      context.switchTheme(themeName)
      return `主题已切换为: ${themeName}`
    } else {
      return '主题切换功能当前不可用'
    }
  }
}

// 状态信息命令
const statusCommand: CommandDefinition = {
  name: 'status',
  description: '显示终端状态信息',
  usage: 'status',
  examples: ['status'],
  aliases: ['stat'],
  handler: async (args: string[], options: Record<string, any>, context?: CommandContext) => {
    let status = '\n=== 终端状态 ===\n'
    status += `会话ID: ${context?.sessionId || '未知'}\n`
    status += `XTerm版本: ${context?.xtermVersion || '未知'}\n`
    status += `命令历史: ${context?.commandHistory?.length || 0} 条\n`
    status += `时间: ${new Date().toLocaleString('zh-CN')}\n`

    // 添加终端信息
    if (context?.terminal) {
      const term = context.terminal
      status += `终端尺寸: ${term.cols || '?'} x ${term.rows || '?'}\n`
    }

    return status
  }
}

// 版本信息命令
const versionCommand: CommandDefinition = {
  name: 'version',
  description: '显示系统版本信息',
  usage: 'version',
  examples: ['version'],
  aliases: ['v', 'ver'],
  handler: async (args: string[], options: Record<string, any>, context?: CommandContext) => {
    return `\nReal Agent Terminal v1.0.0\n极客模式 - 基于 xterm.js\n构建时间: ${new Date().getFullYear()}\n`
  }
}

// 历史命令
const historyCommand: CommandDefinition = {
  name: 'history',
  description: '显示命令历史',
  usage: 'history [数量]',
  examples: [
    'history',
    'history 10'
  ],
  aliases: ['hist'],
  handler: async (args: string[], options: Record<string, any>, context?: CommandContext) => {
    const history = context?.commandHistory || []

    if (history.length === 0) {
      return '命令历史为空'
    }

    let limit = 20 // 默认显示最近20条
    if (args.length > 0) {
      const requestedLimit = parseInt(args[0])
      if (!isNaN(requestedLimit) && requestedLimit > 0) {
        limit = Math.min(requestedLimit, 100) // 最多100条
      }
    }

    const recentHistory = history.slice(-limit)
    let result = `\n最近 ${recentHistory.length} 条命令历史:\n\n`

    recentHistory.forEach((cmd, index) => {
      const lineNumber = (history.length - recentHistory.length + index + 1).toString().padStart(3, ' ')
      result += `${lineNumber}  ${cmd}\n`
    })

    return result
  }
}

// Echo命令（测试用）
const echoCommand: CommandDefinition = {
  name: 'echo',
  description: '输出文本',
  usage: 'echo <文本>',
  examples: [
    'echo Hello World',
    'echo "带空格的文本"'
  ],
  handler: async (args: string[], options: Record<string, any>, context?: CommandContext) => {
    if (args.length === 0) {
      return ''
    }
    return args.join(' ')
  }
}

// 注册所有基础命令
const registerBasicCommands = () => {
  registerCommand(helpCommand)
  registerCommand(clearCommand)
  registerCommand(themeCommand)
  registerCommand(statusCommand)
  registerCommand(versionCommand)
  registerCommand(historyCommand)
  registerCommand(echoCommand)
}

// 执行命令
export const executeCommand = async (
  commandName: string,
  args: string[],
  context?: CommandContext
): Promise<string> => {
  const definition = getCommandDefinition(commandName)

  if (!definition) {
    throw new Error(`命令 '${commandName}' 不存在。输入 /help 查看可用命令。`)
  }

  try {
    const result = await definition.handler(args, {}, context)
    return result
  } catch (error) {
    throw new Error(`命令执行失败: ${error instanceof Error ? error.message : String(error)}`)
  }
}

// 初始化命令系统
export const initializeCommands = () => {
  registerBasicCommands()
}

// 导出命令相关类型和工具
export type { CommandDefinition }
export { registerCommand, getCommandDefinition, isValidCommand }

// 自动初始化
initializeCommands()