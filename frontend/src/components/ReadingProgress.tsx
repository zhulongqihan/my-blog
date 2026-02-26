import { motion, useScroll, useSpring } from 'framer-motion';
import { useLocation } from 'react-router-dom';
import './ReadingProgress.css';

const ReadingProgress = () => {
  const location = useLocation();
  const isArticlePage = location.pathname.startsWith('/article/');
  const { scrollYProgress } = useScroll();
  const scaleX = useSpring(scrollYProgress, {
    stiffness: 100,
    damping: 30,
    restDelta: 0.001,
  });

  if (!isArticlePage) return null;

  return (
    <motion.div
      className="reading-progress"
      style={{ scaleX }}
    />
  );
};

export default ReadingProgress;
