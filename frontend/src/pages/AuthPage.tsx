import { useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { motion } from 'framer-motion';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import axios from 'axios';
import { ArrowLeft, LogIn, UserPlus, ShieldCheck, MessageSquareMore, Sparkles } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import './AuthPage.css';

type AuthMode = 'login' | 'register';

const pageVariants = {
  initial: { opacity: 0, y: 24 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -16 },
};

const getErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError(error)) {
    const message = (error.response?.data as { message?: string } | undefined)?.message;
    return message || fallback;
  }

  return fallback;
};

export default function AuthPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user, isAuthenticated, login, register, logout } = useAuth();
  const mode = searchParams.get('mode') === 'register' ? 'register' : 'login';

  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [registerForm, setRegisterForm] = useState({ username: '', password: '', email: '', nickname: '' });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const title = useMemo(
    () => (mode === 'register' ? '创建你的读者身份' : '继续你的阅读会话'),
    [mode]
  );

  const switchMode = (nextMode: AuthMode) => {
    setError('');
    setSearchParams(nextMode === 'register' ? { mode: 'register' } : {});
  };

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!loginForm.username.trim() || !loginForm.password.trim()) {
      setError('请输入用户名和密码');
      return;
    }

    setIsSubmitting(true);
    setError('');
    try {
      await login(loginForm.username.trim(), loginForm.password);
      navigate('/');
    } catch (submitError) {
      setError(getErrorMessage(submitError, '登录失败，请检查账号或密码'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRegister = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (registerForm.username.trim().length < 3) {
      setError('用户名长度至少 3 位');
      return;
    }
    if (registerForm.password.length < 6) {
      setError('密码长度至少 6 位');
      return;
    }
    if (!registerForm.email.trim()) {
      setError('请输入邮箱');
      return;
    }

    setIsSubmitting(true);
    setError('');
    try {
      await register(
        registerForm.username.trim(),
        registerForm.password,
        registerForm.email.trim(),
        registerForm.nickname.trim() || undefined
      );
      navigate('/');
    } catch (submitError) {
      setError(getErrorMessage(submitError, '注册失败，请稍后重试'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleLogout = async () => {
    await logout();
  };

  return (
    <motion.div className="page-wrapper auth-page" variants={pageVariants} initial="initial" animate="animate" exit="exit">
      <div className="auth-page__grid">
        <section className="auth-page__intro">
          <Link to="/" className="auth-page__back">
            <ArrowLeft size={16} /> 返回首页
          </Link>
          <div className="auth-page__eyebrow">Reader Access</div>
          <h1 className="auth-page__title">{title}</h1>
          <p className="auth-page__summary">
            登录后可以直接以用户身份评论，保留你的读者昵称；注册则会自动完成登录，便于继续阅读和互动。
          </p>

          <div className="auth-page__feature-list">
            <div className="auth-page__feature-card">
              <ShieldCheck size={18} />
              <div>
                <strong>安全认证</strong>
                <span>基于 JWT，会话状态持久化到当前浏览器。</span>
              </div>
            </div>
            <div className="auth-page__feature-card">
              <MessageSquareMore size={18} />
              <div>
                <strong>评论联动</strong>
                <span>登录后评论会直接绑定你的账户信息，无需重复填写。</span>
              </div>
            </div>
            <div className="auth-page__feature-card">
              <Sparkles size={18} />
              <div>
                <strong>阅读身份</strong>
                <span>适合后续继续接入书签、阅读偏好和个人互动记录。</span>
              </div>
            </div>
          </div>
        </section>

        <section className="auth-page__panel">
          {isAuthenticated && user ? (
            <div className="auth-page__account">
              <div className="auth-page__account-badge">当前已登录</div>
              <h2 className="auth-page__account-name">{user.nickname || user.username}</h2>
              <p className="auth-page__account-meta">{user.email} · {user.role === 'ADMIN' ? '管理员' : '读者'}</p>
              <div className="auth-page__account-actions">
                <Link to="/" className="auth-page__primary">继续阅读</Link>
                <button className="auth-page__secondary" onClick={handleLogout}>退出登录</button>
              </div>
            </div>
          ) : (
            <>
              <div className="auth-page__tabs">
                <button
                  className={`auth-page__tab ${mode === 'login' ? 'is-active' : ''}`}
                  onClick={() => switchMode('login')}
                >
                  <LogIn size={15} /> 登录
                </button>
                <button
                  className={`auth-page__tab ${mode === 'register' ? 'is-active' : ''}`}
                  onClick={() => switchMode('register')}
                >
                  <UserPlus size={15} /> 注册
                </button>
              </div>

              {error && <div className="auth-page__error">{error}</div>}

              {mode === 'login' ? (
                <form className="auth-page__form" onSubmit={handleLogin}>
                  <label className="auth-page__field">
                    <span>用户名</span>
                    <input
                      value={loginForm.username}
                      onChange={event => setLoginForm(prev => ({ ...prev, username: event.target.value }))}
                      placeholder="请输入用户名"
                      autoComplete="username"
                    />
                  </label>
                  <label className="auth-page__field">
                    <span>密码</span>
                    <input
                      type="password"
                      value={loginForm.password}
                      onChange={event => setLoginForm(prev => ({ ...prev, password: event.target.value }))}
                      placeholder="请输入密码"
                      autoComplete="current-password"
                    />
                  </label>
                  <button className="auth-page__primary auth-page__submit" disabled={isSubmitting}>
                    {isSubmitting ? '登录中...' : '立即登录'}
                  </button>
                </form>
              ) : (
                <form className="auth-page__form" onSubmit={handleRegister}>
                  <label className="auth-page__field">
                    <span>用户名</span>
                    <input
                      value={registerForm.username}
                      onChange={event => setRegisterForm(prev => ({ ...prev, username: event.target.value }))}
                      placeholder="3-20 位用户名"
                      autoComplete="username"
                    />
                  </label>
                  <label className="auth-page__field">
                    <span>邮箱</span>
                    <input
                      type="email"
                      value={registerForm.email}
                      onChange={event => setRegisterForm(prev => ({ ...prev, email: event.target.value }))}
                      placeholder="you@example.com"
                      autoComplete="email"
                    />
                  </label>
                  <label className="auth-page__field">
                    <span>昵称</span>
                    <input
                      value={registerForm.nickname}
                      onChange={event => setRegisterForm(prev => ({ ...prev, nickname: event.target.value }))}
                      placeholder="可选，用于评论展示"
                      autoComplete="nickname"
                    />
                  </label>
                  <label className="auth-page__field">
                    <span>密码</span>
                    <input
                      type="password"
                      value={registerForm.password}
                      onChange={event => setRegisterForm(prev => ({ ...prev, password: event.target.value }))}
                      placeholder="至少 6 位密码"
                      autoComplete="new-password"
                    />
                  </label>
                  <button className="auth-page__primary auth-page__submit" disabled={isSubmitting}>
                    {isSubmitting ? '注册中...' : '创建账号并登录'}
                  </button>
                </form>
              )}
            </>
          )}
        </section>
      </div>
    </motion.div>
  );
}