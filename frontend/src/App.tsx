import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { AuthProvider } from './contexts/AuthContext';
import { WebSocketProvider } from './contexts/WebSocketContext';
import Header from './components/Header';
import Footer from './components/Footer';
import ScrollToTop from './components/ScrollToTop';
import ReadingProgress from './components/ReadingProgress';
import CursorGlow from './components/CursorGlow';
import HomePage from './pages/HomePage';
import ArticlePage from './pages/ArticlePage';
import AboutPage from './pages/AboutPage';
import ArchivePage from './pages/ArchivePage';
import './App.css';

function App() {
  return (
    <AuthProvider>
      <WebSocketProvider>
      <Router>
        <div className="app">
          <CursorGlow />
          <ReadingProgress />
          <Header />
          <main className="main-content">
            <AnimatePresence mode="wait">
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/article/:id" element={<ArticlePage />} />
                <Route path="/about" element={<AboutPage />} />
                <Route path="/archive" element={<ArchivePage />} />
              </Routes>
            </AnimatePresence>
          </main>
          <Footer />
          <ScrollToTop />
        </div>
      </Router>
      </WebSocketProvider>
    </AuthProvider>
  );
}

export default App;
