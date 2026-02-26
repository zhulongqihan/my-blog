import { motion } from 'framer-motion';
import ArticleCard from '../components/ArticleCard';
import Typewriter from '../components/Typewriter';
import { useArticles, useFeaturedArticles } from '../hooks/useArticles';
import { Loader2 } from 'lucide-react';
import './HomePage.css';

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

const HomePage = () => {
  const { articles: featuredArticles, isLoading: featuredLoading } = useFeaturedArticles();
  const { articles, isLoading, error, hasMore, loadMore } = useArticles({ size: 10 });

  const featuredArticle = featuredArticles[0];
  const regularArticles = articles.filter(a => !a.featured);

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
        </motion.div>

        <motion.div
          className="home-hero__divider"
          initial={{ scaleX: 0 }}
          animate={{ scaleX: 1 }}
          transition={{ duration: 0.8, delay: 0.6 }}
        />
      </section>

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
          <div className="loading-state">
            <Loader2 className="loading-spinner" size={24} />
            <span>加载中...</span>
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
                  category={article.category?.name}
                  tags={article.tags?.map(t => t.name)}
                  index={index + 1}
                />
              ))}
            </div>

            {hasMore && (
              <div className="load-more">
                <button onClick={loadMore} disabled={isLoading} className="load-more__btn">
                  {isLoading ? (
                    <>
                      <Loader2 className="loading-spinner" size={16} />
                      加载中...
                    </>
                  ) : (
                    '加载更多'
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
