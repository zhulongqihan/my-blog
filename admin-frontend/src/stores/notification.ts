import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getUnreadCount } from '@/api/notification'
import type { NotificationItem } from '@/api/notification'

/**
 * 通知 Pinia Store
 * 
 * 管理 WebSocket 连接、消息接收和未读计数
 */
export const useNotificationStore = defineStore('notification', () => {
  const connected = ref(false)
  const onlineCount = ref(0)
  const unreadCount = ref(0)
  const recentNotifications = ref<NotificationItem[]>([])
  const client = ref<Client | null>(null)

  const hasUnread = computed(() => unreadCount.value > 0)

  /**
   * 初始化 WebSocket 连接
   */
  function connect() {
    if (client.value?.active) return

    const token = localStorage.getItem('admin_token')
    const wsUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace('/api', '') + '/ws'

    const stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log('[Admin STOMP]', str)
        }
      },
    })

    stompClient.onConnect = () => {
      connected.value = true

      // 订阅在线人数
      stompClient.subscribe('/topic/online-count', (message) => {
        try {
          const data = JSON.parse(message.body)
          onlineCount.value = data.onlineCount
        } catch (e) { /* ignore */ }
      })

      // 订阅通知（管理员接收所有通知）
      stompClient.subscribe('/topic/notifications', (message) => {
        try {
          const notification: NotificationItem = JSON.parse(message.body)
          recentNotifications.value = [notification, ...recentNotifications.value].slice(0, 20)
          unreadCount.value++
        } catch (e) { /* ignore */ }
      })
    }

    stompClient.onDisconnect = () => {
      connected.value = false
    }

    stompClient.onStompError = () => {
      connected.value = false
    }

    stompClient.activate()
    client.value = stompClient
  }

  /**
   * 断开 WebSocket 连接
   */
  function disconnect() {
    if (client.value?.active) {
      client.value.deactivate()
    }
    connected.value = false
  }

  /**
   * 从 API 加载未读计数
   */
  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      unreadCount.value = res.data as number
    } catch (e) { /* ignore */ }
  }

  /**
   * 重置未读计数
   */
  function resetUnread() {
    unreadCount.value = 0
  }

  /**
   * 清除最近通知
   */
  function clearRecent() {
    recentNotifications.value = []
  }

  return {
    connected,
    onlineCount,
    unreadCount,
    hasUnread,
    recentNotifications,
    connect,
    disconnect,
    fetchUnreadCount,
    resetUnread,
    clearRecent,
  }
})
