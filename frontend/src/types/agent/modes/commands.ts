// 命令系统类型定义
// 用于极客模式的命令行式交互

export interface ParsedCommand {
  command: string
  args: string
  original: string
  metadata?: Record<string, any>
}

export interface CommandHandler {
  name: string
  description: string
  usage: string
  examples: string[]
  execute: (args: string, context?: any) => Promise<string | any>
}

export interface CommandRegistry {
  [key: string]: () => Promise<{ default: CommandHandler }>
}

export interface CommandContext {
  sessionId: string
  agentId: string
  startTime: Date
}