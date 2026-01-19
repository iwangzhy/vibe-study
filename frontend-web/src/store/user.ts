import { defineStore } from 'pinia'
import type { UserInfoVO, LoginResponse } from '@/api/user'

/**
 * 用户状态接口
 */
interface UserState {
  /** 用户信息 */
  userInfo: UserInfoVO | null
  /** 访问令牌 */
  accessToken: string
  /** 是否已登录 */
  isLogin: boolean
}

/**
 * Getters 类型定义
 */
interface UserGetters {
  userId: (state: UserState) => number | undefined
  username: (state: UserState) => string
  nickname: (state: UserState) => string
  avatar: (state: UserState) => string
  email: (state: UserState) => string
  phone: (state: UserState) => string
  hasToken: (state: UserState) => boolean
  [key: string]: (state: UserState) => any
}

/**
 * Actions 类型定义
 */
interface UserActions {
  setLoginInfo(data: LoginResponse): void
  setUserInfo(userInfo: UserInfoVO): void
  setToken(accessToken: string): void
  updateAvatar(avatar: string): void
  updateNickname(nickname: string): void
  logout(): void
}

/**
 * 用户Store
 */
export const useUserStore = defineStore<'user', UserState, UserGetters, UserActions>('user', {
  state: (): UserState => ({
    userInfo: null,
    accessToken: '',
    isLogin: false
  }),

  getters: {
    /**
     * 用户ID
     */
    userId(state): number | undefined {
      return state.userInfo?.id
    },

    /**
     * 用户名
     */
    username(state): string {
      return state.userInfo?.username || ''
    },

    /**
     * 昵称
     */
    nickname(state): string {
      return state.userInfo?.nickname || ''
    },

    /**
     * 头像
     */
    avatar(state): string {
      return state.userInfo?.avatar || ''
    },

    /**
     * 邮箱
     */
    email(state): string {
      return state.userInfo?.email || ''
    },

    /**
     * 手机号
     */
    phone(state): string {
      return state.userInfo?.phone || ''
    },

    /**
     * Token是否有效
     */
    hasToken(state): boolean {
      return !!state.accessToken
    }
  },

  actions: {
    /**
     * 设置登录信息
     */
    setLoginInfo(data: LoginResponse): void {
      this.userInfo = data.userInfo
      this.accessToken = data.token
      this.isLogin = true
    },

    /**
     * 更新用户信息
     */
    setUserInfo(userInfo: UserInfoVO): void {
      this.userInfo = userInfo
    },

    /**
     * 更新Token
     */
    setToken(accessToken: string): void {
      this.accessToken = accessToken
    },

    /**
     * 更新头像
     */
    updateAvatar(avatar: string): void {
      if (this.userInfo) {
        this.userInfo.avatar = avatar
      }
    },

    /**
     * 更新昵称
     */
    updateNickname(nickname: string): void {
      if (this.userInfo) {
        this.userInfo.nickname = nickname
      }
    },

    /**
     * 退出登录
     */
    logout(): void {
      this.userInfo = null
      this.accessToken = ''
      this.isLogin = false
    }
  },

  // 持久化配置
  persist: true
})
