// Help命令实现
import type { CommandHandler } from '@/types/agent/modes/commands'

export default {
  name: 'help',
  description: '显示帮助信息',
  usage: '/help [命令名]',
  examples: [
    '/help',
    '/help clear',
    '/help explain'
  ],
  async execute(args: string, context?: any): Promise<string> {
    const { getCommandHelp } = await import('../useCommandParser')
    const parser = getCommandHelp()

    if (args.trim()) {
      return parser(args.trim())
    }

    return `🎯 Real Agent 极客模式帮助

${parser()}

💡 提示:
- 输入普通文本进行AI对话
- 输入 /命令 使用终端命令
- 支持命令历史记录 (↑↓ 键)
- Ctrl+C 中断当前操作

🚀 极客模式特性:
- 终端风格界面
- 命令行式交互
- 实时流式响应
- 黑客矩阵效果

享受极客体验! 🤖`
  }
} as CommandHandler