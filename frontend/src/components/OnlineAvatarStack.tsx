import { useMemo } from 'react';
import { useWebSocketContext } from '../contexts/WebSocketContext';
import './OnlineAvatarStack.css';

const COLORS = ['#8b7355', '#5c6b4a', '#a68b6a', '#7a8b68', '#9f8a6f'];

export default function OnlineAvatarStack() {
  const { onlineCount } = useWebSocketContext();

  const avatars = useMemo(() => {
    const showCount = Math.min(Math.max(onlineCount, 0), 6);
    return Array.from({ length: showCount }).map((_, index) => ({
      id: index,
      color: COLORS[index % COLORS.length],
      label: String.fromCharCode(65 + (index % 26)),
    }));
  }, [onlineCount]);

  if (onlineCount <= 0) return null;

  return (
    <div className="online-avatar-stack" title={`当前约 ${onlineCount} 人在线`}>
      <div className="online-avatar-stack__list">
        {avatars.map((item, index) => (
          <span
            key={item.id}
            className="online-avatar-stack__item"
            style={{ background: item.color, zIndex: avatars.length - index }}
          >
            {item.label}
          </span>
        ))}
      </div>
      <span className="online-avatar-stack__text">{onlineCount} 在线</span>
    </div>
  );
}
