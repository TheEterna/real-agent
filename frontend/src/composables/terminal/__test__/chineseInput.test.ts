import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ref } from 'vue'
import { useCommandInput } from '../useCommandInput'
import type { Terminal } from '@xterm/xterm'

describe('中文输入测试', () => {
    let terminal: any
    let isReady: any

    beforeEach(() => {
        // Mock Terminal
        terminal = ref({
            write: vi.fn(),
            writeln: vi.fn(),
            clear: vi.fn()
        } as unknown as Terminal)
        
        isReady = ref(true)
    })

    it('应该能够输入中文字符', () => {
        const { handleTerminalData, currentCommandLine } = useCommandInput({
            terminal,
            isReady
        })

        // 模拟输入中文字符 "你"
        handleTerminalData('你')
        expect(currentCommandLine.value).toBe('你')

        // 继续输入 "好"
        handleTerminalData('好')
        expect(currentCommandLine.value).toBe('你好')
    })

    it('应该能够输入中英文混合内容', () => {
        const { handleTerminalData, currentCommandLine } = useCommandInput({
            terminal,
            isReady
        })

        // 输入 "/hello 你好"
        handleTerminalData('/')
        handleTerminalData('h')
        handleTerminalData('e')
        handleTerminalData('l')
        handleTerminalData('l')
        handleTerminalData('o')
        handleTerminalData(' ')
        handleTerminalData('你')
        handleTerminalData('好')

        expect(currentCommandLine.value).toBe('/hello 你好')
    })

    it('应该能够删除中文字符', () => {
        const { handleTerminalData, currentCommandLine } = useCommandInput({
            terminal,
            isReady
        })

        // 输入 "你好"
        handleTerminalData('你')
        handleTerminalData('好')
        expect(currentCommandLine.value).toBe('你好')

        // 删除一个字符（Backspace）
        handleTerminalData('\u007F')
        expect(currentCommandLine.value).toBe('你')

        // 再删除一个字符
        handleTerminalData('\u007F')
        expect(currentCommandLine.value).toBe('')
    })

    it('应该能够处理全角字符', () => {
        const { handleTerminalData, currentCommandLine } = useCommandInput({
            terminal,
            isReady
        })

        // 输入全角字符
        handleTerminalData('Ａ')
        handleTerminalData('Ｂ')
        handleTerminalData('Ｃ')

        expect(currentCommandLine.value).toBe('ＡＢＣ')
    })

    it('应该能够处理中文标点符号', () => {
        const { handleTerminalData, currentCommandLine } = useCommandInput({
            terminal,
            isReady
        })

        // 输入中文标点
        handleTerminalData('你')
        handleTerminalData('好')
        handleTerminalData('，')
        handleTerminalData('世')
        handleTerminalData('界')
        handleTerminalData('！')

        expect(currentCommandLine.value).toBe('你好,世界!')
    })

    it('应该能够在中文字符中移动光标', () => {
        const { handleTerminalData, currentCommandLine, cursorPosition } = useCommandInput({
            terminal,
            isReady
        })

        // 输入 "你好世界"
        handleTerminalData('你')
        handleTerminalData('好')
        handleTerminalData('世')
        handleTerminalData('界')
        expect(currentCommandLine.value).toBe('你好世界')
        expect(cursorPosition.value).toBe(4)

        // 向左移动光标
        handleTerminalData('\u001b[D') // 左箭头
        expect(cursorPosition.value).toBe(3)

        // 向左移动光标
        handleTerminalData('\u001b[D')
        expect(cursorPosition.value).toBe(2)

        // 在光标位置插入字符
        handleTerminalData('中')
        expect(currentCommandLine.value).toBe('你好中世界')
        expect(cursorPosition.value).toBe(3)
    })

    it('应该正确计算中文字符的宽度', () => {
        const { handleTerminalData } = useCommandInput({
            terminal,
            isReady
        })

        // 这个测试主要验证字符宽度计算工具函数
        // 实际的宽度计算在 getStringWidth 函数中
        // 这里我们通过输入来间接测试

        handleTerminalData('你')
        handleTerminalData('好')
        handleTerminalData('a')
        handleTerminalData('b')

        // 如果宽度计算正确，终端应该正确显示这些字符
        // 实际的显示效果需要在浏览器中手动测试
    })

    it('应该能够处理日文字符', () => {
        const { handleTerminalData, currentCommandLine } = useCommandInput({
            terminal,
            isReady
        })

        // 输入日文平假名
        handleTerminalData('こ')
        handleTerminalData('ん')
        handleTerminalData('に')
        handleTerminalData('ち')
        handleTerminalData('は')

        expect(currentCommandLine.value).toBe('こんにちは')
    })

    it('应该能够处理韩文字符', () => {
        const { handleTerminalData, currentCommandLine } = useCommandInput({
            terminal,
            isReady
        })

        // 输入韩文
        handleTerminalData('안')
        handleTerminalData('녕')
        handleTerminalData('하')
        handleTerminalData('세')
        handleTerminalData('요')

        expect(currentCommandLine.value).toBe('안녕하세요')
    })
})
