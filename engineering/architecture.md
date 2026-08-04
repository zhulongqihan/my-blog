# Architecture Notes

```text
Browser
  ├─ React 19 / Vite / TypeScript
  └─ Vue 3 / Element Plus / Pinia
          │
          ▼
Nginx: HTTPS, static assets, API proxy, security headers
          │
          ▼
Spring Boot 3: REST API, Security, service boundary
  ├─ MySQL: source of truth
  ├─ Redis: cache, rate limit, blacklist, counters, feed
  └─ RabbitMQ: notifications, operation logs, dead-letter path
          │
          ▼
WebSocket / STOMP: online count and real-time notification
```

## Core boundaries

- MySQL is the source of truth for articles, users, comments and permissions.
- Redis stores disposable or high-frequency state. View and like counters can be buffered before persistence.
- RabbitMQ handles work that should not delay the request-response path.
- Nginx is the only public entry point. Backend and data services stay on the internal Docker network or localhost bindings.
- The frontend receives AI responses through a proxy boundary. Provider credentials are runtime configuration, not source code.

## Read path

首页、搜索、分类和归档使用文章摘要模型；归档使用投影查询，只选择页面需要的字段；文章详情才加载 Markdown 正文和阅读交互所需的数据。

## Failure posture

服务通过 healthcheck 控制启动顺序；缓存失败时业务读取应回源数据库；消息消费失败由确认和死信路径兜底；前端实时连接失败不会阻断文章阅读。
