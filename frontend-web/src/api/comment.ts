import request from '@/utils/request'
import type {
  Comment,
  CreateCommentParams,
  PageParams,
  PageData,
  ApiResponse
} from '@/types'

// ==================== 评论相关 ====================

/**
 * 获取评论列表
 */
export const getCommentList = (postId: number, params: PageParams) => {
  return request.get<ApiResponse<PageData<Comment>>>(`/api/comment/post/${postId}`, params)
}

/**
 * 获取评论详情
 */
export const getCommentDetail = (commentId: number) => {
  return request.get<ApiResponse<Comment>>(`/api/comment/${commentId}`)
}

/**
 * 发布评论
 */
export const createComment = (data: CreateCommentParams) => {
  return request.post<ApiResponse<Comment>>('/api/comment', data)
}

/**
 * 删除评论
 */
export const deleteComment = (commentId: number) => {
  return request.delete<ApiResponse<void>>(`/api/comment/${commentId}`)
}

/**
 * 点赞评论
 */
export const likeComment = (commentId: number) => {
  return request.post<ApiResponse<void>>(`/api/comment/${commentId}/like`)
}

/**
 * 取消点赞评论
 */
export const unlikeComment = (commentId: number) => {
  return request.delete<ApiResponse<void>>(`/api/comment/${commentId}/like`)
}

/**
 * 获取用户的评论列表
 */
export const getUserComments = (userId: number, params: PageParams) => {
  return request.get<ApiResponse<PageData<Comment>>>(`/api/comment/user/${userId}`, params)
}
