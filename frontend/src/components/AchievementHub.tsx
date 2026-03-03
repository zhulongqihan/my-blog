import { useMemo, useState } from 'react';
import { Trophy, X } from 'lucide-react';
import './AchievementHub.css';

interface AchievementItem {
  key: string;
  title: string;
}

const READING_ITEMS: AchievementItem[] = [
  { key: 'starter', title: '阅读入门（25%）' },
  { key: 'focus', title: '专注阅读（60%）' },
  { key: 'finisher', title: '阅读完成（100%）' },
];

const getUnlockedKeys = () => {
  const keys = Object.keys(localStorage).filter(key => key.startsWith('article-achievements-'));
  const unlocked = new Set<string>();
  keys.forEach(key => {
    try {
      const value = JSON.parse(localStorage.getItem(key) || '[]') as string[];
      value.forEach(item => unlocked.add(item));
    } catch {
      // ignore
    }
  });
  return unlocked;
};

export default function AchievementHub() {
  const [open, setOpen] = useState(false);

  const status = useMemo(() => {
    const unlocked = getUnlockedKeys();
    const total = READING_ITEMS.length;
    const done = READING_ITEMS.filter(item => unlocked.has(item.key)).length;
    return { unlocked, total, done };
  }, [open]);

  return (
    <>
      <button className="achievement-hub__trigger liquid-btn" onClick={() => setOpen(true)}>
        <Trophy size={14} />
        成就 {status.done}/{status.total}
      </button>

      {open && (
        <div className="achievement-hub__mask" onClick={() => setOpen(false)}>
          <div className="achievement-hub__panel" onClick={e => e.stopPropagation()}>
            <div className="achievement-hub__head">
              <h4>阅读成就中心</h4>
              <button className="achievement-hub__close" onClick={() => setOpen(false)}>
                <X size={14} />
              </button>
            </div>

            <div className="achievement-hub__list">
              {READING_ITEMS.map(item => {
                const done = status.unlocked.has(item.key);
                return (
                  <div key={item.key} className={`achievement-hub__item ${done ? 'is-done' : ''}`}>
                    <span>{item.title}</span>
                    <span>{done ? '已解锁' : '未解锁'}</span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
