# My-blog 求职包装与逐版简历记录

> 本文档只包装个人博客项目，不把 AeroFlow Sentinel 的独立实现冒充成博客已经完成的能力。所有数字和技术名词必须能在代码、线上接口或部署记录中复核。

## 当前推荐简历版本

### 项目名称

**My-blog｜全栈内容个人博客与 Agent-ready AI 内容基础设施**

### 项目描述

独立设计并上线一套公网可访问的全栈内容系统，采用 React 19 + TypeScript、Vue 3 管理端、Spring Boot 3、Redis、RabbitMQ 和 Docker Compose；围绕文章高频读路径、异步副作用、实时连接和 AI 阅读场景，形成从内容发布到流式智能阅读的完整闭环。

### 推荐简历要点

- **可控 AI Gateway 与流式阅读：** 将博客 AI 调用从浏览器直连收敛到 Spring Boot Gateway，服务端按文章 ID 和关键词检索已发布内容并注入上下文；通过 SSE 转发 `meta / data / [DONE]` 事件，前端展示检索来源和流式回答，模型密钥仅保留在服务端。
- **Agent-ready 内容工具：** 抽象博客内容工具服务，统一支撑阅读助手和 MCP JSON-RPC 接口，提供 `search_articles`、`get_article` 两个只读工具；为每次调用生成 requestId，并用 Redis 记录检索数量、首字节延迟、总耗时和错误状态，后台可查看最近调用轨迹。
- **高并发内容基础设施：** 针对文章详情、点赞、签到、Feed 和 UV 统计，落地 BloomFilter + 互斥锁 + 逻辑过期缓存防御、Redisson 分布式锁、Redis Lua 原子脚本、BitMap、ZSet 和 HyperLogLog，降低热点读路径和高频写操作对 MySQL 的压力。
- **可靠异步与实时交付：** 使用 RabbitMQ Publisher Confirm、手动 ACK、持久化消息和死信队列承接评论通知与操作日志；通过 STOMP WebSocket 推送在线人数和站内通知，并用 Docker Compose 部署到公网服务器。

## 当前版本的面试证据

面试演示顺序建议是：

1. 打开 https://cyruszhang.online/，展示首页文章流和 AI 生活策划台。
2. 打开任意文章详情页，点击“文章阅读助手”的“开始分析”。
3. 展示回答出现“服务端检索 1 篇文章”和 requestId，说明 AI 不是浏览器直连模型。
4. 解释 `/api/ai/chat` 的 SSE 事件、服务端文章上下文边界和 Redis trace。
5. 需要展示 Agent 接口时，说明 `/api/mcp` 的只读 `tools/list` 与 `tools/call`。
6. 需要展开基础架构时，再讲 Redis、RabbitMQ、WebSocket 和 Docker Compose。

## 逐版简历写法

| 版本阶段 | 简历写法重点 | 不建议写法 |
|----------|--------------|------------|
| 基础博客闭环 | 独立完成 React + Spring Boot 前后端分离博客并部署公网，包含 JWT/RBAC、文章、评论、Markdown 阅读 | 只写“个人博客 CRUD” |
| 后台与容器化 | Vue 管理台、Docker Compose、Nginx、MySQL、Redis、RabbitMQ 全链路上线 | 只列技术栈，不说明解决的运行问题 |
| 高并发版本 | 用缓存三重防御、Lua、Redisson、BitMap、ZSet、HyperLogLog 处理热点读、原子点赞、签到、Feed 和 UV | 没有压测数据时写“QPS 提升 X 倍” |
| AI 创意工坊 | DashScope 兼容接口 + SSE 流式交互，前端按场景生成结构化方案 | 写成“自研大模型”或“训练模型” |
| 当前 Gateway 版本 | 服务端 AI Gateway、受控文章检索、Redis 调用轨迹、MCP 只读工具、线上浏览器闭环 | 把词法检索写成 Milvus/BM25/RAG 全量落地 |

## 当前版本的一句话自我介绍

> 我把一个普通博客逐步做成了公网运行的内容基础设施：先解决缓存、并发、异步和实时通信，再把 AI 阅读能力收敛到服务端 Gateway，并通过 MCP 把只读博客内容工具开放给外部 Agent；每次改动都能在真实网站上演示和追踪。

## 面试中必须保持诚实的边界

- 当前博客检索是标题、摘要、正文的有边界词法评分，不要说已经落地 Milvus、BM25、向量召回或 rerank。
- 当前博客不是 Supervisor-Planner-Executor 多 Agent 巡检系统；那属于独立的 AeroFlow Sentinel 项目。
- 当前博客记录的是 Redis 轨迹，不要说已经接入完整 OpenTelemetry SDK、Jaeger 或 Tempo。
- 没有经过压测或监控确认的数字，不写吞吐提升、准确率、延迟下降百分比。
- 可以说“线上浏览器验证返回 HTTP 200、展示检索来源和流式回答”，不要把一次验收写成长期 SLA。

## 每次更新后的简历同步模板

```md
### 博客版本：YYYY-MM-DD / commit

项目描述是否变化：是 / 否

新增简历要点：
- 用“问题 -> 设计 -> 结果 -> 证据”写一条，不罗列孤立技术名词。

线上证据：
- 页面：
- 接口：
- 真实操作：
- 返回结果：

面试展开问题：
- 为什么需要这次改动？
- 失败场景是什么？
- 如何验证？

限制与未完成项：
- 
```

