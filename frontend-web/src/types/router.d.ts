/**
 * 路由相关类型定义
 */

import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /**
     * 页面标题
     */
    title?: string

    /**
     * 是否需要登录
     */
    requiresAuth?: boolean

    /**
     * 权限标识
     */
    permission?: string

    /**
     * 是否缓存页面
     */
    keepAlive?: boolean

    /**
     * 图标
     */
    icon?: string

    /**
     * 是否隐藏
     */
    hidden?: boolean

    /**
     * 面包屑
     */
    breadcrumb?: boolean
  }
}
