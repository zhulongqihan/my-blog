import { useState, useEffect, useRef, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Menu, X, Feather, Search, Loader2, Sun, Moon } from 'lucide-react';
import { articleApi } from '../services';
import type { Article } from '../types';
import './Header.css';

const SEARCH_HISTORY_KEY = 'search-history';

interface HeaderProps {
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
}

const Header = ({ theme, onToggleTheme }: HeaderProps) => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [suggestions, setSuggestions] = useState<Article[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [activeSuggestionIndex, setActiveSuggestionIndex] = useState(-1);
  const [searchHistory, setSearchHistory] = useState<string[]>([]);
  const location = useLocation();
  const navigate = useNavigate();
  const searchRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  const saveSearchHistory = (query: string) => {
    const normalized = query.trim();
    if (!normalized) return;
    setSearchHistory(prev => {
      const next = [normalized, ...prev.filter(item => item.toLowerCase() !== normalized.toLowerCase())].slice(0, 8);
      localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next));
      return next;
    });
  };

  const clearSearchHistory = () => {
    setSearchHistory([]);
    localStorage.removeItem(SEARCH_HISTORY_KEY);
  };

  useEffect(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(SEARCH_HISTORY_KEY) || '[]') as string[];
      setSearchHistory(Array.isArray(saved) ? saved : []);
    } catch {
      setSearchHistory([]);
    }
  }, []);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // 当路由变化时关闭移动菜单
  useEffect(() => {
    if (isMobileMenuOpen) {
      setIsMobileMenuOpen(false);
    }
    setShowSuggestions(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname]);

  useEffect(() => {
    const onClickOutside = (event: MouseEvent) => {
      if (!searchRef.current?.contains(event.target as Node)) {
        setShowSuggestions(false);
      }
    };
    window.addEventListener('click', onClickOutside);
    return () => window.removeEventListener('click', onClickOutside);
  }, []);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        searchInputRef.current?.focus();
        if (keyword.trim().length >= 2) {
          setShowSuggestions(true);
        }
      }
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [keyword]);

  useEffect(() => {
    const query = keyword.trim();
    if (query.length < 2) {
      setSuggestions([]);
      setActiveSuggestionIndex(-1);
      return;
    }

    const timer = setTimeout(async () => {
      try {
        setIsSearching(true);
        const response = await articleApi.search(query, 0, 5);
        setSuggestions(response.data.content || []);
        setShowSuggestions(true);
        setActiveSuggestionIndex(-1);
      } catch {
        setSuggestions([]);
        setActiveSuggestionIndex(-1);
      } finally {
        setIsSearching(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [keyword]);

  const handleSearchSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const selected =
      activeSuggestionIndex >= 0 ? suggestions[activeSuggestionIndex] : suggestions[0];

    if (selected) {
      navigate(`/article/${selected.id}`);
      setKeyword('');
      setShowSuggestions(false);
      setActiveSuggestionIndex(-1);
      return;
    }

    const query = keyword.trim();
    if (!query) return;
    saveSearchHistory(query);

    try {
      const response = await articleApi.search(query, 0, 1);
      const hit = response.data.content?.[0];
      if (hit) {
        navigate(`/search?q=${encodeURIComponent(query)}`);
        setKeyword('');
        setShowSuggestions(false);
        setActiveSuggestionIndex(-1);
      } else {
        navigate(`/search?q=${encodeURIComponent(query)}`);
        setShowSuggestions(false);
      }
    } catch {
      navigate(`/search?q=${encodeURIComponent(query)}`);
      setShowSuggestions(false);
    }
  };

  const navLinks = [
    { path: '/', label: '首页' },
    { path: '/archive', label: '归档' },
    { path: '/about', label: '关于' },
  ];

  return (
    <header className={`header ${isScrolled ? 'header--scrolled' : ''}`}>
      <div className="header__container">
        <Link to="/" className="header__logo">
          <Feather className="header__logo-icon" size={24} strokeWidth={1.5} />
          <span className="header__logo-text">My Blog</span>
        </Link>

        <nav className="header__nav">
          {navLinks.map(link => (
            <Link
              key={link.path}
              to={link.path}
              className={`header__nav-link ${location.pathname === link.path ? 'header__nav-link--active' : ''}`}
            >
              {link.label}
              {location.pathname === link.path && (
                <motion.span
                  className="header__nav-indicator"
                  layoutId="nav-indicator"
                  transition={{ type: 'spring', stiffness: 380, damping: 30 }}
                />
              )}
            </Link>
          ))}
        </nav>

        <div className="header__search" ref={searchRef}>
          <form onSubmit={handleSearchSubmit} className="header__search-form">
            <Search size={14} className="header__search-icon" />
            <input
              ref={searchInputRef}
              type="text"
              className="header__search-input"
              placeholder="搜索文章... (Ctrl/Cmd + K)"
              value={keyword}
              onChange={e => {
                setKeyword(e.target.value);
                setActiveSuggestionIndex(-1);
              }}
              onFocus={() => {
                if (keyword.trim().length >= 2 || searchHistory.length > 0) {
                  setShowSuggestions(true);
                }
              }}
              onKeyDown={e => {
                if (!showSuggestions || suggestions.length === 0) {
                  if (e.key === 'Escape') setShowSuggestions(false);
                  return;
                }

                if (e.key === 'ArrowDown') {
                  e.preventDefault();
                  setActiveSuggestionIndex(prev => (prev + 1) % suggestions.length);
                }

                if (e.key === 'ArrowUp') {
                  e.preventDefault();
                  setActiveSuggestionIndex(prev => (prev <= 0 ? suggestions.length - 1 : prev - 1));
                }

                if (e.key === 'Escape') {
                  setShowSuggestions(false);
                  setActiveSuggestionIndex(-1);
                }
              }}
            />
            {isSearching && <Loader2 size={14} className="header__search-loading" />}
          </form>

          {showSuggestions && (
            <div className="header__suggestions">
              {keyword.trim().length < 2 && searchHistory.length > 0 ? (
                <>
                  <div className="header__history-head">
                    <span>最近搜索</span>
                    <button className="header__history-clear" onClick={clearSearchHistory}>清空</button>
                  </div>
                  {searchHistory.map(item => (
                    <button
                      key={item}
                      className="header__suggestion-item"
                      onClick={() => {
                        setKeyword(item);
                        navigate(`/search?q=${encodeURIComponent(item)}`);
                        setShowSuggestions(false);
                      }}
                    >
                      <span className="header__suggestion-title">{item}</span>
                      <span className="header__suggestion-meta">历史记录</span>
                    </button>
                  ))}
                </>
              ) : suggestions.length === 0 ? (
                <div className="header__suggestion-empty">未找到匹配文章</div>
              ) : (
                suggestions.map(item => (
                  <button
                    key={item.id}
                    className={`header__suggestion-item ${
                      suggestions[activeSuggestionIndex]?.id === item.id ? 'header__suggestion-item--active' : ''
                    }`}
                    onClick={() => {
                      navigate(`/article/${item.id}`);
                      setKeyword('');
                      setShowSuggestions(false);
                      setActiveSuggestionIndex(-1);
                    }}
                  >
                    <span className="header__suggestion-title">{item.title}</span>
                    <span className="header__suggestion-meta">{item.category?.name || '未分类'}</span>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        <button className="header__theme-toggle" onClick={onToggleTheme} aria-label="切换主题">
          {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
        </button>

        <button
          className="header__mobile-toggle"
          onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          aria-label="Toggle menu"
        >
          {isMobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.div
            className="header__mobile-menu"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.3 }}
          >
            {navLinks.map((link, index) => (
              <motion.div
                key={link.path}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.1 }}
              >
                <Link
                  to={link.path}
                  className={`header__mobile-link ${location.pathname === link.path ? 'header__mobile-link--active' : ''}`}
                >
                  {link.label}
                </Link>
              </motion.div>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
};

export default Header;
