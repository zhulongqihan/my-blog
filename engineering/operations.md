# Operations Runbook

## Deploy

```bash
cd /www/my-blog
git pull --ff-only origin main
docker compose config --quiet
docker compose build nginx backend
docker compose up -d admin nginx backend
```

先确认配置校验通过，再重建应用容器。MySQL、Redis、RabbitMQ 使用命名卷，不在普通应用发布中删除或重建数据。

前台静态资源由 `nginx` 镜像构建，Vue 管理后台由独立的 `admin` 静态服务构建。仅有前台变更时可以执行 `docker compose build nginx && docker compose up -d nginx`；仅有后台变更时执行 `docker compose build admin && docker compose up -d admin`，最后确认 `nginx` 的 `/admin/` 代理仍然健康。首次部署拆分后的编排时，先构建并启动 `admin`，再启动 `nginx`。

## Verify

```bash
bash tools/server-smoke.sh
docker compose ps
```

同时确认 `myblog-admin` 健康，并通过 `/admin/` 和 `/admin/login` 做未登录页面回归。

使用 `tools/k6-read-path.js` 在服务器执行读路径压测，记录 p95、失败率和压测前后的容器资源。

## Rollback

```bash
git log --oneline -5
git checkout <known-good-commit>
docker compose build nginx admin backend
docker compose up -d admin nginx backend
```

回滚前保留当前提交、容器状态和日志；不要删除数据库卷。
