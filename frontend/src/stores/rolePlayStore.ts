import { defineStore } from 'pinia'
import type { SessionMessage } from '@/types/roleplay'

function sortMessages(messages: SessionMessage[]): SessionMessage[] {
  return [...messages].sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
}

export const useRolePlayStore = defineStore('rolePlayStore', {
  state: () => ({
    messagesBySession: {} as Record<string, SessionMessage[]>
  }),
  getters: {
    getMessages: (state) => (sessionCode: string) => {
      const list = state.messagesBySession[sessionCode] || []
      return sortMessages(list)
    }
  },
  actions: {
    setMessages(sessionCode: string, messages: SessionMessage[]) {
      this.messagesBySession[sessionCode] = sortMessages(messages)
    },
    upsertMessage(sessionCode: string, message: SessionMessage) {
      const list = this.messagesBySession[sessionCode] || []
      const idx = list.findIndex(item => item.id === message.id)
      if (idx >= 0) {
        list[idx] = message
      } else {
        list.push(message)
      }
      this.messagesBySession[sessionCode] = sortMessages(list)
    },
    appendMessages(sessionCode: string, messages: SessionMessage[]) {
      const list = this.messagesBySession[sessionCode] || []
      const map = new Map<number, SessionMessage>()
      for (const msg of list) {
        map.set(msg.id, msg)
      }
      for (const msg of messages) {
        map.set(msg.id, msg)
      }
      this.messagesBySession[sessionCode] = sortMessages(Array.from(map.values()))
    },
    clearSession(sessionCode: string) {
      delete this.messagesBySession[sessionCode]
    },
    reset() {
      this.messagesBySession = {}
    }
  }
})
