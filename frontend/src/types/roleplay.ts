export interface RoleDetail {
  id: number
  name: string
  avatarUrl?: string | null
  description?: string | null
  voice?: string | null
  traitsJson?: Record<string, unknown>
  scriptsJson?: Record<string, unknown>
  status: number
  createdAt?: string
  updatedAt?: string
  new?: boolean
}

export interface SessionDetail {
  id: number
  sessionCode: string
  userId: number
  roleId: number
  mode: 'text' | 'voice'
  status: number
  summary?: string | null
  metadata?: Record<string, unknown>
  createdAt?: string
  endedAt?: string | null
}

export interface SessionMessage {
  id: number
  sessionId: number
  messageType: 'USER_TEXT' | 'ASSISTANT_TEXT' | 'ASSISTANT_AUDIO' | 'EVENT' | 'TOOL_CALL' | 'TOOL_RESULT'
  role: 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL'
  content?: string | null
  payload?: Record<string, unknown>
  assetUri?: string | null
  createdAt: string
}
