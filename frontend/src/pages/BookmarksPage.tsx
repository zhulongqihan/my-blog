import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Bookmark, BookOpen, Trash2, Clock } from 'lucide-react';
import { getBookmarks, removeBookmark, type BookmarkEntry } from '../services/bookmarks';
import './BookmarksPage.css';

const BookmarksPage = () => {
  const [bookmarks, setBookmarks] = useState<BookmarkEntry[]>([]);

  useEffect(() => {
    setBookmarks(getBookmarks());
  }, []);

  const handleRemove = (articleId: number) => {
    removeBookmark(articleId);
    setBookmarks(getBookmarks());
  };

  const handleClearAll = () => {
    if (!confirm('确定要清空所有书签吗？')) return;
    bookmarks.forEach(b => removeBookmark(b.articleId));
    setBookmarks([]);
  };

  const formatDate = (iso: string) => {
    const d = new Date(iso);
    return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <motion.div
      className="bookmarks-page"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.4 }}
    >
      <header className="bookmarks-page__header">
        <div className="bookmarks-page__header-left">
          <Bookmark size={28} className="bookmarks-page__icon" />
          <div>
            <h1 className="bookmarks-page__title">我的书签</h1>
            <p className="bookmarks-page__subtitle">
              {bookmarks.length > 0
                ? `收藏了 ${bookmarks.length} 篇文章`
                : '还没有收藏任何文章'}
            </p>
          </div>
        </div>
        {bookmarks.length > 0 && (
          <button className="bookmarks-page__clear" onClick={handleClearAll}>
            <Trash2 size={14} />
            清空全部
          </button>
        )}
      </header>

      {bookmarks.length === 0 ? (
        <div className="bookmarks-page__empty">
          <BookOpen size={48} strokeWidth={1} />
          <p>在阅读文章时点击书签按钮收藏</p>
          <Link to="/" className="bookmarks-page__home-link">浏览文章</Link>
        </div>
      ) : (
        <AnimatePresence mode="popLayout">
          <div className="bookmarks-page__list">
            {bookmarks.map((b, i) => (
              <motion.div
                key={b.articleId}
                className="bookmark-item"
                layout
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20, height: 0, marginBottom: 0 }}
                transition={{ duration: 0.3, delay: i * 0.05 }}
              >
                <Link to={`/article/${b.articleId}`} className="bookmark-item__link">
                  <h3 className="bookmark-item__title">{b.title}</h3>
                  {b.summary && (
                    <p className="bookmark-item__summary">{b.summary.slice(0, 80)}…</p>
                  )}
                  <div className="bookmark-item__meta">
                    <span className="bookmark-item__date">
                      <Clock size={12} />
                      {formatDate(b.savedAt)}
                    </span>
                    {b.progress > 0 && (
                      <span className="bookmark-item__progress">
                        已读 {b.progress}%
                        <span className="bookmark-item__bar">
                          <span className="bookmark-item__bar-fill" style={{ width: `${b.progress}%` }} />
                        </span>
                      </span>
                    )}
                  </div>
                </Link>
                <button
                  className="bookmark-item__remove"
                  onClick={() => handleRemove(b.articleId)}
                  title="移除书签"
                >
                  <Trash2 size={14} />
                </button>
              </motion.div>
            ))}
          </div>
        </AnimatePresence>
      )}
    </motion.div>
  );
};

export default BookmarksPage;
