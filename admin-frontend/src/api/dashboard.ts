import request from '@/utils/request'
import type { Result, DashboardStats } from '@/types'

export function getStats(): Promise<Result<DashboardStats>> {
  return request.get('/api/admin/dashboard/stats')
}

export function getOverview(): Promise<Result<DashboardStats>> {
  return request.get('/api/admin/dashboard/overview')
}
