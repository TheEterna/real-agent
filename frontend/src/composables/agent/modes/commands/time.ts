// Time命令实现
import type { CommandHandler } from '@/types/agent/modes/commands'

export default {
  name: 'time',
  description: '显示当前时间',
  usage: '/time',
  examples: ['/time'],
  async execute(args: string, context?: any): Promise<string> {
    const now = new Date()
    const startTime = now.getTime()
    const iso = now.toISOString()
    const local = now.toLocaleString('zh-CN', {
      timeZone: 'Asia/Shanghai',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      weekday: 'long'
    })

    return `⏰ 当前时间信息:

📅 本地时间: ${local}
🌍 UTC时间: ${iso}
⏱️  时间戳: ${startTime}
🕐 Unix时间: ${Math.floor(startTime / 1000)}

时区: 中国标准时间 (CST, UTC+8)`
  }
} as CommandHandler