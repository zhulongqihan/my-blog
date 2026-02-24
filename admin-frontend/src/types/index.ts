// ==================== 通用响应类型 ====================

/** 后端统一响应体（Admin 接口使用 Result，Auth 接口使用 ApiResponse）*/
export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

/** 分页结果 */
export interface PageResult<T = any> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
  hasPrevious: boolean
  hasNext: boolean
}

// ==================== 认证相关 ====================

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthResponse {
  token: string
  type: string
  id: number
  username: string
  email: string
  nickname: string
  avatar?: string
  role: string
}

// ==================== 仪表盘 ====================

export interface TrendItem {
  date: string
  count: number
}

export interface PopularArticle {
  id: number
  title: string
  viewCount: number
  commentCount: number
}

export interface RecentComment {
  id: number
  content: string
  authorName: string
  articleTitle: string
  createdAt: string
}

export interface CategoryStat {
  id: number
  name: string
  articleCount: number
}

export interface DashboardStats {
  totalViews: number
  totalArticles: number
  totalComments: number
  totalUsers: number
  todayViews: number
  todayArticles: number
  todayComments: number
  todayUsers: number
  viewTrend: TrendItem[]
  articleTrend: TrendItem[]
  popularArticles: PopularArticle[]
  recentComments: RecentComment[]
  categoryStats: CategoryStat[]
}

// ==================== 文章管理 ====================

export interface ArticleQueryParams {
  page?: number
  size?: number
  keyword?: string
  categoryId?: number
  published?: boolean | null
  sortField?: string
  sortOrder?: string
}

export interface ArticleAdminResponse {
  id: number
  title: string
  summary: string
  categoryName: string
  tagNames: string[]
  authorName: string
  viewCount: number
  commentCount: number
  published: boolean
  featured: boolean
  createdAt: string
  updatedAt: string
}

// ==================== 分类管理 ====================

export interface Category {
  id: number
  name: string
  description?: string
  icon?: string
  sortOrder: number
}

export interface CategoryForm {
  name: string
  description?: string
}

// ==================== 标签管理 ====================

export interface Tag {
  id: number
  name: string
  color?: string
}

export interface TagForm {
  name: string
}

// ==================== 评论管理 ====================

export interface Comment {
  id: number
  content: string
  article?: {
    id: number
    title: string
  }
  user?: {
    id: number
    username: string
    nickname: string
  }
  guestName?: string
  guestEmail?: string
  parent?: {
    id: number
  }
  approved: boolean
  createdAt: string
}

// ==================== 操作日志 ====================

export interface OperationLog {
  id: number
  userId: number
  username: string
  operationType: string
  module: string
  description: string
  method: string
  requestUrl: string
  requestMethod: string
  requestParams: string
  responseResult: string
  ipAddress: string
  location: string
  browser: string
  os: string
  executionTime: number
  status: number
  errorMsg: string
  createdAt: string
}
