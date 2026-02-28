# 个人博客系统

> 创建日期：2026年1月27日  
> 最后更新：2026年2月28日  
> 版本：v1.8.1  
> GitHub: [https://github.com/zhulongqihan/my-blog](https://github.com/zhulongqihan/my-blog)  
> 网站：http://cyruszhang.online （备案中）

---

## 项目简介

这是一个全栈个人博客系统，前后端分离架构。后端使用 Spring Boot 3.x 提供 RESTful API，前台页面使用 React 19 + TypeScript + Vite，后台管理系统使用 Vue 3 + Element Plus + Pinia（双前端框架）。前台设计为大地色系极简风格。

**项目亮点**：双前端框架（React + Vue）、Markdown 编辑器 + 图片上传、完整的后台管理系统、JWT + RBAC 权限体系、**Redis 多级缓存系统**（Cache Aside + Write-Behind + 缓存预热 + 监控面板）、**RabbitMQ 消息队列**（评论邮件通知 + 日志异步化 + 死信队列 + 监控）、**WebSocket 实时通知**（STOMP + SockJS + MQ联动 + 在线人数 + 通知中心）、ECharts 数据可视化、**API 限流与防护系统**（Redis Lua 滑动窗口 + AOP + IP 黑白名单）、**酷炫前端交互**（打字机标题 + 阅读进度条 + 鼠标光晕 + 3D卡片倾斜 + 数字滚动动画）。

### 核心特性

- **极简设计** - 大地色系配色，复古温暖的视觉风格
- **JWT 认证** - 基于 Spring Security 的安全认证机制
- **JWT 黑名单** - 解决登出后 Token 依然有效的问题
- **Markdown 编辑器** - 集成 md-editor-v3，支持实时预览、图片上传
- **文章管理** - 后台创建/编辑文章，草稿/发布切换
- **图片上传** - 支持粘贴/拖拽上传，UUID 重命名、按日期分目录
- **Markdown 支持** - 文章内容支持 Markdown 格式
- **代码高亮** - 集成 React Syntax Highlighter
- **分类标签** - 灵活的文章分类和标签系统
- **评论功能** - 支持游客和用户评论
- **响应式设计** - 适配各种屏幕尺寸
- **后台管理** - Vue 3 + Element Plus 后台管理系统
- **数据看板** - ECharts 数据可视化仪表盘
- **API 限流防护** - Redis Lua 滑动窗口算法，自定义注解 + AOP 实现
- **IP 黑白名单** - 支持永久/临时封禁，白名单优先放行
- **Redis 多级缓存** - Spring Cache + 多TTL策略，Cache Aside / Write-Behind 模式
- **浏览量缓冲** - Redis INCR 原子计数 + 定时同步DB，减少数据库压力
- **缓存预热** - 应用启动时自动预加载热点数据
- **缓存监控** - ECharts 可视化命中率、空间分布、Redis 状态、一键清理
- **限流监控** - 实时监控面板，拦截统计、API 排行、事件追踪
- **RabbitMQ 消息队列** - 评论邮件通知、操作日志异步化、死信队列兜底
- **消息可靠投递** - Publisher Confirm + 手动ACK + 消息持久化
- **MQ 监控面板** - 队列状态、消费者数量、测试消息、架构可视化
- **文章归档 API** - 按年月自动分组已发布文章，替代前端硬编码
- **打字机标题** - 首页 Hero 区逐字打出效果 + 闪烁光标
- **阅读进度条** - 文章详情页顶部大地色渐变进度指示
- **鼠标光晕跟随** - 桌面端鼠标移动时淡棕色径向光晕
- **卡片 3D 倾斜** - 文章卡片 hover 时根据鼠标位置微倾斜
- **回到顶部按钮** - 滚动触发，弹簧动画
- **数字滚动动画** - 关于页统计数值 easeOutExpo 缓动计数
- **WebSocket 实时通知** - STOMP + SockJS 协议，JWT 认证，实时推送评论/公告
- **在线人数统计** - WebSocket 连接/断开事件 + AtomicInteger 原子计数
- **通知中心** - 管理后台铃铛实时红点 + 通知列表 + 系统公告广播
- **MQ → WebSocket 联动** - 评论消息队列消费后自动推送实时通知

### 项目目标

- [x] 搭建一个功能完整的个人博客网站
- [x] 使用现代化的 Java 后端技术栈
- [x] 实现前后端分离架构
- [x] 部署到公网供他人访问
- [x] 打造完整的后台管理系统（React + Vue 双框架）

---

## 技术架构

```
┌─────────────────────────────┐   ┌─────────────────────────────┐
│     前台 (React 19)        │   │   后台管理 (Vue 3)        │
│  Vite 7 │ TypeScript 5.9  │   │  Vite 5 │ TypeScript 5.4  │
│  React Router 7            │   │  Element Plus │ Pinia      │
│  Axios │ Framer Motion    │   │  ECharts │ vue-router     │
└─────────────────────────────┘   └─────────────────────────────┘
              │                                │
              └───────────────┬──────────────┘
                              │ HTTP/REST API
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    后端 (Spring Boot 3.2.2)                    │
│  Spring Security + JWT  │  Spring Data JPA + Hibernate   │
│   Redis 多级缓存 + 黑名单  │  AOP 操作日志 + 异步任务       │
│  RBAC 权限体系          │  CompletableFuture 并行查询    │
│  API 限流（Lua滑动窗口） │  IP 黑白名单防护               │
│  Spring Cache（多TTL）  │  定时任务（浏览量同步）         │
│  RabbitMQ 消息队列      │  死信队列 + 邮件通知            │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┼─────────┐
                    ▼         ▼          ▼
┌──────────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   数据库 (H2/MySQL) │ │  Redis 7.x 缓存 │ │  RabbitMQ 3.13 │
│  开发:H2 | 生产:MySQL│ │ 缓存│限流│黑名单 │ │ 评论通知│日志│DLX│
└──────────────────────┘ └─────────────────┘ └─────────────────┘
```

---

## 技术栈详情

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Java** | 17 | 编程语言 |
| **Spring Boot** | 3.2.2 | 核心框架 |
| **Spring Security** | 6.x | 安全认证框架 |
| **Spring Data JPA** | 3.x | 数据持久层 |
| **Hibernate** | 6.4.1 | ORM 框架 |
| **JWT (jjwt)** | 0.12.3 | Token 认证 |
| **Redis** | 7.x | 多级缓存 + JWT黑名单 + 限流 |
| **RabbitMQ** | 3.13 | 消息队列（评论通知 + 日志异步） |
| **MapStruct** | 1.5.5 | 对象转换 |
| **Hutool** | 5.8.24 | 工具库 |
| **H2 Database** | 2.x | 开发环境数据库 |
| **MySQL** | 8.0 | 生产环境数据库 |
| **Lombok** | 1.18.x | 代码简化工具 |
| **Maven** | 3.x | 项目构建工具 |
| **Spring AOP** | 6.x | 切面编程（限流、日志） |
| **Spring Cache** | 6.x | 声明式缓存（多TTL策略） |

### 前台前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **React** | 19.x | UI 框架 |
| **TypeScript** | 5.9 | 类型安全的 JavaScript |
| **Vite** | 7.x | 构建工具 |
| **React Router** | 7.x | 路由管理 |
| **Axios** | 1.13 | HTTP 客户端 |
| **Framer Motion** | 12.x | 动画库 |
| **Lucide React** | - | 图标库 |
| **React Syntax Highlighter** | - | 代码高亮 |

### 后台管理前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Vue** | 3.4 | 渐进式 UI 框架 |
| **Element Plus** | 2.6 | Vue 3 组件库 |
| **TypeScript** | 5.4 | 类型安全 |
| **Vite** | 5.x | 构建工具 |
| **Pinia** | 2.1 | 状态管理 |
| **vue-router** | 4.3 | 路由管理 |
| **ECharts** | 5.5 | 数据可视化 |
| **vue-echarts** | 6.6 | ECharts Vue 封装 |
| **md-editor-v3** | 4.x | Markdown 编辑器 |
| **@element-plus/icons-vue** | 2.3 | Element Plus 图标 |

### 设计规范

| 元素 | 规范 |
|------|------|
| **主背景色** | `#F8F8F8` (温暖的米白色) |
| **主文字色** | `#2D2D2D` (炭灰色) |
| **强调色1** | `#8B7355` (铁锈色) |
| **强调色2** | `#5C6B4A` (橄榄绿) |
| **标题字体** | Playfair Display (衬线体) |
| **正文字体** | Inter (无衬线体) |
| **代码字体** | JetBrains Mono (等宽字体) |

---

## 项目结构

```
myblog/
├── backend/                          # 后端项目 (Spring Boot)
│   ├── src/main/java/com/myblog/
│   │   ├── BlogApplication.java      # 启动类
│   │   ├── config/                   # 配置类
│   │   │   ├── SecurityConfig.java   # Spring Security 配置
│   │   │   ├── RedisConfig.java      # Redis 配置
│   │   │   ├── AsyncConfig.java      # 异步任务 + 定时任务配置
│   │   │   ├── DataInitializer.java  # 数据初始化
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── controller/               # 公共 API 控制器
│   │   │   ├── AuthController.java
│   │   │   ├── ArticleController.java
│   │   │   ├── CommentController.java
│   │   │   └── CategoryTagController.java
│   │   ├── consumer/                  # MQ 消费者
│   │   │   ├── CommentNotificationConsumer.java
│   │   │   ├── LogConsumer.java
│   │   │   └── DeadLetterConsumer.java
│   │   ├── controller/admin/         # 管理后台 API 控制器
│   │   │   ├── AdminArticleController.java
│   │   │   ├── AdminCacheController.java   # 缓存监控 API
│   │   │   ├── AdminCategoryController.java
│   │   │   ├── AdminTagController.java
│   │   │   ├── AdminCommentController.java
│   │   │   ├── AdminDashboardController.java
│   │   │   ├── AdminLogController.java
│   │   │   ├── AdminRateLimitController.java  # 限流监控 API
│   │   │   ├── AdminMQController.java         # MQ 监控 API
│   │   │   └── FileUploadController.java
│   │   ├── common/annotation/        # 自定义注解
│   │   │   └── RateLimit.java         # 限流注解
│   │   ├── common/aspect/            # AOP 切面
│   │   │   └── RateLimitAspect.java   # 限流切面
│   │   ├── dto/                      # 数据传输对象
│   │   │   ├── ArchiveResponse.java    # 归档响应（年-月-文章层级）
│   │   │   └── mq/                     # MQ 消息 DTO
│   │   ├── entity/                   # JPA 实体类
│   │   │   └── Notification.java       # 通知实体
│   │   ├── repository/               # 数据访问层
│   │   │   └── NotificationRepository.java # 通知 Repository
│   │   ├── security/                 # JWT + 安全配置
│   │   │   └── IpBlacklistFilter.java # IP 黑名单过滤器
│   │   ├── service/                  # 业务逻辑层
│   │   │   ├── CacheService.java       # 缓存管理服务
│   │   │   ├── CategoryService.java    # 分类服务（含缓存）
│   │   │   ├── TagService.java         # 标签服务（含缓存）
│   │   │   ├── IpBlacklistService.java # IP 黑白名单服务
│   │   │   ├── MQProducerService.java  # MQ 消息生产者
│   │   │   ├── MQMonitorService.java   # MQ 监控服务
│   │   │   └── NotificationService.java # 通知服务（WebSocket推送）
│   │   ├── websocket/                 # WebSocket 组件
│   │   │   └── WebSocketEventListener.java # 连接事件+在线计数
│   │   ├── task/                      # 定时/启动任务
│   │   │   ├── ViewCountSyncTask.java  # 浏览量同步（5分钟）
│   │   │   └── CacheWarmupTask.java    # 缓存预热
│   │   └── aspect/                   # AOP 切面（操作日志）
│   ├── src/main/resources/
│   │   ├── application.yml           # 通用配置
│   │   ├── application-dev.yml       # 开发环境配置
│   │   ├── application-prod.yml      # 生产环境配置
│   │   └── scripts/
│   │       └── rate_limit.lua        # Redis 滑动窗口限流脚本
│   └── pom.xml                       # Maven 配置
│
├── frontend/                         # 前台前端 (React 19)
│   ├── src/
│   │   ├── components/               # 公共组件
│   │   │   ├── ScrollToTop.tsx         # 回到顶部按钮
│   │   │   ├── ReadingProgress.tsx     # 阅读进度条
│   │   │   ├── CursorGlow.tsx          # 鼠标光晕跟随
│   │   │   ├── Typewriter.tsx          # 打字机效果
│   │   │   └── AnimatedCounter.tsx     # 数字滚动动画
│   │   ├── pages/                    # 页面组件
│   │   ├── services/                 # API 服务
│   │   ├── hooks/                    # 自定义 Hooks
│   │   ├── context/                  # React Context
│   │   ├── types/                    # TypeScript 类型
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
│
├── admin-frontend/                   # 后台管理前端 (Vue 3)
│   ├── src/
│   │   ├── api/                      # API 接口模块
│   │   │   ├── auth.ts               # 认证接口
│   │   │   ├── dashboard.ts          # 仪表盘接口
│   │   │   ├── article.ts            # 文章管理接口
│   │   │   ├── category.ts           # 分类管理接口
│   │   │   ├── tag.ts                # 标签管理接口
│   │   │   ├── comment.ts            # 评论管理接口
│   │   │   ├── log.ts                # 操作日志接口
│   │   │   ├── rateLimit.ts          # 限流监控接口
│   │   │   └── cache.ts              # 缓存监控接口
│   │   ├── layout/                   # 布局组件
│   │   │   └── AdminLayout.vue       # 管理后台布局
│   │   ├── router/                   # 路由配置
│   │   │   └── index.ts
│   │   ├── stores/                   # Pinia 状态管理
│   │   │   └── user.ts               # 用户状态
│   │   ├── types/                    # TypeScript 类型
│   │   │   └── index.ts
│   │   ├── utils/                    # 工具函数
│   │   │   └── request.ts            # Axios 封装
│   │   ├── views/                    # 页面组件
│   │   │   ├── Login.vue             # 管理员登录
│   │   │   ├── Dashboard.vue         # 数据仪表盘
│   │   │   ├── ArticleList.vue       # 文章管理
│   │   │   ├── ArticleEdit.vue       # 文章编辑（Markdown 编辑器）
│   │   │   ├── CategoryManage.vue    # 分类管理
│   │   │   ├── TagManage.vue         # 标签管理
│   │   │   ├── CommentManage.vue     # 评论管理
│   │   │   ├── LogList.vue           # 操作日志
│   │   │   ├── RateLimitMonitor.vue  # 限流监控面板
│   │   │   ├── CacheMonitor.vue      # 缓存监控面板
│   │   │   └── MQMonitor.vue         # MQ 监控面板
│   │   ├── App.vue
│   │   └── main.ts
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
│
└── README.md                         # 本文档
```

---

## 快速启动

### 环境要求

在开始之前，请确保已安装以下软件：

| 软件 | 最低版本 | 推荐版本 | 说明 |
|------|---------|---------|------|
| **Java JDK** | 17 | 17+ | 后端运行环境 |
| **Maven** | 3.6 | 3.8+ | 后端构建工具 |
| **Node.js** | 18 | 20+ | 前端运行环境 |
| **npm** | 9 | 10+ | 前端包管理器 |

### 一键启动（推荐）

项目提供了自动化启动脚本，可以一键启动前后端服务：

**Windows 系统：**
```bash
# 双击运行或在命令行执行
start.bat
```

**Linux/Mac 系统：**
```bash
# 添加执行权限
chmod +x start.sh

# 运行启动脚本
./start.sh
```

启动脚本会自动：
1. 检查环境依赖（Java、Maven、Node.js）
2. 启动后端服务（端口 8080）
3. 安装前端依赖（如果需要）
4. 启动前端服务（端口 5173）

### 手动启动

如果需要分别启动前后端，可以使用以下命令：

**启动后端：**
```bash
cd backend
mvn spring-boot:run
```

**启动前端：**
```bash
cd frontend
npm install  # 首次运行需要安装依赖
npm run dev
```

### 停止服务

**Windows 系统：**
```bash
stop.bat
```

**Linux/Mac 系统：**
```bash
./stop.sh
```

### 访问地址

启动成功后，可以通过以下地址访问：

| 服务 | 地址 | 说明 |
|------|------|------|
| **前台页面** | http://localhost:5173 | 博客主页 |
| **后台管理** | http://localhost:5174/admin/ | 管理后台 |
| **后端 API** | http://localhost:8080 | RESTful API |
| **H2 控制台** | http://localhost:8080/h2-console | 数据库管理 |

**H2 数据库连接信息：**
- JDBC URL: `jdbc:h2:mem:blogdb`
- 用户名: `sa`
- 密码: （留空）

---

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |

---

## API 接口文档

### 认证接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/register` | 用户注册 | 公开 |
| POST | `/api/auth/login` | 用户登录 | 公开 |
| GET | `/api/auth/me` | 获取当前用户信息 | 需认证 |

### 文章接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/articles` | 获取文章列表(分页) | 公开 |
| GET | `/api/articles/{id}` | 获取文章详情 | 公开 |
| GET | `/api/articles/featured` | 获取精选文章 | 公开 |
| GET | `/api/articles/category/{id}` | 按分类获取文章 | 公开 |
| GET | `/api/articles/tag/{id}` | 按标签获取文章 | 公开 |
| GET | `/api/articles/search` | 搜索文章 | 公开 |
| GET | `/api/articles/archive` | 文章归档（按年月分组） | 公开 |
| POST | `/api/articles` | 创建文章 | 需认证 |
| PUT | `/api/articles/{id}` | 更新文章 | 需认证 |
| DELETE | `/api/articles/{id}` | 删除文章 | 需认证 |
| POST | `/api/articles/{id}/like` | 点赞文章 | 公开 |

### 分类和标签接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/categories` | 获取所有分类 | 公开 |
| GET | `/api/tags` | 获取所有标签 | 公开 |

### 评论接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/comments/article/{articleId}` | 获取文章评论 | 公开 |
| POST | `/api/comments/article/{articleId}` | 添加评论 | 公开 |

### 管理后台接口（需管理员权限）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/dashboard/stats` | 仪表盘统计数据 | ADMIN |
| GET | `/api/admin/articles` | 文章列表（分页/搜索/筛选） | ADMIN |
| GET | `/api/admin/articles/{id}` | 获取文章详情（编辑回显） | ADMIN |
| POST | `/api/admin/articles` | 创建文章 | ADMIN |
| PUT | `/api/admin/articles/{id}` | 更新文章 | ADMIN |
| PUT | `/api/admin/articles/{id}/toggle-publish` | 切换发布状态 | ADMIN |
| PUT | `/api/admin/articles/{id}/toggle-featured` | 切换精选状态 | ADMIN |
| DELETE | `/api/admin/articles/{id}` | 删除文章 | ADMIN |
| DELETE | `/api/admin/articles/batch` | 批量删除文章 | ADMIN |
| POST | `/api/admin/upload/image` | 上传图片 | ADMIN |
| GET | `/api/admin/categories` | 分类列表 | ADMIN |
| POST | `/api/admin/categories` | 创建分类 | ADMIN |
| PUT | `/api/admin/categories/{id}` | 更新分类 | ADMIN |
| DELETE | `/api/admin/categories/{id}` | 删除分类 | ADMIN |
| GET | `/api/admin/tags` | 标签列表 | ADMIN |
| POST | `/api/admin/tags` | 创建标签 | ADMIN |
| PUT | `/api/admin/tags/{id}` | 更新标签 | ADMIN |
| DELETE | `/api/admin/tags/{id}` | 删除标签 | ADMIN |
| GET | `/api/admin/comments` | 评论列表（分页） | ADMIN |
| PUT | `/api/admin/comments/{id}/approve` | 审核评论 | ADMIN |
| DELETE | `/api/admin/comments/{id}` | 删除评论 | ADMIN |
| DELETE | `/api/admin/comments/batch` | 批量删除评论 | ADMIN |
| GET | `/api/admin/logs` | 操作日志列表（分页） | ADMIN |

### 限流监控接口（需管理员权限）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/rate-limit/stats` | 限流统计数据（7日趋势、API排行） | ADMIN |
| GET | `/api/admin/rate-limit/events` | 最近限流事件列表 | ADMIN |
| GET | `/api/admin/rate-limit/blacklist` | 获取IP黑名单 | ADMIN |
| POST | `/api/admin/rate-limit/blacklist` | 添加IP到黑名单（支持临时/永久） | ADMIN |
| DELETE | `/api/admin/rate-limit/blacklist/{ip}` | 从IP黑名单移除 | ADMIN |
| GET | `/api/admin/rate-limit/whitelist` | 获取IP白名单 | ADMIN |
| POST | `/api/admin/rate-limit/whitelist` | 添加IP到白名单 | ADMIN |
| DELETE | `/api/admin/rate-limit/whitelist/{ip}` | 从IP白名单移除 | ADMIN |
| GET | `/api/admin/rate-limit/blacklist/log` | 黑名单操作日志 | ADMIN |

### 缓存监控接口（需管理员权限）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/cache/stats` | 缓存统计（Redis信息+各缓存空间） | ADMIN |
| GET | `/api/admin/cache/names` | 获取所有缓存名称 | ADMIN |
| DELETE | `/api/admin/cache/{cacheName}` | 清除指定缓存空间 | ADMIN |
| DELETE | `/api/admin/cache/all` | 清除所有缓存 | ADMIN |

### MQ 监控接口（需管理员权限）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/mq/stats` | MQ 统计（队列/交换机/消费者） | ADMIN |
| GET | `/api/admin/mq/health` | MQ 连接健康检查 | ADMIN |
| POST | `/api/admin/mq/test/{queueName}` | 发送测试消息 | ADMIN |

### 通知与 WebSocket 接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/notifications/online-count` | 获取在线人数 | 公开 |
| GET | `/api/admin/notifications` | 通知列表（分页） | ADMIN |
| GET | `/api/admin/notifications/unread-count` | 未读通知数 | ADMIN |
| PUT | `/api/admin/notifications/{id}/read` | 标记已读 | ADMIN |
| PUT | `/api/admin/notifications/read-all` | 全部已读 | ADMIN |
| POST | `/api/admin/notifications/broadcast` | 发送系统公告 | ADMIN |
| GET | `/api/admin/notifications/ws-stats` | WebSocket 状态 | ADMIN |
| WS | `/ws` | WebSocket STOMP 端点（SockJS） | 公开 |

---

## 数据模型

### 用户 (User)

```java
- id: Long (主键)
- username: String (用户名，唯一)
- email: String (邮箱，唯一)
- password: String (密码，BCrypt加密)
- nickname: String (昵称)
- avatar: String (头像URL)
- bio: String (个人简介)
- role: Enum (ADMIN/USER)
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

### 文章 (Article)

```java
- id: Long (主键)
- title: String (标题)
- content: String (内容，Markdown格式)
- summary: String (摘要)
- coverImage: String (封面图URL)
- author: User (作者)
- category: Category (分类)
- tags: Set<Tag> (标签)
- viewCount: Integer (浏览量)
- likeCount: Integer (点赞数)
- published: Boolean (是否发布)
- featured: Boolean (是否精选)
- publishedAt: LocalDateTime
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

### 分类 (Category)

```java
- id: Long (主键)
- name: String (名称，唯一)
- description: String (描述)
- icon: String (图标)
- sortOrder: Integer (排序)
```

### 标签 (Tag)

```java
- id: Long (主键)
- name: String (名称，唯一)
- color: String (颜色代码)
```

### 评论 (Comment)

```java
- id: Long (主键)
- content: String (内容)
- article: Article (所属文章)
- user: User (评论用户，可选)
- guestName: String (游客名称)
- guestEmail: String (游客邮箱)
- parent: Comment (父评论，支持嵌套)
- approved: Boolean (是否审核通过)
- createdAt: LocalDateTime
```

---

## 已完成功能

### 后端
- [x] Spring Boot 项目初始化
- [x] 数据库实体设计 (User, Article, Category, Tag, Comment)
- [x] RBAC权限体系设计
- [x] JPA Repository 层
- [x] Service 业务层
- [x] RESTful API 控制器
- [x] JWT 认证机制
- [x] JWT 黑名单机制（解决登出问题）
- [x] Spring Security 配置
- [x] Redis 多级缓存（6个命名空间 + 多TTL策略）
- [x] 统一响应体（Result<T>）
- [x] 全局异常处理
- [x] AOP 操作日志（异步记录）
- [x] 异步任务 + 定时任务配置
- [x] CORS 跨域配置
- [x] 数据初始化 (管理员账号、默认分类和标签)
- [x] 配置管理（开发/生产环境分离）

### 后台管理 API
- [x] 管理员登录/登出接口
- [x] 仪表盘统计接口（CompletableFuture 并行查询）
- [x] 文章创建/编辑接口（Markdown 内容）
- [x] 图片上传接口（本地存储 + 静态资源映射）
- [x] 文章管理接口（CRUD + 缓存策略）
- [x] 分类管理接口（CRUD）
- [x] 标签管理接口（CRUD）
- [x] 评论管理接口（审核/删除/批量）
- [x] 操作日志接口（分页查询）
- [x] 限流监控接口（统计/事件/黑白名单CRUD）
- [x] 缓存监控接口（统计/清除/空间管理）
- [x] MQ 监控接口（队列状态/健康检查/测试消息）
- [x] 文章归档接口（按年月分组 + @Cacheable 缓存）
- [x] WebSocket 实时通知（STOMP + SockJS + JWT 认证）
- [x] 通知持久化（Notification 实体 + Repository）
- [x] MQ → WebSocket 联动（评论消费后实时推送）
- [x] 在线人数统计（AtomicInteger + 连接事件监听）
- [x] 通知管理接口（列表/已读/广播/WS状态）

### 前台前端 (React)
- [x] Vite + React + TypeScript 项目初始化
- [x] 路由配置 (React Router)
- [x] 页面组件 (首页、文章页、关于页、归档页)
- [x] 公共组件 (Header, Footer, ArticleCard)
- [x] 大地色系极简设计风格
- [x] 代码语法高亮
- [x] 动画效果 (Framer Motion)
- [x] API 服务层 (Axios)
- [x] 自定义 Hooks
- [x] 认证上下文 (AuthContext)
- [x] 前后端 API 连接
- [x] 归档页接入真实 API（替代硬编码 mock 数据）
- [x] 打字机标题（Typewriter 组件）
- [x] 阅读进度条（ReadingProgress 组件）
- [x] 鼠标光晕跟随（CursorGlow 组件）
- [x] 回到顶部按钮（ScrollToTop 组件）
- [x] 文章卡片 3D 微倾斜（ArticleCard 增强）
- [x] 统计数字滚动动画（AnimatedCounter 组件）
- [x] WebSocket 在线人数显示（OnlineCount 组件）

### 后台管理前端 (Vue 3)
- [x] Vue 3 + Element Plus + TypeScript 项目初始化
- [x] Axios 封装（Bearer Token、401 拦截）
- [x] Pinia 状态管理（用户登录状态）
- [x] 路由配置和导航守卫
- [x] 管理后台布局（侧边栏 + 面包屑 + 用户菜单）
- [x] 管理员登录页面
- [x] 数据仪表盘（ECharts 图表）
- [x] 文章管理页面（搜索/筛选/分页/批量操作）
- [x] 文章创建/编辑页面（Markdown 编辑器 + 图片上传）
- [x] 分类管理页面（CRUD）
- [x] 标签管理页面（CRUD）
- [x] 评论管理页面（审核/删除）
- [x] 操作日志页面（分页查询）
- [x] 限流监控页面（拦截统计、API排行、黑白名单管理、事件追踪）
- [x] 缓存监控页面（命中率分析、空间分布、Redis信息、缓存清理）
- [x] MQ 监控页面（连接状态、队列详情、交换机信息、测试消息）
- [x] 通知中心页面（铃铛实时红点 + 通知列表 + 系统公告广播）
- [x] WebSocket 连接管理（Pinia Store + STOMP + JWT）

### 部署
- [x] 阿里云服务器部署
- [x] MySQL 数据库配置
- [x] Nginx 反向代理
- [x] systemd 服务配置
- [x] 域名解析配置
- [x] **生产环境API接口验证成功**

---

## 项目文档

本项目包含完整的开发和部署文档，存放在本地 `docs/` 目录中（不上传到 GitHub）。

### 文档列表

| 文档 | 说明 | 适用场景 |
|------|------|---------|
| `docs/RUNNING_GUIDE.md` | 详细运行指南 | 本地开发和调试 |
| `docs/DEPLOYMENT_ALIYUN.md` | 阿里云部署教程 | 使用阿里云服务器部署 |
| `docs/DEPLOYMENT_VPS.md` | VPS 部署教程 | 使用其他 VPS 部署 |
| `docs/DEPLOYMENT.md` | 部署方案对比 | 选择部署方案 |
| `docs/ADMIN_GUIDE.md` | 后台管理操作指南 | 后台管理系统使用 |
| `docs/ARTICLE_EDITOR_GUIDE.md` | 文章编辑器实现文档 | Markdown 编辑器功能说明 |
| `docs/RATE_LIMIT_GUIDE.md` | 限流系统操作指南 | API 限流与防护系统使用 |
| `docs/CACHE_GUIDE.md` | 缓存系统操作指南 | Redis 缓存系统使用与面试要点 |
| `docs/MQ_GUIDE.md` | MQ 消息队列指南 | RabbitMQ 架构设计与面试要点 |
| `docs/FRONTEND_ENHANCEMENT_GUIDE.md` | 前端增强指南 | 归档修复 + 酷炫交互组件说明 |
| `docs/WEBSOCKET_GUIDE.md` | WebSocket 实时通知指南 | STOMP + SockJS + MQ 联动 + 部署步骤 |
| `docs/.cursorrules` | 开发规范 | 代码风格和规范 |
| `docs/NEXT_STEPS.md` | 后续改进计划 | 功能扩展参考 |
| `docs/TUTORIAL.md` | 从零开始教程 | 学习项目架构 |
| `docs/DOCS_GUIDE.md` | 文档导航指南 | 快速找到所需文档 |

### 快速导航

- **想要本地运行？** → 查看 `docs/RUNNING_GUIDE.md`
- **想要部署上线？** → 查看 `docs/DEPLOYMENT_ALIYUN.md`
- **想要学习架构？** → 查看 `docs/TUTORIAL.md`
- **想要贡献代码？** → 查看 `docs/.cursorrules`

> **提示**：如果你克隆了这个仓库但没有 `docs/` 目录，这是正常的。这些文档是开发指导文档，不包含在 GitHub 仓库中。你可以根据本 README 的说明直接开始使用项目。

---

## 开发路线图

### 第一阶段：基础架构（已完成）

- [x] 项目架构设计和初始化
- [x] 后端 RESTful API 开发
- [x] 前端页面和组件开发
- [x] JWT 认证机制
- [x] JWT 黑名单机制
- [x] 统一响应体和异常处理
- [x] Redis 多级缓存系统（Cache Aside + Write-Behind）
- [x] AOP 操作日志
- [x] 异步任务 + 定时任务配置
- [x] RBAC 权限体系设计
- [x] 配置管理（开发/生产环境分离）
- [x] 文章、分类、标签功能
- [x] 代码规范和 ESLint 配置
- [x] 本地开发环境搭建
- [x] 启动和停止脚本
- [x] 项目文档编写
- [x] GitHub 仓库创建
- [x] 阿里云服务器部署

### 第二阶段：后台管理系统（已完成）

**后台管理 API：**
- [x] 管理员登录/登出接口
- [x] 仪表盘统计接口（CompletableFuture并行查询）
- [x] 文章创建/编辑接口 + 图片上传
- [x] 文章管理接口（CRUD + 缓存策略）
- [x] 分类标签管理接口
- [x] 评论管理接口
- [x] 操作日志接口

**后台管理前端（Vue 3）：**
- [x] 管理后台界面（Vue 3 + Element Plus）
- [x] 数据仪表盘（ECharts 可视化）
- [x] 文章/分类/标签/评论管理页面
- [x] 文章创建/编辑页面（Markdown 编辑器 + 图片上传）
- [x] 操作日志页面

**前台功能：**
- [x] 文章创建和编辑页面（Markdown 编辑器）
- [x] 图片上传功能（封面图 + 编辑器内粘贴/拖拽上传）
- [ ] 用户登录/注册页面
- [ ] 评论功能前端实现
- [ ] 文章搜索功能
- [ ] 分页组件

### 第三阶段：进阶功能（进行中）

**已完成：**
- [x] API 限流和防护（Redis Lua 滑动窗口 + AOP 切面 + IP 黑白名单）
- [x] 限流监控面板（ECharts 趋势图、实时统计、事件追踪）
- [x] Redis 多级缓存系统（Spring Cache + 多TTL + Cache Aside + Write-Behind）
- [x] 缓存监控面板（ECharts 命中率/空间分布、Redis 状态、缓存管理）
- [x] RabbitMQ 消息队列（评论通知 + 日志异步 + 死信队列 + 监控面板）
- [x] 文章归档 API（按年月自动分组 + 缓存）
- [x] 前端酷炫交互（打字机 + 进度条 + 光晕 + 3D 卡片 + 滚动计数 + 回到顶部）
- [x] WebSocket 实时通知系统（STOMP + SockJS + JWT 认证 + 在线人数 + 通知中心）

**核心功能：**
- [ ] 数据导出功能
- [ ] 性能监控
- [ ] 单元测试

**优化改进：**
- [ ] 响应式设计优化
- [ ] 代码格式化（Prettier）
- [ ] CSS 变量提取
- [ ] 性能优化
- [ ] SEO 优化

**扩展功能：**
- [ ] 暗色模式
- [ ] RSS 订阅
- [ ] 社交分享
- [ ] 文章统计
- [ ] Docker 容器化
- [ ] CI/CD 自动化部署

---

## 开发日志

### 2026-02-28（v1.8.1 安全加固 + 代码审计修复）
- 修复 AuthController 登出 Bug（Token 黑名单参数传递错误，登出功能失效）
- 修复 Redis 反序列化漏洞（LaissezFaireSubTypeValidator → BasicPolymorphicTypeValidator 白名单）
- 修复用户密码哈希通过 API 泄漏（User.password 添加 @JsonIgnore）
- 修复 JPA 实体 @Data 导致 toString/hashCode 无限递归（User/SysRole/SysMenu）
- 修复 WebSocket SecurityContextHolder 线程泄漏（改为仅用 accessor.setUser）
- 修复 WebSocket CORS 过于宽松（从 * 改为复用 cors.allowed-origins）
- 修复浏览量同步定时任务数据丢失竞态条件（先读后删，移除外层 @Transactional）
- 修复文件上传缺少扩展名白名单（防止伪造 Content-Type 上传恶意文件）
- 修复 GlobalExceptionHandler 未返回正确 HTTP 状态码（添加 @ResponseStatus）
- 修复 MQ 监控连接泄漏（createConnection 后 finally close）
- 修复 JwtBlacklistService.getBlacklistCount() NPE 风险
- 修复 RabbitMQ 评论队列 TTL 过短（30s → 5min）
- 修复 Notification 广播消息永远显示未读（markAllAsRead 覆盖广播通知）
- 修复 WebSocketAuthConfig 移除重复 @EnableWebSocketMessageBroker
- 修复 Notification @Index 列名使用物理 snake_case 列名
- React 前端：WebSocket Context 单例化（避免多组件重复创建连接）
- React 前端：修复 WS URL 构建逻辑（使用 window.location.origin 回退）
- Vue 管理前端：同步修复 WS URL 构建
- 更新所有 docs 文档（docs/README.md 索引重写 + MQ 日期修正 + WEBSOCKET_GUIDE 同步修复）

### 2026-02-27（v1.8.0 WebSocket 实时通知系统）
- 新增 WebSocket 实时通知系统（STOMP + SockJS）
- WebSocketConfig：STOMP 端点 /ws + SockJS 回退，/topic + /queue 消息代理
- WebSocketAuthConfig：STOMP CONNECT 帧 JWT 认证拦截器
- WebSocketEventListener：在线人数统计（AtomicInteger + ConcurrentHashMap）+ 实时广播
- Notification 实体：COMMENT/SYSTEM/LIKE 三种类型，支持定向推送与全站广播
- NotificationService：通知持久化 + WebSocket 推送 + MQ 消费者联动
- CommentNotificationConsumer 增强：MQ 消费后自动触发 WebSocket 实时推送
- NotificationController：公开在线人数接口
- AdminNotificationController：通知列表/已读/全部已读/广播/WS 统计（6 个接口）
- React 前端：useWebSocket Hook + OnlineCount 组件（绿色脉冲 + 在线人数）
- Vue 管理前端：Pinia WebSocket Store + 通知中心页面 + 顶栏铃铛未读徽标
- 安全配置：/ws/** 和在线人数接口放行

### 2026-02-26（v1.7.1 归档修复 + 前端增强）
- 修复归档页硬编码 mock 数据：新增 GET /api/articles/archive 接口（按年月分组已发布文章）
- 新增 ArchiveResponse DTO（年→月→文章层级结构 + @Cacheable 缓存）
- ArchivePage.tsx 改为 useEffect + API 调用，支持加载态/错误态/空态
- 新增 ScrollToTop 组件：固定右下角回到顶部按钮，弹簧动画
- 新增 ReadingProgress 组件：文章详情页顶部 2px 大地色渐变阅读进度条
- 新增 CursorGlow 组件：桌面端鼠标跟随 300px 淡棕色径向光晕
- 新增 Typewriter 组件：首页 Hero 标题逐字打出 + 闪烁光标
- 新增 AnimatedCounter 组件：关于页统计数字 easeOutExpo 缓动滚动
- 增强 ArticleCard：hover 时根据鼠标位置 3D 微倾斜（±2deg perspective）
- 所有效果保持大地色系风格，零额外依赖（framer-motion + 原生 JS）

### 2026-02-28（v1.7.0 RabbitMQ 消息队列）
- 实现 RabbitMQ 消息队列集成（v1.7.0）
- 新增 RabbitMQConfig 配置类（3个交换机、3个队列、3个绑定关系）
- 评论通知消费者：用户评论/回复 → MQ → 异步发送邮件通知
- 操作日志消费者：AOP拦截 → MQ → 异步写入数据库（替代@Async方案）
- 死信队列消费者：消费失败消息兜底处理
- MQ生产者服务：统一消息发送入口，每条消息携带唯一messageId
- 消息可靠性：Publisher Confirm + 手动ACK + 消息持久化 + Mandatory
- Jackson2Json序列化：替代默认Java序列化，注册JavaTimeModule
- LogAspect改造：MQ发送失败时降级为@Async直接保存（优雅降级）
- 后台MQ监控API：队列状态、消费者数量、连接健康检查、测试消息
- Admin前端MQ监控页面：连接状态、队列详情、交换机信息、架构说明
- 创建 docs/MQ_GUIDE.md 详细文档

### 2026-02-27
- 实现 Redis 多级缓存系统增强（v1.6.0）
- 多TTL缓存配置：6个命名缓存空间（articleDetail/featuredArticles/popularArticles/categories/tags/dashboardStats）
- 文章详情/推荐/热门缓存（Cache Aside模式，@Cacheable + @CacheEvict）
- 浏览量 Redis 缓冲（INCR 原子计数 + 每5分钟定时同步到数据库，Write-Behind模式）
- 分类/标签新建 Service 层，2小时缓存TTL
- 仪表盘统计缓存（5分钟TTL），修复 todayViews 从 Redis 读取
- 缓存预热（ApplicationRunner 启动时预加载热点数据）
- 所有写操作自动失效关联缓存（文章/分类/标签/管理操作）
- 缓存监控 API（GET/DELETE /api/admin/cache/*）
- 缓存监控前端页面（ECharts 命中率饼图、空间分布、Redis信息面板、缓存一键清理）
- 修复 GenericJackson2JsonRedisSerializer + LocalDateTime 序列化问题（注册 JavaTimeModule）
- 修复 @Cacheable 同类自调用 AOP 失效问题（注入 self 代理）

### 2026-02-26
- 实现 API 限流与防护系统（v1.5.0）
- 自定义 @RateLimit 注解 + AOP 切面，Redis Lua 滑动窗口算法
- 支持 IP/USER/IP_AND_API 三种限流粒度
- 实现 IP 黑白名单机制（永久/临时封禁，白名单优先）
- IpBlacklistFilter 在 JWT 认证之前拦截恶意 IP
- 后台限流监控页面（ECharts 趋势图、API 排行、黑白名单管理、事件追踪）
- 对登录、注册、文章、搜索、评论接口应用限流规则
- 修复 IP/IP_AND_API 限流粒度逻辑 Bug
- 添加 spring-boot-starter-aop 显式依赖

### 2026-02-25
- 实现 Markdown 编辑器 + 文章创建/编辑功能（md-editor-v3）
- 实现图片上传 API（本地存储 + 静态资源映射）
- 文章列表新增「新建文章」、「编辑」、「删除」按钮
- 修复 LogAspect 序列化 User 懒加载字段抛异常的问题
- 修复操作日志排序字段错误（operationTime → createdAt）
- 标签管理增加颜色选择功能
- 分类管理增加排序和图标编辑
- 退出登录对接后端 logout API（JWT 黑名单生效）
- 创建后台管理操作指南文档

### 2026-02-24
- 创建后台管理前端项目（Vue 3 + Element Plus + Pinia + ECharts）
- 实现 7 个管理页面：登录、仪表盘、文章、分类、标签、评论、日志
- 实现 Axios 封装、Pinia 状态管理、路由导航守卫
- 实现 ECharts 数据可视化仪表盘
- 配置 Nginx 反向代理（/admin/ 路径）
- 修复 API 路径重复问题（/api/api/ 双前缀）
- 服务器部署后台管理系统

### 2026-02-22
- 完成后台管理 API 开发（6 个控制器、21 个端点）
- 实现 AdminDashboardController（CompletableFuture 并行查询）
- 实现 AdminArticleController（分页/搜索/筛选/批量操作）
- 实现 AdminCategory/Tag/Comment/LogController
- 服务器部署后端 API 更新

### 2026-02-18
- 修复 RedisConfig 与 Spring Boot 3.x 兼容性问题
- 将 Jackson2JsonRedisSerializer 改为 GenericJackson2JsonRedisSerializer
- 彻底移除 MyBatis-Plus 依赖（解决与 Spring Boot 3.2.2 的 factoryBeanObjectType 冲突）
- OperationLog 实体从 MyBatis-Plus 注解迁移为 JPA 注解
- 创建 application-prod.yml 生产环境配置
- 服务器部署成功，API 接口正常返回数据
- MySQL、Redis、JWT 安全配置均正常

### 2026-02-15
- 完成基础架构搭建（第一阶段）
- 实现 JWT 黑名单机制（解决登出问题）
- 实现 AOP 操作日志（异步记录）
- 实现统一响应体和全局异常处理
- 集成 Redis 缓存（JSON 序列化）
- 配置异步任务线程池
- 设计 RBAC 权限体系
- 实现配置管理（开发/生产环境分离）
- 编写 DDL 脚本和项目文档

### 2026-02-10
- 完成阿里云服务器部署（CentOS 系统）
- 配置 MySQL 数据库和 Nginx
- 后端服务成功启动（8080 端口）
- 前端静态文件部署完成
- 域名解析配置完成（cyruszhang.online）
- 等待 ICP 备案审核

### 2026-02-08
- 创建 GitHub 仓库并上传代码
- 配置 .gitignore 排除构建产物和文档
- 解决 Git 推送问题（SSH 密钥配置）
- 开始阿里云服务器部署

### 2026-01-27
- 项目初始化（Spring Boot + React + Vite）
- 数据库实体设计（User, Article, Category, Tag, Comment）
- 实现 JPA Repository 和 Service 层
- 实现 RESTful API 控制器
- 配置 Spring Security + JWT 认证
- 前端页面和组件开发
- 大地色系极简设计实现
- 代码语法高亮集成
- 解决循环依赖问题（@Lazy 注解）
- 添加数据初始化器
- 创建启动脚本和项目文档

---

## 贡献指南

欢迎提交 Issue 和 Pull Request。

在提交代码前，请确保：
1. 代码符合项目规范（参考 `docs/.cursorrules`）
2. 通过 ESLint 检查（前端）
3. 添加必要的注释和文档

## 参考资源

**官方文档：**
- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security 参考](https://docs.spring.io/spring-security/reference/)
- [React 官方文档](https://react.dev/)
- [Vue 3 官方文档](https://cn.vuejs.org/)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
- [Pinia 文档](https://pinia.vuejs.org/zh/)
- [Vite 官方文档](https://vitejs.dev/)
- [TypeScript 官方文档](https://www.typescriptlang.org/)

**技术参考：**
- [JWT.io](https://jwt.io/) - JWT 认证
- [React Router](https://reactrouter.com/) - 路由管理
- [Axios](https://axios-http.com/) - HTTP 客户端
- [ECharts](https://echarts.apache.org/zh/) - 数据可视化
- [Framer Motion](https://www.framer.com/motion/) - 动画库

## 许可证

MIT License

## 作者

**zhulongqihan**
- GitHub: [@zhulongqihan](https://github.com/zhulongqihan)
- 项目地址: [https://github.com/zhulongqihan/my-blog](https://github.com/zhulongqihan/my-blog)

## 致谢

感谢所有开源项目和社区的支持。

---

> 本项目持续更新中，欢迎 Star 和 Fork。
