import request from '@/utils/request'
import type {
  LoginParams,
  LoginResponse,
  RegisterParams,
  UserInfo,
  UserDetail,
  ApiResponse
} from '@/types'

// ==================== 认证相关 ====================

/**
 * 用户登录
 */
export const login = (data: LoginParams) => {
  return request.post<ApiResponse<LoginResponse>>('/api/user/auth/login', data)
}

/**
 * 用户注册
 */
export const register = (data: RegisterParams) => {
  return request.post<ApiResponse<void>>('/api/user/auth/register', data)
}

/**
 * 发送验证码
 */
export const sendCode = (phone: string) => {
  return request.post<ApiResponse<void>>('/api/user/auth/send-code', { phone })
}

/**
 * 退出登录
 */
export const logout = () => {
  return request.post<ApiResponse<void>>('/api/user/auth/logout')
}

/**
 * 刷新Token
 */
export const refreshToken = (refreshToken: string) => {
  return request.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
    '/api/user/auth/refresh-token',
    { refreshToken }
  )
}

// ==================== 用户信息 ====================

/**
 * 获取用户信息
 */
export const getUserInfo = (userId: number) => {
  return request.get<ApiResponse<UserDetail>>(`/api/user/info/${userId}`)
}

/**
 * 更新用户信息参数
 */
export interface UpdateUserInfoParams {
  nickname?: string
  avatar?: string
  gender?: 0 | 1 | 2
  birthday?: string
  bio?: string
}

/**
 * 更新用户信息
 */
export const updateUserInfo = (data: UpdateUserInfoParams) => {
  return request.put<ApiResponse<UserInfo>>('/api/user/info', data)
}

/**
 * 修改密码参数
 */
export interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
}

/**
 * 修改密码
 */
export const changePassword = (data: ChangePasswordParams) => {
  return request.put<ApiResponse<void>>('/api/user/password', data)
}

/**
 * 上传头像
 */
export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<{ url: string }>>('/api/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 关注相关 ====================

/**
 * 关注用户
 */
export const followUser = (userId: number) => {
  return request.post<ApiResponse<void>>(`/api/user/follow/${userId}`)
}

/**
 * 取消关注
 */
export const unfollowUser = (userId: number) => {
  return request.delete<ApiResponse<void>>(`/api/user/follow/${userId}`)
}

/**
 * 获取关注列表
 */
export const getFollowList = (userId: number, page: number = 1, pageSize: number = 20) => {
  return request.get<ApiResponse<{ list: UserInfo[]; total: number }>>(
    `/api/user/follow/${userId}`,
    { page, pageSize }
  )
}

/**
 * 获取粉丝列表
 */
export const getFansList = (userId: number, page: number = 1, pageSize: number = 20) => {
  return request.get<ApiResponse<{ list: UserInfo[]; total: number }>>(
    `/api/user/fans/${userId}`,
    { page, pageSize }
  )
}

/**
 * 获取共同关注
 */
export const getCommonFollows = (userId: number) => {
  return request.get<ApiResponse<UserInfo[]>>(`/api/user/common-follows/${userId}`)
}

