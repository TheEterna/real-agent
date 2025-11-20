import axios from 'axios'

/**
 * 认证API接口封装
 * 注意：使用独立的axios实例，避免与拦截器循环依赖
 */

const authHttp = axios.create({
  baseURL: '/api',
  timeout: 150000
})

// ==================== 类型定义 ====================

export interface LoginRequest {
  externalId: string
  password: string
}

export interface RegisterRequest {
  externalId: string
  password: string
  nickname?: string
  avatarUrl?: string
}

export interface RefreshRequest {
  refreshToken: string
}

export interface User {
  userId: string
  externalId: string
  nickname?: string
  avatarUrl?: string
}

/**
 * ResponseResult 基础结构
 */
export interface ResponseResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

/**
 * 登录响应数据
 */
export interface LoginData {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: User
}

/**
 * 登录响应
 */
export type LoginResponse = ResponseResult<LoginData>

/**
 * 刷新Token响应数据
 */
export interface RefreshData {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

/**
 * 刷新Token响应
 */
export type RefreshResponse = ResponseResult<RefreshData>

/**
 * 注册响应数据
 */
export interface RegisterData {
  userId: string
  externalId: string
}

/**
 * 注册响应
 */
export type RegisterResponse = ResponseResult<RegisterData>

/**
 * 登出响应
 */
export type LogoutResponse = ResponseResult<void>

/**
 * 当前用户响应数据
 */
export interface CurrentUserData {
  userId: string
}

/**
 * 当前用户响应
 */
export type CurrentUserResponse = ResponseResult<CurrentUserData>

// ==================== API方法 ====================

export const authApi = {
  /**
   * 用户登录
   */
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await authHttp.post<LoginResponse>('/auth/login', data)
    return response.data
  },

  /**
   * 用户注册
   */
  register: async (data: RegisterRequest): Promise<RegisterResponse> => {
    const response = await authHttp.post<RegisterResponse>('/auth/register', data)
    return response.data
  },

  /**
   * 刷新Token
   */
  refresh: async (refreshToken: string): Promise<RefreshResponse> => {
    const response = await authHttp.post<RefreshResponse>('/auth/refresh', { refreshToken })
    return response.data
  },

  /**
   * 登出
   */
  logout: async (accessToken?: string): Promise<LogoutResponse> => {
    const headers = accessToken ? { Authorization: `Bearer ${accessToken}` } : {}
    const response = await authHttp.post<LogoutResponse>('/auth/logout', {}, { headers })
    return response.data
  },

  /**
   * 获取当前用户信息
   */
  getCurrentUser: async (accessToken?: string): Promise<CurrentUserResponse> => {
    const headers = accessToken ? { Authorization: `Bearer ${accessToken}` } : {}
    const response = await authHttp.get<CurrentUserResponse>('/auth/me', { headers })
    return response.data
  }
}
