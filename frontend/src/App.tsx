import { lazy, Suspense, useEffect, useState, useCallback, useMemo } from 'react';
import { BrowserRouter as Router, Routes, Route, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { AuthProvider } from './contexts/AuthContext';
import { WebSocketProvider } from './contexts/WebSocketContext';
import Header from './components/Header';
import Footer from './components/Footer';
import ScrollToTop from './components/ScrollToTop';
import ReadingProgress from './components/ReadingProgress';
import CursorGlow from './components/CursorGlow';
import NotificationToast from './components/NotificationToast';
import OfflineNotice from './components/OfflineNotice';
import PixelSheep from './components/PixelSheep';
import CommandPalette from './components/CommandPalette';
import { AI_AGENT_URL } from './constants/externalLinks';
import { useKeyboardShortcuts, type ShortcutAction } from './hooks/useKeyboardShortcuts';
const HomePage = lazy(() => import('./pages/HomePage'));
const ArticlePage = lazy(() => import('./pages/ArticlePage'));
const AboutPage = lazy(() => import('./pages/AboutPage'));
const ArchivePage = lazy(() => import('./pages/ArchivePage'));
const SearchPage = lazy(() => import('./pages/SearchPage'));
const ReadingStatsPage = lazy(() => import('./pages/ReadingStatsPage'));
const BookmarksPage = lazy(() => import('./pages/BookmarksPage'));
const AuthPage = lazy(() => import('./pages/AuthPage'));
const ShowcasePage = lazy(() => import('./pages/ShowcasePage'));
import './App.css';

function AppContent() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const savedTheme = localStorage.getItem('theme-mode');
    return savedTheme === 'dark' ? 'dark' : 'light';
  });
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const isPresentationMode = location.pathname === '/showcase';

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme-mode', theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme(prev => (prev === 'dark' ? 'light' : 'dark'));
  }, []);

  const openAiAgent = useCallback(() => {
    window.open(AI_AGENT_URL, '_blank', 'noopener,noreferrer');
  }, []);

  const shortcuts = useMemo<ShortcutAction[]>(() => [
    {
      key: 'p', ctrl: true, shift: true, description: '打开命令面板',
      action: () => setCommandPaletteOpen(prev => !prev),
    },
    {
      key: 'd', ctrl: true, description: '切换深色模式',
      action: toggleTheme,
    },
    {
      key: '/', ctrl: true, description: '搜索',
      action: () => navigate('/search'),
    },
    {
      key: 'h', ctrl: true, shift: true, description: '返回首页',
      action: () => navigate('/'),
    },
    {
      key: 'a', ctrl: true, shift: true, description: '打开 AI Agent',
      action: openAiAgent,
    },
  ], [toggleTheme, navigate, openAiAgent]);

  useKeyboardShortcuts(shortcuts);

  return (
    <div className="app">
      <CursorGlow />
      <ReadingProgress />
      <OfflineNotice />
      <NotificationToast />
      <Header theme={theme} onToggleTheme={toggleTheme} />
      <CommandPalette
        isOpen={commandPaletteOpen}
        onClose={() => setCommandPaletteOpen(false)}
        theme={theme}
        onToggleTheme={toggleTheme}
        shortcuts={shortcuts}
      />
      <main className="main-content">
        <AnimatePresence mode="wait">
          <Suspense fallback={<div aria-live="polite">正在加载页面...</div>}>
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/article/:id" element={<ArticlePage />} />
              <Route path="/about" element={<AboutPage />} />
              <Route path="/archive" element={<ArchivePage />} />
              <Route path="/search" element={<SearchPage />} />
              <Route path="/reading-stats" element={<ReadingStatsPage />} />
              <Route path="/bookmarks" element={<BookmarksPage />} />
              <Route path="/auth" element={<AuthPage />} />
              <Route path="/showcase" element={<ShowcasePage />} />
            </Routes>
          </Suspense>
        </AnimatePresence>
      </main>
      <Footer />
      <ScrollToTop />
      {!isPresentationMode && <PixelSheep />}
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <WebSocketProvider>
        <Router>
          <AppContent />
        </Router>
      </WebSocketProvider>
    </AuthProvider>
  );
}

export default App;
