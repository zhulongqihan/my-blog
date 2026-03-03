import { useState, useEffect, useRef, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Menu, X, Feather, Search, Loader2, Sun, Moon } from 'lucide-react';
import { articleApi } from '../services';
import type { Article } from '../types';
import './Header.css';

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
  const location = useLocation();
  const navigate = useNavigate();
  const searchRef = useRef<HTMLDivElement>(null);

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
    const query = keyword.trim();
    if (query.length < 2) {
      setSuggestions([]);
      return;
    }

    const timer = setTimeout(async () => {
      try {
        setIsSearching(true);
        const response = await articleApi.search(query, 0, 5);
        setSuggestions(response.data.content || []);
        setShowSuggestions(true);
      } catch {
        setSuggestions([]);
      } finally {
        setIsSearching(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [keyword]);

  const handleSearchSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const first = suggestions[0];
    if (first) {
      navigate(`/article/${first.id}`);
      setKeyword('');
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
              type="text"
              className="header__search-input"
              placeholder="搜索文章..."
              value={keyword}
              onChange={e => setKeyword(e.target.value)}
              onFocus={() => keyword.trim().length >= 2 && setShowSuggestions(true)}
            />
            {isSearching && <Loader2 size={14} className="header__search-loading" />}
          </form>

          {showSuggestions && (
            <div className="header__suggestions">
              {suggestions.length === 0 ? (
                <div className="header__suggestion-empty">未找到匹配文章</div>
              ) : (
                suggestions.map(item => (
                  <button
                    key={item.id}
                    className="header__suggestion-item"
                    onClick={() => {
                      navigate(`/article/${item.id}`);
                      setKeyword('');
                      setShowSuggestions(false);
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
