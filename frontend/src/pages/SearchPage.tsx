import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Search, ArrowRight, Loader2 } from 'lucide-react';
import { useArticles } from '../hooks/useArticles';
import { useTags } from '../hooks/useCategories';
import './SearchPage.css';

const SEARCH_HISTORY_KEY = 'search-history';

const pageVariants = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
};

const highlightText = (text: string, keyword: string) => {
  if (!keyword.trim()) return text;
  const safeKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const regex = new RegExp(`(${safeKeyword})`, 'ig');
  return text.split(regex).map((part, index) =>
    index % 2 === 1 ? (
      <mark key={`${part}-${index}`} className="search-page__highlight">
        {part}
      </mark>
    ) : (
      part
    )
  );
};

export default function SearchPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const keyword = (searchParams.get('q') || '').trim();
  const { articles, isLoading, error } = useArticles({ keyword, size: 12 });
  const { tags } = useTags();
  const [searchHistory, setSearchHistory] = useState<string[]>([]);
  const [sortMode, setSortMode] = useState<'relevance' | 'latest'>('relevance');

  useEffect(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(SEARCH_HISTORY_KEY) || '[]') as string[];
      setSearchHistory(Array.isArray(saved) ? saved : []);
    } catch {
      setSearchHistory([]);
    }
  }, []);

  useEffect(() => {
    if (!keyword || keyword.length < 2) return;
    setSearchHistory(prev => {
      const next = [keyword, ...prev.filter(item => item.toLowerCase() !== keyword.toLowerCase())].slice(0, 8);
      localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next));
      return next;
    });
  }, [keyword]);

  const title = useMemo(() => {
    if (!keyword) return '搜索文章';
    return `“${keyword}” 的搜索结果`;
  }, [keyword]);

  const sortedArticles = useMemo(() => {
    if (sortMode === 'relevance') return articles;
    return [...articles].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }, [articles, sortMode]);

  const relatedKeywords = useMemo(() => {
    const counter = new Map<string, number>();
    sortedArticles.forEach(article => {
      article.tags?.forEach(tag => {
        const name = tag.name.trim();
        if (!name) return;
        if (keyword && name.toLowerCase() === keyword.toLowerCase()) return;
        counter.set(name, (counter.get(name) || 0) + 1);
      });
      const categoryName = article.category?.name?.trim();
      if (categoryName && (!keyword || categoryName.toLowerCase() !== keyword.toLowerCase())) {
        counter.set(categoryName, (counter.get(categoryName) || 0) + 1);
      }
    });

    return Array.from(counter.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 8)
      .map(([name]) => name);
  }, [sortedArticles, keyword]);

  const quickTags = useMemo(() => tags.slice(0, 10), [tags]);

  const runSearch = (query: string) => {
    const normalized = query.trim();
    if (!normalized) return;
    navigate(`/search?q=${encodeURIComponent(normalized)}`);
  };

  const clearHistory = () => {
    setSearchHistory([]);
    localStorage.removeItem(SEARCH_HISTORY_KEY);
  };

  return (
    <motion.div
      className="page-wrapper search-page"
      variants={pageVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      transition={{ duration: 0.45 }}
    >
      <header className="search-page__head">
        <h1 className="search-page__title">{title}</h1>
        <p className="search-page__meta">
          {keyword ? `共找到 ${articles.length} 篇匹配文章` : '输入关键词开始检索'}
        </p>
      </header>

      <section className="search-page__explore">
        <div className="search-page__explore-row">
          <div className="search-page__explore-label">最近搜索</div>
          {searchHistory.length > 0 && (
            <button className="search-page__clear" onClick={clearHistory}>清空</button>
          )}
        </div>
        {searchHistory.length === 0 ? (
          <p className="search-page__empty-tip">还没有历史记录，试试搜索 Java / React / Docker。</p>
        ) : (
          <div className="search-page__chips">
            {searchHistory.map(item => (
              <button key={item} className="search-page__chip liquid-btn" onClick={() => runSearch(item)}>{item}</button>
            ))}
          </div>
        )}

        <div className="search-page__explore-row search-page__explore-row--spaced">
          <div className="search-page__explore-label">热门标签</div>
          {keyword && (
            <div className="search-page__sort">
              <button
                className={`search-page__sort-btn ${sortMode === 'relevance' ? 'is-active' : ''}`}
                onClick={() => setSortMode('relevance')}
              >
                相关度
              </button>
              <button
                className={`search-page__sort-btn ${sortMode === 'latest' ? 'is-active' : ''}`}
                onClick={() => setSortMode('latest')}
              >
                最新发布
              </button>
            </div>
          )}
        </div>
        <div className="search-page__chips">
          {quickTags.map(tag => (
            <button key={tag.id} className="search-page__chip search-page__chip--tag liquid-btn" onClick={() => runSearch(tag.name)}>
              #{tag.name}
            </button>
          ))}
        </div>
      </section>

      {!keyword && (
        <div className="search-page__empty">
          <Search size={18} />
          <span>请在顶部搜索框输入关键词（支持 Ctrl/Cmd + K 快捷聚焦）</span>
        </div>
      )}

      {keyword && isLoading && (
        <div className="search-page__loading">
          <Loader2 className="loading-spinner" size={18} />
          <span>正在检索相关文章...</span>
        </div>
      )}

      {keyword && !isLoading && error && <div className="search-page__error">{error}</div>}

      {keyword && !isLoading && !error && sortedArticles.length === 0 && (
        <div className="search-page__empty">
          <Search size={18} />
          <span>没有匹配内容，试试换个关键词。</span>
        </div>
      )}

      {keyword && !isLoading && !error && sortedArticles.length > 0 && (
        <>
          {relatedKeywords.length > 0 && (
            <section className="search-page__related">
              <div className="search-page__related-title">同标签推荐</div>
              <div className="search-page__chips">
                {relatedKeywords.map(item => (
                  <button
                    key={item}
                    className="search-page__chip search-page__chip--related liquid-btn"
                    onClick={() => runSearch(item)}
                  >
                    {item}
                  </button>
                ))}
              </div>
            </section>
          )}

          <div className="search-page__list">
            {sortedArticles.map(article => (
              <button
                key={article.id}
                className="search-page__item liquid-btn"
                onClick={() => navigate(`/article/${article.id}`)}
              >
                <div className="search-page__item-title">{highlightText(article.title, keyword)}</div>
                <div className="search-page__item-summary">{highlightText(article.summary || '暂无摘要', keyword)}</div>
                <div className="search-page__item-footer">
                  <span>{article.category?.name || '未分类'}</span>
                  <span className="search-page__item-more">
                    阅读
                    <ArrowRight size={14} />
                  </span>
                </div>
              </button>
            ))}
          </div>
        </>
      )}
    </motion.div>
  );
}
