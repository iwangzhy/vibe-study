/**
 * 环境变量类型定义
 */

interface ImportMetaEnv {
  /**
   * 应用标题
   */
  readonly VITE_APP_TITLE: string

  /**
   * API基础地址
   */
  readonly VITE_APP_BASE_API: string

  /**
   * 文件上传地址
   */
  readonly VITE_APP_UPLOAD_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
