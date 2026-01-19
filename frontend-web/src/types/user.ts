/**
 * 用户模块类型定义
 */

// ==================== 用户信息相关 ====================

/**
 * 用户信息 VO
 */
export interface UserInfoVO {
  id: number
  username: string
  nickname: string
  avatar: string
  email?: string
  phone?: string
  gender: 0 | 1 | 2 // 0-未知，1-男，2-女
  birthday?: string
  bio?: string
  status: 0 | 1 // 0-禁用，1-正常
  followingCount: number
  followerCount: number
  isFollowing: boolean
  createdAt: string
  updatedAt: string
}

/**
 * 更新用户信息请求
 */
export interface UpdateUserRequest {
  nickname?: string
  avatar?: string
  gender?: 0 | 1 | 2
  birthday?: string
  bio?: string
}

// ==================== 认证相关 ====================

/**
 * 登录请求
 */
export interface LoginRequest {
  account: string
  password: string
}

/**
 * 登录响应
 */
export interface LoginResponse {
  token: string
  userInfo: UserInfoVO
}

/**
 * 注册请求
 */
export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  email: string
  code: string
}

/**
 * 发送验证码请求
 */
export interface SendCodeRequest {
  email: string
}

/**
 * 修改密码请求
 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

// ==================== API 响应类型 ====================

/**
 * 统一响应格式
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}
