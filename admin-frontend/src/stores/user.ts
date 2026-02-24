import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AuthResponse } from '@/types'
import { login as apiLogin, getMe } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('admin_token') || '')
  const userInfo = ref<AuthResponse | null>(
    JSON.parse(localStorage.getItem('admin_user') || 'null')
  )

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')

  async function login(username: string, password: string) {
    const res = await apiLogin(username, password)
    const data = res.data
    if (data.role !== 'ADMIN') {
      throw new Error('该账号没有管理员权限')
    }
    token.value = data.token
    userInfo.value = data
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('admin_user', JSON.stringify(data))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
  }

  async function fetchUserInfo() {
    try {
      const res = await getMe()
      userInfo.value = res.data
      localStorage.setItem('admin_user', JSON.stringify(res.data))
    } catch {
      logout()
    }
  }

  return { token, userInfo, isLoggedIn, isAdmin, nickname, login, logout, fetchUserInfo }
})
