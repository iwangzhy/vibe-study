import { defineStore } from 'pinia'

/**
 * 应用状态接口
 */
interface AppState {
  /** 全局Loading状态 */
  loading: boolean
  /** 侧边栏是否展开 */
  sidebarOpened: boolean
  /** 设备类型 */
  device: 'desktop' | 'mobile'
  /** 主题模式 */
  theme: 'light' | 'dark'
}

/**
 * Getters 类型定义
 */
interface AppGetters {
  isMobile: boolean
  isDark: boolean
}

/**
 * Actions 类型定义
 */
interface AppActions {
  showLoading(): void
  hideLoading(): void
  toggleSidebar(): void
  setSidebarOpened(opened: boolean): void
  setDevice(device: 'desktop' | 'mobile'): void
  toggleTheme(): void
  setTheme(theme: 'light' | 'dark'): void
}

/**
 * 应用Store
 */
export const useAppStore = defineStore<'app', AppState, AppGetters, AppActions>('app', {
  state: (): AppState => ({
    loading: false,
    sidebarOpened: true,
    device: 'desktop',
    theme: 'light'
  }),

  getters: {
    /**
     * 是否为移动端
     */
    isMobile(): boolean {
      return this.device === 'mobile'
    },

    /**
     * 是否为暗黑模式
     */
    isDark(): boolean {
      return this.theme === 'dark'
    }
  },

  actions: {
    /**
     * 显示Loading
     */
    showLoading(): void {
      this.loading = true
    },

    /**
     * 隐藏Loading
     */
    hideLoading(): void {
      this.loading = false
    },

    /**
     * 切换侧边栏
     */
    toggleSidebar(): void {
      this.sidebarOpened = !this.sidebarOpened
    },

    /**
     * 设置侧边栏状态
     */
    setSidebarOpened(opened: boolean): void {
      this.sidebarOpened = opened
    },

    /**
     * 设置设备类型
     */
    setDevice(device: 'desktop' | 'mobile'): void {
      this.device = device
    },

    /**
     * 切换主题
     */
    toggleTheme(): void {
      this.theme = this.theme === 'light' ? 'dark' : 'light'
    },

    /**
     * 设置主题
     */
    setTheme(theme: 'light' | 'dark'): void {
      this.theme = theme
    }
  },

  // 持久化配置
  persist: {
    key: 'app-store',
    storage: localStorage,
    paths: ['sidebarOpened', 'theme']
  }
})

