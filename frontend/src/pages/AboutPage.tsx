import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Bot, Code2, Coffee, BookOpen, ExternalLink, GraduationCap, Github, Mail } from 'lucide-react';
import AnimatedCounter from '../components/AnimatedCounter';
import { AI_AGENT_NAME, AI_AGENT_SUMMARY, AI_AGENT_URL } from '../constants/externalLinks';
import { articleApi } from '../services';
import { PROFILE_SKILLS, PROFILE_STATS } from '../config/profile';
import { useTags } from '../hooks/useCategories';
import './AboutPage.css';

const AI_AGENT_STACK = ['Java 17', 'Spring Boot', 'Spring AI Alibaba', 'DashScope', 'Milvus'];

const pageVariants = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
};

const AboutPage = () => {
  const [articleCount, setArticleCount] = useState(0);
  const { tags } = useTags();

  useEffect(() => {
    const loadStats = async () => {
      try {
        const archive = await articleApi.getArchive();
        setArticleCount(archive.data.totalCount || 0);
      } catch {
        setArticleCount(0);
      }
    };

    loadStats();
  }, []);

  return (
    <motion.div
      className="page-wrapper about-page"
      variants={pageVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      transition={{ duration: 0.5 }}
    >
      {/* Header */}
      <header className="about-header">
        <motion.h1
          className="about-header__title"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          关于我
        </motion.h1>
        <motion.div
          className="about-header__divider"
          initial={{ scaleX: 0 }}
          animate={{ scaleX: 1 }}
          transition={{ duration: 0.8, delay: 0.4 }}
        />
      </header>

      {/* Intro */}
      <motion.section
        className="about-intro"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
      >
        <div className="about-intro__avatar">
          <div className="about-intro__avatar-placeholder">
            <Code2 size={40} strokeWidth={1} />
          </div>
        </div>
        <div className="about-intro__content">
          <p className="about-intro__greeting">Hello, World! 👋</p>
          <p className="about-intro__text">
            我是张昌宇，南京大学软件工程硕士在读，本科毕业于北京邮电大学计算机科学与技术专业。
            目前担任班长，热爱全栈开发，专注于 Java 后端与现代前端技术。
          </p>
          <p className="about-intro__text">
            这个博客是我的技术实验场 —— 从 Spring Boot 后端到 React 前端，
            从 Docker 容器化到 WebSocket 实时通信，每一行代码都是学习的印记。
            相信代码不仅是工具，更是表达思想的艺术。
          </p>
        </div>
      </motion.section>

      {/* Stats */}
      <motion.section
        className="about-stats"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6 }}
      >
        <div className="about-stat">
          <Coffee size={24} strokeWidth={1.5} className="about-stat__icon" />
          <AnimatedCounter target={PROFILE_STATS.coffeeCount} suffix="+" className="about-stat__value" />
          <span className="about-stat__label">杯咖啡</span>
        </div>
        <div className="about-stat">
          <BookOpen size={24} strokeWidth={1.5} className="about-stat__icon" />
          <AnimatedCounter target={articleCount} suffix="+" className="about-stat__value" />
          <span className="about-stat__label">篇文章</span>
        </div>
        <div className="about-stat">
          <GraduationCap size={24} strokeWidth={1.5} className="about-stat__icon" />
          <AnimatedCounter target={PROFILE_STATS.projectCount} suffix="+" duration={1500} className="about-stat__value" />
          <span className="about-stat__label">个项目</span>
        </div>
      </motion.section>

      {/* Skills */}
      <motion.section
        className="about-skills"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.7 }}
      >
        <h2 className="about-section-title">技术栈</h2>
        <div className="about-skills__list">
          {PROFILE_SKILLS.map((skill, index) => (
            <motion.div
              key={skill.name}
              className="about-skill"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.8 + index * 0.1 }}
            >
              <div className="about-skill__header">
                <span className="about-skill__name">{skill.name}</span>
                <span className="about-skill__level">{skill.level}%</span>
              </div>
              <div className="about-skill__bar">
                <motion.div
                  className="about-skill__progress"
                  initial={{ width: 0 }}
                  animate={{ width: `${skill.level}%` }}
                  transition={{ duration: 1, delay: 1 + index * 0.1 }}
                />
              </div>
            </motion.div>
          ))}
        </div>
      </motion.section>

      <motion.section
        className="about-tags"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.9 }}
      >
        <h2 className="about-section-title">标签云</h2>
        <div className="about-tags__cloud">
          {tags.slice(0, 32).map((tag, index) => {
            const level = 0.8 + ((index % 6) * 0.08);
            return (
              <motion.span
                key={tag.id}
                className="about-tags__item"
                style={{
                  fontSize: `${level}rem`,
                  color: index % 2 === 0 ? 'var(--accent-rust)' : 'var(--accent-olive)',
                }}
                initial={{ opacity: 0, y: 8, scale: 0.9 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ delay: 1 + index * 0.03 }}
              >
                #{tag.name}
              </motion.span>
            );
          })}
        </div>
      </motion.section>

      {/* Contact */}
      <motion.section
        className="about-projects"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.95 }}
      >
        <h2 className="about-section-title">相关项目</h2>
        <div className="about-project-card">
          <div className="about-project-card__header">
            <div className="about-project-card__title-wrap">
              <span className="about-project-card__badge">AI Agent</span>
              <h3 className="about-project-card__title">{AI_AGENT_NAME}</h3>
            </div>
            <Bot size={22} strokeWidth={1.6} className="about-project-card__icon" />
          </div>
          <p className="about-project-card__text">
            {AI_AGENT_SUMMARY}
            它和博客里的 AI、部署、工程化相关文章是一条连续的内容线：文章负责把思考写清楚，项目负责把这些思考真正跑起来。
          </p>
          <div className="about-project-card__tags">
            {AI_AGENT_STACK.map(item => (
              <span key={item} className="about-project-card__tag">{item}</span>
            ))}
          </div>
          <a
            href={AI_AGENT_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="about-project-card__link"
          >
            <ExternalLink size={16} />
            <span>访问在线演示</span>
          </a>
        </div>
      </motion.section>

      <motion.section
        className="about-contact"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 1 }}
      >
        <h2 className="about-section-title">联系方式</h2>
        <p className="about-contact__text">如果你有任何问题或想法，欢迎通过以下方式联系我：</p>
        <div className="about-contact__info">
          <a href="mailto:522025320209@smail.nju.edu.cn" className="about-contact__link">
            <Mail size={16} /> 522025320209@smail.nju.edu.cn
          </a>
          <a href="https://github.com/zhulongqihan" target="_blank" rel="noopener noreferrer" className="about-contact__link">
            <Github size={16} /> github.com/zhulongqihan
          </a>
        </div>
      </motion.section>
    </motion.div>
  );
};

export default AboutPage;
