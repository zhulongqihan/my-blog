import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Calendar, Clock, ArrowLeft, Tag, Loader2 } from 'lucide-react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { tomorrow } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useArticle } from '../hooks/useArticles';
import './ArticlePage.css';

const pageVariants = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
};

// Custom code block style matching our theme
const codeStyle = {
  ...tomorrow,
  'pre[class*="language-"]': {
    ...tomorrow['pre[class*="language-"]'],
    background: '#2D2D2D',
    borderRadius: '4px',
    padding: '1.5rem',
    margin: '1.5rem 0',
    fontSize: '0.875rem',
    lineHeight: '1.7',
  },
  'code[class*="language-"]': {
    ...tomorrow['code[class*="language-"]'],
    background: 'transparent',
    fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
  },
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

// Parse markdown-like content to JSX
const parseContent = (content: string) => {
  if (!content) return null;

  const parts = content.split(/(```[\s\S]*?```)/g);

  return parts.map((part, index) => {
    // Code blocks
    if (part.startsWith('```')) {
      const match = part.match(/```(\w+)?\n([\s\S]*?)```/);
      if (match) {
        const [, language = 'text', code] = match;
        return (
          <SyntaxHighlighter
            key={index}
            language={language}
            style={codeStyle}
            showLineNumbers={true}
            lineNumberStyle={{ color: '#666', paddingRight: '1rem' }}
          >
            {code.trim()}
          </SyntaxHighlighter>
        );
      }
    }

    // Process regular text
    const lines = part.split('\n');
    return lines.map((line, lineIndex) => {
      const key = `${index}-${lineIndex}`;

      // Headings
      if (line.startsWith('## ')) {
        return (
          <h2 key={key} className="article-content__h2">
            {line.slice(3)}
          </h2>
        );
      }
      if (line.startsWith('### ')) {
        return (
          <h3 key={key} className="article-content__h3">
            {line.slice(4)}
          </h3>
        );
      }

      // List items
      if (line.startsWith('- ')) {
        const processedLine = line
          .slice(2)
          .split(/(`[^`]+`)/)
          .map((segment, i) => {
            if (segment.startsWith('`') && segment.endsWith('`')) {
              return (
                <code key={i} className="inline-code">
                  {segment.slice(1, -1)}
                </code>
              );
            }
            return segment;
          });
        return (
          <li key={key} className="article-content__li">
            {processedLine}
          </li>
        );
      }

      // Regular paragraph
      if (line.trim()) {
        const processedLine = line.split(/(`[^`]+`)/).map((segment, i) => {
          if (segment.startsWith('`') && segment.endsWith('`')) {
            return (
              <code key={i} className="inline-code">
                {segment.slice(1, -1)}
              </code>
            );
          }
          return segment;
        });
        return (
          <p key={key} className="article-content__p">
            {processedLine}
          </p>
        );
      }

      return null;
    });
  });
};

const ArticlePage = () => {
  const { id } = useParams();
  const { article, isLoading, error } = useArticle(Number(id));

  if (isLoading) {
    return (
      <motion.div
        className="page-wrapper article-page"
        variants={pageVariants}
        initial="initial"
        animate="animate"
        exit="exit"
      >
        <Link to="/" className="article-page__back">
          <ArrowLeft size={16} strokeWidth={1.5} />
          返回首页
        </Link>
        <div className="loading-state">
          <Loader2 className="loading-spinner" size={24} />
          <span>加载中...</span>
        </div>
      </motion.div>
    );
  }

  if (error || !article) {
    return (
      <motion.div
        className="page-wrapper article-page"
        variants={pageVariants}
        initial="initial"
        animate="animate"
        exit="exit"
      >
        <Link to="/" className="article-page__back">
          <ArrowLeft size={16} strokeWidth={1.5} />
          返回首页
        </Link>
        <div className="error-state">
          <p>{error || '文章不存在'}</p>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div
      className="page-wrapper article-page"
      variants={pageVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      transition={{ duration: 0.5 }}
    >
      {/* Back Link */}
      <Link to="/" className="article-page__back">
        <ArrowLeft size={16} strokeWidth={1.5} />
        返回首页
      </Link>

      {/* Article Header */}
      <header className="article-header">
        {article.category && (
          <motion.span
            className="article-header__category"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
          >
            {article.category.name}
          </motion.span>
        )}

        <motion.h1
          className="article-header__title"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          {article.title}
        </motion.h1>

        <motion.div
          className="article-header__meta"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
        >
          <span className="article-header__meta-item">
            <Calendar size={14} strokeWidth={1.5} />
            {formatDate(article.createdAt)}
          </span>
          <span className="article-header__meta-item">
            <Clock size={14} strokeWidth={1.5} />
            {estimateReadTime(article.content)}
          </span>
        </motion.div>

        {article.tags && article.tags.length > 0 && (
          <motion.div
            className="article-header__tags"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.5 }}
          >
            {article.tags.map(tag => (
              <span key={tag.id} className="article-header__tag">
                <Tag size={12} strokeWidth={1.5} />
                {tag.name}
              </span>
            ))}
          </motion.div>
        )}
      </header>

      {/* Divider */}
      <motion.div
        className="article-divider"
        initial={{ scaleX: 0 }}
        animate={{ scaleX: 1 }}
        transition={{ duration: 0.8, delay: 0.6 }}
      />

      {/* Article Content */}
      <motion.article
        className="article-content"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.7 }}
      >
        {parseContent(article.content)}
      </motion.article>

      {/* Article Footer */}
      <footer className="article-footer">
        <div className="article-footer__divider"></div>
        <p className="article-footer__thanks">感谢阅读 ✦</p>
      </footer>
    </motion.div>
  );
};

export default ArticlePage;
