/**
 * 用户模块 API
 */

import request from '@/utils/request'
import type { ApiResponse } from '@/types'

// ==================== 类型定义 ====================

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

// ==================== 认证接口 ====================

/**
 * 用户登录
 */
export const login = (data: LoginRequest) => {
  return request.post<ApiResponse<LoginResponse>>('/api/user/auth/login', data)
}

/**
 * 用户注册
 */
export const register = (data: RegisterRequest) => {
  return request.post<ApiResponse<LoginResponse>>('/api/user/auth/register', data)
}

/**
 * 发送邮箱验证码
 */
export const sendEmailCode = (data: SendCodeRequest) => {
  return request.post<ApiResponse<void>>('/api/user/auth/send-code', data)
}

/**
 * 修改密码
 */
export const changePassword = (data: ChangePasswordRequest) => {
  return request.put<ApiResponse<void>>('/api/user/auth/change-password', data)
}

// ==================== 用户信息接口 ====================

/**
 * 获取用户信息
 */
export const getUserInfo = (id: number) => {
  return request.get<ApiResponse<UserInfoVO>>(`/api/user/info/${id}`)
}

/**
 * 获取当前登录用户信息
 */
export const getCurrentUserInfo = () => {
  return request.get<ApiResponse<UserInfoVO>>('/api/user/info/current')
}

/**
 * 更新用户信息
 */
export const updateUserInfo = (data: UpdateUserRequest) => {
  return request.put<ApiResponse<UserInfoVO>>('/api/user/info', data)
}

// ==================== 用户关系接口 ====================

/**
 * 关注用户
 */
export const followUser = (id: number) => {
  return request.post<ApiResponse<void>>(`/api/user/follow/${id}`)
}

/**
 * 取消关注用户
 */
export const unfollowUser = (id: number) => {
  return request.delete<ApiResponse<void>>(`/api/user/follow/${id}`)
}
