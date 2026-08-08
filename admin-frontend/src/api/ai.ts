import request from '@/utils/request'
import type { Result } from '@/types'

export interface AiSource {
  articleId: number
  title: string
  score: number
}

export interface AiTrace {
  requestId: string
  mode: string
  model: string
  status: string
  retrievalCount: number
  inputChars: number
  outputChars: number
  timeToFirstTokenMs: number
  totalLatencyMs: number
  createdAt: string
  sources: AiSource[]
  error?: string
}

export interface AiTraceStats {
  traces: AiTrace[]
  total: number
  successRate: number
  avgLatencyMs: number
}

export function getAiTraces(limit = 50): Promise<Result<AiTraceStats>> {
  return request.get(`/admin/ai/traces?limit=${limit}`)
}
