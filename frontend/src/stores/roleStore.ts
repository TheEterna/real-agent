import { defineStore } from 'pinia'
import type { RoleDetail } from '@/types/roleplay'
import { fetchRoles } from '@/services/roleplay'

export interface Role {
  id: number
  voice: string
  name: string
  desc: string
  avatar: string | null
}

const FALLBACK_AVATAR = '/default-avatar.png'

function mapRole(detail: RoleDetail): Role {
  return {
    id: detail.id,
    slug: detail.slug,
    name: detail.name,
    desc: detail.description || '这个角色正等你创造故事。',
    avatar: detail.avatarUrl || FALLBACK_AVATAR
  }
}

export const useRoleStore = defineStore('roleStore', {
  state: () => ({
    roles: [] as Role[],
    loading: false,
    loaded: false,
    error: '' as string | null
  }),
  getters: {
    getById: (state) => (id: number) => state.roles.find(r => r.id === id),
    getBySlug: (state) => (slug: string) => state.roles.find(r => r.slug === slug)
  },
  actions: {
    async loadRoles(force = false) {
      if (this.loading) return
      if (this.loaded && !force) return
      this.loading = true
      this.error = ''
      try {
        let list = await fetchRoles(true)
        list = list.data
        this.roles = Array.isArray(list) ? list.map(mapRole) : []
        this.loaded = true
      } catch (error: any) {
        this.error = error?.message || '角色列表加载失败'
        throw error
      } finally {
        this.loading = false
      }
    }
  }
})
