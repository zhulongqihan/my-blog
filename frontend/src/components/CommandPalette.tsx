import { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Search, Home, Archive, BarChart3, User, BookOpen,
  Moon, Sun, Command, Sparkles, Award, Bot
} from 'lucide-react';
import { AI_AGENT_NAME, AI_AGENT_URL } from '../constants/externalLinks';
import { articleApi } from '../services';
import type { Article } from '../types';
import type { ShortcutAction } from '../hooks/useKeyboardShortcuts';
import './CommandPalette.css';

interface CommandPaletteProps {
  isOpen: boolean;
  onClose: () => void;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  shortcuts: ShortcutAction[];
}

interface CommandItem {
  id: string;
  icon: React.ReactNode;
  label: string;
  description?: string;
  shortcut?: string;
  action: () => void;
  category: 'navigation' | 'action' | 'article';
}

const CommandPalette = ({ isOpen, onClose, theme, onToggleTheme, shortcuts }: CommandPaletteProps) => {
  const [query, setQuery] = useState('');
  const [activeIndex, setActiveIndex] = useState(0);
  const [articles, setArticles] = useState<Article[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  // Load articles for search
  useEffect(() => {
    if (isOpen) {
      articleApi.getList(0, 50).then(res => {
        if (res.data?.content) setArticles(res.data.content);
      }).catch(() => {});
    }
  }, [isOpen]);

  // Focus input when opening
  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setActiveIndex(0);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [isOpen]);

  const runAndClose = useCallback((action: () => void) => {
    action();
    onClose();
  }, [onClose]);

  // Build command items
  const commands = useMemo<CommandItem[]>(() => {
    const nav: CommandItem[] = [
      {
        id: 'nav-home', icon: <Home size={18} />, label: '首页',
        description: '返回首页', category: 'navigation',
        action: () => runAndClose(() => navigate('/')),
      },
      {
        id: 'nav-archive', icon: <Archive size={18} />, label: '归档',
        description: '浏览所有文章', category: 'navigation',
        action: () => runAndClose(() => navigate('/archive')),
      },
      {
        id: 'nav-stats', icon: <BarChart3 size={18} />, label: '阅读统计',
        description: '查看阅读数据', category: 'navigation',
        action: () => runAndClose(() => navigate('/reading-stats')),
      },
      {
        id: 'nav-about', icon: <User size={18} />, label: '关于',
        description: '了解作者', category: 'navigation',
        action: () => runAndClose(() => navigate('/about')),
      },
      {
        id: 'nav-search', icon: <Search size={18} />, label: '搜索页',
        description: '打开搜索页面', category: 'navigation',
        action: () => runAndClose(() => navigate('/search')),
      },
      {
        id: 'nav-ai-agent', icon: <Bot size={18} />, label: AI_AGENT_NAME,
        description: '打开独立部署的 AI Agent 项目', category: 'navigation',
        shortcut: shortcuts.find(s => s.key.toLowerCase() === 'a' && s.ctrl && s.shift) ? 'Ctrl + Shift + A' : undefined,
        action: () => runAndClose(() => window.open(AI_AGENT_URL, '_blank', 'noopener,noreferrer')),
      },
    ];

    const actions: CommandItem[] = [
      {
        id: 'act-theme', icon: theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />,
        label: theme === 'dark' ? '切换到亮色模式' : '切换到暗色模式',
        description: '切换主题', category: 'action',
        shortcut: shortcuts.find(s => s.key.toLowerCase() === 'd' && s.ctrl) ? 'Ctrl + D' : undefined,
        action: () => runAndClose(onToggleTheme),
      },
      {
        id: 'act-top', icon: <Sparkles size={18} />,
        label: '回到顶部', description: '滚动到页面顶部', category: 'action',
        action: () => runAndClose(() => window.scrollTo({ top: 0, behavior: 'smooth' })),
      },
      {
        id: 'act-achievements', icon: <Award size={18} />,
        label: '成就系统', description: '查看你的成就', category: 'action',
        action: () => {
          onClose();
          // Find and click achievement button
          const btn = document.querySelector('.achievement-trigger, [data-achievement-btn]') as HTMLElement;
          if (btn) btn.click();
        },
      },
    ];

    const articleItems: CommandItem[] = articles.map(a => ({
      id: `article-${a.id}`,
      icon: <BookOpen size={18} />,
      label: a.title,
      description: a.summary?.slice(0, 60) || '',
      category: 'article' as const,
      action: () => runAndClose(() => navigate(`/article/${a.id}`)),
    }));

    return [...nav, ...actions, ...articleItems];
  }, [navigate, theme, onToggleTheme, articles, shortcuts, runAndClose]);

  // Filter commands by query
  const filtered = useMemo(() => {
    if (!query.trim()) return commands;
    const q = query.toLowerCase();
    return commands.filter(
      c => c.label.toLowerCase().includes(q) ||
           c.description?.toLowerCase().includes(q) ||
           c.category.includes(q)
    );
  }, [commands, query]);

  // Reset active index when filtered list changes
  useEffect(() => {
    setActiveIndex(0);
  }, [filtered.length]);

  // Scroll active item into view
  useEffect(() => {
    const list = listRef.current;
    if (!list) return;
    const active = list.children[activeIndex] as HTMLElement;
    if (active) {
      active.scrollIntoView({ block: 'nearest' });
    }
  }, [activeIndex]);

  // Keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setActiveIndex(i => (i + 1) % filtered.length);
        break;
      case 'ArrowUp':
        e.preventDefault();
        setActiveIndex(i => (i - 1 + filtered.length) % filtered.length);
        break;
      case 'Enter':
        e.preventDefault();
        if (filtered[activeIndex]) filtered[activeIndex].action();
        break;
      case 'Escape':
        e.preventDefault();
        onClose();
        break;
    }
  };

  // Group filtered items by category
  const grouped = useMemo(() => {
    const groups: { category: string; label: string; items: (CommandItem & { globalIndex: number })[] }[] = [];
    const catOrder = ['navigation', 'action', 'article'];
    const catLabels: Record<string, string> = {
      navigation: '导航',
      action: '操作',
      article: '文章',
    };

    let globalIndex = 0;
    for (const cat of catOrder) {
      const items = filtered
        .filter(c => c.category === cat)
        .map(c => ({ ...c, globalIndex: globalIndex++ }));
      if (items.length > 0) {
        groups.push({ category: cat, label: catLabels[cat] || cat, items });
      }
    }
    return groups;
  }, [filtered]);

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          className="command-palette__overlay"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.15 }}
          onClick={onClose}
        >
          <motion.div
            className="command-palette"
            initial={{ opacity: 0, scale: 0.95, y: -20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -20 }}
            transition={{ duration: 0.15 }}
            onClick={e => e.stopPropagation()}
          >
            <div className="command-palette__search">
              <Command size={18} className="command-palette__search-icon" />
              <input
                ref={inputRef}
                type="text"
                className="command-palette__input"
                placeholder="输入命令或搜索文章..."
                value={query}
                onChange={e => setQuery(e.target.value)}
                onKeyDown={handleKeyDown}
              />
              <kbd className="command-palette__kbd">ESC</kbd>
            </div>

            <div className="command-palette__list" ref={listRef}>
              {grouped.length === 0 ? (
                <div className="command-palette__empty">没有匹配的结果</div>
              ) : (
                grouped.map(group => (
                  <div key={group.category} className="command-palette__group">
                    <div className="command-palette__group-label">{group.label}</div>
                    {group.items.map(item => (
                      <button
                        key={item.id}
                        className={`command-palette__item ${
                          item.globalIndex === activeIndex ? 'command-palette__item--active' : ''
                        }`}
                        onClick={item.action}
                        onMouseEnter={() => setActiveIndex(item.globalIndex)}
                      >
                        <span className="command-palette__item-icon">{item.icon}</span>
                        <span className="command-palette__item-content">
                          <span className="command-palette__item-label">{item.label}</span>
                          {item.description && (
                            <span className="command-palette__item-desc">{item.description}</span>
                          )}
                        </span>
                        {item.shortcut && (
                          <kbd className="command-palette__item-kbd">{item.shortcut}</kbd>
                        )}
                      </button>
                    ))}
                  </div>
                ))
              )}
            </div>

            <div className="command-palette__footer">
              <span><kbd>↑↓</kbd> 导航</span>
              <span><kbd>Enter</kbd> 执行</span>
              <span><kbd>Esc</kbd> 关闭</span>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default CommandPalette;
