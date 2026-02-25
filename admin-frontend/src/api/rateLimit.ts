import request from '@/utils/request'
import type { Result } from '@/types'

/** 限流统计数据 */
export interface RateLimitStats {
  todayBlocked: number
  dailyStats: { date: string; count: number }[]
  blacklistCount: number
  whitelistCount: number
  apiStats: { api: string; count: number }[]
}

/** 限流事件 */
export interface RateLimitEvent {
  timestamp: string
  key: string
  ip: string
}

/** 黑名单操作日志 */
export interface BlacklistLogEntry {
  time: string
  ip: string
  action: string
  reason: string
}

// ==================== 限流统计 ====================

export function getRateLimitStats(): Promise<Result<RateLimitStats>> {
  return request.get('/admin/rate-limit/stats')
}

export function getRecentEvents(limit = 50): Promise<Result<RateLimitEvent[]>> {
  return request.get('/admin/rate-limit/events', { params: { limit } })
}

// ==================== IP黑名单 ====================

export function getBlacklist(): Promise<Result<string[]>> {
  return request.get('/admin/rate-limit/blacklist')
}

export function addToBlacklist(data: { ip: string; reason?: string; duration?: string }): Promise<Result<void>> {
  return request.post('/admin/rate-limit/blacklist', data)
}

export function removeFromBlacklist(ip: string): Promise<Result<void>> {
  return request.delete(`/admin/rate-limit/blacklist/${ip}`)
}

// ==================== IP白名单 ====================

export function getWhitelist(): Promise<Result<string[]>> {
  return request.get('/admin/rate-limit/whitelist')
}

export function addToWhitelist(data: { ip: string }): Promise<Result<void>> {
  return request.post('/admin/rate-limit/whitelist', data)
}

export function removeFromWhitelist(ip: string): Promise<Result<void>> {
  return request.delete(`/admin/rate-limit/whitelist/${ip}`)
}

// ==================== 操作日志 ====================

export function getBlacklistLog(): Promise<Result<BlacklistLogEntry[]>> {
  return request.get('/admin/rate-limit/blacklist/log')
}
