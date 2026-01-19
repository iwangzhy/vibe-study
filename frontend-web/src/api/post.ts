import request from '@/utils/request'
import type {
  Post,
  CreatePostParams,
  UpdatePostParams,
  PageParams,
  PageData,
  ApiResponse
} from '@/types'

// ==================== 动态相关 ====================

/**
 * 获取动态列表（首页信息流）
 */
export const getPostList = (params: PageParams) => {
  return request.get<ApiResponse<PageData<Post>>>('/api/post/list', params)
}

/**
 * 获取用户动态列表
 */
export const getUserPosts = (userId: number, params: PageParams) => {
  return request.get<ApiResponse<PageData<Post>>>(`/api/post/user/${userId}`, params)
}

/**
 * 获取动态详情
 */
export const getPostDetail = (postId: number) => {
  return request.get<ApiResponse<Post>>(`/api/post/${postId}`)
}

/**
 * 发布动态
 */
export const createPost = (data: CreatePostParams) => {
  return request.post<ApiResponse<Post>>('/api/post', data)
}

/**
 * 更新动态
 */
export const updatePost = (data: UpdatePostParams) => {
  return request.put<ApiResponse<Post>>(`/api/post/${data.id}`, data)
}

/**
 * 删除动态
 */
export const deletePost = (postId: number) => {
  return request.delete<ApiResponse<void>>(`/api/post/${postId}`)
}

/**
 * 点赞动态
 */
export const likePost = (postId: number) => {
  return request.post<ApiResponse<void>>(`/api/post/${postId}/like`)
}

/**
 * 取消点赞
 */
export const unlikePost = (postId: number) => {
  return request.delete<ApiResponse<void>>(`/api/post/${postId}/like`)
}

/**
 * 收藏动态
 */
export const collectPost = (postId: number) => {
  return request.post<ApiResponse<void>>(`/api/post/${postId}/collect`)
}

/**
 * 取消收藏
 */
export const uncollectPost = (postId: number) => {
  return request.delete<ApiResponse<void>>(`/api/post/${postId}/collect`)
}

/**
 * 获取用户收藏的动态
 */
export const getCollectedPosts = (params: PageParams) => {
  return request.get<ApiResponse<PageData<Post>>>('/api/post/collected', params)
}

/**
 * 获取用户点赞的动态
 */
export const getLikedPosts = (params: PageParams) => {
  return request.get<ApiResponse<PageData<Post>>>('/api/post/liked', params)
}
