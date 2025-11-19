import { ref, computed, watch, Ref } from 'vue'
import { useTerminalStore, type SimpleCommand, type ParsedCommand, type ParseError } from '@/stores/terminalStore'
import { useCommandHandler } from "@/composables/terminal/useCommandHandler";
import { Terminal } from "@xterm/xterm";

export interface UseCommandInputOptions {
    historySize?: number
    commandPrompt?: string,
    terminal?: Ref<Terminal | undefined>,
    isReady: Ref<boolean>,
}

export function useCommandInput(options: UseCommandInputOptions = { isReady: ref(false), terminal: undefined }) {
    // 配置
    const {
        historySize = 100,
        commandPrompt = '[root@real-agent-terminal]# ',
        terminal,
        isReady,
    } = options

    // 使用 Pinia store
    const terminalStore = useTerminalStore()

    // 响应式状态
    const currentInput = ref('')
    const currentCommandLine = ref('') // 终端当前命令行
    const cursorPosition = ref(0) // 光标位置（0 表示行首，值越大越靠右）
    const suggestions = ref<SimpleCommand[]>([])
    const selectedSuggestionIndex = ref(-1)
    const commandHistory = ref<string[]>([])
    const historyIndex = ref(-1) // -1 表示当前输入，0+ 表示历史命令索引（0是最新）
    const temporarySavedInput = ref('') // 浏览历史前保存的临时输入
    const showSuggestions = ref(false)
    const isLoading = ref(false)

    // 输入法组合态状态
    const isComposing = ref(false) // 是否处于输入法组合态
    const compositionValue = ref('') // 组合态中的临时输入
    const compositionStartCursor = ref(0) // 组合态开始时的光标位置
    const compositionStartText = ref('') // 组合态开始时的文本内容
    const isCompositionEndHandled = ref(false) // 标记组合态结束时是否已处理输入

    // 解析结果
    const parseResult = ref<{
        command?: ParsedCommand
        error?: ParseError
    }>({})

    const {
        write: write,
        writeln: writeln,
        handleCommandExecute: onExecute,
        handleCommandError: onError,
        showPrompt
    } = useCommandHandler({
        terminal: terminal,
        isReady: isReady,
        currentCommandLine: currentCommandLine
    });

    // 计算属性：当前是否有有效输入
    const hasInput = computed(() => currentInput.value.trim().length > 0)

    // 计算属性：当前是否有建议
    const hasSuggestions = computed(() => suggestions.value.length > 0)

    // 计算属性：当前选中的建议
    const selectedSuggestion = computed(() => {
        if (selectedSuggestionIndex.value >= 0 && selectedSuggestionIndex.value < suggestions.value.length) {
            return suggestions.value[selectedSuggestionIndex.value]
        }
        return null
    })

    // 工具函数：计算字符串的显示宽度（中文字符占2个宽度）
    const getStringWidth = (str: string): number => {
        let width = 0
        for (let i = 0; i < str.length; i++) {
            const code = str.charCodeAt(i)
            // 中文字符范围：0x4E00-0x9FFF（基本汉字）
            // 全角字符范围：0xFF00-0xFFEF
            // 其他宽字符：0x3000-0x303F（CJK符号和标点）
            if ((code >= 0x4E00 && code <= 0x9FFF) ||
                (code >= 0xFF00 && code <= 0xFFEF) ||
                (code >= 0x3000 && code <= 0x303F)) {
                width += 2
            } else {
                width += 1
            }
        }
        return width
    }

    // 工具函数：判断字符是否为中文或全角字符
    const isWideChar = (char: string): boolean => {
        if (char.length === 0) return false
        const code = char.charCodeAt(0)
        return (code >= 0x4E00 && code <= 0x9FFF) ||
            (code >= 0xFF00 && code <= 0xFFEF) ||
            (code >= 0x3000 && code <= 0x303F)
    }

    // 更新命令建议
    const updateSuggestions = (input: string) => {
        if (!input || !input.startsWith('/')) {
            suggestions.value = []
            showSuggestions.value = false
            selectedSuggestionIndex.value = -1
            return
        }

        // 移除前缀后进行匹配
        const query = input.slice(1).trim()

        if (query.length === 0) {
            // 显示所有命令
            suggestions.value = terminalStore.getAllCommands()
        } else {
            // 获取匹配的命令建议
            suggestions.value = terminalStore.getCommandSuggestions(query)
        }

        showSuggestions.value = suggestions.value.length > 0
        selectedSuggestionIndex.value = suggestions.value.length > 0 ? 0 : -1
    }

    // 处理输入变化
    const handleInput = (input: string) => {
        currentInput.value = input
        historyIndex.value = -1 // 重置历史索引

        // 更新建议
        updateSuggestions(input)

        // 如果输入完整命令（不只是前缀），尝试解析（但不显示错误）
        const trimmed = input.trim()
        if (trimmed.length > 1 && trimmed.startsWith('/')) {
            const result = terminalStore.parseCommand(input)
            parseResult.value = result
            // 注意：这里只保存解析结果，不显示错误
            // 错误信息只在用户按回车执行命令时才显示
        } else {
            parseResult.value = {}
        }
    }

    // Tab补全：完成当前选中的建议
    const handleTabComplete = (): boolean => {
        if (!hasSuggestions.value || selectedSuggestionIndex.value < 0) {
            return false
        }

        const suggestion = suggestions.value[selectedSuggestionIndex.value]

        // 构建完整命令
        let completedCommand = `/${suggestion.name}`

        // 如果命令有必需参数，添加占位符
        const requiredParams = suggestion.parameters?.filter(p => p.required) || []
        if (requiredParams.length > 0) {
            completedCommand += ' '
            // 添加第一个必需参数的占位符
            const firstParam = requiredParams[0]
            completedCommand += `<${firstParam.name}>`
        }

        currentInput.value = completedCommand
        currentCommandLine.value = completedCommand
        cursorPosition.value = completedCommand.length // 光标移到行尾
        showSuggestions.value = false
        selectedSuggestionIndex.value = -1
        updateDisplay()

        return true
    }

    // 向上选择建议
    const selectPreviousSuggestion = (): boolean => {
        if (!hasSuggestions.value) return false

        if (selectedSuggestionIndex.value > 0) {
            selectedSuggestionIndex.value--
        } else {
            selectedSuggestionIndex.value = suggestions.value.length - 1
        }
        return true
    }

    // 向下选择建议
    const selectNextSuggestion = (): boolean => {
        if (!hasSuggestions.value) return false

        if (selectedSuggestionIndex.value < suggestions.value.length - 1) {
            selectedSuggestionIndex.value++
        } else {
            selectedSuggestionIndex.value = 0
        }
        return true
    }

    // 选择指定索引的建议
    const selectSuggestion = (index: number) => {
        if (index >= 0 && index < suggestions.value.length) {
            selectedSuggestionIndex.value = index
            handleTabComplete()
        }
    }

    // 从历史记录向上翻（显示更老的命令）
    const selectPreviousHistory = (): boolean => {
        if (commandHistory.value.length === 0) return false

        // 第一次进入历史浏览模式：保存当前输入
        if (historyIndex.value === -1) {
            temporarySavedInput.value = currentCommandLine.value
            historyIndex.value = 0
        } else if (historyIndex.value < commandHistory.value.length - 1) {
            // 继续向上浏览更老的历史
            historyIndex.value++
        } else {
            // 已经到达最老的历史记录，不再移动
            return false
        }

        // ⚠️ 只更新 currentCommandLine，不更新 currentInput（避免触发 watch 导致 historyIndex 重置）
        const historyCommand = commandHistory.value[historyIndex.value]
        currentCommandLine.value = historyCommand
        cursorPosition.value = historyCommand.length // 光标移到行尾
        updateSuggestions(historyCommand)
        updateDisplay()
        return true
    }

    // 从历史记录向下翻（显示更新的命令）
    const selectNextHistory = (): boolean => {
        if (historyIndex.value === -1) {
            // 已经在当前输入状态，无法继续向下
            return false
        }

        if (historyIndex.value > 0) {
            // 返回更新的历史命令
            historyIndex.value--
            const historyCommand = commandHistory.value[historyIndex.value]
            // ⚠️ 只更新 currentCommandLine，不更新 currentInput
            currentCommandLine.value = historyCommand
            cursorPosition.value = historyCommand.length
            updateSuggestions(historyCommand)
            updateDisplay()
            return true
        } else {
            // 返回到原始输入状态
            historyIndex.value = -1
            const savedInput = temporarySavedInput.value
            // ⚠️ 只更新 currentCommandLine，不更新 currentInput
            currentCommandLine.value = savedInput
            cursorPosition.value = savedInput.length
            temporarySavedInput.value = '' // 清空临时保存
            updateSuggestions(savedInput)
            updateDisplay()
            return true
        }
    }

    // 执行命令
    const executeCommand = async () => {
        const input = currentInput.value.trim()
        if (!input) return

        // 重置历史浏览状态
        historyIndex.value = -1
        temporarySavedInput.value = ''

        // 添加到历史记录（去重：只有当最新的历史命令与当前输入不同时才添加）
        if (commandHistory.value.length === 0 || commandHistory.value[0] !== input) {
            commandHistory.value.unshift(input) // 新命令放在索引0
            if (commandHistory.value.length > historySize) {
                commandHistory.value.pop() // 移除最老的命令
            }
        }

        // 解析命令
        const result = terminalStore.parseCommand(input)

        if (result.error) {
            if (onError) {
                onError(result.error)
            }
            return
        }

        if (result.command && onExecute) {
            try {
                await onExecute(result.command)
            } catch (error) {
                console.error('Command execution error:', error)
                if (onError) {
                    onError({
                        message: error instanceof Error ? error.message : '命令执行失败'
                    })
                }
            }
        }

        // 清空输入
        currentInput.value = ''
        currentCommandLine.value = ''
        cursorPosition.value = 0
        suggestions.value = []
        showSuggestions.value = false
        selectedSuggestionIndex.value = -1
    }

    // 清空输入
    const clearInput = () => {
        currentInput.value = ''
        currentCommandLine.value = ''
        cursorPosition.value = 0
        suggestions.value = []
        showSuggestions.value = false
        selectedSuggestionIndex.value = -1
        parseResult.value = {}
        historyIndex.value = -1
        temporarySavedInput.value = ''
        updateDisplay()
    }

    // 获取命令帮助信息
    const getCommandHelp = (commandName: string): SimpleCommand | undefined => {
        return terminalStore.getCommand(commandName)
    }

    // 获取所有命令
    const getAllCommands = (): SimpleCommand[] => {
        return terminalStore.getAllCommands()
    }

    // 更新终端显示
    const updateDisplay = () => {
        if (!write) return

        // 清除当前行
        write('\r\x1b[K')

        // 重新写入命令行
        write(commandPrompt + currentCommandLine.value)

        // 计算光标位置（基于字符宽度）
        const prefix = currentCommandLine.value.slice(0, cursorPosition.value)
        const prefixWidth = getStringWidth(prefix)
        const totalWidth = getStringWidth(currentCommandLine.value)
        const offset = totalWidth - prefixWidth

        // 移动光标到正确位置
        if (offset > 0) {
            write(`\x1b[${offset}D`) // 向左移动光标
        }
    }

    // 光标移动到行首
    const moveCursorToStart = () => {
        cursorPosition.value = 0
        updateDisplay()
    }

    // 光标移动到行尾
    const moveCursorToEnd = () => {
        cursorPosition.value = currentCommandLine.value.length
        updateDisplay()
    }

    // 光标向左移动（适配宽字符）
    const moveCursorLeft = () => {
        if (cursorPosition.value > 0) {
            cursorPosition.value--
            updateDisplay()
        }
    }

    // 光标向右移动（适配宽字符）
    const moveCursorRight = () => {
        if (cursorPosition.value < currentCommandLine.value.length) {
            cursorPosition.value++
            updateDisplay()
        }
    }

    // 处理终端原始数据输入
    const handleTerminalData = (data: string) => {
        // 组合态结束后，忽略已处理的字符数据
        if (isCompositionEndHandled.value) {
            isCompositionEndHandled.value = false
            return
        }

        // 如果是组合态且不是确认输入（如回车），则忽略输入
        if (isComposing.value) {
            // 在组合态下，只处理回车和换行
            if (data === '\r' || data === '\n') {
                // 结束组合态
                isComposing.value = false
                compositionValue.value = ''
            } else {
                // 忽略其他输入，由compositionupdate事件处理
                return
            }
        }

        const char = data

        // 处理 Shift + Enter（换行）
        if (char === '\x1b[13;2u' || char === '\x1b[13;2~' || char === '\n') {
            // 在当前位置插入换行符
            const beforeCursor = currentCommandLine.value.slice(0, cursorPosition.value)
            const afterCursor = currentCommandLine.value.slice(cursorPosition.value)
            currentCommandLine.value = beforeCursor + '\n' + afterCursor
            cursorPosition.value = beforeCursor.length + 1
            updateDisplay()
            return
        }

        // 处理普通回车（执行命令）
        if (char === '\r') {
            // 如果当前行以反斜杠结尾，则视为换行
            if (currentCommandLine.value.endsWith('\\')) {
                // 移除反斜杠并添加换行
                currentCommandLine.value = currentCommandLine.value.slice(0, -1) + '\n'
                cursorPosition.value = currentCommandLine.value.length
                updateDisplay()
                return
            }
            
            // 检查是否有未闭合的引号或括号
            const line = currentCommandLine.value.trim()
            if (line) {
                const openQuotes = (line.match(/"/g) || []).length % 2 !== 0
                const openSingleQuotes = (line.match(/'/g) || []).length % 2 !== 0
                const openParens = (line.match(/\(/g) || []).length > (line.match(/\)/g) || []).length
                const openBrackets = (line.match(/\[/g) || []).length > (line.match(/\]/g) || []).length
                const openBraces = (line.match(/\{/g) || []).length > (line.match(/\}/g) || []).length
                
                if (openQuotes || openSingleQuotes || openParens || openBrackets || openBraces) {
                    // 如果存在未闭合的符号，添加换行
                    currentCommandLine.value += '\n'
                    cursorPosition.value = currentCommandLine.value.length
                    updateDisplay()
                    return
                }
            }
            
            // 如果当前行为空，显示新的提示符
            if (!currentCommandLine.value.trim()) {
                if (showPrompt) showPrompt()
                return
            }
            
            // 执行命令
            handleInput(currentCommandLine.value)
            executeCommand()
            return
        }

        // 退格（Backspace）
        if (char === '\u007F' || char === '\b') {
            if (cursorPosition.value > 0) {
                // 在光标位置删除字符
                currentCommandLine.value =
                    currentCommandLine.value.slice(0, cursorPosition.value - 1) +
                    currentCommandLine.value.slice(cursorPosition.value)
                cursorPosition.value--
                handleInput(currentCommandLine.value)
                updateDisplay()
            }
            return
        }

        // Delete 键
        if (char === '\u001b[3~') {
            if (cursorPosition.value < currentCommandLine.value.length) {
                // 删除光标后的字符
                currentCommandLine.value =
                    currentCommandLine.value.slice(0, cursorPosition.value) +
                    currentCommandLine.value.slice(cursorPosition.value + 1)
                handleInput(currentCommandLine.value)
                updateDisplay()
            }
            return
        }

        // Home 键 - 移动到行首
        if (char === '\u001b[H' || char === '\u001b[1~') {
            moveCursorToStart()
            return
        }

        // End 键 - 移动到行尾
        if (char === '\u001b[F' || char === '\u001b[4~') {
            moveCursorToEnd()
            return
        }

        // 左箭头 - 向左移动光标
        if (char === '\u001b[D') {
            moveCursorLeft()
            return
        }

        // 右箭头 - 向右移动光标
        if (char === '\u001b[C') {
            moveCursorRight()
            return
        }

        // Tab - 补全
        if (char === '\t') {
            if (showSuggestions.value) {
                handleTabComplete()
            } else {
                // 如果没有建议，显示所有命令建议
                updateSuggestions(currentCommandLine.value)
            }
            return
        }

        // 向上箭头 - 历史记录或建议选择
        if (char === '\u001b[A') {
            if (showSuggestions.value) {
                selectPreviousSuggestion()
            } else {
                selectPreviousHistory()
            }
            return
        }

        // 向下箭头 - 历史记录或建议选择
        if (char === '\u001b[B') {
            if (showSuggestions.value) {
                selectNextSuggestion()
            } else {
                selectNextHistory()
            }
            return
        }

        // 普通字符 - 在光标位置插入
        // 修改判断条件以支持中文字符和其他Unicode字符
        if (char.length > 0 && !char.startsWith('\u001b')) {
            const charCode = char.charCodeAt(0)
            // 允许可打印ASCII字符（32-126）和所有Unicode字符（>= 128）
            // 这样可以支持中文、日文、韩文等多字节字符
            if ((charCode >= 32 && charCode <= 126) || charCode >= 128) {
                // 在光标位置插入字符
                currentCommandLine.value =
                    currentCommandLine.value.slice(0, cursorPosition.value) +
                    char +
                    currentCommandLine.value.slice(cursorPosition.value)
                cursorPosition.value++
                handleInput(currentCommandLine.value)
                updateDisplay()
            }
        }
    }

    // 监听终端元素变化以添加输入法事件监听
    watch(() => terminal?.value?.element, (element, oldElement, onCleanup) => {
        if (!element) return

        // 添加输入法组合事件监听
        const handleCompositionStart = () => {
            isComposing.value = true
            compositionValue.value = ''
            // 记录当前光标位置和文本（组合态开始时的基准）
            compositionStartCursor.value = cursorPosition.value
            compositionStartText.value = currentCommandLine.value
        }


        const handleCompositionEnd = (e: CompositionEvent) => {
            if (!isComposing.value) return

            isComposing.value = false

            if (e.data) {
                // 基于开始时的上下文计算最终文本
                const before = compositionStartText.value.slice(0, compositionStartCursor.value)
                const after = compositionStartText.value.slice(compositionStartCursor.value)

                // 更新命令行为最终确认的文本
                currentCommandLine.value = before + e.data + after
                cursorPosition.value = compositionStartCursor.value + e.data.length

                // 处理输入并更新显示
                handleInput(currentCommandLine.value)
                updateDisplay()

                // 标记已处理，避免handleTerminalData重复处理
                isCompositionEndHandled.value = true
            }

            // 重置组合状态
            compositionValue.value = ''
            compositionStartText.value = ''
            compositionStartCursor.value = 0
        }

        element.addEventListener('compositionstart', handleCompositionStart)
        element.addEventListener('compositionend', handleCompositionEnd as EventListener)

        // 清理函数
        onCleanup(() => {
            element.removeEventListener('compositionstart', handleCompositionStart)
            element.removeEventListener('compositionend', handleCompositionEnd as EventListener)
        })
    }, { immediate: true })

    // 监听输入变化
    watch(currentInput, (newValue) => {
        handleInput(newValue)
    })

    // 粘贴文本（一次性插入，光标跳到末尾）
    const pasteText = (text: string) => {
        if (!text) return

        // 在当前光标位置插入文本
        const before = currentCommandLine.value.slice(0, cursorPosition.value)
        const after = currentCommandLine.value.slice(cursorPosition.value)
        const newCommand = before + text + after

        // 更新命令行
        currentCommandLine.value = newCommand

        // 光标移动到插入文本的末尾
        cursorPosition.value = before.length + text.length

        // 更新输入状态和建议
        handleInput(newCommand)

        // 更新显示
        updateDisplay()
    }

    return {
        // 状态
        currentInput,
        currentCommandLine,
        cursorPosition,  // ⚠️ 暴露光标位置
        suggestions,
        selectedSuggestionIndex,
        commandHistory,
        showSuggestions,
        isLoading,
        parseResult,

        // 计算属性
        hasInput,
        hasSuggestions,
        selectedSuggestion,

        // 方法
        handleInput,
        handleTabComplete,
        pasteText,  // ⚠️ 暴露粘贴方法
        selectPreviousSuggestion,
        selectNextSuggestion,
        selectSuggestion,
        selectPreviousHistory,
        selectNextHistory,
        executeCommand,
        clearInput,
        getCommandHelp,
        getAllCommands,
        handleTerminalData,
        updateDisplay
    }
}