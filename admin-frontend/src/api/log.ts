import request from '@/utils/request'
import type { Result, PageResult, OperationLog } from '@/types'

export function getLogs(page = 1, size = 20): Promise<Result<PageResult<OperationLog>>> {
  return request.get('/admin/logs', { params: { page, size } })
}
