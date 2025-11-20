/**
 * Token 存储管理工具
 * 使用 LocalStorage 存储 Token，提供统一的访问接口
 */

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'
const TOKEN_EXPIRY_KEY = 'token_expiry'

export const tokenStorage = {
    /**
     * 获取 Access Token
     */
    getAccessToken(): string | null {
        return localStorage.getItem(ACCESS_TOKEN_KEY)
    },

    /**
     * 获取 Refresh Token
     */
    getRefreshToken(): string | null {
        return localStorage.getItem(REFRESH_TOKEN_KEY)
    },

    /**
     * 设置 Token
     * @param accessToken Access Token
     * @param refreshToken Refresh Token
     * @param expiresIn 有效期（秒）
     */
    setTokens(accessToken: string, refreshToken: string, expiresIn: number): void {
        localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
        localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
        // 存储过期时间 (当前时间 + expiresIn秒 - 30秒提前量)
        // 减去30秒是为了提前刷新，避免临界点问题
        const expiryTime = Date.now() + (expiresIn - 30) * 1000
        localStorage.setItem(TOKEN_EXPIRY_KEY, expiryTime.toString())
    },

    /**
     * 清除所有 Token
     */
    clearTokens(): void {
        localStorage.removeItem(ACCESS_TOKEN_KEY)
        localStorage.removeItem(REFRESH_TOKEN_KEY)
        localStorage.removeItem(TOKEN_EXPIRY_KEY)
    },

    /**
     * 检查 Token 是否即将过期（或已过期）
     */
    isTokenExpiringSoon(): boolean {
        const expiryTime = localStorage.getItem(TOKEN_EXPIRY_KEY)
        if (!expiryTime) {
            return true // 没有过期时间，视为已过期
        }
        return Date.now() >= parseInt(expiryTime, 10)
    },

    /**
     * 获取 Token 剩余有效时间（秒）
     */
    getTokenRemainingTime(): number {
        const expiryTime = localStorage.getItem(TOKEN_EXPIRY_KEY)
        if (!expiryTime) {
            return 0
        }
        const remaining = (parseInt(expiryTime, 10) - Date.now()) / 1000
        return Math.max(0, remaining)
    }
}
