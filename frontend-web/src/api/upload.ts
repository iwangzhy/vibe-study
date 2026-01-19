import request from '@/utils/request'
import type {ApiResponse, UploadResponse} from '@/types'

// ==================== 文件上传 ====================

/**
 * 上传图片
 */
export const uploadImage = (file: File, folder: string = 'images') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('folder', folder)
  
  return request.post<ApiResponse<UploadResponse>>('/api/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 上传视频
 */
export const uploadVideo = (file: File, folder: string = 'videos') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('folder', folder)
  
  return request.post<ApiResponse<UploadResponse>>('/api/upload/video', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 批量上传图片
 */
export const uploadImages = (files: File[], folder: string = 'images') => {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  formData.append('folder', folder)
  
  return request.post<ApiResponse<UploadResponse[]>>('/api/upload/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
