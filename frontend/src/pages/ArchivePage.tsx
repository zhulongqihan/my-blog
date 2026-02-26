import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Calendar, FileText, Loader2 } from 'lucide-react';
import { articleApi } from '../services';
import type { ArchiveResponse } from '../types';
import './ArchivePage.css';

const pageVariants = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
};

const ArchivePage = () => {
  const [archive, setArchive] = useState<ArchiveResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchArchive = async () => {
      try {
        setIsLoading(true);
        const response = await articleApi.getArchive();
        setArchive(response.data);
      } catch (err) {
        setError('获取归档数据失败');
        console.error('Failed to fetch archive:', err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchArchive();
  }, []);

  return (
    <motion.div
      className="page-wrapper archive-page"
      variants={pageVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      transition={{ duration: 0.5 }}
    >
      {/* Header */}
      <header className="archive-header">
        <motion.h1
          className="archive-header__title"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          文章归档
        </motion.h1>
        <motion.p
          className="archive-header__count"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
        >
          <FileText size={16} strokeWidth={1.5} />共 {archive?.totalCount ?? 0} 篇文章
        </motion.p>
        <motion.div
          className="archive-header__divider"
          initial={{ scaleX: 0 }}
          animate={{ scaleX: 1 }}
          transition={{ duration: 0.8, delay: 0.5 }}
        />
      </header>

      {/* Content */}
      {isLoading ? (
        <div className="loading-state">
          <Loader2 className="loading-spinner" size={24} />
          <span>加载中...</span>
        </div>
      ) : error ? (
        <div className="error-state">
          <p>{error}</p>
          <p className="error-hint">请确保后端服务已启动</p>
        </div>
      ) : !archive || archive.years.length === 0 ? (
        <div className="empty-state">
          <p>暂无已发布的文章</p>
        </div>
      ) : (
        /* Timeline */
        <div className="archive-timeline">
          {archive.years.map((yearData, yearIndex) => (
            <motion.div
              key={yearData.year}
              className="archive-year"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.6 + yearIndex * 0.1 }}
            >
              <h2 className="archive-year__title">{yearData.year}</h2>

              {yearData.months.map(monthData => (
                <div key={monthData.month} className="archive-month">
                  <h3 className="archive-month__title">
                    <Calendar size={14} strokeWidth={1.5} />
                    {monthData.monthName}
                  </h3>

                  <ul className="archive-list">
                    {monthData.articles.map((article, articleIndex) => (
                      <motion.li
                        key={article.id}
                        className="archive-item"
                        initial={{ opacity: 0, x: -10 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 0.8 + articleIndex * 0.05 }}
                      >
                        <span className="archive-item__date">{article.date}</span>
                        <Link to={`/article/${article.id}`} className="archive-item__title">
                          {article.title}
                        </Link>
                      </motion.li>
                    ))}
                  </ul>
                </div>
              ))}
            </motion.div>
          ))}
        </div>
      )}
    </motion.div>
  );
};

export default ArchivePage;
