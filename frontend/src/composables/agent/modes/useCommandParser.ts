// 命令解析器 - 极客模式专用
// 支持 /command args 格式的命令解析

import type { ParsedCommand } from '@/types/agent/modes/commands'

export const useCommandParser = () => {
  // 解析命令字符串
  const parseCommand = (input: string): ParsedCommand | null => {
    if (!input.startsWith('/')) return null

    const trimmedInput = input.trim()
    const spaceIndex = trimmedInput.indexOf(' ')

    let command: string
    let args: string

    if (spaceIndex === -1) {
      // 没有参数的命令
      command = trimmedInput.slice(1).toLowerCase()
      args = ''
    } else {
      // 有参数的命令
      command = trimmedInput.slice(1, spaceIndex).toLowerCase()
      args = trimmedInput.slice(spaceIndex + 1).trim()
    }

    return {
      command,
      args,
      original: trimmedInput,
      metadata: extractMetadata(args)
    }
  }

  // 从参数中提取元数据
  const extractMetadata = (args: string): Record<string, any> => {
    const metadata: Record<string, any> = {}

    // 解析 --flag=value 格式的参数
    const flagRegex = /--(\w+)(?:=(\S+))?/g
    let match

    while ((match = flagRegex.exec(args)) !== null) {
      const key = match[1]
      const value = match[2] || true
      metadata[key] = value
    }

    // 解析 -f value 格式的参数
    const shortFlagRegex = /-([a-zA-Z])\s+(\S+)/g
    while ((match = shortFlagRegex.exec(args)) !== null) {
      const key = match[1]
      const value = match[2]
      metadata[key] = value
    }

    return metadata
  }

  // 检查命令是否有效
  const isValidCommand = (command: string): boolean => {
    return getAvailableCommands().includes(command)
  }

  // 获取可用命令列表
  const getAvailableCommands = (): string[] => {
    return [
      'help',
      'clear',
      'time',
      'status',
      'theme',
      'version'
    ]
  }

  // 获取命令帮助信息
  const getCommandHelp = (command?: string): string => {
    const helpInfo = {
      help: '显示帮助信息\n用法: /help [命令名]',
      clear: '清空终端屏幕\n用法: /clear',
      time: '显示当前时间\n用法: /time',
      status: '显示系统状态信息\n用法: /status',
      theme: '管理终端主题\n用法: /theme [list|set <主题名>]',
      version: '显示版本信息\n用法: /version'
    }

    if (command && helpInfo[command as keyof typeof helpInfo]) {
      return helpInfo[command as keyof typeof helpInfo]
    }

    // 返回所有命令的帮助
    return `可用命令:
${Object.entries(helpInfo).map(([cmd, desc]) =>
  `  /${cmd} - ${desc.split('\n')[0]}`
).join('\n')}

输入 /help <命令名> 查看具体命令的详细用法`
  }

  // 命令自动补全
  const getCommandSuggestions = (partial: string): string[] => {
    const commands = getAvailableCommands()

    if (!partial.startsWith('/')) {
      return []
    }

    const commandPart = partial.slice(1).toLowerCase()

    return commands
      .filter(cmd => cmd.startsWith(commandPart))
      .map(cmd => `/${cmd}`)
  }

  return {
    parseCommand,
    isValidCommand,
    getAvailableCommands,
    getCommandHelp,
    getCommandSuggestions,
    extractMetadata
  }
}