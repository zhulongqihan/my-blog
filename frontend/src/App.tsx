import { useEffect, useState, useCallback, useMemo } from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
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
import { useKeyboardShortcuts, type ShortcutAction } from './hooks/useKeyboardShortcuts';
import HomePage from './pages/HomePage';
import ArticlePage from './pages/ArticlePage';
import AboutPage from './pages/AboutPage';
import ArchivePage from './pages/ArchivePage';
import SearchPage from './pages/SearchPage';
import ReadingStatsPage from './pages/ReadingStatsPage';
import BookmarksPage from './pages/BookmarksPage';
import './App.css';

function AppContent() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const savedTheme = localStorage.getItem('theme-mode');
    return savedTheme === 'dark' ? 'dark' : 'light';
  });
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme-mode', theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme(prev => (prev === 'dark' ? 'light' : 'dark'));
  }, []);

  const shortcuts = useMemo<ShortcutAction[]>(() => [
    {
      key: 'k', ctrl: true, description: '打开命令面板',
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
  ], [toggleTheme, navigate]);

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
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/article/:id" element={<ArticlePage />} />
            <Route path="/about" element={<AboutPage />} />
            <Route path="/archive" element={<ArchivePage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/reading-stats" element={<ReadingStatsPage />} />
            <Route path="/bookmarks" element={<BookmarksPage />} />
          </Routes>
        </AnimatePresence>
      </main>
      <Footer />
      <ScrollToTop />
      <PixelSheep />
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
