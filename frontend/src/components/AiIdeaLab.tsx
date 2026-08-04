import { useMemo, useRef, useState } from 'react';
import { Bot, Sparkles, Wand2, Loader2, Copy, Check, PenSquare, Map, GraduationCap, MessageCircleMore } from 'lucide-react';
import { sendChatMessage } from '../services/aiApi';
import './AiIdeaLab.css';

type LabModeId = 'travel' | 'study' | 'writing' | 'planning';

interface SectionResult {
  summary: string;
  steps: string[];
  deliverables: string[];
  reminders: string[];
}

const EMPTY_RESULT: SectionResult = {
  summary: '',
  steps: [],
  deliverables: [],
  reminders: [],
};

const LAB_MODES: Array<{
  id: LabModeId;
  title: string;
  subtitle: string;
  icon: typeof Map;
  placeholder: string;
  actionLabel: string;
  quickPrompts: string[];
  systemPrompt: string;
}> = [
  {
    id: 'travel',
    title: '旅行与出行',
    subtitle: '路线、清单、备选方案',
    icon: Map,
    actionLabel: '生成出行方案',
    placeholder: '例如：帮我规划 3 天游南京的轻松路线，预算 2500 左右，想兼顾园林、夜景和本地小吃',
    quickPrompts: [
      '周末去杭州，想避开纯打卡路线，给我一个更松弛的两日安排',
      '帮我整理一次面试异地出行清单，包含证件、着装、电子设备和临时应对预案',
      '想做一份上海雨天一日路线，重点是室内展览、咖啡馆和地铁衔接',
    ],
    systemPrompt: `你是细致的中文旅行策划助手。请围绕用户的真实出行场景，输出【严格四段】内容：\n[结果速览]\n- 用 2~4 句概括整体策略、适用人群和节奏\n\n[行动建议]\n- 使用 1. 2. 3. 编号，给 4~6 步路线或准备建议\n\n[可直接使用]\n- 给 4~8 条可直接照抄的内容，例如时间表、打包清单、候选地点、对比建议\n\n[补充提醒]\n- 给 4~6 条风险提醒、预算提醒或替代方案\n\n不要输出其他标题。内容使用中文，语气自然，避免空话。`,
  },
  {
    id: 'study',
    title: '学习与准备',
    subtitle: '复习拆解、计划节奏、阶段目标',
    icon: GraduationCap,
    actionLabel: '生成学习方案',
    placeholder: '例如：我想在 6 周内补齐 Spring Boot、Redis 和项目表达，目标是准备 Java 后端实习面试',
    quickPrompts: [
      '帮我做一个 14 天的英语复习计划，每天 1.5 小时，重点是口语和听力',
      '我想在一个月内把前端工程化补到能讲项目，帮我拆任务节奏',
      '给我做一份考研复习周计划，要求每天晚上还能留一点项目时间',
    ],
    systemPrompt: `你是擅长学习拆解和阶段规划的中文助手。请围绕用户目标，输出【严格四段】内容：\n[结果速览]\n- 用 2~4 句概括目标难度、整体节奏和核心抓手\n\n[行动建议]\n- 使用 1. 2. 3. 编号，给 4~6 步阶段推进建议\n\n[可直接使用]\n- 给 4~8 条可直接执行的周计划、每日任务、复习模板或练习方式\n\n[补充提醒]\n- 给 4~6 条常见误区、节奏风险和调整办法\n\n不要输出其他标题。内容使用中文，强调可执行性。`,
  },
  {
    id: 'writing',
    title: '文案与表达',
    subtitle: '润色、自我介绍、开场白、长文起稿',
    icon: PenSquare,
    actionLabel: '生成表达草案',
    placeholder: '例如：帮我把一段偏生硬的项目介绍改得自然一点，适合面试自我介绍，时长控制在 90 秒',
    quickPrompts: [
      '帮我写一段更自然的实习投递自我介绍，不要太像模板',
      '把“我最近很焦虑但又不想放弃写作”扩成一篇真诚一点的博客开头',
      '给我一版适合微信发给老师的请教消息，语气礼貌但不要太拘谨',
    ],
    systemPrompt: `你是擅长中文表达、润色和内容起稿的助手。请围绕用户场景输出【严格四段】内容：\n[结果速览]\n- 用 2~4 句说明适合的表达方向、语气和结构\n\n[行动建议]\n- 使用 1. 2. 3. 编号，给 4~6 步表达组织建议\n\n[可直接使用]\n- 给 4~8 条可直接复制或稍改就能使用的句子、段落、开场白或版本选项\n\n[补充提醒]\n- 给 4~6 条语气边界、适用场景和避免模板感的提醒\n\n不要输出其他标题。内容使用中文，语言自然，不要营销腔。`,
  },
  {
    id: 'planning',
    title: '日常计划',
    subtitle: '任务拆分、节奏安排、执行清单',
    icon: MessageCircleMore,
    actionLabel: '生成执行清单',
    placeholder: '例如：我这周要准备答辩、改项目、投简历，帮我做一个不那么窒息的 7 天安排',
    quickPrompts: [
      '帮我安排一个周末大扫除计划，控制在半天内，按房间分步骤',
      '给我做一个搬宿舍清单，包含打包顺序、断舍离和最后检查项',
      '我总是在晚上拖延，帮我做一个可执行的下班后 3 小时安排模板',
    ],
    systemPrompt: `你是擅长把复杂事务拆成可执行清单的中文助手。请围绕用户当前约束输出【严格四段】内容：\n[结果速览]\n- 用 2~4 句概括总体安排原则、优先级和节奏感\n\n[行动建议]\n- 使用 1. 2. 3. 编号，给 4~6 步执行顺序建议\n\n[可直接使用]\n- 给 4~8 条可直接照做的时间块安排、待办模板或检查清单\n\n[补充提醒]\n- 给 4~6 条风险点、拖延应对或备选安排\n\n不要输出其他标题。内容使用中文，强调具体、务实。`,
  },
];

const parseSection = (text: string): SectionResult => {
  const getBlock = (name: string, nextNames: string[]) => {
    const nextPattern = nextNames.length ? `(?=\\n\\[(?:${nextNames.join('|')})\\]|$)` : '$';
    const reg = new RegExp(`\\[${name}\\]\\s*([\\s\\S]*?)${nextPattern}`, 'm');
    return (text.match(reg)?.[1] || '').trim();
  };

  const summary = getBlock('结果速览', ['行动建议', '可直接使用', '补充提醒']);
  const stepsBlock = getBlock('行动建议', ['可直接使用', '补充提醒']);
  const deliverablesBlock = getBlock('可直接使用', ['补充提醒']);
  const remindersBlock = getBlock('补充提醒', []);

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
    deliverables: toList(deliverablesBlock),
    reminders: toList(remindersBlock),
  };
};

const AiIdeaLab = () => {
  const [mode, setMode] = useState<LabModeId>('travel');
  const [goal, setGoal] = useState('');
  const [result, setResult] = useState<SectionResult>(EMPTY_RESULT);
  const [rawOutput, setRawOutput] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState('');
  const [copiedKey, setCopiedKey] = useState('');
  const abortRef = useRef<AbortController | null>(null);

  const activeMode = useMemo(() => LAB_MODES.find(item => item.id === mode) || LAB_MODES[0], [mode]);

  const runIdea = (input?: string) => {
    const query = (input || goal).trim();
    if (!query || isGenerating) return;

    setError('');
    setRawOutput('');
    setResult(EMPTY_RESULT);
    setIsGenerating(true);

    const controller = sendChatMessage(
      {
        systemPrompt: activeMode.systemPrompt,
        query: query,
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
      result.summary ? `[结果速览]\n${result.summary}` : '',
      result.steps.length > 0 ? `[行动建议]\n${result.steps.map((item, idx) => `${idx + 1}. ${item}`).join('\n')}` : '',
      result.deliverables.length > 0 ? `[可直接使用]\n${result.deliverables.map(item => `- ${item}`).join('\n')}` : '',
      result.reminders.length > 0 ? `[补充提醒]\n${result.reminders.map(item => `- ${item}`).join('\n')}` : '',
    ].filter(Boolean);

    return blocks.join('\n\n');
  };

  return (
    <section className="ai-idea-lab">
      <div className="ai-idea-lab__head">
        <div className="ai-idea-lab__title-wrap">
          <Bot size={16} />
          <h3 className="ai-idea-lab__title">AI 生活策划台</h3>
          <span className="ai-idea-lab__badge">Visitor Ready</span>
        </div>
        <p className="ai-idea-lab__desc">不只给博客内部功能建议，而是把出行、学习、表达和日常安排整理成可以直接拿去用的中文方案。</p>
      </div>

      <div className="ai-idea-lab__mode-grid">
        {LAB_MODES.map(item => {
          const Icon = item.icon;
          const isActive = item.id === activeMode.id;
          return (
            <button
              key={item.id}
              className={`ai-idea-lab__mode-card${isActive ? ' ai-idea-lab__mode-card--active' : ''}`}
              onClick={() => {
                setMode(item.id);
                setGoal('');
                setResult(EMPTY_RESULT);
                setRawOutput('');
                setError('');
              }}
            >
              <span className="ai-idea-lab__mode-icon"><Icon size={16} /></span>
              <span className="ai-idea-lab__mode-title">{item.title}</span>
              <span className="ai-idea-lab__mode-subtitle">{item.subtitle}</span>
            </button>
          );
        })}
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
          placeholder={activeMode.placeholder}
          rows={3}
        />
        <div className="ai-idea-lab__actions">
          <button className="ai-idea-lab__btn liquid-btn" onClick={() => runIdea()} disabled={isGenerating || !goal.trim()}>
            {isGenerating ? <Loader2 size={15} className="ai-idea-lab__loading" /> : <Sparkles size={15} />}
            {activeMode.actionLabel}
          </button>
          {isGenerating && (
            <button className="ai-idea-lab__btn ai-idea-lab__btn--ghost liquid-btn" onClick={stop}>
              停止
            </button>
          )}
        </div>
      </div>

      <div className="ai-idea-lab__quick-list">
        {activeMode.quickPrompts.map(text => (
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
              <div className="ai-idea-lab__card-title">结果速览</div>
              <button className="ai-idea-lab__copy" onClick={() => copyText('summary', result.summary)}>
                {copiedKey === 'summary' ? <Check size={13} /> : <Copy size={13} />}
              </button>
            </div>
            <p>{result.summary || (isGenerating ? '正在生成摘要...' : '暂无')}</p>
          </div>

          <div className="ai-idea-lab__card">
            <div className="ai-idea-lab__card-head">
              <div className="ai-idea-lab__card-title">行动建议</div>
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
              <div className="ai-idea-lab__card-title">可直接使用</div>
              <button className="ai-idea-lab__copy" onClick={() => copyText('deliverables', result.deliverables.map(item => `- ${item}`).join('\n'))}>
                {copiedKey === 'deliverables' ? <Check size={13} /> : <Copy size={13} />}
              </button>
            </div>
            <ul>
              {result.deliverables.length > 0 ? result.deliverables.map((item, index) => <li key={`${item}-${index}`}>{item}</li>) : <li>正在整理可直接使用的内容...</li>}
            </ul>
          </div>

          <div className="ai-idea-lab__card">
            <div className="ai-idea-lab__card-head">
              <div className="ai-idea-lab__card-title">补充提醒</div>
              <button className="ai-idea-lab__copy" onClick={() => copyText('reminders', result.reminders.map(item => `- ${item}`).join('\n'))}>
                {copiedKey === 'reminders' ? <Check size={13} /> : <Copy size={13} />}
              </button>
            </div>
            <ul>
              {result.reminders.length > 0 ? result.reminders.map((item, index) => <li key={`${item}-${index}`}>{item}</li>) : <li>正在补充提醒与边界...</li>}
            </ul>
          </div>

          <button className="ai-idea-lab__copy-all" onClick={() => copyText('all', buildChecklistText())}>
            {copiedKey === 'all' ? <Check size={14} /> : <Copy size={14} />} 复制完整方案
          </button>
        </div>
      )}
    </section>
  );
};

export default AiIdeaLab;
