import { useMemo, useState } from 'react';
import { Trophy, X, BookOpen, Heart, Clock, Eye, Star, Zap, Award, Target, Flame, Coffee, Sparkles, Moon } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import './AchievementHub.css';

interface AchievementItem {
  key: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  category: 'reading' | 'explore' | 'social' | 'special';
  rarity: 'common' | 'rare' | 'epic' | 'legendary';
}

const CATEGORIES = [
  { id: 'all', label: '全部' },
  { id: 'reading', label: '📖 阅读' },
  { id: 'explore', label: '🧭 探索' },
  { id: 'social', label: '💬 社交' },
  { id: 'special', label: '✨ 特殊' },
] as const;

const RARITY_LABELS: Record<string, string> = {
  common: '普通',
  rare: '稀有',
  epic: '史诗',
  legendary: '传说',
};

const ALL_ACHIEVEMENTS: AchievementItem[] = [
  // 阅读类
  { key: 'starter', title: '初出茅庐', description: '阅读进度达到 25%', icon: <BookOpen size={18} />, category: 'reading', rarity: 'common' },
  { key: 'focus', title: '渐入佳境', description: '阅读进度达到 60%', icon: <Target size={18} />, category: 'reading', rarity: 'rare' },
  { key: 'finisher', title: '一字不漏', description: '完整阅读一篇文章', icon: <Trophy size={18} />, category: 'reading', rarity: 'epic' },
  { key: 'speed-reader', title: '一目十行', description: '3 分钟内完成一篇文章', icon: <Zap size={18} />, category: 'reading', rarity: 'rare' },
  { key: 'marathon', title: '阅读马拉松', description: '单次阅读超过 10 分钟', icon: <Clock size={18} />, category: 'reading', rarity: 'rare' },
  { key: 'deep-reader', title: '深度潜水', description: '展开所有代码块并阅读', icon: <Eye size={18} />, category: 'reading', rarity: 'epic' },
  // 探索类
  { key: 'first-visit', title: '新世界的大门', description: '第一次访问博客', icon: <Sparkles size={18} />, category: 'explore', rarity: 'common' },
  { key: 'explorer', title: '探索者', description: '阅读 3 篇不同文章', icon: <Star size={18} />, category: 'explore', rarity: 'rare' },
  { key: 'collector', title: '收藏家', description: '阅读 5 篇不同文章', icon: <Award size={18} />, category: 'explore', rarity: 'epic' },
  { key: 'toc-navigator', title: '目录达人', description: '使用文章目录跳转 3 次', icon: <Target size={18} />, category: 'explore', rarity: 'common' },
  { key: 'font-tweaker', title: '排版控', description: '调整过字体大小', icon: <Coffee size={18} />, category: 'explore', rarity: 'common' },
  { key: 'reading-mode', title: '沉浸主义者', description: '启用阅读模式', icon: <Moon size={18} />, category: 'explore', rarity: 'common' },
  // 社交类
  { key: 'first-like', title: '初次心动', description: '第一次点赞', icon: <Heart size={18} />, category: 'social', rarity: 'common' },
  { key: 'liker', title: '点赞狂魔', description: '点赞 3 篇不同文章', icon: <Heart size={18} />, category: 'social', rarity: 'rare' },
  { key: 'sharer', title: '布道者', description: '分享过一篇文章', icon: <Flame size={18} />, category: 'social', rarity: 'rare' },
  // 特殊类
  { key: 'night-owl', title: '夜猫子', description: '在深夜 0-5 点访问', icon: <Moon size={18} />, category: 'special', rarity: 'epic' },
  { key: 'early-bird', title: '早起的鸟儿', description: '在清晨 5-7 点访问', icon: <Sparkles size={18} />, category: 'special', rarity: 'epic' },
  { key: 'perfectionist', title: '完美主义者', description: '解锁所有阅读成就', icon: <Award size={18} />, category: 'special', rarity: 'legendary' },
];

/** 读取全局已解锁成就 */
export function getUnlockedKeys(): Set<string> {
  // 来自单篇文章记录
  const articleKeys = Object.keys(localStorage).filter(k => k.startsWith('article-achievements-'));
  const unlocked = new Set<string>();
  articleKeys.forEach(k => {
    try {
      const v = JSON.parse(localStorage.getItem(k) || '[]') as string[];
      v.forEach(item => unlocked.add(item));
    } catch { /* ignore */ }
  });
  // 来自全局成就记录
  try {
    const global = JSON.parse(localStorage.getItem('global-achievements') || '[]') as string[];
    global.forEach(item => unlocked.add(item));
  } catch { /* ignore */ }
  return unlocked;
}

/** 解锁一个全局成就（外部调用） */
export function unlockGlobalAchievement(key: string): boolean {
  const existing = getUnlockedKeys();
  if (existing.has(key)) return false;
  try {
    const arr = JSON.parse(localStorage.getItem('global-achievements') || '[]') as string[];
    arr.push(key);
    localStorage.setItem('global-achievements', JSON.stringify(arr));
  } catch { /* ignore */ }
  return true;
}

export default function AchievementHub() {
  const [open, setOpen] = useState(false);
  const [activeCategory, setActiveCategory] = useState<string>('all');

  const status = useMemo(() => {
    const unlocked = getUnlockedKeys();
    const total = ALL_ACHIEVEMENTS.length;
    const done = ALL_ACHIEVEMENTS.filter(item => unlocked.has(item.key)).length;
    return { unlocked, total, done };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const filtered = useMemo(() => {
    if (activeCategory === 'all') return ALL_ACHIEVEMENTS;
    return ALL_ACHIEVEMENTS.filter(a => a.category === activeCategory);
  }, [activeCategory]);

  const percent = Math.round((status.done / status.total) * 100);

  return (
    <>
      <button className="achievement-hub__trigger liquid-btn" onClick={() => setOpen(true)}>
        <Trophy size={14} />
        成就 {status.done}/{status.total}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            className="achievement-hub__mask"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setOpen(false)}
          >
            <motion.div
              className="achievement-hub__panel"
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0, y: 20 }}
              transition={{ type: 'spring', damping: 25, stiffness: 300 }}
              onClick={e => e.stopPropagation()}
            >
              <div className="achievement-hub__head">
                <div>
                  <h4>🏆 成就中心</h4>
                  <p className="achievement-hub__subtitle">已解锁 {status.done}/{status.total} · {percent}%</p>
                </div>
                <button className="achievement-hub__close" onClick={() => setOpen(false)}>
                  <X size={14} />
                </button>
              </div>

              {/* 进度条 */}
              <div className="achievement-hub__progress-bar">
                <motion.div
                  className="achievement-hub__progress-fill"
                  initial={{ width: 0 }}
                  animate={{ width: `${percent}%` }}
                  transition={{ duration: 0.6, ease: 'easeOut' }}
                />
              </div>

              {/* 分类切换 */}
              <div className="achievement-hub__tabs">
                {CATEGORIES.map(cat => (
                  <button
                    key={cat.id}
                    className={`achievement-hub__tab${activeCategory === cat.id ? ' is-active' : ''}`}
                    onClick={() => setActiveCategory(cat.id)}
                  >
                    {cat.label}
                  </button>
                ))}
              </div>

              {/* 成就列表 */}
              <div className="achievement-hub__list">
                {filtered.map((item, i) => {
                  const done = status.unlocked.has(item.key);
                  return (
                    <motion.div
                      key={item.key}
                      className={`achievement-hub__card rarity-${item.rarity}${done ? ' is-done' : ''}`}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: i * 0.03 }}
                    >
                      <div className={`achievement-hub__icon rarity-${item.rarity}`}>
                        {done ? item.icon : <span className="achievement-hub__lock">?</span>}
                      </div>
                      <div className="achievement-hub__info">
                        <span className="achievement-hub__name">{done ? item.title : '???'}</span>
                        <span className="achievement-hub__desc">{item.description}</span>
                      </div>
                      <span className={`achievement-hub__rarity rarity-${item.rarity}`}>
                        {RARITY_LABELS[item.rarity]}
                      </span>
                    </motion.div>
                  );
                })}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
