# AI Agent 项目集成说明

> 版本：v2.1.1  
> 日期：2026-03-09

## 1. 背景

博客主站已经具备文章系统、后台管理、AI 创意工坊和多项高并发能力。为了让作品集展示更完整，我将另一个独立项目 AeroFlow Sentinel 接入到博客中，作为外部项目入口。

这个 AI Agent 项目本身是一个航旅预订链路稳定性分析系统，和博客不是同一个代码库、也不是同一个运行进程，因此博客侧最合适的集成方式不是硬嵌入，而是提供稳定、统一、可发现的跳转入口。

## 2. 当前真实部署情况

- 博客部署路径：`/www/my-blog`
- 博客运行方式：Docker Compose 5 容器（Nginx + Backend + MySQL + Redis + RabbitMQ）
- AI Agent 部署路径：`/home/root/apps/superbizagent/SuperBizAgent-release-2026-01-02`
- AI Agent 运行方式：独立 Java 进程，监听 `9900` 端口
- AI Agent 当前访问地址：`http://cyruszhang.online:9900`

## 3. 为什么采用“外部链接入口”而不是反向代理新页面

### 3.1 这样做的优点

- 不破坏博客现有路由结构
- 不引入额外跨项目耦合
- 可以保留 AI Agent 自己的页面和交互风格
- 部署时只需要更新博客前端，不必改动博客后端接口
- 简历展示时，博客和 Agent 项目可以分别独立说明

### 3.2 为什么没有直接用子域名

当前服务器上 `agent.cyruszhang.online` 的 DNS 还没有配置，所以现阶段使用 `http://cyruszhang.online:9900` 是最直接且真实可用的方案。

如果后续补齐 DNS，可以再升级为：

- `agent.cyruszhang.online` → `118.31.221.81`
- 再通过 Nginx 做 80/443 端口转发

## 4. 博客内的四个入口点

### 4.1 顶部导航栏

- 作用：主入口，用户一眼可见
- 行为：点击后新窗口打开 AI Agent

### 4.2 关于页“相关项目”卡片

- 作用：适合简历展示和项目说明
- 内容：项目简介、技术栈标签、在线演示按钮

### 4.3 页脚链接

- 作用：作为全站兜底入口
- 适合桌面端和移动端用户在浏览结束时继续跳转

### 4.4 命令面板

- 作用：提高高级用户访问效率
- 支持快捷键 `Ctrl/Cmd + Shift + A`

## 5. 实现文件

- `frontend/src/constants/externalLinks.ts`：统一维护 AI Agent 外部链接常量
- `frontend/src/components/Header.tsx`：顶部导航外链入口
- `frontend/src/pages/AboutPage.tsx`：相关项目展示卡片
- `frontend/src/components/Footer.tsx`：页脚入口
- `frontend/src/components/CommandPalette.tsx`：命令面板入口
- `frontend/src/App.tsx`：全局快捷键入口

## 6. 后续可选优化

1. 配置 `agent.cyruszhang.online` 域名，去掉 `:9900`
2. 给 AI Agent 增加 systemd 或 Docker 守护，避免手动起进程
3. 如果未来希望博客内直接预览，可新增一个项目展示页，用卡片或 iframe 做二级展示

## 7. 面试表达建议

你可以这样说：

“我的博客主站本身是一个完整的全栈系统，但我另外还做了一个独立的 AI Agent 项目。为了让作品集形成联动，我没有简单把链接塞进 README，而是在博客导航、关于页、页脚和命令面板里统一接入了外部入口。这样既保持了两个系统的独立部署边界，也让项目矩阵对外展示更完整。”