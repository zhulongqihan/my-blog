import { useMemo, useRef, useState } from 'react';
import { BookOpen, Check, Copy, Loader2, MessageCircleQuestion, Sparkles } from 'lucide-react';
import { sendChatMessage } from '../services/aiApi';
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

const buildContext = (article: Article) => {
  const content = article.content.length > 12000 ? `${article.content.slice(0, 12000)}\n[正文已截取]` : article.content;
  return `\n\n【文章标题】\n${article.title}\n\n【文章摘要】\n${article.summary || '无'}\n\n【文章正文】\n${content}`;
};

const ArticleReaderAssistant = ({ article }: ArticleReaderAssistantProps) => {
  const [action, setAction] = useState<ReaderAction>('summary');
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState('');
  const [isRunning, setIsRunning] = useState(false);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  const systemPrompt = useMemo(() => `你是一个严谨的中文技术阅读助手。你只能根据用户提供的文章上下文回答，不要补造文章没有提到的事实。回答要克制、具体，优先引用文章中的概念和取舍；如果上下文不足，请明确说明。不要输出“作为 AI”之类的套话。${buildContext(article)}`, [article]);

  const run = () => {
    const query = action === 'ask' ? question.trim() : actionPrompts[action];
    if (!query || isRunning) return;

    abortRef.current?.abort();
    setAnswer('');
    setError('');
    setCopied(false);
    setIsRunning(true);
    abortRef.current = sendChatMessage(
      { systemPrompt, query },
      {
        onChunk: chunk => setAnswer(previous => previous + chunk),
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
      <div className="reader-assistant__header">
        <div className="reader-assistant__mark"><Sparkles size={15} /></div>
        <div>
          <p className="reader-assistant__eyebrow">READER&apos;S LAB</p>
          <h2>把这篇文章读得更深一点</h2>
        </div>
        <span className="reader-assistant__source"><BookOpen size={13} /> 当前文章上下文</span>
      </div>

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
            onChange={event => setQuestion(event.target.value)}
            onKeyDown={event => { if (event.key === 'Enter') run(); }}
            placeholder="例如：这篇文章里最值得迁移到其他项目的设计是什么？"
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

      {error && <p className="reader-assistant__error">{error}</p>}
      {answer && (
        <div className="reader-assistant__answer">
          <div className="reader-assistant__answer-head">
            <span>RESPONSE / CONTEXTUAL</span>
            <button onClick={copyAnswer}>{copied ? <Check size={13} /> : <Copy size={13} />} {copied ? '已复制' : '复制'}</button>
          </div>
          <div className="reader-assistant__answer-body">{answer}</div>
        </div>
      )}
    </section>
  );
};

export default ArticleReaderAssistant;
