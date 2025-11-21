import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type LoginRequest, type RegisterRequest, type User } from '@/api/auth'

/**
 * 认证状态管理
 * 使用 Pinia 持久化插件自动管理 Token 和用户信息
 */
export const useAuthStore = defineStore('auth', () => {
    // ==================== 状态 ====================
    const accessToken = ref<string | null>(null)
    const refreshToken = ref<string | null>(null)
    const tokenExpiry = ref<number | null>(null)
    const user = ref<User | null>(null)

    // ==================== 计算属性 ====================
    const isAuthenticated = computed(() => !!accessToken.value)

    const isTokenExpiring = computed(() => {
        if (!tokenExpiry.value) return true
        return Date.now() >= tokenExpiry.value
    })

    // ==================== 方法 ====================

    /**
     * 设置 Token
     */
    const setTokens = (access: string, refresh: string, expiresIn: number) => {
        accessToken.value = access
        refreshToken.value = refresh
        // 提前30秒标记为即将过期
        tokenExpiry.value = Date.now() + (expiresIn - 30) * 1000
    }

    /**
     * 获取 Access Token
     */
    const getAccessToken = (): string | null => {
        return accessToken.value
    }

    /**
     * 获取 Refresh Token
     */
    const getRefreshToken = (): string | null => {
        return refreshToken.value
    }

    /**
     * 清除认证状态
     */
    const clearAuth = () => {
        accessToken.value = null
        refreshToken.value = null
        tokenExpiry.value = null
        user.value = null
    }

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

            const { accessToken: access, refreshToken: refresh, expiresIn, user: userInfo } = response.data

            // 存储 Token
            setTokens(access, refresh, expiresIn)

            // 存储用户信息
            user.value = userInfo
            console.log("登录成功")
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
            if (accessToken.value) {
                const response = await authApi.logout(accessToken.value)
                if (response.code !== 200) {
                    console.warn('登出接口返回异常:', response.message)
                }
            }
        } catch (error) {
            console.error('登出接口调用失败:', error)
        } finally {
            // 无论接口是否成功，都清除本地状态
            clearAuth()
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
            const response = await authApi.getCurrentUser(accessToken.value || undefined)

            if (response.code !== 200) {
                throw new Error(response.message || '获取用户信息失败')
            }

            // user.value = response.data
            console.log('当前用户:', response.data)
        } catch (error) {
            console.error('获取用户信息失败:', error)
            // 如果获取失败，可能是token invalid，清除本地状态
            clearAuth()
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
        accessToken,
        refreshToken,
        tokenExpiry,
        user,
        isAuthenticated,
        isTokenExpiring,

        // 方法
        setTokens,
        getAccessToken,
        getRefreshToken,
        clearAuth,
        login,
        register,
        logout,
        fetchCurrentUser,
        init
    }
}, {
    // 配置持久化（pinia-plugin-persistedstate）
    persist: true
})
