import { motion } from 'framer-motion';
import { Code2, Coffee, BookOpen, Briefcase } from 'lucide-react';
import AnimatedCounter from '../components/AnimatedCounter';
import './AboutPage.css';

const pageVariants = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
};

const skills = [
  { name: 'Java / Spring Boot', level: 90 },
  { name: 'React / TypeScript', level: 85 },
  { name: 'MySQL / PostgreSQL', level: 80 },
  { name: 'Docker / K8s', level: 70 },
];

const AboutPage = () => {
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
            我是一名热爱编程的全栈开发者，专注于 Java 后端开发和现代前端技术。
            工作之余，我喜欢在这个博客记录学习心得、分享技术见解。
          </p>
          <p className="about-intro__text">
            相信代码不仅仅是工具，更是一种表达思想的艺术。
            在追求技术深度的同时，我也注重代码的优雅与可维护性。
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
          <AnimatedCounter target={1000} suffix="+" className="about-stat__value" />
          <span className="about-stat__label">杯咖啡</span>
        </div>
        <div className="about-stat">
          <BookOpen size={24} strokeWidth={1.5} className="about-stat__icon" />
          <AnimatedCounter target={50} suffix="+" className="about-stat__value" />
          <span className="about-stat__label">篇文章</span>
        </div>
        <div className="about-stat">
          <Briefcase size={24} strokeWidth={1.5} className="about-stat__icon" />
          <AnimatedCounter target={5} suffix="+" duration={1500} className="about-stat__value" />
          <span className="about-stat__label">年经验</span>
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
          {skills.map((skill, index) => (
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

      {/* Contact */}
      <motion.section
        className="about-contact"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 1 }}
      >
        <h2 className="about-section-title">联系方式</h2>
        <p className="about-contact__text">如果你有任何问题或想法，欢迎通过以下方式联系我：</p>
        <div className="about-contact__info">
          <code className="about-contact__email">hello@myblog.com</code>
        </div>
      </motion.section>
    </motion.div>
  );
};

export default AboutPage;
