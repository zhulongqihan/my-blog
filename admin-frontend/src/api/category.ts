import request from '@/utils/request'
import type { Result, Category, CategoryForm } from '@/types'

export function getCategories(): Promise<Result<Category[]>> {
  return request.get('/admin/categories')
}

export function createCategory(data: CategoryForm): Promise<Result<Category>> {
  return request.post('/admin/categories', data)
}

export function updateCategory(id: number, data: CategoryForm): Promise<Result<Category>> {
  return request.put(`/admin/categories/${id}`, data)
}

export function deleteCategory(id: number): Promise<Result<string>> {
  return request.delete(`/admin/categories/${id}`)
}
