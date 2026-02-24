import request from '@/utils/request'
import type { Result, Tag, TagForm } from '@/types'

export function getTags(): Promise<Result<Tag[]>> {
  return request.get('/admin/tags')
}

export function createTag(data: TagForm): Promise<Result<Tag>> {
  return request.post('/admin/tags', data)
}

export function updateTag(id: number, data: TagForm): Promise<Result<Tag>> {
  return request.put(`/admin/tags/${id}`, data)
}

export function deleteTag(id: number): Promise<Result<string>> {
  return request.delete(`/admin/tags/${id}`)
}
