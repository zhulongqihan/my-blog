import { useMemo, useRef, useState } from 'react';
import { BookOpen, Check, ChevronDown, Copy, Loader2, MessageCircleQuestion, Sparkles } from 'lucide-react';
import { sendChatMessage, type AiStreamMeta } from '../services/aiApi';
import type { Article } from '../types';
import './ArticleReaderAssistant.css';

type ReaderAction = 'summary' | 'tradeoffs' | 'ask';

interface ArticleReaderAssistantProps {
  article: Article;
}

const actionPrompts: Record<Exclude<ReaderAction, 'ask'>, string> = {
  summary: '请把这篇文章压缩成一张适合快速复习的知识卡片：先给一句核心结论，再列出 3 个关键观点，最后给出 1 个读者可以继续思考的问题。',
  tradeoffs: '请只基于文章内容，提炼作者做过的工程取舍。按“问题 / 选择 / 放弃了什么 / 为什么”四列结构输出，最多 5 条。',
};

const ArticleReaderAssistant = ({ article }: ArticleReaderAssistantProps) => {
  const [action, setAction] = useState<ReaderAction>('summary');
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState('');
  const [isRunning, setIsRunning] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const [meta, setMeta] = useState<AiStreamMeta | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  const systemPrompt = useMemo(() => '你是一个严谨的中文技术阅读助手。回答要克制、具体，优先引用文章中的概念和取舍；如果上下文不足，请明确说明。不要输出“作为 AI”之类的套话。', []);

  const run = () => {
    const query = action === 'ask' ? question.trim() : actionPrompts[action];
    if (!query || isRunning) return;

    abortRef.current?.abort();
    setIsOpen(true);
    setAnswer('');
    setError('');
    setCopied(false);
    setMeta(null);
    setIsRunning(true);
    abortRef.current = sendChatMessage(
      { systemPrompt, query, articleId: article.id, mode: 'reader' },
      {
        onChunk: chunk => setAnswer(previous => previous + chunk),
        onMeta: nextMeta => setMeta(nextMeta),
        onEnd: () => setIsRunning(false),
        onError: message => {
          setError(message);
          setIsRunning(false);
        },
      },
    );
  };

  const copyAnswer = async () => {
    if (!answer) return;
    await navigator.clipboard?.writeText(answer);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  };

  return (
    <section className="reader-assistant" aria-label="文章阅读助手">
      <button
        type="button"
        className="reader-assistant__header"
        aria-expanded={isOpen}
        onClick={() => setIsOpen(previous => !previous)}
      >
        <div className="reader-assistant__mark"><Sparkles size={15} /></div>
        <div>
          <p className="reader-assistant__eyebrow">READER&apos;S LAB</p>
          <h2>把这篇文章读得更深一点</h2>
        </div>
        <span className="reader-assistant__source"><BookOpen size={13} /> 当前文章上下文</span>
        <ChevronDown className={`reader-assistant__chevron${isOpen ? ' is-open' : ''}`} size={17} aria-hidden="true" />
      </button>

      {!isOpen && <p className="reader-assistant__collapsed-hint">需要时再打开：回答只围绕这篇文章和服务端检索到的已发布内容。</p>}

      {isOpen && <>
      <p className="reader-assistant__scope">回答只围绕这篇文章和服务端检索到的已发布内容；上下文不足时会明确说明。</p>

      <div className="reader-assistant__actions">
        <button className={action === 'summary' ? 'is-active' : ''} onClick={() => setAction('summary')}>
          <BookOpen size={14} /> 知识卡片
        </button>
        <button className={action === 'tradeoffs' ? 'is-active' : ''} onClick={() => setAction('tradeoffs')}>
          <Sparkles size={14} /> 工程取舍
        </button>
        <button className={action === 'ask' ? 'is-active' : ''} onClick={() => setAction('ask')}>
          <MessageCircleQuestion size={14} /> 继续追问
        </button>
      </div>

      {action === 'ask' && (
        <div className="reader-assistant__question">
          <input
            value={question}
            name="article-question"
            autoComplete="off"
            onChange={event => setQuestion(event.target.value)}
            onKeyDown={event => { if (event.key === 'Enter') run(); }}
              placeholder="例如：这篇文章里最值得迁移到其他项目的设计是…"
            aria-label="向文章提问"
          />
        </div>
      )}

      <div className="reader-assistant__footer">
        <span>{action === 'ask' ? '问题会限定在当前文章上下文内' : '回答会以流式方式生成'}</span>
        <button className="reader-assistant__run" onClick={run} disabled={isRunning || (action === 'ask' && !question.trim())}>
          {isRunning ? <Loader2 size={14} className="reader-assistant__spin" /> : <Sparkles size={14} />}
          {isRunning ? '分析中...' : '开始分析'}
        </button>
      </div>

      {meta && <p className="reader-assistant__source"><BookOpen size={13} /> 服务端检索 {meta.sources.length} 篇文章 · {meta.requestId}</p>}
        {error && <p className="reader-assistant__error" role="alert">{error}</p>}
      {answer && (
        <div className="reader-assistant__answer">
          <div className="reader-assistant__answer-head">
            <span>RESPONSE / CONTEXTUAL</span>
            <button onClick={copyAnswer}>{copied ? <Check size={13} /> : <Copy size={13} />} {copied ? '已复制' : '复制'}</button>
          </div>
          <div className="reader-assistant__answer-body" aria-live="polite">{answer}</div>
        </div>
      )}
      </>}
    </section>
  );
};

export default ArticleReaderAssistant;
