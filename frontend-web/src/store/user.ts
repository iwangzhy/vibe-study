import { defineStore } from 'pinia'
import type { UserInfo, LoginResponse } from '@/types'

/**
 * 用户状态接口
 */
interface UserState {
  /** 用户信息 */
  userInfo: UserInfo | null
  /** 访问令牌 */
  accessToken: string
  /** 刷新令牌 */
  refreshToken: string
  /** 是否已登录 */
  isLogin: boolean
}

/**
 * Getters 类型定义
 */
interface UserGetters {
  userId: number | undefined
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  hasToken: boolean
}

/**
 * Actions 类型定义
 */
interface UserActions {
  setLoginInfo(data: LoginResponse): void
  setUserInfo(userInfo: UserInfo): void
  setToken(accessToken: string, refreshToken?: string): void
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
    refreshToken: '',
    isLogin: false
  }),

  getters: {
    /**
     * 用户ID
     */
    userId(): number | undefined {
      return this.userInfo?.id
    },

    /**
     * 用户名
     */
    username(): string {
      return this.userInfo?.username || ''
    },

    /**
     * 昵称
     */
    nickname(): string {
      return this.userInfo?.nickname || ''
    },

    /**
     * 头像
     */
    avatar(): string {
      return this.userInfo?.avatar || ''
    },

    /**
     * 邮箱
     */
    email(): string {
      return this.userInfo?.email || ''
    },

    /**
     * 手机号
     */
    phone(): string {
      return this.userInfo?.phone || ''
    },

    /**
     * Token是否有效
     */
    hasToken(): boolean {
      return !!this.accessToken
    }
  },

  actions: {
    /**
     * 设置登录信息
     */
    setLoginInfo(data: LoginResponse): void {
      this.userInfo = data.userInfo
      this.accessToken = data.accessToken
      this.refreshToken = data.refreshToken
      this.isLogin = true
    },

    /**
     * 更新用户信息
     */
    setUserInfo(userInfo: UserInfo): void {
      this.userInfo = userInfo
    },

    /**
     * 更新Token
     */
    setToken(accessToken: string, refreshToken?: string): void {
      this.accessToken = accessToken
      if (refreshToken) {
        this.refreshToken = refreshToken
      }
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
      this.refreshToken = ''
      this.isLogin = false
    }
  },

  // 持久化配置
  persist: {
    key: 'user-store',
    storage: localStorage,
    paths: ['userInfo', 'accessToken', 'refreshToken', 'isLogin']
  }
})

