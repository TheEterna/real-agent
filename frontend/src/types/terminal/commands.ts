/**
 * 终端命令系统类型定义
 * 基于Linux终端规范设计，支持现代化命令行交互
 */

// 命令类别
export enum CommandCategory {
  SYSTEM = 'system',        // 系统控制命令
  AI = 'ai',               // AI交互命令
  FILE = 'file',           // 文件操作命令
  PROJECT = 'project',     // 项目管理命令
  CONNECTION = 'connection' // 连接相关命令
}

// 参数类型
export enum ParameterType {
  STRING = 'string',
  NUMBER = 'number',
  BOOLEAN = 'boolean',
  PATH = 'path',
  URL = 'url',
  JSON = 'json'
}

// 命令参数定义
export interface CommandParameter {
  name: string                    // 参数名称
  type: ParameterType            // 参数类型
  required: boolean              // 是否必需
  shortFlag?: string             // 短标记 (如 -h)
  longFlag?: string              // 长标记 (如 --help)
  description: string            // 参数描述
  defaultValue?: any             // 默认值
  validation?: RegExp            // 验证正则
  choices?: string[]             // 可选值列表
}

// 命令执行上下文
export interface CommandContext {
  sessionId: string              // 会话ID
  isConnected: boolean           // 是否连接到服务器
  currentPath: string            // 当前路径
  history: string[]              // 命令历史
  environment: Record<string, string> // 环境变量
  user?: {                       // 用户信息
    id: string
    username: string
    permissions: string[]
  }
}

// 命令执行结果
export interface CommandResult {
  success: boolean               // 执行是否成功
  output?: string               // 输出内容
  error?: string                // 错误信息
  data?: any                    // 结构化数据
  renderType: CommandRenderType // 渲染类型
  metadata?: {                  // 元数据
    executionTime?: number      // 执行时间(ms)
    exitCode?: number           // 退出码
    warnings?: string[]         // 警告信息
  }
}

// 渲染类型
export enum CommandRenderType {
  TEXT = 'text',                // 纯文本
  TABLE = 'table',              // 表格
  JSON = 'json',                // JSON数据
  PROGRESS = 'progress',        // 进度条
  INTERACTIVE = 'interactive',  // 交互式
  HTML = 'html',                // HTML内容
  MARKDOWN = 'markdown'         // Markdown内容
}

// 命令权限级别
export enum CommandPermission {
  PUBLIC = 'public',            // 公开命令
  USER = 'user',               // 需要用户权限
  ADMIN = 'admin',             // 需要管理员权限
  SYSTEM = 'system'            // 系统级命令
}

// 命令定义接口
export interface Command {
  // 基础信息
  name: string                  // 命令名称 (不含/)
  aliases?: string[]            // 别名
  description: string           // 描述
  usage: string                // 使用方法
  examples?: string[]           // 使用示例

  // 分类和权限
  category: CommandCategory     // 命令类别
  permission: CommandPermission // 权限级别

  // 执行相关
  needsBackend: boolean         // 是否需要后端处理
  needsConnection: boolean      // 是否需要服务器连接
  parameters: CommandParameter[] // 参数定义

  // 处理函数
  handler?: CommandHandler      // 前端处理函数(可选)

  // 配置
  enabled: boolean              // 是否启用
  hidden?: boolean              // 是否隐藏(不在帮助中显示)
  deprecated?: boolean          // 是否已弃用
  version?: string              // 版本号

  // 元数据
  tags?: string[]               // 标签
  relatedCommands?: string[]    // 相关命令
  createdAt?: Date             // 创建时间
  updatedAt?: Date             // 更新时间
}

// 命令处理函数类型
export type CommandHandler = (
  args: ParsedCommand,
  context: CommandContext
) => Promise<CommandResult> | CommandResult

// 解析后的命令
export interface ParsedCommand {
  original: string              // 原始输入
  command: string              // 命令名称
  args: string[]               // 位置参数
  flags: Record<string, any>   // 标记参数
  options: Record<string, any> // 选项参数
}

// 命令解析错误
export interface CommandParseError {
  type: 'UNKNOWN_COMMAND' | 'INVALID_SYNTAX' | 'MISSING_PARAMETER' | 'INVALID_PARAMETER'
  message: string
  suggestion?: string
  position?: number
}

// 命令提示信息
export interface CommandSuggestion {
  command: Command
  score: number                // 匹配分数
  matchType: 'exact' | 'prefix' | 'fuzzy' | 'alias'
  highlightRanges?: Array<[number, number]> // 高亮范围
}

// 命令执行选项
export interface CommandExecutionOptions {
  timeout?: number             // 超时时间(ms)
  silent?: boolean             // 静默执行
  dryRun?: boolean            // 干运行模式
  verbose?: boolean           // 详细输出
  background?: boolean        // 后台执行
}

// 命令注册表配置
export interface CommandRegistry {
  commands: Map<string, Command>
  aliases: Map<string, string>
  categories: Map<CommandCategory, Command[]>

  // 方法
  register(command: Command): void
  unregister(name: string): void
  get(name: string): Command | undefined
  search(query: string): CommandSuggestion[]
  getByCategory(category: CommandCategory): Command[]
}

// 默认命令配置
export const DEFAULT_COMMANDS = {
  // 系统控制命令
  SYSTEM: [
    'exit', 'quit', 'clear', 'cls', 'help', 'history',
    'alias', 'unalias', 'env', 'version'
  ],

  // AI交互命令
  AI: [
    'chat', 'ask', 'plan', 'analyze', 'explain', 'generate',
    'translate', 'summarize', 'review', 'optimize'
  ],

  // 文件操作命令 (需要连接)
  FILE: [
    'ls', 'dir', 'cat', 'head', 'tail', 'grep', 'find',
    'pwd', 'cd', 'mkdir', 'rmdir', 'rm', 'cp', 'mv',
    'chmod', 'chown', 'ln', 'touch', 'which', 'whereis'
  ],

  // 项目管理命令
  PROJECT: [
    'project', 'task', 'status', 'build', 'test', 'deploy',
    'config', 'log', 'git', 'npm', 'docker'
  ],

  // 连接命令
  CONNECTION: [
    'connect', 'disconnect', 'ping', 'ssh', 'servers', 'session'
  ]
} as const

// 特殊命令前缀
export const COMMAND_PREFIX = '/' as const

// 命令解析配置
export interface CommandParserConfig {
  prefix: string               // 命令前缀
  caseSensitive: boolean       // 大小写敏感
  enableAliases: boolean       // 启用别名
  enableFuzzyMatch: boolean    // 启用模糊匹配
  maxSuggestions: number       // 最大建议数量
  suggestionThreshold: number  // 建议阈值(0-1)
}

// 默认解析器配置
export const DEFAULT_PARSER_CONFIG: CommandParserConfig = {
  prefix: COMMAND_PREFIX,
  caseSensitive: false,
  enableAliases: true,
  enableFuzzyMatch: true,
  maxSuggestions: 5,
  suggestionThreshold: 0.3
}