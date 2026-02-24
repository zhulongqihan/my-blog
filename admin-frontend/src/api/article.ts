import request from '@/utils/request'
import type { Result, PageResult, ArticleAdminResponse, ArticleQueryParams } from '@/types'

export function getArticles(params: ArticleQueryParams): Promise<Result<PageResult<ArticleAdminResponse>>> {
  return request.get('/admin/articles', { params })
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
