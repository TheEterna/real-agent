// 终端命令解析器
// 解析用户输入的命令，支持参数和选项

import { ref } from 'vue'

// 解析后的命令结构
export interface ParsedCommand {
  command: string
  args: string[]
  options: Record<string, string | boolean>
  rawInput: string
}

// 命令定义接口
export interface CommandDefinition {
  name: string
  description: string
  usage: string
  examples: string[]
  aliases?: string[]
  handler: (args: string[], options: Record<string, any>, context?: any) => Promise<string>
}

export function useCommandParser() {
  // 可用命令注册表
  const registeredCommands = ref<Map<string, CommandDefinition>>(new Map())

  // 解析命令字符串
  const parseCommand = (input: string): ParsedCommand | null => {
    const trimmed = input.trim()
    if (!trimmed.startsWith('/')) {
      return null
    }

    // 移除开头的 /
    const commandPart = trimmed.substring(1)
    if (!commandPart) {
      return null
    }

    // 使用正则表达式解析命令、参数和选项
    const tokens = tokenize(commandPart)
    if (tokens.length === 0) {
      return null
    }

    const command = tokens[0]
    const args: string[] = []
    const options: Record<string, string | boolean> = {}

    // 解析剩余的tokens
    for (let i = 1; i < tokens.length; i++) {
      const token = tokens[i]

      if (token.startsWith('--')) {
        // 长选项 --option=value 或 --option
        const optionPart = token.substring(2)
        const equalIndex = optionPart.indexOf('=')

        if (equalIndex !== -1) {
          const key = optionPart.substring(0, equalIndex)
          const value = optionPart.substring(equalIndex + 1)
          options[key] = value
        } else {
          options[optionPart] = true
        }
      } else if (token.startsWith('-') && token.length > 1) {
        // 短选项 -o 或 -o value
        const option = token.substring(1)

        // 检查下一个token是否是值
        if (i + 1 < tokens.length && !tokens[i + 1].startsWith('-')) {
          options[option] = tokens[i + 1]
          i++ // 跳过下一个token
        } else {
          options[option] = true
        }
      } else {
        // 普通参数
        args.push(token)
      }
    }

    return {
      command,
      args,
      options,
      rawInput: input
    }
  }

  // 将命令字符串分割为tokens
  const tokenize = (input: string): string[] => {
    const tokens: string[] = []
    let current = ''
    let inQuotes = false
    let quoteChar = ''

    for (let i = 0; i < input.length; i++) {
      const char = input[i]

      if (!inQuotes && (char === '"' || char === "'")) {
        inQuotes = true
        quoteChar = char
      } else if (inQuotes && char === quoteChar) {
        inQuotes = false
        quoteChar = ''
      } else if (!inQuotes && char === ' ') {
        if (current.trim()) {
          tokens.push(current.trim())
          current = ''
        }
      } else {
        current += char
      }
    }

    if (current.trim()) {
      tokens.push(current.trim())
    }

    return tokens
  }

  // 注册命令
  const registerCommand = (definition: CommandDefinition) => {
    registeredCommands.value.set(definition.name, definition)

    // 注册别名
    if (definition.aliases) {
      definition.aliases.forEach(alias => {
        registeredCommands.value.set(alias, definition)
      })
    }
  }

  // 获取命令定义
  const getCommandDefinition = (commandName: string): CommandDefinition | undefined => {
    return registeredCommands.value.get(commandName)
  }

  // 检查命令是否有效
  const isValidCommand = (commandName: string): boolean => {
    return registeredCommands.value.has(commandName)
  }

  // 获取所有可用命令
  const getAvailableCommands = (): CommandDefinition[] => {
    const commands = new Map<string, CommandDefinition>()

    // 去重，只保留主命令（非别名）
    registeredCommands.value.forEach((definition, name) => {
      if (name === definition.name) {
        commands.set(name, definition)
      }
    })

    return Array.from(commands.values()).sort((a, b) => a.name.localeCompare(b.name))
  }

  // 获取命令建议（用于自动完成）
  const getCommandSuggestions = (input: string): string[] => {
    const trimmed = input.trim()
    let searchTerm = ''

    if (trimmed.startsWith('/')) {
      searchTerm = trimmed.substring(1).toLowerCase()
    } else {
      searchTerm = trimmed.toLowerCase()
    }

    if (!searchTerm) {
      return getAvailableCommands().map(cmd => `/${cmd.name}`)
    }

    const suggestions: string[] = []

    // 精确匹配
    registeredCommands.value.forEach((definition, commandName) => {
      if (commandName.toLowerCase().startsWith(searchTerm)) {
        suggestions.push(`/${commandName}`)
      }
    })

    // 模糊匹配
    if (suggestions.length < 5) {
      registeredCommands.value.forEach((definition, commandName) => {
        if (!commandName.toLowerCase().startsWith(searchTerm) &&
            commandName.toLowerCase().includes(searchTerm)) {
          suggestions.push(`/${commandName}`)
        }
      })
    }

    // 去重并排序
    return Array.from(new Set(suggestions))
      .sort((a, b) => a.localeCompare(b))
      .slice(0, 10) // 最多返回10个建议
  }

  // 格式化命令帮助
  const formatCommandHelp = (commandName: string): string => {
    const definition = getCommandDefinition(commandName)
    if (!definition) {
      return `命令 '${commandName}' 不存在`
    }

    let help = `\n命令: ${definition.name}\n`
    help += `描述: ${definition.description}\n`
    help += `用法: /${definition.usage}\n`

    if (definition.aliases && definition.aliases.length > 0) {
      help += `别名: ${definition.aliases.map(a => `/${a}`).join(', ')}\n`
    }

    if (definition.examples && definition.examples.length > 0) {
      help += `\n示例:\n`
      definition.examples.forEach(example => {
        help += `  /${example}\n`
      })
    }

    return help
  }

  // 格式化所有命令的帮助
  const formatAllCommandsHelp = (): string => {
    const commands = getAvailableCommands()

    let help = '\n可用命令:\n\n'

    commands.forEach(cmd => {
      help += `  /${cmd.name.padEnd(12)} - ${cmd.description}\n`
    })

    help += '\n使用 "/help <命令名>" 查看具体命令的详细帮助\n'

    return help
  }

  // 验证命令参数
  const validateCommandArgs = (parsed: ParsedCommand): { valid: boolean; error?: string } => {
    const definition = getCommandDefinition(parsed.command)
    if (!definition) {
      return { valid: false, error: `命令 '${parsed.command}' 不存在` }
    }

    // 这里可以添加更多的参数验证逻辑
    // 例如：必需参数检查、参数类型检查等

    return { valid: true }
  }

  return {
    // 核心功能
    parseCommand,
    registerCommand,
    getCommandDefinition,
    isValidCommand,
    getAvailableCommands,
    getCommandSuggestions,

    // 帮助和验证
    formatCommandHelp,
    formatAllCommandsHelp,
    validateCommandArgs,

    // 状态
    registeredCommands
  }
}

export type UseCommandParserReturn = ReturnType<typeof useCommandParser>