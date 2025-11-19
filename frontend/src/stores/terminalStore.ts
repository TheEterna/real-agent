/**
 * 极客模式终端命令存储
 * 简单的命令定义和管理，去除过度设计
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 简化的命令接口
export interface SimpleCommand {
  name: string
  aliases?: string[]
  description: string
  usage: string
  parameters?: SimpleParameter[]
  examples?: string[]
  category: 'system' | 'ai' | 'file' | 'connection'
}

export interface SimpleParameter {
  name: string
  required: boolean
  description: string
  shortFlag?: string
  longFlag?: string
  defaultValue?: any
}

// 解析结果 - 匹配后端 TerminalCommandRequest
export interface ParsedCommand {
  original: string      // 原始输入
  command: string       // 命令名称（不含 /）
  options: string[]     // 选项参数列表（--开头的参数）
  arguments: string[]   // 普通参数列表
}

// 简化的错误类型
export interface ParseError {
  message: string
  suggestion?: string
}

export const useTerminalStore = defineStore('terminal', () => {
  // 命令列表
  const commands = ref<SimpleCommand[]>([
    // 系统控制命令
    {
      name: 'help',
      aliases: ['h', '?'],
      description: '显示帮助信息',
      usage: 'help [command]',
      examples: ['help', 'help connect'],
      category: 'system',
      parameters: [
        {
          name: 'command',
          required: false,
          description: '要查看帮助的命令名称'
        }
      ]
    },

    {
      name: 'clear',
      aliases: ['cls'],
      description: '清空终端屏幕',
      usage: 'clear',
      category: 'system'
    },

    {
      name: 'history',
      description: '显示命令历史',
      usage: 'history [-n number]',
      examples: ['history', 'history -n 10'],
      category: 'system',
      parameters: [
        {
          name: 'number',
          required: false,
          shortFlag: '-n',
          longFlag: '--number',
          description: '显示最近的N条命令',
          defaultValue: 20
        }
      ]
    },

    // AI交互命令
    {
      name: 'chat',
      aliases: ['ask'],
      description: '与AI助手对话',
      usage: 'chat <message>',
      examples: ['chat 你好', 'chat 帮我写一个Python函数'],
      category: 'ai',
      parameters: [
        {
          name: 'message',
          required: true,
          description: '要发送给AI的消息'
        }
      ]
    },

    {
      name: 'plan',
      description: '让AI制定计划',
      usage: 'plan <task> [--detail]',
      examples: ['plan 学习Vue3', 'plan 开发网站 --detail'],
      category: 'ai',
      parameters: [
        {
          name: 'task',
          required: true,
          description: '要制定计划的任务'
        },
        {
          name: 'detail',
          required: false,
          longFlag: '--detail',
          description: '生成详细计划',
          defaultValue: false
        }
      ]
    },

    // 连接命令
    {
      name: 'connect',
      description: '连接到远程服务器',
      usage: 'connect <server> [--port port] [--user username]',
      examples: [
        'connect example.com',
        'connect 192.168.1.100 --port 2222 --user admin'
      ],
      category: 'connection',
      parameters: [
        {
          name: 'server',
          required: true,
          description: '服务器地址或名称'
        },
        {
          name: 'port',
          required: false,
          longFlag: '--port',
          description: 'SSH端口号',
          defaultValue: 22
        },
        {
          name: 'user',
          required: false,
          longFlag: '--user',
          description: '用户名'
        }
      ]
    },

    {
      name: 'disconnect',
      description: '断开服务器连接',
      usage: 'disconnect',
      category: 'connection'
    },

    // 文件操作命令
    {
      name: 'ls',
      aliases: ['dir'],
      description: '列出目录内容',
      usage: 'ls [path] [-l] [-a]',
      examples: ['ls', 'ls /home', 'ls -la'],
      category: 'file',
      parameters: [
        {
          name: 'path',
          required: false,
          description: '要列出的目录路径',
          defaultValue: '.'
        },
        {
          name: 'long',
          required: false,
          shortFlag: '-l',
          description: '详细格式显示',
          defaultValue: false
        },
        {
          name: 'all',
          required: false,
          shortFlag: '-a',
          description: '显示隐藏文件',
          defaultValue: false
        }
      ]
    },

    {
      name: 'pwd',
      description: '显示当前工作目录',
      usage: 'pwd',
      category: 'file'
    },

    {
      name: 'cat',
      description: '显示文件内容',
      usage: 'cat <file>',
      examples: ['cat readme.txt', 'cat /etc/hosts'],
      category: 'file',
      parameters: [
        {
          name: 'file',
          required: true,
          description: '要查看的文件路径'
        }
      ]
    }
  ])

  // 计算属性：按分类分组的命令
  const commandsByCategory = computed(() => {
    const grouped: Record<string, SimpleCommand[]> = {
      system: [],
      ai: [],
      file: [],
      connection: []
    }

    commands.value.forEach(cmd => {
      grouped[cmd.category].push(cmd)
    })

    return grouped
  })

  // 获取单个命令
  const getCommand = (name: string): SimpleCommand | undefined => {
    return commands.value.find(cmd =>
      cmd.name === name.toLowerCase() ||
      cmd.aliases?.includes(name.toLowerCase())
    )
  }

  // 获取所有命令
  const getAllCommands = (): SimpleCommand[] => {
    return commands.value
  }

  // 获取命令建议（用于自动补全）
  const getCommandSuggestions = (query: string): SimpleCommand[] => {
    if (!query) return commands.value

    const lowerQuery = query.toLowerCase()

    return commands.value.filter(cmd => {
      // 匹配命令名称
      if (cmd.name.toLowerCase().includes(lowerQuery)) return true

      // 匹配别名
      if (cmd.aliases?.some(alias => alias.toLowerCase().includes(lowerQuery))) return true

      return false
    }).sort((a, b) => {
      // 精确匹配优先
      const aExact = a.name.toLowerCase() === lowerQuery
      const bExact = b.name.toLowerCase() === lowerQuery
      if (aExact && !bExact) return -1
      if (!aExact && bExact) return 1

      // 按名称排序
      return a.name.localeCompare(b.name)
    })
  }

  /**
   * 命令解析 - 新规则
   * 1. 命令部分：必须在开头，只能有一个（如 /plan）
   * 2. 选项参数：--开头，有多个时只取第一个
   * 3. 普通参数：其余所有内容作为字符串数组
   * 
   * 示例：
   * /plan --detailed 规划一下 我去马来西亚的旅游
   * => command: "plan", options: ["detailed"], arguments: ["规划一下", "我去马来西亚的旅游"]
   * 
   * /plan 规划一下 --detailed 我去马来西亚的旅游
   * => command: "plan", options: ["detailed"], arguments: ["规划一下", "我去马来西亚的旅游"]
   */
  const parseCommand = (input: string): { command?: ParsedCommand; error?: ParseError } => {
    const trimmed = input.trim()
    if (!trimmed) {
      return { error: { message: '请输入命令' } }
    }

    if (!trimmed.startsWith('/')) {
      return {
        error: {
          message: '命令必须以 / 开头',
          suggestion: `尝试: /${trimmed}`
        }
      }
    }

    const withoutPrefix = trimmed.slice(1)
    if (!withoutPrefix) {
      return { error: { message: '命令名称不能为空' } }
    }

    // 按空格分词
    const tokens = withoutPrefix.split(/\s+/).filter(t => t.length > 0)
    if (tokens.length === 0) {
      return { error: { message: '无效的命令格式' } }
    }

    // 1. 提取命令名称（第一个token）
    const commandName = tokens[0]
    const command = getCommand(commandName)

    if (!command) {
      const suggestions = getCommandSuggestions(commandName)
      const suggestion = suggestions.length > 0
        ? `您是否想输入: ${suggestions[0].name}?`
        : undefined

      return {
        error: {
          message: `未知命令: ${commandName}`,
          suggestion
        }
      }
    }

    // 2. 解析选项和参数
    const options: string[] = []
    const args: string[] = []
    let firstOptionFound = false

    for (let i = 1; i < tokens.length; i++) {
      const token = tokens[i]
      
      if (token.startsWith('--')) {
        // 选项参数：只取第一个
        if (!firstOptionFound) {
          const optionName = token.substring(2) // 去掉 --
          if (optionName) {
            options.push(optionName)
            firstOptionFound = true
          }
        }
        // 后续的选项参数被忽略
      } else {
        // 普通参数
        args.push(token)
      }
    }

    return {
      command: {
        original: input,
        command: commandName,
        options: options,
        arguments: args
      }
    }
  }

  return {
    commands,
    commandsByCategory,
    getCommand,
    getAllCommands,
    getCommandSuggestions,
    parseCommand
  }
})