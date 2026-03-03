import { useEffect, useMemo, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import ArticleCard from '../components/ArticleCard';
import Typewriter from '../components/Typewriter';
import AiIdeaLab from '../components/AiIdeaLab';
import ParticleBackground from '../components/ParticleBackground';
import { useArticles, useFeaturedArticles } from '../hooks/useArticles';
import { Loader2, RefreshCw, Shuffle, BookOpenCheck, ArrowRight } from 'lucide-react';
import './HomePage.css';

interface ReadingProgressEntry {
  articleId: number;
  title: string;
  percent: number;
  updatedAt: number;
  path: string;
}

const pageVariants = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
};

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
};

// 估算阅读时间
const estimateReadTime = (content: string) => {
  const wordsPerMinute = 300;
  const words = content?.length || 0;
  const minutes = Math.ceil(words / wordsPerMinute);
  return `${minutes} 分钟`;
};

const QUOTES = [
  '代码会过时，但思考方式会复利。',
  '先做能跑的，再做优雅的，最后做可维护的。',
  '复杂问题从拆分开始，优秀体验从细节开始。',
  '把重复的事情自动化，把时间留给创造。',
  '写给未来的你，也写给陌生的读者。',
];

const HomePage = () => {
  const navigate = useNavigate();
  const { articles: featuredArticles, isLoading: featuredLoading } = useFeaturedArticles();
  const { articles, isLoading, error, hasMore, loadMore } = useArticles({ size: 10 });
  const loadMoreRef = useRef<HTMLDivElement>(null);
  const [scrollY, setScrollY] = useState(0);
  const [quoteIndex, setQuoteIndex] = useState(0);
  const [isPickingRandom, setIsPickingRandom] = useState(false);
  const [rollingTitle, setRollingTitle] = useState('点击抽取一篇随缘文章');
  const [resumeEntry, setResumeEntry] = useState<ReadingProgressEntry | null>(null);

  const featuredArticle = featuredArticles[0];
  const regularArticles = articles.filter(a => !a.featured);

  useEffect(() => {
    const today = new Date().toISOString().slice(0, 10);
    const storageKey = 'daily-quote-index';
    const dateKey = 'daily-quote-date';
    const savedDate = localStorage.getItem(dateKey);
    const savedIndex = Number(localStorage.getItem(storageKey));

    if (savedDate === today && Number.isInteger(savedIndex) && savedIndex >= 0 && savedIndex < QUOTES.length) {
      setQuoteIndex(savedIndex);
      return;
    }

    const nextIndex = Math.floor(Math.random() * QUOTES.length);
    setQuoteIndex(nextIndex);
    localStorage.setItem(storageKey, String(nextIndex));
    localStorage.setItem(dateKey, today);
  }, []);

  useEffect(() => {
    const target = loadMoreRef.current;
    if (!target) return;

    const observer = new IntersectionObserver(
      entries => {
        if (entries[0].isIntersecting && hasMore && !isLoading) {
          loadMore();
        }
      },
      { rootMargin: '120px 0px' }
    );

    observer.observe(target);
    return () => observer.disconnect();
  }, [hasMore, isLoading, loadMore]);

  useEffect(() => {
    try {
      const raw = JSON.parse(localStorage.getItem('article-reading-progress-map') || '{}') as Record<string, ReadingProgressEntry>;
      const candidates = Object.values(raw).filter(item => item.percent > 0 && item.percent < 100);
      if (candidates.length === 0) {
        setResumeEntry(null);
        return;
      }
      const latest = candidates.sort((a, b) => b.updatedAt - a.updatedAt)[0];
      setResumeEntry(latest);
    } catch {
      setResumeEntry(null);
    }
  }, []);

  useEffect(() => {
    const onScroll = () => setScrollY(window.scrollY);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const quote = useMemo(() => QUOTES[quoteIndex], [quoteIndex]);

  const pickRandomArticle = () => {
    if (isPickingRandom || regularArticles.length === 0) return;

    setIsPickingRandom(true);
    const candidates = regularArticles;
    let tick = 0;
    const timer = window.setInterval(() => {
      tick += 1;
      const random = candidates[Math.floor(Math.random() * candidates.length)];
      setRollingTitle(random.title);
      if (tick >= 9) {
        window.clearInterval(timer);
        setIsPickingRandom(false);
        navigate(`/article/${random.id}`);
      }
    }, 90);
  };

  return (
    <motion.div
      className="page-wrapper home-page"
      variants={pageVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      transition={{ duration: 0.5 }}
    >
      {/* Hero Section */}
      <section className="home-hero">
        <ParticleBackground />
        <div className="home-hero__parallax home-hero__parallax--back" style={{ transform: `translateY(${scrollY * 0.12}px)` }} />
        <div className="home-hero__parallax home-hero__parallax--front" style={{ transform: `translateY(${scrollY * 0.2}px)` }} />

        <motion.div
          className="home-hero__content"
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, delay: 0.2 }}
        >
          <h1 className="home-hero__title">
            <Typewriter text="代码与思考的" delay={400} speed={120} />
            <span className="home-hero__title-accent">
              <Typewriter text="交汇之处" delay={1300} speed={150} />
            </span>
          </h1>
          <p className="home-hero__subtitle">
            关于编程、技术与生活的个人博客。在这里，我记录学习的点滴，分享技术的思考。
          </p>

          <div className="home-quote">
            <p className="home-quote__text">“{quote}”</p>
            <button className="home-quote__refresh liquid-btn" onClick={() => setQuoteIndex(prev => (prev + 1) % QUOTES.length)}>
              <RefreshCw size={14} />
              换一句
            </button>
          </div>

          <div className="home-random-pick">
            <p className="home-random-pick__text">{rollingTitle}</p>
            <button className="home-random-pick__btn liquid-btn" onClick={pickRandomArticle} disabled={isPickingRandom || regularArticles.length === 0}>
              <Shuffle size={14} />
              {isPickingRandom ? '抽取中...' : '随机文章推荐'}
            </button>
          </div>
        </motion.div>

        <motion.div
          className="home-hero__divider"
          initial={{ scaleX: 0 }}
          animate={{ scaleX: 1 }}
          transition={{ duration: 0.8, delay: 0.6 }}
        />
      </section>

      {resumeEntry && (
        <section className="home-resume">
          <div className="home-resume__icon">
            <BookOpenCheck size={16} />
          </div>
          <div className="home-resume__content">
            <p className="home-resume__label">继续阅读</p>
            <p className="home-resume__title">{resumeEntry.title}</p>
            <p className="home-resume__meta">已读 {resumeEntry.percent}% · 最近阅读 {new Date(resumeEntry.updatedAt).toLocaleString('zh-CN')}</p>
          </div>
          <button className="home-resume__btn liquid-btn" onClick={() => navigate(resumeEntry.path)}>
            继续
            <ArrowRight size={14} />
          </button>
        </section>
      )}

      <AiIdeaLab />

      {/* Featured Article */}
      {!featuredLoading && featuredArticle && (
        <section className="home-featured">
          <h2 className="section-title">
            <span className="section-title__text">精选文章</span>
            <span className="section-title__line"></span>
          </h2>
          <ArticleCard
            id={featuredArticle.id}
            title={featuredArticle.title}
            summary={featuredArticle.summary || ''}
            date={formatDate(featuredArticle.createdAt)}
            readTime={estimateReadTime(featuredArticle.content)}
            coverImage={featuredArticle.coverImage}
            category={featuredArticle.category?.name}
            tags={featuredArticle.tags?.map(t => t.name)}
            featured={true}
            index={0}
          />
        </section>
      )}

      {/* Recent Articles */}
      <section className="home-recent">
        <h2 className="section-title">
          <span className="section-title__text">最新文章</span>
          <span className="section-title__line"></span>
        </h2>

        {isLoading && articles.length === 0 ? (
          <div className="home-skeleton">
            {Array.from({ length: 4 }).map((_, idx) => (
              <div key={idx} className="home-skeleton__item">
                <div className="home-skeleton__title" />
                <div className="home-skeleton__line" />
                <div className="home-skeleton__line home-skeleton__line--short" />
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="error-state">
            <p>{error}</p>
            <p className="error-hint">请确保后端服务已启动</p>
          </div>
        ) : regularArticles.length === 0 ? (
          <div className="empty-state">
            <p>暂无文章</p>
          </div>
        ) : (
          <>
            <div className="home-recent__list">
              {regularArticles.map((article, index) => (
                <ArticleCard
                  key={article.id}
                  id={article.id}
                  title={article.title}
                  summary={article.summary || ''}
                  date={formatDate(article.createdAt)}
                  readTime={estimateReadTime(article.content)}
                  coverImage={article.coverImage}
                  category={article.category?.name}
                  tags={article.tags?.map(t => t.name)}
                  index={index + 1}
                />
              ))}
            </div>

            {hasMore && (
              <div className="load-more" ref={loadMoreRef}>
                <button onClick={loadMore} disabled={isLoading} className="load-more__btn liquid-btn">
                  {isLoading ? (
                    <>
                      <Loader2 className="loading-spinner" size={16} />
                      正在加载更多...
                    </>
                  ) : (
                    '向下滚动自动加载 / 点击加载更多'
                  )}
                </button>
              </div>
            )}
          </>
        )}
      </section>
    </motion.div>
  );
};

export default HomePage;
