import { useWebSocket } from '../hooks/useWebSocket';
import './OnlineCount.css';

/**
 * 在线人数显示组件
 * 
 * 显示在 Footer 区域，通过 WebSocket 实时更新在线人数
 */
export default function OnlineCount() {
  const { connected, onlineCount } = useWebSocket();

  return (
    <span className="online-count" title={connected ? '实时连接中' : '连接中...'}>
      <span className={`online-dot ${connected ? 'connected' : 'disconnected'}`} />
      <span className="online-text">
        {connected ? `${onlineCount} 人在线` : '连接中...'}
      </span>
    </span>
  );
}
