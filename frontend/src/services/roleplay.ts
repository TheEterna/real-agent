import http from './http'
import type { RoleDetail, SessionDetail, SessionMessage } from '@/types/roleplay'

export interface ApiResp<T> { code: number; msg?: string; data: T }

export interface CreateSessionPayload {
  userId: number
  roleId: number
}

export interface UpdateSessionMetadataPayload {
  [key: string]: unknown
}

export interface CreateMessagePayload {
  messageType: 'USER_TEXT' | 'ASSISTANT_TEXT' | 'ASSISTANT_AUDIO' | 'EVENT' | 'TOOL_CALL' | 'TOOL_RESULT'
  role: 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL'
  content?: string
  payload?: Record<string, unknown>
  assetUri?: string
}

export function fetchRoles(activeOnly = true) {
  return http.get<RoleDetail[]>('/roles', { params: { activeOnly } })
}

export function fetchRoleById(id: number) {
  return http.get<RoleDetail>(`/roles/${id}`)
}

export function createSession(payload: CreateSessionPayload) {
  return http.post<SessionDetail>('/sessions', payload)
}

export function fetchSession(sessionCode: string) {
  return http.get<SessionDetail>(`/sessions/${encodeURIComponent(sessionCode)}`)
}

export function endSession(sessionCode: string, summary?: string) {
  return http.put<SessionDetail>(`/sessions/${encodeURIComponent(sessionCode)}/end`, summary ? { summary } : {})
}

export function updateSessionMetadata(sessionCode: string, metadata: UpdateSessionMetadataPayload) {
  return http.put<SessionDetail>(`/sessions/${encodeURIComponent(sessionCode)}/metadata`, metadata)
}

export function fetchSessionMessages(sessionCode: string, params?: { messageType?: string; limit?: number; offset?: number }) {
  return http.get<SessionMessage[]>(`/sessions/${encodeURIComponent(sessionCode)}/messages`, { params })
}

export function createSessionMessage(sessionCode: string, payload: CreateMessagePayload) {
  return http.post<SessionMessage>(`/sessions/${encodeURIComponent(sessionCode)}/messages`, payload)
}

export function countSessionMessages(sessionCode: string) {
  return http.get<number>(`/sessions/${encodeURIComponent(sessionCode)}/messages/count`)
}

export function fetchUserSessions(userId: number, activeOnly = false) {
  return http.get<ApiResp<SessionDetail>[]>(`/sessions/user/${userId}`, { params: { activeOnly } })
}
