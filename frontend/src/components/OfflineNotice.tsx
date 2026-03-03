import { useEffect, useState } from 'react';
import { WifiOff, Wifi } from 'lucide-react';
import './OfflineNotice.css';

export default function OfflineNotice() {
  const [online, setOnline] = useState(navigator.onLine);
  const [showRecover, setShowRecover] = useState(false);

  useEffect(() => {
    const handleOnline = () => {
      setOnline(true);
      setShowRecover(true);
      window.setTimeout(() => setShowRecover(false), 2200);
    };

    const handleOffline = () => {
      setOnline(false);
      setShowRecover(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  if (online && !showRecover) return null;

  return (
    <div className={`offline-notice ${online ? 'offline-notice--online' : 'offline-notice--offline'}`}>
      {online ? <Wifi size={14} /> : <WifiOff size={14} />}
      <span>{online ? '网络已恢复' : '当前离线：部分实时功能不可用'}</span>
    </div>
  );
}
