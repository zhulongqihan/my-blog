import { useRef, useState } from 'react';
import { motion, useMotionValue, useSpring, useTransform } from 'framer-motion';
import { Calendar, Clock, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import LazyImage from './LazyImage';
import './ArticleCard.css';

interface ArticleCardProps {
  id: number;
  title: string;
  summary: string;
  date: string;
  readTime: string;
  coverImage?: string;
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
  coverImage,
  category,
  tags,
  featured = false,
  index = 0,
}: ArticleCardProps) => {
  const cardRef = useRef<HTMLElement>(null);
  const [isHovered, setIsHovered] = useState(false);
  const mouseX = useMotionValue(0.5);
  const mouseY = useMotionValue(0.5);

  const tiltAmount = featured ? 6 : 5;
  const rotateX = useSpring(useTransform(mouseY, [0, 1], [tiltAmount, -tiltAmount]), { stiffness: 250, damping: 25 });
  const rotateY = useSpring(useTransform(mouseX, [0, 1], [-tiltAmount, tiltAmount]), { stiffness: 250, damping: 25 });

  // Glare effect - a bright spot that follows cursor
  const glareX = useTransform(mouseX, [0, 1], [0, 100]);
  const glareY = useTransform(mouseY, [0, 1], [0, 100]);
  const glareOpacity = useSpring(isHovered ? 0.12 : 0, { stiffness: 200, damping: 30 });

  const handleMouseMove = (e: React.MouseEvent) => {
    const card = cardRef.current;
    if (!card) return;
    const rect = card.getBoundingClientRect();
    mouseX.set((e.clientX - rect.left) / rect.width);
    mouseY.set((e.clientY - rect.top) / rect.height);
  };

  const handleMouseEnter = () => setIsHovered(true);
  const handleMouseLeave = () => {
    mouseX.set(0.5);
    mouseY.set(0.5);
    setIsHovered(false);
  };

  return (
    <motion.article
      ref={cardRef}
      className={`article-card ${featured ? 'article-card--featured' : ''} ${isHovered ? 'article-card--hovered' : ''}`}
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: index * 0.1 }}
      style={{ rotateX, rotateY, transformPerspective: 900 }}
      onMouseMove={handleMouseMove}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      {/* Glare overlay */}
      <motion.div
        className="article-card__glare"
        style={{
          background: useTransform(
            [glareX, glareY],
            ([x, y]) => `radial-gradient(circle at ${x}% ${y}%, rgba(255,255,255,0.35) 0%, transparent 60%)`
          ),
          opacity: glareOpacity,
        }}
      />
      <Link to={`/article/${id}`} className="article-card__link">
        <div className="article-card__content">
          {coverImage && (
            <LazyImage
              src={coverImage}
              alt={title}
              className="article-card__cover"
            />
          )}

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
