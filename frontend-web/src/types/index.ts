/**
 * 全局通用类型定义
 */

// ==================== 通用类型 ====================

/**
 * 统一响应结果
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

/**
 * 分页请求参数
 */
export interface PageParams {
  page: number
  pageSize: number
}

/**
 * 分页响应数据
 */
export interface PageData<T = any> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

// ==================== 用户相关 ====================

/**
 * 用户信息
 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  gender?: 0 | 1 | 2 // 0-未知，1-男，2-女
  birthday?: string
  bio?: string
  status?: 0 | 1 // 0-禁用，1-正常
  createdAt?: string
  updatedAt?: string
}

/**
 * 用户统计信息
 */
export interface UserStats {
  postCount: number // 动态数
  followCount: number // 关注数
  fansCount: number // 粉丝数
  likeCount?: number // 获赞数
}

/**
 * 用户详细信息（包含统计）
 */
export interface UserDetail extends UserInfo {
  stats: UserStats
  isFollowed?: boolean // 是否已关注
  isFollowedMe?: boolean // 是否关注我
}

// ==================== 认证相关 ====================

/**
 * 登录请求参数
 */
export interface LoginParams {
  account: string
  password: string
}

/**
 * 登录响应数据
 */
export interface LoginResponse {
  userInfo: UserInfo
  accessToken: string
  refreshToken: string
}

/**
 * 注册请求参数
 */
export interface RegisterParams {
  phone: string
  code: string
  password: string
  confirmPassword: string
}

/**
 * Token刷新响应
 */
export interface RefreshTokenResponse {
  accessToken: string
  refreshToken: string
}

// ==================== 动态相关 ====================

/**
 * 动态信息
 */
export interface Post {
  id: number
  userId: number
  content: string
  images?: string[]
  videoUrl?: string
  likeCount: number
  commentCount: number
  shareCount: number
  isLiked: boolean
  isCollected: boolean
  createdAt: string
  updatedAt: string
  user?: UserInfo
}

/**
 * 发布动态参数
 */
export interface CreatePostParams {
  content: string
  images?: string[]
  videoUrl?: string
}

/**
 * 更新动态参数
 */
export interface UpdatePostParams {
  id: number
  content: string
  images?: string[]
}

// ==================== 评论相关 ====================

/**
 * 评论信息
 */
export interface Comment {
  id: number
  postId: number
  userId: number
  parentId?: number
  content: string
  likeCount: number
  isLiked: boolean
  createdAt: string
  user?: UserInfo
  children?: Comment[]
}

/**
 * 发布评论参数
 */
export interface CreateCommentParams {
  postId: number
  content: string
  parentId?: number
}

// ==================== 关注相关 ====================

/**
 * 关注/粉丝列表项
 */
export interface FollowItem extends UserInfo {
  followTime?: string
  isFollowed?: boolean
}

// ==================== 消息相关 ====================

/**
 * 消息类型
 */
export enum MessageType {
  TEXT = 'text',
  IMAGE = 'image',
  VIDEO = 'video',
  SYSTEM = 'system'
}

/**
 * 消息信息
 */
export interface Message {
  id: number
  senderId: number
  receiverId: number
  type: MessageType
  content: string
  isRead: boolean
  createdAt: string
  sender?: UserInfo
}

/**
 * 会话信息
 */
export interface Conversation {
  id: number
  userId: number
  lastMessage?: Message
  unreadCount: number
  updatedAt: string
  user?: UserInfo
}

// ==================== 通知相关 ====================

/**
 * 通知类型
 */
export enum NotificationType {
  LIKE = 'like', // 点赞
  COMMENT = 'comment', // 评论
  FOLLOW = 'follow', // 关注
  MENTION = 'mention', // @提及
  SYSTEM = 'system' // 系统通知
}

/**
 * 通知信息
 */
export interface Notification {
  id: number
  userId: number
  type: NotificationType
  content: string
  relatedId?: number // 关联ID（动态ID、评论ID等）
  isRead: boolean
  createdAt: string
  fromUser?: UserInfo
}

// ==================== 搜索相关 ====================

/**
 * 搜索类型
 */
export enum SearchType {
  ALL = 'all',
  USER = 'user',
  POST = 'post',
  TOPIC = 'topic'
}

/**
 * 搜索参数
 */
export interface SearchParams extends PageParams {
  keyword: string
  type?: SearchType
}

/**
 * 搜索结果
 */
export interface SearchResult {
  users?: UserInfo[]
  posts?: Post[]
  topics?: Topic[]
}

// ==================== 话题相关 ====================

/**
 * 话题信息
 */
export interface Topic {
  id: number
  name: string
  description?: string
  postCount: number
  followCount: number
  isFollowed?: boolean
  createdAt: string
}

// ==================== 表单相关 ====================

/**
 * 表单规则类型
 */
export interface FormRule {
  required?: boolean
  message?: string
  trigger?: 'blur' | 'change'
  min?: number
  max?: number
  pattern?: RegExp
  validator?: (rule: any, value: any, callback: any) => void
}

export type FormRules = Record<string, FormRule[]>

// ==================== 文件上传 ====================

/**
 * 文件上传参数
 */
export interface UploadParams {
  file: File
  folder?: string // 上传目录
}

/**
 * 文件上传响应
 */
export interface UploadResponse {
  url: string
  name: string
  size: number
}
