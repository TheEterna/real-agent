// Clear命令实现
import type { CommandHandler } from '@/types/agent/modes/commands'

export default {
  name: 'clear',
  description: '清空终端屏幕',
  usage: '/clear',
  examples: ['/clear'],
  async execute(args: string, context?: any): Promise<string> {
    // 返回特殊标记，告诉UI清空消息列表
    return '__CLEAR_TERMINAL__'
  }
} as CommandHandler