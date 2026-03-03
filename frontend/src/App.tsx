import { useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
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
import PixelCat from './components/PixelCat';
import HomePage from './pages/HomePage';
import ArticlePage from './pages/ArticlePage';
import AboutPage from './pages/AboutPage';
import ArchivePage from './pages/ArchivePage';
import SearchPage from './pages/SearchPage';
import './App.css';

function App() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const savedTheme = localStorage.getItem('theme-mode');
    return savedTheme === 'dark' ? 'dark' : 'light';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme-mode', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => (prev === 'dark' ? 'light' : 'dark'));
  };

  return (
    <AuthProvider>
      <WebSocketProvider>
      <Router>
        <div className="app">
          <CursorGlow />
          <ReadingProgress />
          <OfflineNotice />
          <NotificationToast />
          <Header theme={theme} onToggleTheme={toggleTheme} />
          <main className="main-content">
            <AnimatePresence mode="wait">
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/article/:id" element={<ArticlePage />} />
                <Route path="/about" element={<AboutPage />} />
                <Route path="/archive" element={<ArchivePage />} />
                <Route path="/search" element={<SearchPage />} />
              </Routes>
            </AnimatePresence>
          </main>
          <Footer />
          <ScrollToTop />
          <PixelCat />
        </div>
      </Router>
      </WebSocketProvider>
    </AuthProvider>
  );
}

export default App;
