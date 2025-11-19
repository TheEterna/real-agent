/**
 * Terminal API 接口封装
 */
import http from '@/services/http'
import type {Command} from '@/types/terminal/commands'

/**
 * 统一响应格式
 */
export interface ResponseResult<T = any> {
    code: number
    message: string
    data: T
    startTime: number
}

/**
 * 终端命令执行请求
 */
export interface TerminalCommandRequest {
    rawCommand: string           // 生命令，没有进行过处理
    commandName: string           // 命令名称
    agent?: string           // 使用的 agent 策略
    arguments: string[]      // 参数列表
    options: string[]        // 选项参数列表
    sessionId?: string       // 会话ID
    metadata?: Record<string, any>  // 元数据
}

/**
 * 终端命令执行结果
 */
export interface TerminalCommandResult {
    success: boolean
    output: string
    error?: string
    suggestion?: string
    metadata?: {
        executionTime?: number
        exitCode?: number
        [key: string]: any
    }
}

/**
 * Terminal API
 */
export const terminalApi = {
    /**
     * 执行命令 - 网关接口
     */
    executeCommand(request: TerminalCommandRequest): Promise<ResponseResult<TerminalCommandResult>> {
        return http.post('/terminal/gateway', request)
    },

    /**
     * 获取所有命令配置
     */
    getAllCommands(params?: { category?: string; enabled?: boolean }): Promise<ResponseResult<Command[]>> {
        return http.get('/terminal/commands', {params})
    },

    /**
     * 获取可见的命令
     */
    getVisibleCommands(): Promise<ResponseResult<Command[]>> {
        return http.get('/terminal/commands/visible')
    },

    /**
     * 获取单个命令详情
     */
    getCommand(name: string): Promise<ResponseResult<Command>> {
        return http.get(`/terminal/commands/${name}`)
    },

    /**
     * 创建新命令
     */
    createCommand(command: Command): Promise<ResponseResult<Command>> {
        return http.post('/terminal/commands', command)
    },

    /**
     * 更新命令
     */
    updateCommand(name: string, command: Command): Promise<ResponseResult<Command>> {
        return http.put(`/terminal/commands/${name}`, command)
    },

    /**
     * 删除命令
     */
    deleteCommand(name: string): Promise<ResponseResult<void>> {
        return http.delete(`/terminal/commands/${name}`)
    },

    /**
     * 启用/禁用命令
     */
    toggleCommand(name: string, enabled: boolean): Promise<ResponseResult<Command>> {
        return http.patch(`/terminal/commands/${name}/toggle`, null, {
            params: {enabled}
        })
    }
}
