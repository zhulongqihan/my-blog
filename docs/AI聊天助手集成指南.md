# AI 智能助手实施指南

> **版本**: v2.1.1  
> **日期**: 2026-03-09  
> **技术栈**: DashScope qwen-plus + React 19 + SSE Streaming + Nginx 服务端 Key 注入  
> **目标**: 在博客中集成可落地的 AI 创意工坊，并保持生产环境 Key 零暴露

---

## 📋 目录

1. [项目概述](#1-项目概述)
2. [架构设计](#2-架构设计)
3. [当前生产实现](#3-当前生产实现)
4. [代码实施步骤](#4-代码实施步骤)
5. [技术要点详解](#5-技术要点详解)
6. [测试与调试](#6-测试与调试)
7. [部署到生产环境](#7-部署到生产环境)
8. [常见问题排查](#8-常见问题排查)
9. [面试话术参考](#9-面试话术参考)

---

## 1. 项目概述

### 1.1 为什么做这个功能

面试目标并不只是“接一个聊天框”，而是把 AI 能力以真实可上线的方式接入已有项目。

当前博客采用 **DashScope OpenAI 兼容接口** 方案，而不是早期的 Dify Cloud 方案，原因是：
- ✅ 部署链路更短，前端只需要对接兼容的 `/chat/completions`
- ✅ 通过 Nginx 注入 Authorization，前端仓库不暴露任何真实 Key
- ✅ 支持 SSE 流式输出，仍然能展示流式渲染与交互设计能力
- ✅ 更适合当前低配 ECS 的稳定部署，不依赖额外第三方工作流平台

### 1.2 功能描述

- 页面右下角浮动 AI 聊天气泡（✨ 图标）
- 首页集成 AI 创意工坊，输入需求后生成结构化前端方案
- SSE 流式输出，逐段渲染摘要 / 步骤 / 代码要点 / 验收清单
- 通过 `/ai-api/` 反向代理转发到 DashScope，前端不接触真实 Key
- 支持暗/亮主题，大地色调设计一致
- 支持复制结果片段与整单复制

### 1.3 技术选型理由

| 方案 | 优点 | 缺点 | 选择 |
|------|------|------|------|
| **DashScope + Nginx Key 注入** | 接口兼容 OpenAI、接入轻、服务端保密 Key、易部署 | 需要自己处理前端流式解析 | ✅ 选用 |
| Dify Cloud + DeepSeek | 有可视化编排、RAG 开箱即用 | 依赖第三方平台、生产接入更重 | 历史方案 |
| Langchain + 自建后端 | 灵活度最高 | 开发量大、需要自己管向量数据库 | ❌ |
| Coze | 字节生态 | 国际版限制多 | ❌ |
| 直接调 OpenAI API | 简单 | 没有 RAG、无法展示 Agent 经验 | ❌ |

---

## 2. 架构设计

### 2.1 数据流

```
用户输入
  ↓
React AiIdeaLab 组件
  ↓ POST /ai-api/chat/completions (SSE)
Nginx 反向代理
  ├── 注入 Authorization: Bearer YOUR_DASHSCOPE_API_KEY
  └── 转发至 DashScope OpenAI 兼容接口
  ↓
DashScope qwen-plus
  ↓ SSE 流式返回
AiIdeaLab 逐 chunk 解析并拼接结构化内容
  ↓
react-markdown + react-syntax-highlighter 渲染
```

### 2.2 文件结构

```
frontend/
├── .env.development          # VITE_AI_API_URL, VITE_AI_API_KEY（开发环境可选）
├── .env.production           # 生产环境通常留空，走 Nginx 注入
├── src/
│   ├── services/
│   │   ├── api.ts             # 已有 — axios 封装
│   │   ├── index.ts           # 已有 — API 聚合
│   │   └── aiApi.ts           # DashScope SSE 流式 API
│   ├── components/
│   │   ├── AiIdeaLab.tsx      # AI 创意工坊组件
│   │   └── AiIdeaLab.css      # 对应样式
│   └── App.tsx                # 已有 — 页面挂载与主题/命令面板
└── docker/nginx/nginx.conf    # 生产环境注入 Authorization 并关闭 SSE 缓冲
```

### 2.3 DashScope API 接口规范

**端点**: `POST {AI_API_URL}/chat/completions`

**请求头**:
```
Content-Type: application/json
```

**请求体**:
```json
{
  "model": "qwen-plus",
  "stream": true,
  "messages": [
    { "role": "system", "content": "你是一个前端创意工坊助手" },
    { "role": "user", "content": "请帮我设计一个作品集首页" }
  ]
}
```

**SSE 响应格式**:
```
data: {"choices":[{"delta":{"content":"方"}}]}
data: {"choices":[{"delta":{"content":"案"}}]}
data: [DONE]
```

---

## 3. 当前生产实现

当前线上实现已经稳定运行，关键点如下：

1. **前端调用地址**：默认请求 `/ai-api/chat/completions`
2. **Nginx 负责注入 Key**：真实 DashScope Key 仅保留在服务器配置，不进入 GitHub 仓库
3. **生产部署位置**：博客运行在 `/www/my-blog`，Nginx 容器将 `/ai-api/` 代理到 DashScope
4. **代码入口**：核心逻辑位于 `frontend/src/components/AiIdeaLab.tsx` 与 `frontend/src/services/aiApi.ts`
5. **安全边界**：仓库中只能保留占位符 `YOUR_DASHSCOPE_API_KEY`，真实 Key 在服务器注入

如果后续你重新换模型或换服务商，优先保留这个安全边界，再调整前端请求体和服务端代理。

### 4.1 前端服务层

- `aiApi.ts` 负责发起流式请求、读取 SSE chunk，并把文本逐步回传给组件
- `AI_API_URL` 默认取 `/ai-api`，开发环境可通过环境变量覆盖
- 若将来切换模型，只需要调整 `model` 字段与请求体结构

### 4.2 服务端代理层

- 生产环境由 `docker/nginx/nginx.conf` 处理 `/ai-api/` 代理
- `proxy_buffering off` 是 SSE 流式输出的必要条件
- Authorization 头由 Nginx 注入，因此前端构建产物和仓库都不包含真实 Key

### 4.3 生产环境建议

- 仓库中的环境变量文件只保留占位符
- 服务器上注入真实 Key 后，避免再次提交到 Git 仓库
- 改完 Nginx 后用 `docker compose up -d --build nginx` 或整套重建验证配置
3. 选择应用类型：**Agent**
4. 应用名称：`Blog AI Assistant`
5. 描述：`个人博客 AI 助手，可以回答博客内容相关问题`

### 3.5 配置 Agent 应用

#### 系统提示词（System Prompt）:

```
你是一个个人博客的 AI 助手，博客属于一位南京大学软件工程硕士研究生。

你的职责：
1. 友好地回答访客关于博客内容的问题
2. 基于知识库中的博客文章提供准确信息
3. 如果问题超出博客内容范围，礼貌地说明并尽量提供帮助
4. 回复使用中文，保持专业但友好的语气
5. 适当使用 Markdown 格式（代码块、列表等）使回复更易读

注意事项：
- 你代表博客作者（Cyrus Zhang），但不要假装是作者本人
- 涉及技术问题时，尽量引用知识库中的文章内容
- 回复要简洁有价值，避免过于冗长
```

#### 模型配置:
- **模型**: DeepSeek Chat（deepseek-chat）
- **温度**: 0.7（平衡创造性和准确性）
- **最大 Token**: 2048

### 3.6 创建知识库（Knowledge Base / RAG）

1. 在 Dify 左侧菜单进入 **知识库（Knowledge）**
2. 点击 **创建知识库**
3. 名称：`Blog Articles`
4. **上传文档**：
   - 方式一：导出博客文章为 `.md` 或 `.txt` 文件上传
   - 方式二：直接从博客数据库导出文章内容
   - 建议上传 10-20 篇代表性文章
5. **索引方式**: 选择 **高质量模式（High Quality）** — 使用 embedding 模型
6. **分段设置**: 默认即可（500 tokens/段，50 tokens 重叠）
7. 等待索引完成（通常 1-5 分钟）

### 3.7 将知识库关联到 Agent

1. 回到 Agent 应用的编辑页面
2. 在 **上下文（Context）** 区域，点击 **添加（Add）**
3. 选择刚创建的 `Blog Articles` 知识库
4. 检索设置：
   - **Top K**: 3（每次检索返回 3 段最相关内容）
   - **Score 阈值**: 0.5（相关度大于 0.5 才返回）

### 3.8 获取 Dify API Key

1. 在 Agent 应用页面，点击左侧 **访问 API（API Access）**
2. 点击右上角 **API 密钥（API Key）**
3. 创建新密钥，复制保存（格式：`app-xxxxxxxxxxxxxxxxxxxxxxxx`）
4. 记录 API 基础 URL：`https://api.dify.ai/v1`

### 3.9 测试 API 连通性

在终端中测试（替换你的 API Key）：

```bash
curl -X POST 'https://api.dify.ai/v1/chat-messages' \
  -H 'Authorization: Bearer app-你的API密钥' \
  -H 'Content-Type: application/json' \
  -d '{
    "inputs": {},
    "query": "你好，介绍一下这个博客",
    "response_mode": "blocking",
    "user": "test-user"
  }'
```

如果返回 JSON 响应，说明配置成功。

---

## 4. 代码实施步骤

### Step 1: 安装依赖

```bash
cd frontend
npm install react-markdown remark-gfm
```

需要安装的包：
- `react-markdown` — 将 AI 回复的 Markdown 文本渲染为 React 组件
- `remark-gfm` — 支持 GFM（GitHub Flavored Markdown），包括表格、任务列表等

> 注意：`react-syntax-highlighter` 已安装，无需重复安装

### Step 2: 配置环境变量

**修改 `frontend/.env.development`**:
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_DEBUG=true
VITE_DIFY_API_URL=https://api.dify.ai/v1
VITE_DIFY_API_KEY=app-你的Dify应用API密钥
```

**修改 `frontend/.env.production`**:
```env
VITE_API_BASE_URL=/api
VITE_DEBUG=false
VITE_DIFY_API_URL=https://api.dify.ai/v1
VITE_DIFY_API_KEY=app-你的Dify应用API密钥
```

> ⚠️ 生产环境建议通过 Nginx 代理隐藏 API Key，详见 [第9节](#9-部署到生产环境)

### Step 3: 创建 Dify API 服务 (`difyApi.ts`)

**文件路径**: `frontend/src/services/difyApi.ts`

**核心功能**:
- `sendChatMessage()` — 发送聊天消息，返回 SSE 流式响应
- SSE 事件解析器 — 解析 `text/event-stream` 格式
- 对话 ID 管理 — 维持多轮对话上下文
- 错误处理 — 网络错误、API 限流、认证失败

**关键技术点**:
```typescript
// 使用原生 fetch（不是 axios）实现 SSE
const response = await fetch(`${DIFY_API_URL}/chat-messages`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${DIFY_API_KEY}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    inputs: {},
    query: message,
    response_mode: 'streaming',
    conversation_id: conversationId || '',
    user: userId,
  }),
});

// 逐行读取 SSE 流
const reader = response.body!.getReader();
const decoder = new TextDecoder();
```

**为什么用 fetch 而不是 axios**:
- axios 不支持原生 SSE 流式读取
- fetch 的 `ReadableStream` 可以逐 chunk 读取，实现打字机效果
- 无需额外依赖（如 `eventsource` 库）

### Step 4: 创建 AI 聊天组件 (`AiChat.tsx`)

**文件路径**: `frontend/src/components/AiChat.tsx`

**组件结构**:
```
<div className="ai-chat">
  {/* 气泡按钮 */}
  <button className="ai-chat__bubble">✨</button>
  
  {/* 聊天面板（展开时显示） */}
  <div className="ai-chat__panel">
    {/* 头部 - 标题 + 关闭 */}
    <div className="ai-chat__header">
      <span>AI 助手</span>
      <button onClick={close}>✕</button>
    </div>
    
    {/* 消息列表 */}
    <div className="ai-chat__messages">
      {messages.map(msg => (
        <div className={`ai-chat__message --${msg.role}`}>
          {msg.role === 'assistant' 
            ? <ReactMarkdown>{msg.content}</ReactMarkdown>
            : msg.content}
        </div>
      ))}
    </div>
    
    {/* 输入区 */}
    <div className="ai-chat__input-area">
      <input placeholder="问我任何问题..." />
      <button>发送</button>
    </div>
  </div>
</div>
```

**状态管理**:
```typescript
interface Message {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number;
}

// 核心状态
const [isOpen, setIsOpen] = useState(false);          // 面板展开/折叠
const [messages, setMessages] = useState<Message[]>([]); // 消息列表
const [input, setInput] = useState('');                 // 输入内容
const [isStreaming, setIsStreaming] = useState(false);   // 是否正在流式输出
const [conversationId, setConversationId] = useState(''); // Dify 对话 ID
```

**流式输出实现**:
```typescript
// 发送消息后，逐 chunk 追加到最后一条 assistant 消息
const handleSend = async () => {
  // 1. 添加 user 消息
  // 2. 添加空的 assistant 消息（占位）
  // 3. 调用 difyApi.sendChatMessage()，传入 onChunk 回调
  // 4. onChunk 中：setMessages(prev => 更新最后一条消息的 content)
  // 5. 流结束时 setIsStreaming(false)
};
```

**Markdown 渲染配置**:
```typescript
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';

// 在渲染 assistant 消息时：
<ReactMarkdown
  remarkPlugins={[remarkGfm]}
  components={{
    code({ node, inline, className, children, ...props }) {
      const match = /language-(\w+)/.exec(className || '');
      return !inline && match ? (
        <SyntaxHighlighter style={oneDark} language={match[1]}>
          {String(children).replace(/\n$/, '')}
        </SyntaxHighlighter>
      ) : (
        <code className={className} {...props}>{children}</code>
      );
    },
  }}
>
  {message.content}
</ReactMarkdown>
```

### Step 5: 创建聊天样式 (`AiChat.css`)

**文件路径**: `frontend/src/components/AiChat.css`

**设计原则**:
- 完全使用项目已有 CSS 变量（`--bg-primary`, `--accent-rust` 等）
- BEM 命名规范（与其他组件一致）
- 响应式设计（移动端全屏，桌面端浮动面板）
- 流畅过渡动画（使用 `--transition-normal`）

**核心样式规格**:

| 元素 | 样式 |
|------|------|
| 气泡按钮 | 56px 圆形, `--accent-rust` 背景, 右下角固定, z-index: 1000 |
| 聊天面板 | 400px × 560px, 右下角定位, 圆角 12px, 阴影 |
| 消息气泡（用户） | `--accent-rust` 背景, 白色文字, 右对齐 |
| 消息气泡（AI） | `--bg-secondary` 背景, 左对齐, 带 Markdown 样式 |
| 输入框 | 圆角, `--border-color` 边框, focus 高亮 |
| 发送按钮 | `--accent-olive` 背景, 白色文字 |
| 移动端（<768px） | 全屏面板，气泡缩至 48px |

**动画效果**:
- 面板展开：`transform: scale(0.9) translateY(20px)` → `scale(1) translateY(0)`
- 新消息：`fadeInUp` 进场动画
- 流式输出：闪烁光标动画
- 气泡按钮：悬停缩放 + 脉冲动画

### Step 6: 挂载到 App.tsx

在 `frontend/src/App.tsx` 中：

```tsx
import AiChat from './components/AiChat';

// 在 <PixelCat /> 之后添加
<PixelCat />
<AiChat />
```

### Step 7: 可选 — Nginx 反向代理（生产环境）

在 Nginx 配置中添加 Dify API 代理，隐藏 API Key：

```nginx
location /dify-api/ {
    proxy_pass https://api.dify.ai/v1/;
    proxy_set_header Authorization "Bearer app-你的API密钥";
    proxy_set_header Content-Type "application/json";
    proxy_buffering off;           # 关键：SSE 不能缓冲
    proxy_cache off;
    proxy_read_timeout 120s;
}
```

这样前端只需请求 `/dify-api/chat-messages`，API Key 隐藏在服务器端。

---

## 5. Dify 平台配置详细教程

### 5.1 Dify 是什么

Dify 是一个开源的 LLM 应用开发平台，提供：
- **可视化工作流编排** — 拖拽式构建 AI 工作流
- **RAG 引擎** — 知识库管理 + 向量检索
- **Agent 框架** — 支持工具调用、推理链
- **API 服务** — 一键发布为 API，支持 SSE 流式

### 5.2 Agent vs Chatbot 的区别

| 类型 | 特点 | 适用场景 |
|------|------|----------|
| Chatbot | 简单对话，直接调用 LLM | 纯聊天 |
| **Agent** | 可调用工具、知识库检索、推理链 | **我们的场景** ✅ |
| Workflow | 复杂编排，多步骤流程 | 企业级流程 |

我们选择 **Agent** 类型，因为它：
- 自动决定何时检索知识库
- 支持 ReAct 推理模式
- 面试中可以展示 Agent 的理解

### 5.3 知识库优化建议

**文章格式优化**:
```markdown
# 文章标题：Spring Boot 整合 Redis 缓存

## 元信息
- 作者：Cyrus Zhang
- 分类：后端开发
- 标签：Spring Boot, Redis, 缓存

## 正文
（文章内容）
```

**分段策略**:
- 每段 500 tokens + 50 tokens 重叠
- 确保代码块不被截断
- 元信息帮助提高检索精度

---

## 6. DeepSeek API 配置教程

### 6.1 为什么选 DeepSeek

| 维度 | DeepSeek | GPT-4 | Claude |
|------|----------|-------|--------|
| 中文能力 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 价格 | 极低（¥1/百万 tokens） | 昂贵 | 昂贵 |
| 免费额度 | 新用户赠送 | 无 | 有限 |
| Dify 集成 | 原生支持 ✅ | 原生支持 | 原生支持 |
| 代码能力 | 优秀 | 优秀 | 优秀 |

### 6.2 API Key 安全

- **开发环境**: 放在 `.env.development` 中（已 gitignore）
- **生产环境**: 通过 Nginx 代理隐藏，不暴露给前端
- **Dify 平台**: DeepSeek Key 存在 Dify 服务端，不经过前端

---

## 7. 技术要点详解

### 7.1 SSE（Server-Sent Events）流式传输

**什么是 SSE**:
- HTTP 长连接，服务端持续推送数据
- 与 WebSocket 的区别：SSE 是单向（服务端→客户端），基于 HTTP
- 非常适合 LLM 流式输出场景

**前端实现原理**:
```
1. fetch() 发起 POST 请求
2. 获取 response.body（ReadableStream）
3. 创建 reader = body.getReader()
4. 循环 reader.read() 获取每个 chunk
5. TextDecoder 将 Uint8Array 转为字符串
6. 按 "\n\n" 分割 SSE 事件
7. 解析 "data: {...}" 获取 JSON
8. 提取 answer 字段追加到消息内容
9. 触发 React 重新渲染
10. 直到 event === "message_end" 结束
```

### 7.2 对话上下文管理

Dify 通过 `conversation_id` 管理多轮对话：
- 首次对话：不传 `conversation_id`，Dify 返回新的 ID
- 后续对话：传入之前的 `conversation_id`，实现上下文关联
- 新对话：清空 `conversation_id`，开始新的对话链

### 7.3 用户标识

Dify 要求传入 `user` 字段标识用户：
```typescript
// 生成匿名用户 ID，存储在 localStorage
const getUserId = () => {
  let userId = localStorage.getItem('dify-user-id');
  if (!userId) {
    userId = `blog-visitor-${crypto.randomUUID()}`;
    localStorage.setItem('dify-user-id', userId);
  }
  return userId;
};
```

### 7.4 错误处理策略

| 错误码 | 含义 | 处理方式 |
|--------|------|----------|
| 400 | 请求参数错误 | 检查请求体格式 |
| 401 | API Key 无效 | 检查环境变量配置 |
| 429 | 请求频率超限 | 显示"请稍后再试"，自动重试 |
| 500 | Dify 服务器错误 | 显示友好提示 |
| 网络错误 | 连接失败 | 提示检查网络 |

---

## 8. 测试与调试

### 8.1 开发环境测试

```bash
# 1. 确保环境变量已配置
cat frontend/.env.development

# 2. 启动开发服务器
cd frontend && npm run dev

# 3. 打开浏览器 http://localhost:5173
# 4. 点击右下角 ✨ 气泡
# 5. 输入"你好"测试连通性
# 6. 输入"这个博客有哪些文章？"测试 RAG
```

### 8.2 调试 SSE 流

打开浏览器 DevTools → Network 面板：
1. 找到 `chat-messages` 请求
2. 切换到 **EventStream** 标签页
3. 可以看到每个 SSE 事件的实时数据

### 8.3 常见测试用例

| 测试场景 | 输入 | 期望 |
|----------|------|------|
| 基本对话 | "你好" | 友好的欢迎语 |
| RAG 检索 | "博客有哪些技术文章？" | 基于知识库回答 |
| 代码问题 | "Spring Boot 怎么配置 Redis？" | 带代码块的回答 |
| 多轮对话 | 连续追问 | 保持上下文 |
| 空输入 | "" | 禁止发送 |
| 超长输入 | 1000+ 字 | 正常处理 |
| 网络断开 | 断网发送 | 友好错误提示 |

---

## 9. 部署到生产环境

### 9.1 Nginx 配置

在服务器的 Nginx 配置中添加 Dify API 代理：

```nginx
# 在 server 块中添加
location /dify-api/ {
    proxy_pass https://api.dify.ai/v1/;
    proxy_set_header Host api.dify.ai;
    proxy_set_header Authorization "Bearer app-你的API密钥";
    proxy_set_header Content-Type "application/json";
    
    # SSE 关键配置
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 120s;
    proxy_send_timeout 120s;
    
    # 允许前端跨域（如果需要）
    add_header Access-Control-Allow-Origin *;
    add_header Access-Control-Allow-Methods "POST, OPTIONS";
    add_header Access-Control-Allow-Headers "Content-Type, Authorization";
}
```

### 9.2 生产环境变量

```env
# frontend/.env.production
VITE_DIFY_API_URL=/dify-api    # 通过 Nginx 代理
VITE_DIFY_API_KEY=              # 空，Key 在 Nginx 配置中
```

### 9.3 Docker 重建部署

```bash
# SSH 到服务器
ssh root@118.31.221.81

# 拉取最新代码
cd /opt/myblog && git pull

# 重建前端 + Nginx
docker compose build frontend nginx
docker compose up -d
```

---

## 10. 常见问题排查

### Q1: CORS 错误

**现象**: 浏览器报 `Access-Control-Allow-Origin` 错误

**解决**:
- Dify Cloud 默认允许 CORS，检查 API URL 是否正确
- 如果仍有问题，使用 Nginx 代理方案

### Q2: SSE 流中断

**现象**: 回答生成到一半突然停止

**解决**:
- 检查网络连接稳定性
- 增加 `proxy_read_timeout` 值
- DeepSeek 的 max_tokens 可能太小

### Q3: API Key 无效

**现象**: 返回 401 错误

**解决**:
- 确认 Dify 应用的 API Key（不是 DeepSeek 的 Key）
- 确认 Key 以 `app-` 开头
- 在 Dify 平台重新生成 Key

### Q4: 知识库检索不到内容

**现象**: AI 回答说不知道博客内容

**解决**:
- 确认知识库索引已完成
- 确认 Agent 已关联知识库
- 调低 Score 阈值（如 0.3）
- 检查上传文档的内容质量

### Q5: 打字机效果不流畅

**现象**: 文字不是逐字出现，而是一大段跳出

**解决**:
- 确认 `response_mode` 为 `streaming`
- 确认 Nginx 的 `proxy_buffering off` 已配置
- 检查前端是否每次 chunk 都触发了 setState

---

## 11. 面试话术参考

### 11.1 "你有使用 Dify 的经验吗？"

> 有的。我在个人博客项目中集成了基于 Dify 的 AI 聊天助手功能。
> 
> 我使用 Dify Cloud 搭建了一个 Agent 类型的应用，底层接入 DeepSeek 大模型，并创建了知识库实现 RAG 检索。当用户提问时，Agent 会自动检索博客文章内容，结合上下文生成回答。
> 
> 前端使用 React + SSE 流式传输实现了打字机效果的实时对话体验。整个流程涉及：模型选型、Prompt Engineering、知识库构建与分段策略、API 集成、SSE 流式解析等。

### 11.2 "说说 RAG 的原理？"

> RAG（Retrieval-Augmented Generation）的核心思想是在 LLM 生成回答前，先从外部知识库中检索相关内容，将检索结果作为上下文注入到提示词中，让模型基于这些信息生成更准确的回答。
> 
> 在我的项目中，知识库的构建流程是：
> 1. 将博客文章上传到 Dify 知识库
> 2. Dify 将文章分段（Chunking），每段约 500 tokens
> 3. 每段文本通过 embedding 模型转为向量，存入向量数据库
> 4. 用户提问时，问题也转为向量，在向量库中查找最相似的 Top-K 段落
> 5. 将检索到的段落作为 context 注入提示词，LLM 基于此生成回答

### 11.3 "Agent 和普通 Chatbot 有什么区别？"

> Agent 相比普通 Chatbot 最大的区别是具备**自主决策能力**。
> 
> 普通 Chatbot 只是简单地将用户输入传给 LLM 然后返回结果。而 Agent 可以：
> 1. **判断是否需要使用工具** — 比如检索知识库、调用 API
> 2. **规划执行步骤** — 使用 ReAct 等推理框架分步解决问题
> 3. **迭代优化** — 如果首次检索结果不够好，可以调整查询再次检索
> 
> 在 Dify 中，Agent 类型的应用会自动通过 Function Calling 机制判断何时检索知识库，这比静态配置的 RAG 管道更灵活。

---

## 附录 A: 完整环境变量清单

| 变量名 | 开发环境值 | 生产环境值 | 说明 |
|--------|-----------|-----------|------|
| `VITE_API_BASE_URL` | `http://localhost:8080` | `/api` | 后端 API |
| `VITE_DEBUG` | `true` | `false` | 调试模式 |
| `VITE_DIFY_API_URL` | `https://api.dify.ai/v1` | `/dify-api` | Dify API |
| `VITE_DIFY_API_KEY` | `app-xxx` | （空，Nginx 注入） | Dify Key |

## 附录 B: 参考链接

- [Dify 官方文档](https://docs.dify.ai/)
- [Dify API 参考](https://docs.dify.ai/guides/application-publishing/developing-with-apis)
- [DeepSeek 平台](https://platform.deepseek.com/)
- [react-markdown 文档](https://github.com/remarkjs/react-markdown)
- [SSE 规范 (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)

---

> 📝 本文档由 GitHub Copilot 生成，作为 v1.9.2 AI 聊天助手功能的完整实施参考。
