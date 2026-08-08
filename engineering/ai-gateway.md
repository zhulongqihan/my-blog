# AI Gateway 与 Agent-ready 内容工具

## 目标

把博客里的 AI 功能从“浏览器直连模型接口”升级为可观测、可控、可被外部智能体复用的内容基础设施，同时保持现有 SSE 交互不变。

## 已落地闭环

```text
React 阅读助手 / AI 创意工坊
          │ POST /api/ai/chat
          ▼
Spring Boot AI Gateway
  ├─ Redis Lua/AOP 限流
  ├─ BlogContentToolService：按文章 ID + 关键词做受控上下文检索
  ├─ SSE 代理：meta → provider chunks → [DONE]
  └─ Redis List：requestId / TTFT / total latency / source / error
          │
          ├─ DashScope OpenAI-compatible Chat Completions
          └─ /api/mcp：initialize / tools/list / tools/call
```

## 关键设计

- **密钥边界**：前端只访问同源 `/api/ai/chat`，`DASHSCOPE_API_KEY` 仅注入 Backend；Nginx 只负责 SSE 透传和关闭缓冲。
- **上下文边界**：阅读助手传 `articleId`，服务端只检索已发布文章；自定义 Prompt 有长度上限，文章上下文最多 9000 字符。
- **工具复用**：`search_articles` 与 `get_article` 共用 `BlogContentToolService`，AI Gateway 与 MCP 不维护两套内容查询逻辑。
- **可观测性**：每次请求生成 `requestId`，记录 `retrievalCount`、输入/输出字符数、首字节延迟、总耗时和错误状态，后台 `/admin/ai-observability` 可查看最近 200 条轨迹。
- **流式协议**：Gateway 先发送 `meta` 事件，再转发 OpenAI 兼容的 `data` chunk；前端同时兼容 `meta/error/data/[DONE]`，便于展示来源和定位故障。

## MCP 最小协议面

`POST /api/mcp` 支持 MCP 2025-06-18 的最小 JSON-RPC 方法：

- `initialize`
- `tools/list`
- `tools/call`：`search_articles`、`get_article`

这使博客内容可以被支持 MCP 的 Agent/IDE 作为受控上下文源接入。工具当前只读公开文章，不执行写操作。

## 面试时的诚实表述

可以说：

> 我没有把模型调用留在前端，而是做了一个 Spring Boot AI Gateway：统一限流、服务端上下文检索、SSE 转发和请求轨迹；同一内容工具再通过 MCP 暴露给外部 Agent。当前检索层先采用有边界的标题/摘要/正文词法评分，来源和时延都通过后台面板可观测，后续可以无损替换为向量召回 + BM25 + rerank。

不应直接说当前已经落地 Milvus、BM25、OpenTelemetry SDK 或完整 Supervisor 多 Agent 编排；这些是下一阶段可接入的能力。

## 后续演进

1. 将 `BlogContentToolService` 的候选集替换为向量召回 + BM25 混合检索，并在 trace 中记录召回和重排分数。
2. 接入 Spring AI ChatClient/Advisor，保留当前 Gateway 与工具接口，增加结构化输出校验和 tool-calling loop。
3. 使用 OpenTelemetry GenAI 语义约定补齐 `chat`、`retrieval`、`execute_tool` span，并将 trace 导出到 Jaeger/Tempo。
