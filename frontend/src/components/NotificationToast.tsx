import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { useWebSocketContext } from '../contexts/WebSocketContext';
import './NotificationToast.css';

interface ToastItem {
  id: number;
  type: string;
  title: string;
  content: string;
  senderName: string;
}

/**
 * 全站通知弹窗组件
 * 
 * 监听 WebSocket 推送的通知，以浮动 Toast 形式展示在页面右上角。
 * 每条通知默认显示 5 秒后自动消失，最多同时显示 3 条。
 */
export default function NotificationToast() {
  const { notifications } = useWebSocketContext();
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const prevLengthRef = useRef(0);

  // 监听新通知到达
  useEffect(() => {
    if (notifications.length > prevLengthRef.current && notifications.length > 0) {
      const newest = notifications[0];
      const toast: ToastItem = {
        id: newest.id || Date.now(),
        type: newest.type,
        title: newest.title,
        content: newest.content,
        senderName: newest.senderName,
      };

      setToasts((prev) => [toast, ...prev].slice(0, 3));

      // 5 秒后自动移除
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== toast.id));
      }, 5000);
    }
    prevLengthRef.current = notifications.length;
  }, [notifications]);

  const removeToast = (id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'SYSTEM':
        return '📢';
      case 'COMMENT':
        return '💬';
      default:
        return '🔔';
    }
  };

  return (
    <div className="notification-toast-container">
      <AnimatePresence>
        {toasts.map((toast) => (
          <motion.div
            key={toast.id}
            className="notification-toast"
            initial={{ opacity: 0, x: 100, scale: 0.9 }}
            animate={{ opacity: 1, x: 0, scale: 1 }}
            exit={{ opacity: 0, x: 100, scale: 0.9 }}
            transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            onClick={() => removeToast(toast.id)}
          >
            <div className="notification-toast-icon">
              {getIcon(toast.type)}
            </div>
            <div className="notification-toast-body">
              <div className="notification-toast-title">{toast.title}</div>
              <div className="notification-toast-content">{toast.content}</div>
              <div className="notification-toast-time">来自 {toast.senderName} · 刚刚</div>
            </div>
            <button
              className="notification-toast-close"
              onClick={(e) => {
                e.stopPropagation();
                removeToast(toast.id);
              }}
            >
              ×
            </button>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}
