# Operations Runbook

## Deploy

```bash
cd /www/my-blog
git pull --ff-only origin main
docker compose config --quiet
docker compose build nginx backend
docker compose up -d
```

先确认配置校验通过，再重建应用容器。MySQL、Redis、RabbitMQ 使用命名卷，不在普通应用发布中删除或重建数据。

## Verify

```bash
bash tools/server-smoke.sh
docker compose ps
```

使用 `tools/k6-read-path.js` 在服务器执行读路径压测，记录 p95、失败率和压测前后的容器资源。

## Rollback

```bash
git log --oneline -5
git checkout <known-good-commit>
docker compose build nginx backend
docker compose up -d
```

回滚前保留当前提交、容器状态和日志；不要删除数据库卷。
