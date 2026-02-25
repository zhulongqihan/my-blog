import request from '@/utils/request'
import type { Result } from '@/types'

/** Redis 服务器信息 */
export interface RedisInfo {
  redisVersion: string
  usedMemory: string
  usedMemoryPeak: string
  connectedClients: string
  uptimeInDays: string
  totalKeys: number
  keyspaceHits: number
  keyspaceMisses: number
  hitRate: string
}

/** 缓存空间统计 */
export interface CacheSpaceStat {
  name: string
  description: string
  keyCount: number
}

/** 缓存综合统计 */
export interface CacheStats {
  redisInfo: RedisInfo
  cacheSpaces: CacheSpaceStat[]
}

// ==================== 缓存监控 ====================

export function getCacheStats(): Promise<Result<CacheStats>> {
  return request.get('/admin/cache/stats')
}

export function getCacheNames(): Promise<Result<string[]>> {
  return request.get('/admin/cache/names')
}

// ==================== 缓存管理 ====================

export function evictCache(cacheName: string): Promise<Result<string>> {
  return request.delete(`/admin/cache/${cacheName}`)
}

export function evictAllCaches(): Promise<Result<string>> {
  return request.delete('/admin/cache/all')
}
