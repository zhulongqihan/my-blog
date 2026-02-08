import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Calendar, FileText } from 'lucide-react';
import './ArchivePage.css';

// Mock archive data
const archiveData = [
  {
    year: '2026',
    months: [
      {
        month: '一月',
        articles: [
          { id: 1, title: '使用 Spring Boot 3 构建现代化的 RESTful API', date: '01-25' },
          { id: 2, title: 'React 状态管理的演进：从 Redux 到 Zustand', date: '01-20' },
          { id: 3, title: '深入理解 JVM 垃圾回收机制', date: '01-15' },
          { id: 4, title: '写代码之外：程序员的软技能修炼', date: '01-10' },
          { id: 5, title: 'Docker 容器化部署实战指南', date: '01-05' },
        ],
      },
    ],
  },
  {
    year: '2025',
    months: [
      {
        month: '十二月',
        articles: [
          { id: 6, title: '2025 年度技术总结与展望', date: '12-31' },
          { id: 7, title: 'TypeScript 5.0 新特性详解', date: '12-20' },
          { id: 8, title: '微服务架构实践：从单体到分布式', date: '12-10' },
        ],
      },
      {
        month: '十一月',
        articles: [
          { id: 9, title: 'Git 进阶：分支管理与工作流', date: '11-25' },
          { id: 10, title: 'PostgreSQL 性能调优实战', date: '11-15' },
        ],
      },
    ],
  },
];

const pageVariants = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
};

const ArchivePage = () => {
  const totalArticles = archiveData.reduce(
    (acc, year) =>
      acc + year.months.reduce((monthAcc, month) => monthAcc + month.articles.length, 0),
    0
  );

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
          <FileText size={16} strokeWidth={1.5} />共 {totalArticles} 篇文章
        </motion.p>
        <motion.div
          className="archive-header__divider"
          initial={{ scaleX: 0 }}
          animate={{ scaleX: 1 }}
          transition={{ duration: 0.8, delay: 0.5 }}
        />
      </header>

      {/* Timeline */}
      <div className="archive-timeline">
        {archiveData.map((yearData, yearIndex) => (
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
                  {monthData.month}
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
    </motion.div>
  );
};

export default ArchivePage;
