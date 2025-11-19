/**
 * 命令解析测试
 * 用于验证新的命令解析逻辑
 */

import { useTerminalStore } from '@/stores/terminalStore'
import { createPinia, setActivePinia } from 'pinia'

// 测试用例
const testCases = [
  {
    name: '标准示例',
    input: '/plan --detailed 规划一下 我去马来西亚的旅游',
    expected: {
      command: 'plan',
      options: ['detailed'],
      arguments: ['规划一下', '我去马来西亚的旅游']
    }
  },
  {
    name: '选项在中间',
    input: '/plan 规划一下 --detailed 我去马来西亚的旅游',
    expected: {
      command: 'plan',
      options: ['detailed'],
      arguments: ['规划一下', '我去马来西亚的旅游']
    }
  },
  {
    name: '多个选项（只取第一个）',
    input: '/plan --detailed --brief 规划一下 我去马来西亚的旅游',
    expected: {
      command: 'plan',
      options: ['detailed'],
      arguments: ['规划一下', '我去马来西亚的旅游']
    }
  },
  {
    name: '没有选项',
    input: '/chat 你好世界',
    expected: {
      command: 'chat',
      options: [],
      arguments: ['你好世界']
    }
  },
  {
    name: '只有命令',
    input: '/clear',
    expected: {
      command: 'clear',
      options: [],
      arguments: []
    }
  },
  {
    name: '选项在最后',
    input: '/plan 规划一下 我去马来西亚的旅游 --detailed',
    expected: {
      command: 'plan',
      options: ['detailed'],
      arguments: ['规划一下', '我去马来西亚的旅游']
    }
  }
]

// 运行测试
function runTests() {
  setActivePinia(createPinia())
  const store = useTerminalStore()

  console.log('=== 命令解析测试 ===\n')

  let passed = 0
  let failed = 0

  testCases.forEach((testCase, index) => {
    const result = store.parseCommand(testCase.input)
    
    if (result.error) {
      console.log(`❌ 测试 ${index + 1} [${testCase.name}] 失败`)
      console.log(`   输入: ${testCase.input}`)
      console.log(`   错误: ${result.error.message}`)
      failed++
      return
    }

    const cmd = result.command!
    const isCorrect = 
      cmd.command === testCase.expected.command &&
      JSON.stringify(cmd.options) === JSON.stringify(testCase.expected.options) &&
      JSON.stringify(cmd.arguments) === JSON.stringify(testCase.expected.arguments)

    if (isCorrect) {
      console.log(`✅ 测试 ${index + 1} [${testCase.name}] 通过`)
      passed++
    } else {
      console.log(`❌ 测试 ${index + 1} [${testCase.name}] 失败`)
      console.log(`   输入: ${testCase.input}`)
      console.log(`   期望:`, testCase.expected)
      console.log(`   实际:`, {
        command: cmd.command,
        options: cmd.options,
        arguments: cmd.arguments
      })
      failed++
    }
  })

  console.log(`\n=== 测试结果 ===`)
  console.log(`通过: ${passed}/${testCases.length}`)
  console.log(`失败: ${failed}/${testCases.length}`)
}

// 导出测试函数
export { runTests }

// 如果直接运行此文件，执行测试
if (import.meta.vitest) {
  const { describe, it, expect, beforeEach } = import.meta.vitest
  
  describe('命令解析测试', () => {
    beforeEach(() => {
      setActivePinia(createPinia())
    })

    testCases.forEach((testCase) => {
      it(testCase.name, () => {
        const store = useTerminalStore()
        const result = store.parseCommand(testCase.input)
        
        expect(result.error).toBeUndefined()
        expect(result.command).toBeDefined()
        expect(result.command!.command).toBe(testCase.expected.command)
        expect(result.command!.options).toEqual(testCase.expected.options)
        expect(result.command!.arguments).toEqual(testCase.expected.arguments)
      })
    })
  })
}
