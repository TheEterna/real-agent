import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type LoginRequest, type RegisterRequest, type User } from '@/api/auth'
import { tokenStorage } from '@/utils/tokenStorage'

/**
 * 认证状态管理
 */
export const useAuthStore = defineStore('auth', () => {
    // ==================== 状态 ====================
    const user = ref<User | null>(null)

    // ==================== 计算属性 ====================
    const isAuthenticated = computed(() => !!tokenStorage.getAccessToken())

    // ==================== 方法 ====================

    /**
     * 用户登录
     */
    const login = async (credentials: LoginRequest) => {
        try {
            const response = await authApi.login(credentials)

            // ResponseResult格式: { code, message, data: { accessToken, refreshToken, expiresIn, user } }
            if (response.code !== 200) {
                throw new Error(response.message || '登录失败')
            }

            const { accessToken, refreshToken, expiresIn, user: userInfo } = response.data

            // 存储 Token
            tokenStorage.setTokens(accessToken, refreshToken, expiresIn)

            // 存储用户信息
            user.value = userInfo

            return response
        } catch (error) {
            console.error('登录失败:', error)
            throw error
        }
    }

    /**
     * 用户注册
     */
    const register = async (data: RegisterRequest) => {
        try {
            const response = await authApi.register(data)

            if (response.code !== 200) {
                throw new Error(response.message || '注册失败')
            }

            return response
        } catch (error) {
            console.error('注册失败:', error)
            throw error
        }
    }

    /**
     * 登出
     */
    const logout = async () => {
        try {
            const accessToken = tokenStorage.getAccessToken()
            if (accessToken) {
                const response = await authApi.logout(accessToken)
                if (response.code !== 200) {
                    console.warn('登出接口返回异常:', response.message)
                }
            }
        } catch (error) {
            console.error('登出接口调用失败:', error)
        } finally {
            // 无论接口是否成功，都清除本地状态
            tokenStorage.clearTokens()
            user.value = null
        }
    }

    /**
     * 获取当前用户信息
     */
    const fetchCurrentUser = async () => {
        if (!isAuthenticated.value) {
            user.value = null
            return
        }

        try {
            const accessToken = tokenStorage.getAccessToken()
            const response = await authApi.getCurrentUser(accessToken || undefined)

            if (response.code !== 200) {
                throw new Error(response.message || '获取用户信息失败')
            }

            console.log('当前用户:', response.data)
        } catch (error) {
            console.error('获取用户信息失败:', error)
            // 如果获取失败，可能是token invalid，清除本地状态
            tokenStorage.clearTokens()
            user.value = null
        }
    }

    /**
     * 初始化（应用启动时调用）
     */
    const init = async () => {
        if (isAuthenticated.value) {
            await fetchCurrentUser()
        }
    }

    return {
        // 状态
        user,
        isAuthenticated,

        // 方法
        login,
        register,
        logout,
        fetchCurrentUser,
        init
    }
})
