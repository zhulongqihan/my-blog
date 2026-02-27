import request from '@/utils/request'
import type { Result } from '@/types'

/** 通知项 */
export interface NotificationItem {
  id: number
  type: 'COMMENT' | 'SYSTEM' | 'LIKE'
  title: string
  content: string
  senderName: string
  relatedId: number | null
  relatedType: string | null
  isRead: boolean
  createdAt: string
}

/** 通知分页 */
export interface NotificationPage {
  content: NotificationItem[]
  totalElements: number
  totalPages: number
  number: number
}

/** WebSocket 状态 */
export interface WSStats {
  totalNotifications: number
  status: string
  onlineCount: number
  onlineUsers: Record<string, string>
}

// ==================== 通知管理 ====================

export function getNotifications(page = 0, size = 20): Promise<Result<NotificationPage>> {
  return request.get('/admin/notifications', { params: { page, size } })
}

export function getUnreadCount(): Promise<Result<number>> {
  return request.get('/admin/notifications/unread-count')
}

export function markAsRead(id: number): Promise<Result<string>> {
  return request.put(`/admin/notifications/${id}/read`)
}

export function markAllAsRead(): Promise<Result<string>> {
  return request.put('/admin/notifications/read-all')
}

export function sendBroadcast(title: string, content: string): Promise<Result<NotificationItem>> {
  return request.post('/admin/notifications/broadcast', { title, content })
}

export function getWSStats(): Promise<Result<WSStats>> {
  return request.get('/admin/notifications/ws-stats')
}
