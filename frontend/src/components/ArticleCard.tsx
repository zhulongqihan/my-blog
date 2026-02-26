import { useRef } from 'react';
import { motion, useMotionValue, useSpring, useTransform } from 'framer-motion';
import { Calendar, Clock, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import './ArticleCard.css';

interface ArticleCardProps {
  id: number;
  title: string;
  summary: string;
  date: string;
  readTime: string;
  category?: string;
  tags?: string[];
  featured?: boolean;
  index?: number;
}

const ArticleCard = ({
  id,
  title,
  summary,
  date,
  readTime,
  category,
  tags,
  featured = false,
  index = 0,
}: ArticleCardProps) => {
  const cardRef = useRef<HTMLElement>(null);
  const mouseX = useMotionValue(0.5);
  const mouseY = useMotionValue(0.5);

  const rotateX = useSpring(useTransform(mouseY, [0, 1], [2, -2]), { stiffness: 300, damping: 30 });
  const rotateY = useSpring(useTransform(mouseX, [0, 1], [-2, 2]), { stiffness: 300, damping: 30 });

  const handleMouseMove = (e: React.MouseEvent) => {
    const card = cardRef.current;
    if (!card) return;
    const rect = card.getBoundingClientRect();
    mouseX.set((e.clientX - rect.left) / rect.width);
    mouseY.set((e.clientY - rect.top) / rect.height);
  };

  const handleMouseLeave = () => {
    mouseX.set(0.5);
    mouseY.set(0.5);
  };

  return (
    <motion.article
      ref={cardRef}
      className={`article-card ${featured ? 'article-card--featured' : ''}`}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: index * 0.1 }}
      style={{ rotateX, rotateY, transformPerspective: 800 }}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
    >
      <Link to={`/article/${id}`} className="article-card__link">
        <div className="article-card__content">
          {category && <span className="article-card__category">{category}</span>}

          <h2 className="article-card__title">{title}</h2>

          <p className="article-card__summary">{summary}</p>

          <div className="article-card__meta">
            <span className="article-card__meta-item">
              <Calendar size={14} strokeWidth={1.5} />
              {date}
            </span>
            <span className="article-card__meta-item">
              <Clock size={14} strokeWidth={1.5} />
              {readTime}
            </span>
          </div>

          {tags && tags.length > 0 && (
            <div className="article-card__tags">
              {tags.map(tag => (
                <span key={tag} className="article-card__tag">
                  {tag}
                </span>
              ))}
            </div>
          )}

          <span className="article-card__read-more">
            阅读全文
            <ArrowRight size={14} strokeWidth={1.5} />
          </span>
        </div>
      </Link>
    </motion.article>
  );
};

export default ArticleCard;
