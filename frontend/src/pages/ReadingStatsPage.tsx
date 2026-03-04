import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { BarChart3, BookOpen, Flame, Trophy, ArrowLeft, Trash2 } from 'lucide-react';
import { getUnlockedKeys } from '../components/AchievementHub';
import './ReadingStatsPage.css';

interface ReadingProgressEntry {
  articleId: number;
  title: string;
  percent: number;
  updatedAt: number;
  path: string;
}

/** 获取最近 7 天的阅读天数 */
function getReadingStreak(): number {
  const map = JSON.parse(localStorage.getItem('article-reading-progress-map') || '{}') as Record<string, ReadingProgressEntry>;
  const days = new Set<string>();
  Object.values(map).forEach(entry => {
    if (entry.updatedAt) {
      days.add(new Date(entry.updatedAt).toISOString().slice(0, 10));
    }
  });
  // 计算从今天往回数的连续天数
  let streak = 0;
  const today = new Date();
  for (let i = 0; i < 30; i++) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    if (days.has(key)) {
      streak++;
    } else if (i > 0) {
      break;
    }
  }
  return streak;
}

/** 获取日历热力图数据（最近35天）*/
function getHeatmapData(): { date: string; count: number }[] {
  const map = JSON.parse(localStorage.getItem('article-reading-progress-map') || '{}') as Record<string, ReadingProgressEntry>;
  const dayCounts: Record<string, number> = {};
  Object.values(map).forEach(entry => {
    if (entry.updatedAt) {
      const day = new Date(entry.updatedAt).toISOString().slice(0, 10);
      dayCounts[day] = (dayCounts[day] || 0) + 1;
    }
  });
  const result: { date: string; count: number }[] = [];
  const today = new Date();
  for (let i = 34; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    result.push({ date: key, count: dayCounts[key] || 0 });
  }
  return result;
}

export default function ReadingStatsPage() {
  const [, setRefresh] = useState(0);

  const stats = useMemo(() => {
    const map = JSON.parse(localStorage.getItem('article-reading-progress-map') || '{}') as Record<string, ReadingProgressEntry>;
    const entries = Object.values(map);
    const totalArticles = entries.length;
    const completedArticles = entries.filter(e => e.percent >= 100).length;
    const avgProgress = totalArticles > 0 ? Math.round(entries.reduce((s, e) => s + e.percent, 0) / totalArticles) : 0;
    const streak = getReadingStreak();
    const achievementCount = getUnlockedKeys().size;

    // 按更新时间排序（最近的在前）
    const recentArticles = [...entries]
      .sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0))
      .slice(0, 10);

    const heatmap = getHeatmapData();

    return { totalArticles, completedArticles, avgProgress, streak, achievementCount, recentArticles, heatmap };
  }, []);

  const clearStats = () => {
    if (confirm('确定要清除所有阅读记录吗？此操作不可恢复。')) {
      localStorage.removeItem('article-reading-progress-map');
      localStorage.removeItem('global-achievements');
      // 清除单篇文章的成就记录
      Object.keys(localStorage)
        .filter(k => k.startsWith('article-achievements-'))
        .forEach(k => localStorage.removeItem(k));
      setRefresh(p => p + 1);
      window.location.reload();
    }
  };

  const getHeatColor = (count: number) => {
    if (count === 0) return 'var(--bg-tertiary)';
    if (count === 1) return 'var(--accent-olive-light)';
    if (count <= 3) return 'var(--accent-olive)';
    return 'var(--accent-rust)';
  };

  return (
    <motion.div
      className="reading-stats-page"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.4 }}
    >
      <div className="reading-stats-page__header">
        <Link to="/" className="reading-stats-page__back">
          <ArrowLeft size={16} /> 返回首页
        </Link>
        <h1>📊 阅读统计</h1>
        <p className="reading-stats-page__subtitle">你的阅读足迹与数据一览</p>
      </div>

      {/* 概览卡片 */}
      <div className="reading-stats-page__overview">
        {[
          { icon: <BookOpen size={20} />, label: '已读文章', value: stats.totalArticles, suffix: '篇' },
          { icon: <BarChart3 size={20} />, label: '平均进度', value: stats.avgProgress, suffix: '%' },
          { icon: <Flame size={20} />, label: '连续阅读', value: stats.streak, suffix: '天' },
          { icon: <Trophy size={20} />, label: '已解锁成就', value: stats.achievementCount, suffix: '个' },
        ].map((card, i) => (
          <motion.div
            key={card.label}
            className="reading-stats-page__stat-card"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.08 }}
          >
            <span className="reading-stats-page__stat-icon">{card.icon}</span>
            <span className="reading-stats-page__stat-value">{card.value}<small>{card.suffix}</small></span>
            <span className="reading-stats-page__stat-label">{card.label}</span>
          </motion.div>
        ))}
      </div>

      {/* 热力图 */}
      <div className="reading-stats-page__section">
        <h3>阅读热力图（近 35 天）</h3>
        <div className="reading-stats-page__heatmap">
          {stats.heatmap.map((day, i) => (
            <motion.div
              key={day.date}
              className="reading-stats-page__heat-cell"
              style={{ background: getHeatColor(day.count) }}
              title={`${day.date}: ${day.count} 篇`}
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ delay: i * 0.015 }}
            />
          ))}
        </div>
        <div className="reading-stats-page__heat-legend">
          <span>少</span>
          <div className="reading-stats-page__heat-cell" style={{ background: 'var(--bg-tertiary)' }} />
          <div className="reading-stats-page__heat-cell" style={{ background: 'var(--accent-olive-light)' }} />
          <div className="reading-stats-page__heat-cell" style={{ background: 'var(--accent-olive)' }} />
          <div className="reading-stats-page__heat-cell" style={{ background: 'var(--accent-rust)' }} />
          <span>多</span>
        </div>
      </div>

      {/* 完成度 */}
      <div className="reading-stats-page__section">
        <h3>文章完成度</h3>
        <div className="reading-stats-page__completion-bar">
          <div className="reading-stats-page__completion-track">
            <motion.div
              className="reading-stats-page__completion-fill"
              initial={{ width: 0 }}
              animate={{ width: stats.totalArticles > 0 ? `${(stats.completedArticles / stats.totalArticles) * 100}%` : '0%' }}
              transition={{ duration: 0.8, ease: 'easeOut' }}
            />
          </div>
          <span className="reading-stats-page__completion-text">
            {stats.completedArticles} / {stats.totalArticles} 篇已读完
          </span>
        </div>
      </div>

      {/* 最近阅读 */}
      <div className="reading-stats-page__section">
        <h3>最近阅读</h3>
        {stats.recentArticles.length === 0 ? (
          <p className="reading-stats-page__empty">还没有阅读记录，去看看文章吧！</p>
        ) : (
          <div className="reading-stats-page__recent-list">
            {stats.recentArticles.map((entry, i) => (
              <motion.div
                key={entry.articleId}
                className="reading-stats-page__recent-item"
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}
              >
                <Link to={entry.path} className="reading-stats-page__recent-title">
                  {entry.title}
                </Link>
                <div className="reading-stats-page__recent-meta">
                  <div className="reading-stats-page__mini-bar">
                    <div
                      className="reading-stats-page__mini-fill"
                      style={{ width: `${Math.min(entry.percent, 100)}%` }}
                    />
                  </div>
                  <span>{entry.percent}%</span>
                  <span className="reading-stats-page__recent-time">
                    {entry.updatedAt ? new Date(entry.updatedAt).toLocaleDateString('zh-CN') : ''}
                  </span>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>

      {/* 清除数据 */}
      <div className="reading-stats-page__actions">
        <button className="reading-stats-page__clear-btn" onClick={clearStats}>
          <Trash2 size={14} /> 清除所有阅读记录
        </button>
      </div>
    </motion.div>
  );
}
