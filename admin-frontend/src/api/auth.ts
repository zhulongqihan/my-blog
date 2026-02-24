import request from '@/utils/request'
import type { Result, AuthResponse } from '@/types'

export function login(username: string, password: string): Promise<Result<AuthResponse>> {
  return request.post('/api/auth/login', { username, password })
}

export function getMe(): Promise<Result<AuthResponse>> {
  return request.get('/api/auth/me')
}

export function logout(): Promise<Result<string>> {
  return request.post('/api/auth/logout')
}
