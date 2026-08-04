// 文章列表摘要
export interface ArticleSummary {
  id: number;
  title: string;
  summary: string;
  coverImage?: string;
  author: AuthorInfo;
  category?: CategoryInfo;
  tags: TagInfo[];
  viewCount: number;
  likeCount: number;
  commentCount: number;
  published: boolean;
  featured: boolean;
  readTimeMinutes?: number;
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
}

// 文章详情
export interface Article extends ArticleSummary {
  content: string;
}

export interface AuthorInfo {
  id: number;
  username: string;
  nickname: string;
  avatar?: string;
}

export interface CategoryInfo {
  id: number;
  name: string;
  icon?: string;
}

export interface TagInfo {
  id: number;
  name: string;
  color?: string;
}

// 分类和标签
export interface Category {
  id: number;
  name: string;
  description?: string;
  icon?: string;
  sortOrder: number;
}

export interface Tag {
  id: number;
  name: string;
  color?: string;
}

// 评论相关
export interface Comment {
  id: number;
  content: string;
  user?: UserInfo;
  guestName?: string;
  replies?: Comment[];
  createdAt: string;
}

export interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  avatar?: string;
}

// 认证相关
export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  nickname?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  nickname: string;
  avatar?: string;
  role: string;
}

// API 响应包装
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// 分页响应
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// 文章请求
export interface ArticleRequest {
  title: string;
  summary?: string;
  content: string;
  coverImage?: string;
  categoryId?: number;
  tagIds?: number[];
  published?: boolean;
  featured?: boolean;
}

// 评论请求
export interface CommentRequest {
  content: string;
  parentId?: number;
  guestName?: string;
  guestEmail?: string;
}

// 文章归档
export interface ArchiveResponse {
  totalCount: number;
  years: YearArchive[];
}

export interface YearArchive {
  year: number;
  count: number;
  months: MonthArchive[];
}

export interface MonthArchive {
  month: number;
  monthName: string;
  articles: ArticleBrief[];
}

export interface ArticleBrief {
  id: number;
  title: string;
  date: string;
  category?: string;
}
