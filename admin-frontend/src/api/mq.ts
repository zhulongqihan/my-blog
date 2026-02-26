import request from '@/utils/request'
import type { Result } from '@/types'

/** 队列信息 */
export interface QueueInfo {
  name: string
  description: string
  messageCount: number
  consumerCount: number
  status: string
  error?: string
}

/** 交换机信息 */
export interface ExchangeInfo {
  name: string
  type: string
  description: string
}

/** MQ 综合统计 */
export interface MQStats {
  connected: boolean
  queues: QueueInfo[]
  exchanges: ExchangeInfo[]
}

/** MQ 健康检查 */
export interface MQHealth {
  connected: boolean
  status: string
}

// ==================== MQ 监控 ====================

export function getMQStats(): Promise<Result<MQStats>> {
  return request.get('/admin/mq/stats')
}

export function getMQHealth(): Promise<Result<MQHealth>> {
  return request.get('/admin/mq/health')
}

export function sendTestMessage(queueName: string): Promise<Result<string>> {
  return request.post(`/admin/mq/test/${queueName}`)
}
