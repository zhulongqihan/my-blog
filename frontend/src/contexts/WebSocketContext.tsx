import { createContext, useContext, ReactNode } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';

/**
 * WebSocket 全局上下文
 * 
 * 将 WebSocket 连接提升为应用级单例，
 * 避免多组件各自创建独立连接导致资源浪费和计数异常。
 */

interface WebSocketContextValue {
  connected: boolean;
  onlineCount: number;
  notifications: Array<{
    id: number;
    type: string;
    title: string;
    content: string;
    senderName: string;
    relatedId: number | null;
    relatedType: string | null;
    isRead: boolean;
    createdAt: string;
  }>;
  clearNotifications: () => void;
}

const WebSocketContext = createContext<WebSocketContextValue>({
  connected: false,
  onlineCount: 0,
  notifications: [],
  clearNotifications: () => {},
});

export function WebSocketProvider({ children }: { children: ReactNode }) {
  const ws = useWebSocket();
  return (
    <WebSocketContext.Provider value={ws}>
      {children}
    </WebSocketContext.Provider>
  );
}

export function useWebSocketContext() {
  return useContext(WebSocketContext);
}
