import { useRef, useState } from 'react';
import { Bot, Sparkles, Wand2, Loader2, Copy, Check } from 'lucide-react';
import { getUserId, sendChatMessage } from '../services/difyApi';
import './AiIdeaLab.css';

interface SectionResult {
  summary: string;
  steps: string[];
  code: string[];
  acceptance: string[];
}

const EMPTY_RESULT: SectionResult = {
  summary: '',
  steps: [],
  code: [],
  acceptance: [],
};

const PROMPT_TEMPLATE = `你是资深前端工程师和 Agent 规划助手。\n
请基于用户需求输出【严格四段】内容：\n[方案摘要]\n- 2~4 句\n\n[执行步骤]\n- 使用 1. 2. 3. 编号，给 4~6 步\n\n[代码要点]\n- 列出关键组件/状态/事件/性能注意点，4~8 条\n\n[验收清单]\n- 给 4~6 条可测试标准\n
不要输出其他标题。内容使用中文。`;

const parseSection = (text: string): SectionResult => {
  const getBlock = (name: string, nextNames: string[]) => {
    const nextPattern = nextNames.length ? `(?=\\n\\[(?:${nextNames.join('|')})\\]|$)` : '$';
    const reg = new RegExp(`\\[${name}\\]\\s*([\\s\\S]*?)${nextPattern}`, 'm');
    return (text.match(reg)?.[1] || '').trim();
  };

  const summary = getBlock('方案摘要', ['执行步骤', '代码要点', '验收清单']);
  const stepsBlock = getBlock('执行步骤', ['代码要点', '验收清单']);
  const codeBlock = getBlock('代码要点', ['验收清单']);
  const acceptanceBlock = getBlock('验收清单', []);

  const toList = (raw: string) =>
    raw
      .split('\n')
      .map(line => line.trim())
      .filter(Boolean)
      .map(line => line.replace(/^[-*]\s*/, '').replace(/^\d+[.)]\s*/, '').trim())
      .filter(Boolean);

  return {
    summary,
    steps: toList(stepsBlock),
    code: toList(codeBlock),
    acceptance: toList(acceptanceBlock),
  };
};

const QUICK_PROMPTS = [
  '做一个带粒子背景和视差的首页 Hero 区域',
  '做一个高质量文章分享卡片导出功能',
  '设计一个更有趣的文章阅读交互体验',
];

const AiIdeaLab = () => {
  const [goal, setGoal] = useState('');
  const [result, setResult] = useState<SectionResult>(EMPTY_RESULT);
  const [rawOutput, setRawOutput] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState('');
  const [copiedKey, setCopiedKey] = useState('');
  const abortRef = useRef<AbortController | null>(null);
  const userIdRef = useRef(getUserId());

  const runIdea = (input?: string) => {
    const query = (input || goal).trim();
    if (!query || isGenerating) return;

    setError('');
    setRawOutput('');
    setResult(EMPTY_RESULT);
    setIsGenerating(true);

    const controller = sendChatMessage(
      {
        query: `${PROMPT_TEMPLATE}\n\n用户需求：${query}`,
        userId: userIdRef.current,
      },
      {
        onChunk: chunk => {
          setRawOutput(prev => {
            const next = prev + chunk;
            setResult(parseSection(next));
            return next;
          });
        },
        onEnd: () => {
          setIsGenerating(false);
          abortRef.current = null;
        },
        onError: msg => {
          setError(msg);
          setIsGenerating(false);
          abortRef.current = null;
        },
      }
    );

    abortRef.current = controller;
  };

  const stop = () => {
    abortRef.current?.abort();
    abortRef.current = null;
    setIsGenerating(false);
  };

  const copyText = async (key: string, text: string) => {
    if (!text.trim()) return;
    try {
      await navigator.clipboard.writeText(text);
      setCopiedKey(key);
      window.setTimeout(() => setCopiedKey(''), 1200);
    } catch {
      setCopiedKey('');
    }
  };

  const buildChecklistText = () => {
    const blocks = [
      result.summary ? `[方案摘要]\n${result.summary}` : '',
      result.steps.length > 0 ? `[执行步骤]\n${result.steps.map((item, idx) => `${idx + 1}. ${item}`).join('\n')}` : '',
      result.code.length > 0 ? `[代码要点]\n${result.code.map(item => `- ${item}`).join('\n')}` : '',
      result.acceptance.length > 0 ? `[验收清单]\n${result.acceptance.map(item => `- ${item}`).join('\n')}` : '',
    ].filter(Boolean);

    return blocks.join('\n\n');
  };

  return (
    <section className="ai-idea-lab">
      <div className="ai-idea-lab__head">
        <div className="ai-idea-lab__title-wrap">
          <Bot size={16} />
          <h3 className="ai-idea-lab__title">AI 创意工坊</h3>
          <span className="ai-idea-lab__badge">Agent Style</span>
        </div>
        <p className="ai-idea-lab__desc">输入目标，AI 直接给你可落地方案（摘要 / 步骤 / 代码要点 / 验收清单）。</p>
      </div>

      <div className="ai-idea-lab__input-wrap">
        <textarea
          className="ai-idea-lab__input"
          value={goal}
          onChange={e => setGoal(e.target.value)}
          onKeyDown={e => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
              e.preventDefault();
              runIdea();
            }
          }}
          placeholder="例如：我想做一个有高级感、且移动端流畅的文章封面动效"
          rows={3}
        />
        <div className="ai-idea-lab__actions">
          <button className="ai-idea-lab__btn" onClick={() => runIdea()} disabled={isGenerating || !goal.trim()}>
            {isGenerating ? <Loader2 size={15} className="ai-idea-lab__loading" /> : <Sparkles size={15} />}
            生成方案
          </button>
          {isGenerating && (
            <button className="ai-idea-lab__btn ai-idea-lab__btn--ghost" onClick={stop}>
              停止
            </button>
          )}
        </div>
      </div>

      <div className="ai-idea-lab__quick-list">
        {QUICK_PROMPTS.map(text => (
          <button key={text} className="ai-idea-lab__quick-item" onClick={() => runIdea(text)}>
            <Wand2 size={13} /> {text}
          </button>
        ))}
      </div>

      {error && <div className="ai-idea-lab__error">{error}</div>}

      {(rawOutput || isGenerating) && (
        <div className="ai-idea-lab__result">
          <div className="ai-idea-lab__card">
            <div className="ai-idea-lab__card-head">
              <div className="ai-idea-lab__card-title">方案摘要</div>
              <button className="ai-idea-lab__copy" onClick={() => copyText('summary', result.summary)}>
                {copiedKey === 'summary' ? <Check size={13} /> : <Copy size={13} />}
              </button>
            </div>
            <p>{result.summary || (isGenerating ? '正在生成摘要...' : '暂无')}</p>
          </div>

          <div className="ai-idea-lab__card">
            <div className="ai-idea-lab__card-head">
              <div className="ai-idea-lab__card-title">执行步骤</div>
              <button className="ai-idea-lab__copy" onClick={() => copyText('steps', result.steps.map((item, idx) => `${idx + 1}. ${item}`).join('\n'))}>
                {copiedKey === 'steps' ? <Check size={13} /> : <Copy size={13} />}
              </button>
            </div>
            <ol>
              {result.steps.length > 0 ? result.steps.map((step, index) => <li key={`${step}-${index}`}>{step}</li>) : <li>正在生成步骤...</li>}
            </ol>
          </div>

          <div className="ai-idea-lab__card">
            <div className="ai-idea-lab__card-head">
              <div className="ai-idea-lab__card-title">代码要点</div>
              <button className="ai-idea-lab__copy" onClick={() => copyText('code', result.code.map(item => `- ${item}`).join('\n'))}>
                {copiedKey === 'code' ? <Check size={13} /> : <Copy size={13} />}
              </button>
            </div>
            <ul>
              {result.code.length > 0 ? result.code.map((item, index) => <li key={`${item}-${index}`}>{item}</li>) : <li>正在提取代码要点...</li>}
            </ul>
          </div>

          <div className="ai-idea-lab__card">
            <div className="ai-idea-lab__card-head">
              <div className="ai-idea-lab__card-title">验收清单</div>
              <button className="ai-idea-lab__copy" onClick={() => copyText('acceptance', result.acceptance.map(item => `- ${item}`).join('\n'))}>
                {copiedKey === 'acceptance' ? <Check size={13} /> : <Copy size={13} />}
              </button>
            </div>
            <ul>
              {result.acceptance.length > 0 ? result.acceptance.map((item, index) => <li key={`${item}-${index}`}>{item}</li>) : <li>正在生成验收清单...</li>}
            </ul>
          </div>

          <button className="ai-idea-lab__copy-all" onClick={() => copyText('all', buildChecklistText())}>
            {copiedKey === 'all' ? <Check size={14} /> : <Copy size={14} />} 复制完整任务清单
          </button>
        </div>
      )}
    </section>
  );
};

export default AiIdeaLab;
