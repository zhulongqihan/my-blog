import { useState, useEffect, useRef, useCallback } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { sendChatMessage, getUserId } from '../services/difyApi';
import './AiChat.css';

// ==================== 类型定义 ====================

interface Message {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number;
}

// ==================== 常量 ====================

const STORAGE_KEY_MESSAGES = 'ai-chat-messages';
const STORAGE_KEY_CONVERSATION = 'ai-chat-conversation-id';
const MAX_STORED_MESSAGES = 50;

const WELCOME_TIPS = [
  '👋 介绍一下这个博客',
  '💻 博主都用什么技术栈？',
  '📝 有哪些推荐的文章？',
];

// ==================== Markdown 渲染组件 ====================

const MarkdownComponents: Record<string, React.ComponentType<Record<string, unknown>>> = {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  code({ inline, className, children, ...props }: any) {
    const match = /language-(\w+)/.exec(className || '');
    const codeString = String(children).replace(/\n$/, '');

    if (!inline && match) {
      return (
        <SyntaxHighlighter
          style={oneDark}
          language={match[1]}
          PreTag="div"
          customStyle={{
            margin: 0,
            borderRadius: '8px',
            fontSize: '12.5px',
          }}
        >
          {codeString}
        </SyntaxHighlighter>
      );
    }

    return (
      <code className={className} {...props}>
        {children}
      </code>
    );
  },
};

// ==================== 主组件 ====================

export default function AiChat() {
  // ---------- 状态 ----------
  const [isOpen, setIsOpen] = useState(false);
  const [isClosing, setIsClosing] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [conversationId, setConversationId] = useState('');
  const [error, setError] = useState('');

  // ---------- Refs ----------
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const userId = useRef(getUserId());

  // ---------- 持久化 ----------

  // 加载历史消息
  useEffect(() => {
    try {
      const savedMessages = localStorage.getItem(STORAGE_KEY_MESSAGES);
      const savedConversation = localStorage.getItem(STORAGE_KEY_CONVERSATION);
      if (savedMessages) {
        setMessages(JSON.parse(savedMessages));
      }
      if (savedConversation) {
        setConversationId(savedConversation);
      }
    } catch {
      // 解析失败，忽略
    }
  }, []);

  // 保存消息到 localStorage
  useEffect(() => {
    if (messages.length > 0) {
      const toStore = messages.slice(-MAX_STORED_MESSAGES);
      localStorage.setItem(STORAGE_KEY_MESSAGES, JSON.stringify(toStore));
    }
  }, [messages]);

  // 保存对话 ID
  useEffect(() => {
    if (conversationId) {
      localStorage.setItem(STORAGE_KEY_CONVERSATION, conversationId);
    }
  }, [conversationId]);

  // ---------- 自动滚动 ----------
  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  // ---------- 面板开关 ----------
  const openPanel = useCallback(() => {
    setIsOpen(true);
    setIsClosing(false);
    setError('');
    setTimeout(() => inputRef.current?.focus(), 300);
  }, []);

  const closePanel = useCallback(() => {
    setIsClosing(true);
    setTimeout(() => {
      setIsOpen(false);
      setIsClosing(false);
    }, 200);
  }, []);

  // ---------- 发送消息 ----------
  const handleSend = useCallback(async (text?: string) => {
    const query = (text || input).trim();
    if (!query || isStreaming) return;

    setInput('');
    setError('');

    // 添加用户消息
    const userMsg: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: query,
      timestamp: Date.now(),
    };

    // 添加空的 AI 消息（占位）
    const aiMsg: Message = {
      id: `ai-${Date.now()}`,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
    };

    setMessages(prev => [...prev, userMsg, aiMsg]);
    setIsStreaming(true);

    // 调用 Dify API（SSE 流式）
    const controller = sendChatMessage(
      {
        query,
        conversationId: conversationId || undefined,
        userId: userId.current,
      },
      {
        onChunk(chunk) {
          // 逐 chunk 追加到最后一条 AI 消息
          setMessages(prev => {
            const updated = [...prev];
            const lastMsg = updated[updated.length - 1];
            if (lastMsg && lastMsg.role === 'assistant') {
              lastMsg.content += chunk;
            }
            return updated;
          });
        },

        onEnd(newConversationId, _messageId) {
          setIsStreaming(false);
          if (newConversationId) {
            setConversationId(newConversationId);
          }
          abortControllerRef.current = null;
        },

        onError(errMsg) {
          setIsStreaming(false);
          setError(errMsg);
          // 移除空的 AI 占位消息
          setMessages(prev => {
            const updated = [...prev];
            const lastMsg = updated[updated.length - 1];
            if (lastMsg && lastMsg.role === 'assistant' && !lastMsg.content) {
              updated.pop();
            }
            return updated;
          });
          abortControllerRef.current = null;
        },
      }
    );

    abortControllerRef.current = controller;
  }, [input, isStreaming, conversationId]);

  // ---------- 停止生成 ----------
  const handleStop = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setIsStreaming(false);
  }, []);

  // ---------- 新对话 ----------
  const handleNewChat = useCallback(() => {
    if (isStreaming) handleStop();
    setMessages([]);
    setConversationId('');
    setError('');
    localStorage.removeItem(STORAGE_KEY_MESSAGES);
    localStorage.removeItem(STORAGE_KEY_CONVERSATION);
  }, [isStreaming, handleStop]);

  // ---------- 按键处理 ----------
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }, [handleSend]);

  // ---------- 输入框高度自适应 ----------
  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    const el = e.target;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 100) + 'px';
  }, []);

  // ---------- 清理 ----------
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  // ==================== 渲染 ====================

  return (
    <>
      {/* 浮动气泡按钮 */}
      <button
        className={`ai-chat__bubble ${isOpen ? 'ai-chat__bubble--hidden' : ''}`}
        onClick={openPanel}
        title="AI 助手"
        aria-label="打开 AI 聊天助手"
      >
        ✨
      </button>

      {/* 聊天面板 */}
      {isOpen && (
        <div className={`ai-chat__panel ${isClosing ? 'ai-chat__panel--closing' : ''}`}>
          {/* ---- Header ---- */}
          <div className="ai-chat__header">
            <div className="ai-chat__header-left">
              <span className="ai-chat__header-icon">🤖</span>
              <div className="ai-chat__header-info">
                <span className="ai-chat__header-title">AI 助手</span>
                <span className="ai-chat__header-subtitle">
                  Powered by Dify + DeepSeek
                </span>
              </div>
            </div>
            <div className="ai-chat__header-actions">
              <button
                className="ai-chat__header-btn"
                onClick={handleNewChat}
                title="新对话"
              >
                🗑️
              </button>
              <button
                className="ai-chat__header-btn"
                onClick={closePanel}
                title="关闭"
              >
                ✕
              </button>
            </div>
          </div>

          {/* ---- Messages ---- */}
          <div className="ai-chat__messages">
            {messages.length === 0 ? (
              // 欢迎界面
              <div className="ai-chat__welcome">
                <div className="ai-chat__welcome-icon">🧠</div>
                <div className="ai-chat__welcome-title">你好！我是博客 AI 助手</div>
                <div className="ai-chat__welcome-desc">
                  我可以回答关于这个博客的问题，基于 RAG 知识库检索博客文章内容。试试问我：
                </div>
                <div className="ai-chat__welcome-tips">
                  {WELCOME_TIPS.map((tip, i) => (
                    <button
                      key={i}
                      className="ai-chat__welcome-tip"
                      onClick={() => handleSend(tip)}
                    >
                      {tip}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              // 消息列表
              messages.map(msg => (
                <div
                  key={msg.id}
                  className={`ai-chat__msg ai-chat__msg--${msg.role}`}
                >
                  <div className="ai-chat__msg-avatar">
                    {msg.role === 'user' ? '👤' : '🤖'}
                  </div>
                  <div className="ai-chat__msg-content">
                    {msg.role === 'assistant' ? (
                      msg.content ? (
                        <>
                          <ReactMarkdown
                            remarkPlugins={[remarkGfm]}
                            components={MarkdownComponents}
                          >
                            {msg.content}
                          </ReactMarkdown>
                          {/* 流式输出时显示光标 */}
                          {isStreaming &&
                            msg.id === messages[messages.length - 1]?.id && (
                              <span className="ai-chat__cursor" />
                            )}
                        </>
                      ) : isStreaming &&
                        msg.id === messages[messages.length - 1]?.id ? (
                        // 等待首个 chunk 的思考动画
                        <div className="ai-chat__thinking">
                          <div className="ai-chat__thinking-dots">
                            <div className="ai-chat__thinking-dot" />
                            <div className="ai-chat__thinking-dot" />
                            <div className="ai-chat__thinking-dot" />
                          </div>
                          <span>思考中...</span>
                        </div>
                      ) : null
                    ) : (
                      msg.content
                    )}
                  </div>
                </div>
              ))
            )}

            {/* 错误提示 */}
            {error && (
              <div className="ai-chat__error">
                <span className="ai-chat__error-icon">⚠️</span>
                <span>{error}</span>
                <button
                  className="ai-chat__error-retry"
                  onClick={() => {
                    setError('');
                    // 重发最后一条用户消息
                    const lastUserMsg = [...messages]
                      .reverse()
                      .find(m => m.role === 'user');
                    if (lastUserMsg) handleSend(lastUserMsg.content);
                  }}
                >
                  重试
                </button>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* ---- Input Area ---- */}
          <div className="ai-chat__input-area">
            <div className="ai-chat__input-wrapper">
              <textarea
                ref={inputRef}
                className="ai-chat__input"
                value={input}
                onChange={handleInputChange}
                onKeyDown={handleKeyDown}
                placeholder={isStreaming ? 'AI 正在回复...' : '问我任何关于博客的问题...'}
                disabled={isStreaming}
                rows={1}
              />
            </div>
            {isStreaming ? (
              <button
                className="ai-chat__send-btn ai-chat__send-btn--stop"
                onClick={handleStop}
                title="停止生成"
              >
                ■
              </button>
            ) : (
              <button
                className="ai-chat__send-btn"
                onClick={() => handleSend()}
                disabled={!input.trim()}
                title="发送"
              >
                ↑
              </button>
            )}
          </div>

          {/* ---- Footer ---- */}
          <div className="ai-chat__footer">
            由 <a href="https://dify.ai" target="_blank" rel="noopener noreferrer">Dify</a> +{' '}
            <a href="https://deepseek.com" target="_blank" rel="noopener noreferrer">DeepSeek</a> 驱动
          </div>
        </div>
      )}
    </>
  );
}
