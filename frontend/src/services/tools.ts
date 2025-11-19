import http from './http'

// 通用返回包装与类型
export interface ResponseResult<T=any>{
  code: number
  message: string
  data: T
    timestamp: number
}

function isResponseResult(resp: any): resp is ResponseResult<any> {
  return resp && typeof resp === 'object' && 'code' in resp && 'message' in resp && 'timestamp' in resp
}

// 工具相关类型（仅前端使用）
export interface McpToolSpec { server: string; client: string }
export interface ToolSpec {
  name: string
  description?: string
  category?: string
  inputSchema?: any | null
  inputSchemaRaw?: string | null
  mcpToolSpec?: McpToolSpec | null
}
export interface ToolListItem { id: string; spec: ToolSpec }

function safeParseJson(str: any): any | null {
  if (typeof str !== 'string') return null
  try { return JSON.parse(str) } catch { return null }
}

export async function listTools(): Promise<ToolListItem[]>{
  try{
    const raw = await http.get('/tools')
    // 优先按统一包装解包
    const payload = isResponseResult(raw) ? raw.data : (Array.isArray(raw) ? raw : (raw?.items || []))
    if (!Array.isArray(payload)) return []
    // 解析 inputSchema 字符串
    return payload.map((it: any) => {
      const spec = it?.spec || {}
      const parsed = safeParseJson(spec.inputSchema)
      return {
        id: String(it?.id ?? spec?.name ?? ''),
        spec: {
          name: spec?.name,
          description: spec?.description,
          category: spec?.category,
          inputSchema: parsed,
          inputSchemaRaw: spec?.inputSchema ?? null,
          mcpToolSpec: spec?.mcpToolSpec ?? null
        }
      } as ToolListItem
    })
  }catch{
    // 后端未就绪时返回占位数据（与旧实现保持兼容，但补充 id/spec 结构）
    return [
      { id: 'http.request', spec: { name: 'http.request', description: 'HTTP 请求工具', category: 'network', inputSchema: null, inputSchemaRaw: null, mcpToolSpec: null }},
      { id: 'math.eval', spec: { name: 'math.eval', description: '数学表达式求值', category: 'utility', inputSchema: null, inputSchemaRaw: null, mcpToolSpec: null }},
      { id: 'time.now', spec: { name: 'time.now', description: '获取当前时间', category: 'utility', inputSchema: null, inputSchemaRaw: null, mcpToolSpec: null }}
    ]
  }
}

// 兼容两种调用：executeTool(toolName, args) 或 executeTool({toolName, args})
export async function executeTool(toolNameOrPayload: any, maybeArgs?: any){
  const payload = typeof toolNameOrPayload === 'string'
    ? { toolName: toolNameOrPayload, args: maybeArgs ?? {} }
    : (toolNameOrPayload?.toolName ? { toolName: toolNameOrPayload.toolName, args: toolNameOrPayload.args ?? {} } : toolNameOrPayload)

  const raw = await http.post('/tools/execute', payload)
  // 若后端执行接口也采用统一包装，则直接返回原文，或按需解包
  // 保持对调用方的向后兼容：默认返回 raw
  return raw
}

// 审批通过后直接执行（与 executeTool 一致，提供语义化别名）
export async function approveExecute(toolName: string, args: any){
  return executeTool(toolName, args)
}
