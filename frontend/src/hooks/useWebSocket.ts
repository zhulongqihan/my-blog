import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * WebSocket STOMP Hook
 * 
 * 提供：
 * - 自动连接/断开管理
 * - 在线人数实时获取
 * - 系统通知订阅
 * - 连接状态监控
 * - 自动重连机制
 */

interface Notification {
  id: number;
  type: string;
  title: string;
  content: string;
  senderName: string;
  relatedId: number | null;
  relatedType: string | null;
  isRead: boolean;
  createdAt: string;
}

interface UseWebSocketReturn {
  connected: boolean;
  onlineCount: number;
  notifications: Notification[];
  clearNotifications: () => void;
}

// 构建 WebSocket URL：优先使用独立环境变量，否则基于当前页面 origin
const WS_URL = import.meta.env.VITE_WS_URL || new URL('/ws', window.location.origin).href;

export function useWebSocket(): UseWebSocketReturn {
  const [connected, setConnected] = useState(false);
  const [onlineCount, setOnlineCount] = useState(0);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const clientRef = useRef<Client | null>(null);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,        // 5秒后自动重连
      heartbeatIncoming: 10000,    // 服务端心跳间隔
      heartbeatOutgoing: 10000,    // 客户端心跳间隔
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log('[STOMP]', str);
        }
      },
    });

    // 连接成功回调
    client.onConnect = () => {
      setConnected(true);

      // 订阅在线人数广播
      client.subscribe('/topic/online-count', (message) => {
        try {
          const data = JSON.parse(message.body);
          setOnlineCount(data.onlineCount);
        } catch (e) {
          if (import.meta.env.DEV) console.warn('[WS] online-count parse failed:', e);
        }
      });

      // 订阅全站通知（系统公告 + 评论通知）
      client.subscribe('/topic/notifications', (message) => {
        try {
          const notification: Notification = JSON.parse(message.body);
          setNotifications((prev) => [notification, ...prev].slice(0, 50));
        } catch (e) {
          if (import.meta.env.DEV) console.warn('[WS] notification parse failed:', e);
        }
      });
    };

    // 连接断开回调
    client.onDisconnect = () => {
      setConnected(false);
    };

    // 连接错误回调
    client.onStompError = (frame) => {
      console.error('[STOMP Error]', frame.headers['message']);
      setConnected(false);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      if (client.active) {
        client.deactivate();
      }
    };
  }, []);

  return { connected, onlineCount, notifications, clearNotifications };
}
