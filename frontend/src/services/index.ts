import { get, post, put, del } from './api';
import type {
  Article,
  ArticleRequest,
  PageResponse,
  Category,
  Tag,
  Comment,
  CommentRequest,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
} from '../types';

// ==================== 文章 API ====================

export const articleApi = {
  // 获取文章列表（分页）
  getList: (page = 0, size = 10) => get<PageResponse<Article>>('/articles', { page, size }),

  // 获取单篇文章
  getById: (id: number) => get<Article>(`/articles/${id}`),

  // 获取精选文章
  getFeatured: () => get<Article[]>('/articles/featured'),

  // 获取热门文章
  getPopular: (limit = 5) => get<Article[]>('/articles/popular', { limit }),

  // 按分类获取文章
  getByCategory: (categoryId: number, page = 0, size = 10) =>
    get<PageResponse<Article>>(`/articles/category/${categoryId}`, { page, size }),

  // 按标签获取文章
  getByTag: (tagId: number, page = 0, size = 10) =>
    get<PageResponse<Article>>(`/articles/tag/${tagId}`, { page, size }),

  // 搜索文章
  search: (keyword: string, page = 0, size = 10) =>
    get<PageResponse<Article>>('/articles/search', { keyword, page, size }),

  // 创建文章（需要登录）
  create: (data: ArticleRequest) => post<Article>('/articles', data),

  // 更新文章（需要登录）
  update: (id: number, data: ArticleRequest) => put<Article>(`/articles/${id}`, data),

  // 删除文章（需要登录）
  delete: (id: number) => del<void>(`/articles/${id}`),
};

// ==================== 分类 API ====================

export const categoryApi = {
  // 获取所有分类
  getAll: () => get<Category[]>('/categories'),

  // 创建分类（需要管理员）
  create: (data: Partial<Category>) => post<Category>('/categories', data),

  // 更新分类（需要管理员）
  update: (id: number, data: Partial<Category>) => put<Category>(`/categories/${id}`, data),

  // 删除分类（需要管理员）
  delete: (id: number) => del<void>(`/categories/${id}`),
};

// ==================== 标签 API ====================

export const tagApi = {
  // 获取所有标签
  getAll: () => get<Tag[]>('/tags'),

  // 创建标签（需要管理员）
  create: (data: Partial<Tag>) => post<Tag>('/tags', data),

  // 更新标签（需要管理员）
  update: (id: number, data: Partial<Tag>) => put<Tag>(`/tags/${id}`, data),

  // 删除标签（需要管理员）
  delete: (id: number) => del<void>(`/tags/${id}`),
};

// ==================== 评论 API ====================

export const commentApi = {
  // 获取文章评论
  getByArticle: (articleId: number, page = 0, size = 20) =>
    get<PageResponse<Comment>>(`/comments/article/${articleId}`, { page, size }),

  // 创建评论
  create: (articleId: number, data: CommentRequest) =>
    post<Comment>(`/comments/article/${articleId}`, data),

  // 删除评论（需要登录）
  delete: (id: number) => del<void>(`/comments/${id}`),
};

// ==================== 认证 API ====================

export const authApi = {
  // 登录
  login: (data: LoginRequest) => post<AuthResponse>('/auth/login', data),

  // 注册
  register: (data: RegisterRequest) => post<AuthResponse>('/auth/register', data),

  // 获取当前用户信息
  getCurrentUser: () => get<AuthResponse>('/auth/me'),
};
