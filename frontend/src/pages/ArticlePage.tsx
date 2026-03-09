import { useEffect, useMemo, useState, useCallback, useRef, type CSSProperties } from 'react';
import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Calendar, Clock, ArrowLeft, Tag, Loader2, BookOpen, Minimize2, Copy, Check, Image as ImageIcon, X, ChevronLeft, ChevronRight, Share2, Heart, Type, RotateCcw, Timer, Play, Pause, RefreshCw, ChevronDown, ChevronRight as ChevronRightSmall, Bookmark } from 'lucide-react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { tomorrow } from 'react-syntax-highlighter/dist/esm/styles/prism';
import QRCode from 'qrcode';
import { useArticle } from '../hooks/useArticles';
import { articleApi } from '../services';
import { isBookmarked as checkBookmarked, toggleBookmark } from '../services/bookmarks';
import { unlockGlobalAchievement } from '../components/AchievementHub';
import ProjectBridge from '../components/ProjectBridge';
import type { Article } from '../types';
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

const slugifyHeading = (text: string, index: number) => {
  const normalized = text
    .toLowerCase()
    .trim()
    .replace(/[\s\W]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return `h-${normalized || 'section'}-${index}`;
};

const extractHeadings = (content: string) => {
  if (!content) return [] as Array<{ level: 2 | 3; text: string; id: string }>;
  const lines = content.split('\n');
  const headings: Array<{ level: 2 | 3; text: string; id: string }> = [];

  lines.forEach((line, index) => {
    if (line.startsWith('## ')) {
      const text = line.slice(3).trim();
      headings.push({ level: 2, text, id: slugifyHeading(text, index) });
    }
    if (line.startsWith('### ')) {
      const text = line.slice(4).trim();
      headings.push({ level: 3, text, id: slugifyHeading(text, index) });
    }
  });

  return headings;
};

const extractImageSources = (content: string) => {
  const matches = Array.from(content.matchAll(/!\[[^\]]*\]\(([^)\s]+)(?:\s+"[^"]*")?\)/g));
  return matches.map(match => match[1]);
};

interface ReadingProgressEntry {
  articleId: number;
  title: string;
  percent: number;
  updatedAt: number;
  path: string;
}

// Parse markdown-like content to JSX
const parseContent = (
  content: string,
  onCopyCode: (code: string, key: string) => void,
  copiedCodeKey: string | null,
  headingIdMap: Map<string, string>,
  onOpenImage: (src: string) => void,
  isCodeCollapsed: (key: string) => boolean,
  onToggleCodeCollapse: (key: string) => void
) => {
  if (!content) return null;

  const parts = content.split(/(```[\s\S]*?```)/g);

  return parts.map((part, index) => {
    // Code blocks
    if (part.startsWith('```')) {
      const match = part.match(/```(\w+)?\n([\s\S]*?)```/);
      if (match) {
        const [, language = 'text', code] = match;
        const codeKey = `code-${index}`;
        const collapsed = isCodeCollapsed(codeKey);
        return (
          <div key={index} className="article-content__code-wrap">
            <div className="article-content__code-toolbar">
              <div className="article-content__code-actions">
                <span className="article-content__code-lang">{language}</span>
                <button
                  className="article-content__code-collapse"
                  onClick={() => onToggleCodeCollapse(codeKey)}
                >
                  {collapsed ? <ChevronRightSmall size={13} /> : <ChevronDown size={13} />}
                  {collapsed ? '展开' : '折叠'}
                </button>
              </div>
              <button
                className="article-content__code-copy"
                onClick={() => onCopyCode(code.trim(), codeKey)}
              >
                {copiedCodeKey === codeKey ? (
                  <>
                    <Check size={13} /> 已复制
                  </>
                ) : (
                  <>
                    <Copy size={13} /> 复制
                  </>
                )}
              </button>
            </div>
            {!collapsed && (
              <SyntaxHighlighter
                language={language}
                style={codeStyle}
                showLineNumbers={true}
                lineNumberStyle={{ color: '#666', paddingRight: '1rem' }}
              >
                {code.trim()}
              </SyntaxHighlighter>
            )}
          </div>
        );
      }
    }

    // Process regular text
    const lines = part.split('\n');
    return lines.map((line, lineIndex) => {
      const key = `${index}-${lineIndex}`;

      // Headings
      if (line.startsWith('## ')) {
        const text = line.slice(3);
        return (
          <h2 key={key} id={headingIdMap.get(`## ${text}`) || undefined} className="article-content__h2">
            {text}
          </h2>
        );
      }

      // Markdown image: ![alt](url)
      const imageMatch = line.match(/^!\[([^\]]*)\]\(([^)\s]+)(?:\s+"([^"]*)")?\)$/);
      if (imageMatch) {
        const [, altText, src] = imageMatch;
        return (
          <figure key={key} className="article-content__image-wrap">
            <button className="article-content__image-btn" onClick={() => onOpenImage(src)}>
              <img src={src} alt={altText || '文章配图'} className="article-content__image" loading="lazy" />
              <span className="article-content__image-hint"><ImageIcon size={14} /> 点击查看大图</span>
            </button>
          </figure>
        );
      }
      if (line.startsWith('### ')) {
        const text = line.slice(4);
        return (
          <h3 key={key} id={headingIdMap.get(`### ${text}`) || undefined} className="article-content__h3">
            {text}
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
  const [isReadingMode, setIsReadingMode] = useState(() => {
    return localStorage.getItem('article-reading-mode') === 'true';
  });
  const [copiedCodeKey, setCopiedCodeKey] = useState<string | null>(null);
  const [activeHeadingId, setActiveHeadingId] = useState<string>('');
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  const [isExporting, setIsExporting] = useState(false);
  const [scrollY, setScrollY] = useState(0);
  const [fontScale, setFontScale] = useState(1);
  const [likedCount, setLikedCount] = useState(0);
  const [isLiked, setIsLiked] = useState(false);
  const [isLikeLoading, setIsLikeLoading] = useState(false);
  const [isBookmarkedState, setIsBookmarkedState] = useState(false);
  const [likeBursts, setLikeBursts] = useState<Array<{ id: number; x: number; y: number }>>([]);
  const [achievementToast, setAchievementToast] = useState('');
  const [readingProgress, setReadingProgress] = useState(0);
  const [unlockedAchievements, setUnlockedAchievements] = useState<string[]>([]);
  const [resumePosition, setResumePosition] = useState<number | null>(null);
  const [collapsedCodeBlocks, setCollapsedCodeBlocks] = useState<Record<string, boolean>>({});
  const [isFocusRunning, setIsFocusRunning] = useState(false);
  const [focusSeconds, setFocusSeconds] = useState(25 * 60);
  const [focusPreset, setFocusPreset] = useState(25);
  const [focusToast, setFocusToast] = useState('');
  const [relatedArticles, setRelatedArticles] = useState<Article[]>([]);
  const pageEnteredAt = useRef(Date.now());
  const tocJumpCount = useRef(0);

  const headings = useMemo(() => extractHeadings(article?.content || ''), [article?.content]);
  const imageSources = useMemo(() => extractImageSources(article?.content || ''), [article?.content]);
  const headingIdMap = useMemo(() => {
    const map = new Map<string, string>();
    headings.forEach(item => {
      const key = `${item.level === 2 ? '##' : '###'} ${item.text}`;
      map.set(key, item.id);
    });
    return map;
  }, [headings]);

  // 加载文章后初始化点赞状态
  useEffect(() => {
    if (!article?.id) return;
    setLikedCount(article.likeCount ?? 0);
    articleApi.getLikeStatus(article.id).then(res => {
      const data = (res as unknown as { data: { liked: boolean; likeCount: number } }).data;
      if (data) {
        setIsLiked(data.liked);
        setLikedCount(data.likeCount);
      }
    }).catch(() => { /* 静默失败 */ });

    // 初始化书签状态
    setIsBookmarkedState(checkBookmarked(article.id));
  }, [article?.id, article?.likeCount]);

  // 加载文章后恢复阅读进度
  useEffect(() => {
    if (!article?.id) return;
    const progressMap = JSON.parse(localStorage.getItem('article-reading-progress-map') || '{}') as Record<string, ReadingProgressEntry>;
    const saved = progressMap[String(article.id)];
    if (saved?.percent > 0) {
      setReadingProgress(saved.percent);
    }
  }, [article?.id]);

  useEffect(() => {
    localStorage.setItem('article-reading-mode', String(isReadingMode));
  }, [isReadingMode]);

  useEffect(() => {
    const savedScale = Number(localStorage.getItem('article-font-scale') || '1');
    setFontScale([0, 1, 2].includes(savedScale) ? savedScale : 1);
  }, []);

  useEffect(() => {
    if (!article?.id) return;
    const key = `article-achievements-${article.id}`;
    try {
      const saved = JSON.parse(localStorage.getItem(key) || '[]') as string[];
      setUnlockedAchievements(Array.isArray(saved) ? saved : []);
    } catch {
      setUnlockedAchievements([]);
    }
  }, [article?.id]);

  useEffect(() => {
    if (!article?.id) return;
    localStorage.setItem(`article-achievements-${article.id}`, JSON.stringify(unlockedAchievements));
  }, [article?.id, unlockedAchievements]);

  useEffect(() => {
    localStorage.setItem('article-font-scale', String(fontScale));
  }, [fontScale]);

  useEffect(() => {
    if (!article?.id) return;
    const saved = Number(localStorage.getItem(`article-last-position-${article.id}`) || '0');
    setResumePosition(saved > 220 ? saved : null);
  }, [article?.id]);

  useEffect(() => {
    const onScroll = () => {
      setScrollY(window.scrollY);

      if (article?.id) {
        localStorage.setItem(`article-last-position-${article.id}`, String(window.scrollY));
      }

      const content = document.querySelector('.article-content') as HTMLElement | null;
      if (!content) return;

      const viewportHeight = window.innerHeight;
      const rect = content.getBoundingClientRect();
      const total = Math.max(content.scrollHeight - viewportHeight * 0.55, 1);
      const viewed = Math.min(Math.max(-rect.top + viewportHeight * 0.25, 0), total);
      const percent = Math.round((viewed / total) * 100);
      setReadingProgress(percent);
    };

    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
    return () => window.removeEventListener('scroll', onScroll);
  }, [article?.id]);

  useEffect(() => {
    if (!article?.id) return;
    const progressMap = JSON.parse(localStorage.getItem('article-reading-progress-map') || '{}') as Record<string, ReadingProgressEntry>;
    progressMap[String(article.id)] = {
      articleId: article.id,
      title: article.title,
      percent: Math.min(Math.max(readingProgress, 0), 100),
      updatedAt: Date.now(),
      path: `/article/${article.id}`,
    };
    localStorage.setItem('article-reading-progress-map', JSON.stringify(progressMap));
  }, [article?.id, article?.title, readingProgress]);

  useEffect(() => {
    if (!article?.id) return;
    const loadRelatedArticles = async () => {
      try {
        if (article.category?.id) {
          const response = await articleApi.getByCategory(article.category.id, 0, 6);
          const filtered = (response.data.content || []).filter(item => item.id !== article.id).slice(0, 3);
          if (filtered.length > 0) {
            setRelatedArticles(filtered);
            return;
          }
        }

        const fallback = await articleApi.getList(0, 6);
        setRelatedArticles((fallback.data.content || []).filter(item => item.id !== article.id).slice(0, 3));
      } catch {
        setRelatedArticles([]);
      }
    };

    loadRelatedArticles();
  }, [article?.id, article?.category?.id]);

  useEffect(() => {
    if (!article?.id) return;

    const milestones = [
      { key: 'starter', threshold: 25, title: '📖 初出茅庐：已阅读 25%' },
      { key: 'focus', threshold: 60, title: '🎯 渐入佳境：已阅读 60%' },
      { key: 'finisher', threshold: 100, title: '🏆 一字不漏：已完整读完' },
    ];

    milestones.forEach(milestone => {
      if (readingProgress >= milestone.threshold && !unlockedAchievements.includes(milestone.key)) {
        setUnlockedAchievements(prev => [...prev, milestone.key]);
        unlockGlobalAchievement(milestone.key);
        setAchievementToast(milestone.title);
        window.setTimeout(() => setAchievementToast(''), 2200);
      }
    });

    // 计时成就
    const elapsedMs = Date.now() - pageEnteredAt.current;
    if (readingProgress >= 100 && elapsedMs < 3 * 60 * 1000 && !unlockedAchievements.includes('speed-reader')) {
      setUnlockedAchievements(prev => [...prev, 'speed-reader']);
      unlockGlobalAchievement('speed-reader');
      setAchievementToast('⚡ 一目十行：3 分钟内读完');
      window.setTimeout(() => setAchievementToast(''), 2200);
    }
    if (elapsedMs > 10 * 60 * 1000 && !unlockedAchievements.includes('marathon')) {
      setUnlockedAchievements(prev => [...prev, 'marathon']);
      unlockGlobalAchievement('marathon');
      setAchievementToast('⏱️ 阅读马拉松：阅读超过 10 分钟');
      window.setTimeout(() => setAchievementToast(''), 2200);
    }

    // 探索类成就：文章数量
    const progressMap = JSON.parse(localStorage.getItem('article-reading-progress-map') || '{}');
    const readArticleCount = Object.keys(progressMap).length;
    if (readArticleCount >= 3) unlockGlobalAchievement('explorer');
    if (readArticleCount >= 5) unlockGlobalAchievement('collector');

    // 完美主义者：所有阅读成就
    const readingKeys = ['starter', 'focus', 'finisher', 'speed-reader', 'marathon', 'deep-reader'];
    if (readingKeys.every(k => unlockedAchievements.includes(k))) {
      unlockGlobalAchievement('perfectionist');
    }
  }, [readingProgress, unlockedAchievements, article?.id]);

  // 时间段成就 + 首次访问
  useEffect(() => {
    const hour = new Date().getHours();
    if (hour >= 0 && hour < 5) unlockGlobalAchievement('night-owl');
    if (hour >= 5 && hour < 7) unlockGlobalAchievement('early-bird');
    unlockGlobalAchievement('first-visit');
  }, []);

  useEffect(() => {
    if (headings.length === 0) return;
    const observer = new IntersectionObserver(
      entries => {
        const visible = entries
          .filter(entry => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (visible[0]) {
          setActiveHeadingId(visible[0].target.id);
        }
      },
      { rootMargin: '-120px 0px -70% 0px', threshold: [0, 1] }
    );

    headings.forEach(item => {
      const el = document.getElementById(item.id);
      if (el) observer.observe(el);
    });

    return () => observer.disconnect();
  }, [headings, article?.content]);

  const scrollToHeading = (id: string) => {
    const element = document.getElementById(id);
    if (!element) return;
    element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    tocJumpCount.current += 1;
    if (tocJumpCount.current >= 3) unlockGlobalAchievement('toc-navigator');
  };

  const handleCopyCode = async (code: string, key: string) => {
    try {
      await navigator.clipboard.writeText(code);
      setCopiedCodeKey(key);
      window.setTimeout(() => setCopiedCodeKey(null), 1400);
    } catch {
      setCopiedCodeKey(null);
    }
  };

  const openImage = (src: string) => {
    const index = imageSources.indexOf(src);
    if (index >= 0) setLightboxIndex(index);
  };

  const closeLightbox = () => setLightboxIndex(null);

  const toggleCodeCollapse = (key: string) => {
    setCollapsedCodeBlocks(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const isCodeCollapsed = (key: string) => Boolean(collapsedCodeBlocks[key]);

  const handleResumeReading = () => {
    if (!resumePosition) return;
    window.scrollTo({ top: resumePosition, behavior: 'smooth' });
  };

  const handleLikeClick = useCallback(async (event: React.MouseEvent<HTMLButtonElement>) => {
    if (isLikeLoading || !article?.id) return;
    setIsLikeLoading(true);
    // 粒子动画
    const rect = event.currentTarget.getBoundingClientRect();
    const burstId = Date.now();
    setLikeBursts(prev => [
      ...prev,
      {
        id: burstId,
        x: rect.left + rect.width / 2,
        y: rect.top + rect.height / 2,
      },
    ]);
    window.setTimeout(() => {
      setLikeBursts(prev => prev.filter(item => item.id !== burstId));
    }, 720);
    try {
      const res = await articleApi.toggleLike(article.id);
      const data = (res as unknown as { data: { liked: boolean; likeCount: number } }).data;
      if (data) {
        setIsLiked(data.liked);
        setLikedCount(data.likeCount);
        if (data.liked) {
          unlockGlobalAchievement('first-like');
          // 检查是否点赞了多篇
          const likedArticles = JSON.parse(localStorage.getItem('liked-articles') || '[]') as number[];
          if (!likedArticles.includes(article.id)) {
            likedArticles.push(article.id);
            localStorage.setItem('liked-articles', JSON.stringify(likedArticles));
          }
          if (likedArticles.length >= 3) unlockGlobalAchievement('liker');
        }
      }
    } catch {
      // 静默失败
    } finally {
      setIsLikeLoading(false);
    }
  }, [isLikeLoading, article?.id]);

  const handleBookmarkClick = useCallback(() => {
    if (!article) return;
    const added = toggleBookmark({
      articleId: article.id,
      title: article.title,
      summary: article.summary || '',
      progress: readingProgress,
    });
    setIsBookmarkedState(added);
    if (added) {
      unlockGlobalAchievement('bookmark-first');
    }
  }, [article, readingProgress]);

  const switchImage = (direction: 'prev' | 'next') => {
    if (lightboxIndex === null || imageSources.length === 0) return;
    const delta = direction === 'prev' ? -1 : 1;
    const nextIndex = (lightboxIndex + delta + imageSources.length) % imageSources.length;
    setLightboxIndex(nextIndex);
  };

  const drawWrappedText = (
    context: CanvasRenderingContext2D,
    text: string,
    x: number,
    y: number,
    maxWidth: number,
    lineHeight: number,
    maxLines: number
  ) => {
    const words = text.split('');
    let line = '';
    let drawY = y;
    let lines = 0;

    for (const word of words) {
      const testLine = line + word;
      if (context.measureText(testLine).width > maxWidth && line) {
        context.fillText(line, x, drawY);
        line = word;
        drawY += lineHeight;
        lines += 1;
        if (lines >= maxLines) {
          context.fillText('...', x, drawY);
          return drawY;
        }
      } else {
        line = testLine;
      }
    }

    if (line) context.fillText(line, x, drawY);
    return drawY;
  };

  const exportShareCard = async () => {
    if (!article || isExporting) return;
    setIsExporting(true);
    try {
      const canvas = document.createElement('canvas');
      canvas.width = 1080;
      canvas.height = 1350;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      const style = getComputedStyle(document.documentElement);
      const bg = style.getPropertyValue('--bg-secondary') || '#f0ede8';
      const accent = style.getPropertyValue('--accent-rust') || '#8b7355';
      const textPrimary = style.getPropertyValue('--text-primary') || '#2d2d2d';
      const textSecondary = style.getPropertyValue('--text-secondary') || '#5c5c5c';

      ctx.fillStyle = bg;
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
      gradient.addColorStop(0, `${accent.trim()}22`);
      gradient.addColorStop(1, `${accent.trim()}00`);
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      ctx.fillStyle = accent;
      ctx.fillRect(90, 120, 8, 130);

      const qrDataUrl = await QRCode.toDataURL(window.location.href, {
        margin: 1,
        width: 220,
        color: {
          dark: '#2D2D2D',
          light: '#FFFFFF',
        },
      });

      ctx.fillStyle = textPrimary;
      ctx.font = '700 62px Inter';
      drawWrappedText(ctx, article.title, 120, 180, 840, 88, 3);

      ctx.fillStyle = textSecondary;
      ctx.font = '400 36px Inter';
      drawWrappedText(ctx, article.summary || '来自我的技术博客分享', 120, 450, 840, 54, 6);

      ctx.fillStyle = textSecondary;
      ctx.font = '500 32px Inter';
      ctx.fillText(`作者：${article.author?.nickname || article.author?.username || '博主'}`, 120, 960);
      ctx.fillText(`日期：${formatDate(article.createdAt)}`, 120, 1015);

      ctx.font = '500 30px Inter';
      ctx.fillText(`阅读原文：${window.location.href}`, 120, 1120);

      ctx.fillStyle = accent;
      ctx.font = '600 34px Inter';
      ctx.fillText('My Blog · 分享卡片', 120, 1230);

      const qrImage = new Image();
      qrImage.src = qrDataUrl;
      await new Promise(resolve => {
        qrImage.onload = resolve;
      });
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(820, 1040, 180, 180);
      ctx.drawImage(qrImage, 830, 1050, 160, 160);

      const link = document.createElement('a');
      link.href = canvas.toDataURL('image/png');
      link.download = `article-${article.id}-share-card.png`;
      link.click();
    } finally {
      setIsExporting(false);
    }
  };

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (lightboxIndex === null) return;
      if (event.key === 'Escape') closeLightbox();
      if (event.key === 'ArrowLeft') switchImage('prev');
      if (event.key === 'ArrowRight') switchImage('next');
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [lightboxIndex, imageSources.length]);

  useEffect(() => {
    if (!isFocusRunning) return;
    const timer = window.setInterval(() => {
      setFocusSeconds(prev => {
        if (prev <= 1) {
          window.clearInterval(timer);
          setIsFocusRunning(false);
          setFocusToast('⏰ 专注计时完成，休息一下再继续！');
          window.setTimeout(() => setFocusToast(''), 2400);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => window.clearInterval(timer);
  }, [isFocusRunning]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (!event.shiftKey || event.key.toLowerCase() !== 'r') return;
      if (!resumePosition) return;
      event.preventDefault();
      window.scrollTo({ top: resumePosition, behavior: 'smooth' });
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [resumePosition]);

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
      className={`page-wrapper article-page ${isReadingMode ? 'article-page--reading' : ''} article-page--font-${fontScale}`}
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

      {article.coverImage && (
        <div className="article-cover" style={{ transform: `translateY(${scrollY * 0.12}px)` }}>
          <img src={article.coverImage} alt={article.title} className="article-cover__image" />
        </div>
      )}

      <div className="article-page__toolbar">
        <span className="article-page__progress">阅读进度 {readingProgress}%</span>
        <div className="article-page__focus-box">
          <span className="article-page__focus-time">
            <Timer size={14} />
            {String(Math.floor(focusSeconds / 60)).padStart(2, '0')}:{String(focusSeconds % 60).padStart(2, '0')}
          </span>
          <div className="article-page__focus-presets">
            {[15, 25, 45].map(item => (
              <button
                key={item}
                className={`article-page__focus-preset ${focusPreset === item ? 'is-active' : ''}`}
                onClick={() => {
                  if (isFocusRunning) return;
                  setFocusPreset(item);
                  setFocusSeconds(item * 60);
                }}
              >
                {item}m
              </button>
            ))}
          </div>
          <button className="article-page__reading-toggle liquid-btn" onClick={() => setIsFocusRunning(prev => !prev)}>
            {isFocusRunning ? <Pause size={14} /> : <Play size={14} />}
            {isFocusRunning ? '暂停' : '专注'}
          </button>
          <button
            className="article-page__reading-toggle liquid-btn"
            onClick={() => {
              setIsFocusRunning(false);
              setFocusSeconds(focusPreset * 60);
            }}
          >
            <RefreshCw size={14} /> 重置
          </button>
        </div>
        {resumePosition && (
          <button className="article-page__reading-toggle liquid-btn" onClick={handleResumeReading}>
            <RotateCcw size={14} />
            恢复上次位置
          </button>
        )}
        <button className={`article-page__reading-toggle liquid-btn${isLiked ? ' is-liked' : ''}`} onClick={handleLikeClick} disabled={isLikeLoading}>
          <Heart size={14} fill={isLiked ? 'currentColor' : 'none'} />
          {isLiked ? '已赞' : '点赞'} {likedCount}
        </button>
        <button className={`article-page__reading-toggle liquid-btn${isBookmarkedState ? ' is-bookmarked' : ''}`} onClick={handleBookmarkClick}>
          <Bookmark size={14} fill={isBookmarkedState ? 'currentColor' : 'none'} />
          {isBookmarkedState ? '已收藏' : '收藏'}
        </button>
        <div className="article-page__font-group">
          <button className="article-page__reading-toggle liquid-btn" onClick={() => { setFontScale(0); unlockGlobalAchievement('font-tweaker'); }} disabled={fontScale === 0}>
            <Type size={14} /> A-
          </button>
          <button className="article-page__reading-toggle liquid-btn" onClick={() => setFontScale(1)} disabled={fontScale === 1}>
            <Type size={14} /> A
          </button>
          <button className="article-page__reading-toggle liquid-btn" onClick={() => { setFontScale(2); unlockGlobalAchievement('font-tweaker'); }} disabled={fontScale === 2}>
            <Type size={14} /> A+
          </button>
        </div>
        <button
          className="article-page__reading-toggle liquid-btn"
          onClick={() => { setIsReadingMode(prev => !prev); unlockGlobalAchievement('reading-mode'); }}
        >
          {isReadingMode ? <Minimize2 size={14} /> : <BookOpen size={14} />}
          {isReadingMode ? '退出阅读模式' : '阅读模式'}
        </button>
        <button className="article-page__reading-toggle liquid-btn" onClick={() => { exportShareCard(); unlockGlobalAchievement('sharer'); }} disabled={isExporting}>
          <Share2 size={14} />
          {isExporting ? '导出中...' : '分享卡片'}
        </button>
      </div>

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
        {parseContent(
          article.content,
          handleCopyCode,
          copiedCodeKey,
          headingIdMap,
          openImage,
          isCodeCollapsed,
          toggleCodeCollapse
        )}
      </motion.article>

      {headings.length > 0 && (
        <aside className="article-toc">
          <div className="article-toc__title">目录</div>
          <div className="article-toc__list">
            {headings.map(item => (
              <button
                key={item.id}
                className={`article-toc__item article-toc__item--h${item.level} ${activeHeadingId === item.id ? 'article-toc__item--active' : ''}`}
                onClick={() => scrollToHeading(item.id)}
              >
                {item.text}
              </button>
            ))}
          </div>
        </aside>
      )}

      {/* Article Footer */}
      <footer className="article-footer">
        <div className="article-footer__divider"></div>
        <p className="article-footer__thanks">感谢阅读 ✦</p>
      </footer>

      <ProjectBridge
        compact={true}
        title="如果这篇内容对你有帮助，可以顺着这条线再往前走一步"
        description="这篇文章讨论的很多问题，最后都会落到一个更现实的命题上：如何把技术判断变成真正运行中的系统。我最近在做的 AI Agent 项目，正是把这些关于稳定性、工作流和落地成本的思考继续往前推进。"
        articleLinks={relatedArticles}
      />

      {lightboxIndex !== null && imageSources[lightboxIndex] && (
        <div className="article-lightbox" role="dialog" aria-modal="true">
          <button className="article-lightbox__mask" onClick={closeLightbox} aria-label="关闭图片预览" />
          <div className="article-lightbox__content">
            <img src={imageSources[lightboxIndex]} alt="预览大图" className="article-lightbox__image" />
            {imageSources.length > 1 && (
              <>
                <button className="article-lightbox__nav article-lightbox__nav--prev" onClick={() => switchImage('prev')}>
                  <ChevronLeft size={20} />
                </button>
                <button className="article-lightbox__nav article-lightbox__nav--next" onClick={() => switchImage('next')}>
                  <ChevronRight size={20} />
                </button>
              </>
            )}
            <button className="article-lightbox__close" onClick={closeLightbox}>
              <X size={18} />
            </button>
            <div className="article-lightbox__index">{lightboxIndex + 1} / {imageSources.length}</div>
          </div>
        </div>
      )}

      {likeBursts.map(burst => (
        <div key={burst.id} className="article-like-burst" style={{ left: burst.x, top: burst.y }}>
          {Array.from({ length: 8 }).map((_, index) => (
            <span key={index} className="article-like-burst__dot" style={{ '--i': index } as CSSProperties} />
          ))}
          <span className="article-like-burst__plus">+1</span>
        </div>
      ))}

      {achievementToast && <div className="article-achievement-toast">{achievementToast}</div>}
      {focusToast && <div className="article-focus-toast">{focusToast}</div>}
    </motion.div>
  );
};

export default ArticlePage;
