import { ArrowUpRight, CheckCircle2, ExternalLink, GitBranch, Layers3, Radio, ServerCog, ShieldCheck, Sparkles, TimerReset } from 'lucide-react';
import { Link } from 'react-router-dom';
import './ShowcasePage.css';

const architecture = [
  { label: '体验层', value: 'React 19 前台 · Vue 3 管理台', icon: Layers3 },
  { label: '服务层', value: 'Spring Boot 3 · JWT · RBAC', icon: ServerCog },
  { label: '数据层', value: 'MySQL · Redis · RabbitMQ', icon: Radio },
  { label: '交付层', value: 'Docker Compose · Nginx · HTTPS', icon: GitBranch },
];

const decisions = [
  {
    number: '01',
    title: '把高频读路径做轻',
    description: '列表接口使用摘要 DTO 与投影查询，避免把全文和关联对象带入首页、搜索与归档；热点结果交给 Redis 缓存。',
    evidence: 'ArticleSummaryResponse · ArchiveArticleProjection',
  },
  {
    number: '02',
    title: '把非关键链路异步化',
    description: '评论通知、操作日志和实时提醒通过 RabbitMQ 与 WebSocket 解耦，阅读主链路不需要等待外部副作用完成。',
    evidence: 'Publisher Confirm · 手动 ACK · STOMP',
  },
  {
    number: '03',
    title: '把线上风险显式化',
    description: '容器使用健康检查，Nginx 统一处理安全响应头和代理边界，敏感配置只从运行时环境进入部署。',
    evidence: '6 services · healthcheck · secret-free repo',
  },
];

const signals = [
  ['PUBLIC SYSTEM', 'cyruszhang.online'],
  ['DEPLOYMENT', 'Docker Compose / 6 services'],
  ['CONTENT PATH', 'Draft → Publish → Read → Discuss'],
  ['REALTIME', 'WebSocket + RabbitMQ'],
];

const ShowcasePage = () => (
  <div className="page-wrapper showcase-page">
    <section className="showcase-hero">
      <div className="showcase-hero__copy">
        <p className="showcase-kicker">SYSTEM NOTES / 2026</p>
        <h1>把内容做成一套可以长期运行的系统。</h1>
        <p className="showcase-hero__lead">
          这不是一个只展示文章的页面，而是一套从写作、发布、阅读到运维都真实跑在公网的全栈内容基础设施。
        </p>
        <div className="showcase-hero__actions">
          <a className="showcase-primary-action" href="https://github.com/zhulongqihan/my-blog" target="_blank" rel="noreferrer">
            查看源码 <ArrowUpRight size={15} />
          </a>
          <Link className="showcase-secondary-action" to="/about">了解系统背景</Link>
          <Link className="showcase-secondary-action" to="/archive">阅读项目文章</Link>
        </div>
      </div>

      <div className="showcase-status-card" aria-label="线上系统状态">
        <div className="showcase-status-card__head">
          <span className="showcase-status-dot" />
          <span>PUBLIC RUNTIME</span>
          <span className="showcase-status-card__code">LIVE / HTTPS</span>
        </div>
        <div className="showcase-status-card__title">My Blog Platform</div>
        <div className="showcase-status-card__url">cyruszhang.online</div>
        <div className="showcase-signal-list">
          {signals.map(([label, value]) => (
            <div key={label} className="showcase-signal">
              <span>{label}</span>
              <strong>{value}</strong>
            </div>
          ))}
        </div>
      </div>
    </section>

    <nav className="showcase-index" aria-label="系统展示导航">
      <span className="showcase-index__label">现场目录</span>
      <a href="#system-map">01 系统地图 <ArrowUpRight size={14} /></a>
      <a href="#engineering-notes">02 工程取舍 <ArrowUpRight size={14} /></a>
      <a href="#proof-of-work">03 验证信号 <ArrowUpRight size={14} /></a>
    </nav>

    <section id="system-map" className="showcase-section showcase-section--wide" aria-labelledby="system-map-title">
      <div className="showcase-section__heading">
        <p className="showcase-kicker">SYSTEM MAP</p>
        <h2 id="system-map-title">一条完整的内容生产链路</h2>
        <p>前台负责体验，后台负责秩序，后端负责边界，基础设施负责让这些选择在低配服务器上持续运行。</p>
      </div>
      <div className="showcase-architecture">
        {architecture.map(({ label, value, icon: Icon }, index) => (
          <div className="showcase-architecture__item" key={label}>
            <div className="showcase-architecture__icon"><Icon size={17} /></div>
            <span>{label}</span>
            <strong>{value}</strong>
            {index < architecture.length - 1 && <div className="showcase-architecture__connector" aria-hidden="true" />}
          </div>
        ))}
      </div>
    </section>

    <section id="engineering-notes" className="showcase-section" aria-labelledby="engineering-notes-title">
      <div className="showcase-section__heading">
        <p className="showcase-kicker">ENGINEERING NOTES</p>
        <h2 id="engineering-notes-title">我真正想展示的是取舍</h2>
        <p>技术栈只是表面，能解释为什么这样做、哪里没有这样做，才是项目最有价值的部分。</p>
      </div>
      <div className="showcase-decision-grid">
        {decisions.map(item => (
          <article className="showcase-decision" key={item.number}>
            <span className="showcase-decision__number">{item.number}</span>
            <h3>{item.title}</h3>
            <p>{item.description}</p>
            <code>{item.evidence}</code>
          </article>
        ))}
      </div>
    </section>

    <section id="proof-of-work" className="showcase-proof" aria-labelledby="proof-of-work-title">
      <div>
        <p className="showcase-kicker">PROOF OF WORK</p>
        <h2 id="proof-of-work-title">从功能到可验证的系统</h2>
        <p>缓存、队列、实时连接、限流和部署不是孤立的名词，它们共同服务于一条可观测、可恢复的内容链路。</p>
      </div>
      <div className="showcase-proof__list">
        <div><CheckCircle2 size={17} /><span>摘要接口与归档投影降低列表数据负担</span></div>
        <div><ShieldCheck size={17} /><span>运行时注入敏感配置，仓库只保留安全模板</span></div>
        <div><TimerReset size={17} /><span>前端路由拆包，文章详情按需加载高亮模块</span></div>
        <div><Sparkles size={17} /><span>AI SSE 能力与独立 Agent 项目形成内容线索</span></div>
      </div>
    </section>

    <section className="showcase-next">
      <div>
        <p className="showcase-kicker">NEXT ITERATION</p>
        <h2>让每一篇文章都成为一次工程复盘</h2>
        <p>接下来会继续把项目取舍和线上实践整理成可阅读、可验证、可复用的工程记录。</p>
      </div>
      <a href="https://github.com/zhulongqihan/my-blog" target="_blank" rel="noreferrer">GitHub <ExternalLink size={15} /></a>
    </section>
  </div>
);

export default ShowcasePage;
