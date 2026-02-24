import request from '@/utils/request'
import type { Result, PageResult, Comment } from '@/types'

export function getComments(page = 1, size = 10): Promise<Result<PageResult<Comment>>> {
  return request.get('/api/admin/comments', { params: { page, size } })
}

export function approveComment(id: number): Promise<Result<string>> {
  return request.put(`/api/admin/comments/${id}/approve`)
}

export function deleteComment(id: number): Promise<Result<string>> {
  return request.delete(`/api/admin/comments/${id}`)
}

export function batchDeleteComments(ids: number[]): Promise<Result<string>> {
  return request.delete('/api/admin/comments/batch', { data: ids })
}
