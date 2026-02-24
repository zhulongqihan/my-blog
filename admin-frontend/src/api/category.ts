import request from '@/utils/request'
import type { Result, Category, CategoryForm } from '@/types'

export function getCategories(): Promise<Result<Category[]>> {
  return request.get('/api/admin/categories')
}

export function createCategory(data: CategoryForm): Promise<Result<Category>> {
  return request.post('/api/admin/categories', data)
}

export function updateCategory(id: number, data: CategoryForm): Promise<Result<Category>> {
  return request.put(`/api/admin/categories/${id}`, data)
}

export function deleteCategory(id: number): Promise<Result<string>> {
  return request.delete(`/api/admin/categories/${id}`)
}
