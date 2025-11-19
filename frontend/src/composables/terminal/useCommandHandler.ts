import type {ParsedCommand, ParseError} from "@/stores/terminalStore";
import {Terminal} from "@xterm/xterm";
import { useTerminalStore } from '@/stores/terminalStore'
import {nextTick, Ref, ref} from "vue";
import { terminalApi, type TerminalCommandRequest } from '@/api/terminal';

export interface useCommandHandlerOptions {
    terminal?: Ref<Terminal | undefined, Terminal | undefined>,
    isReady: Ref<boolean>,
    currentCommandLine: Ref<string>
}


export function useCommandHandler(options: useCommandHandlerOptions = {isReady: ref(false), terminal: undefined, currentCommandLine: ref("")}) {
    const terminalStore = useTerminalStore()

    const commandPrompt = "[root@real-agent-terminal]# "
    const {
        terminal,
        isReady,
        currentCommandLine
    
    } = options
    
    // 写入方法
    const write = (data: string) => {
        if (terminal && isReady) {
            terminal.value?.write(data)
            terminal.value?.scrollToBottom()
        }
    }

    const writeln = (data: string) => {
        if (terminal && isReady) {
            terminal.value?.writeln(data)
            terminal.value?.scrollToBottom()
        }
    }
    
    // 命令执行
    async function handleCommandExecute(parsed: ParsedCommand) {
        try {
            writeln(`\r\n${commandPrompt}${parsed.original}`)

            // 本地命令（不需要调用后端）
            switch (parsed.command.toLowerCase()) {
                case 'clear':
                case 'cls':
                    terminal?.value?.clear()
                    showPrompt()
                    return

                case 'help':
                    showHelp(parsed.arguments[0])
                    showPrompt()
                    return
            }

            // 调用后端 API 执行命令
            writeln('⚙️ 命令处理中...')
            
            const request: TerminalCommandRequest = {
                rawCommand: parsed.original,
                commandName: parsed.command,
                arguments: parsed.arguments,
                options: parsed.options,
                sessionId: 'default-session', // TODO: 从配置或上下文获取
            }

            const response = await terminalApi.executeCommand(request)
            
            if (response.data.success) {
                // 成功执行
                writeln('\r\n' + response.data.output)
                
                // 显示执行时间（如果有）
                if (response.data.metadata?.executionTime) {
                    writeln(`\r\n✅ 完成 (耗时: ${response.data.metadata.executionTime}ms)`)
                } else {
                    writeln('\r\n✅ 完成')
                }
            } else {
                // 执行失败
                writeln(`\r\n❌ 错误: ${response.data.error || '命令执行失败'}`)
                if (response.data.suggestion) {
                    writeln(`💡 ${response.data.suggestion}`)
                }
            }
            
            showPrompt()
        } catch (error) {
            writeln(`\r\n❌ 错误: ${error instanceof Error ? error.message : '未知错误'}`)
            showPrompt()
        }
    }

    // 显示帮助信息
    function showHelp(commandName?: string) {
        if (commandName) {
            // 显示特定命令的详细帮助
            const command = terminalStore.getCommand(commandName)
            if (!command) {
                writeln(`\r\n❌ 未知命令: ${commandName}`)
                return
            }

            writeln('\r\n╔═══════════════════════════════════════════════════════════╗')
            writeln(`║  命令: /${command.name}`)

            if (command.aliases && command.aliases.length > 0) {
                writeln(`║  别名: ${command.aliases.join(', ')}`)
            }

            writeln(`║  描述: ${command.description}`)
            writeln(`║  用法: ${command.usage}`)

            if (command.parameters && command.parameters.length > 0) {
                writeln('║')
                writeln('║  参数:')
                command.parameters.forEach(param => {
                    const required = param.required ? '[必需]' : '[可选]'
                    const flags = []
                    if (param.shortFlag) flags.push(param.shortFlag)
                    if (param.longFlag) flags.push(param.longFlag)
                    const flagStr = flags.length > 0 ? ` (${flags.join(', ')})` : ''

                    writeln(`║    ${param.name}${flagStr} ${required}`)
                    writeln(`║      ${param.description}`)
                    if (param.defaultValue !== undefined) {
                        writeln(`║      默认值: ${param.defaultValue}`)
                    }
                })
            }

            if (command.examples && command.examples.length > 0) {
                writeln('║')
                writeln('║  示例:')
                command.examples.forEach(ex => {
                    writeln(`║    ${ex}`)
                })
            }

            writeln('╚═══════════════════════════════════════════════════════════╝')
        } else {
            // 显示所有命令列表
            writeln('\r\n╔═══════════════════════════════════════════════════════════╗')
            writeln('║                    可用命令列表                           ║')
            writeln('╚═══════════════════════════════════════════════════════════╝')
            writeln('')

            const categories = {
                system: '🔧 系统控制',
                ai: '🤖 AI交互',
                file: '📁 文件操作',
                connection: '🔌 连接管理'
            }

            const commandsByCategory = terminalStore.commandsByCategory

            Object.entries(categories).forEach(([cat, label]) => {
                const catCommands = commandsByCategory[cat as keyof typeof commandsByCategory] || []

                if (catCommands.length > 0) {
                    writeln(`${label}:`)
                    catCommands.forEach(cmd => {
                        const aliases = cmd.aliases && cmd.aliases.length > 0
                            ? ` (${cmd.aliases.join(', ')})`
                            : ''
                        writeln(`  /${cmd.name}${aliases}`)
                        writeln(`    ${cmd.description}`)
                    })
                    writeln('')
                }
            })

            writeln('💡 使用 /help <命令名> 查看详细帮助')
            writeln('💡 使用 Tab 键自动补全命令')
            writeln('💡 使用 ↑↓ 键浏览历史命令')
        }
    }

    // 命令错误
    function handleCommandError(error: ParseError) {
        writeln(`\r\n❌ ${error.message}`)
        if (error.suggestion) {
            writeln(`💡 ${error.suggestion}`)
        }
        showPrompt()
    }

    // 显示提示符
    function showPrompt() {
        nextTick(() => {
            write(`\r\n${commandPrompt}`)
            currentCommandLine.value = ''
        })
    }

    return {
        write: write,
        writeln: writeln,
        handleCommandExecute: handleCommandExecute,
        handleCommandError: handleCommandError,
        showPrompt: showPrompt

    }
}
