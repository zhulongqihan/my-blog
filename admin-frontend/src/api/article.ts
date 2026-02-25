import request from '@/utils/request'
import type { Result, PageResult, ArticleAdminResponse, ArticleQueryParams, ArticleDetail, ArticleForm } from '@/types'

export function getArticles(params: ArticleQueryParams): Promise<Result<PageResult<ArticleAdminResponse>>> {
  return request.get('/admin/articles', { params })
}

export function getArticleDetail(id: number): Promise<Result<ArticleDetail>> {
  return request.get(`/admin/articles/${id}`)
}

export function createArticle(data: ArticleForm): Promise<Result<ArticleDetail>> {
  return request.post('/admin/articles', data)
}

export function updateArticle(id: number, data: ArticleForm): Promise<Result<ArticleDetail>> {
  return request.put(`/admin/articles/${id}`, data)
}

export function publishArticle(id: number): Promise<Result<string>> {
  return request.put(`/admin/articles/${id}/publish`)
}

export function unpublishArticle(id: number): Promise<Result<string>> {
  return request.put(`/admin/articles/${id}/unpublish`)
}

export function topArticle(id: number): Promise<Result<string>> {
  return request.put(`/admin/articles/${id}/top`)
}

export function untopArticle(id: number): Promise<Result<string>> {
  return request.put(`/admin/articles/${id}/untop`)
}

export function batchDeleteArticles(ids: number[]): Promise<Result<string>> {
  return request.delete('/admin/articles/batch', { data: ids })
}

export function uploadImage(file: File): Promise<Result<{ url: string }>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
