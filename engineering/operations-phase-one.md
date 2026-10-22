# 运行容量与状态保护

## 进程和内存预算

共享主机上的运行与构建使用不同预算：Java 和前端源码构建在开发机串行进行，服务器只接收已验证产物；`docker/runtime/` 只将这些产物复制进已有运行镜像，不编译、不下载依赖。不要在运行主机上执行 Maven、Vite 或全量镜像盘点。发布入口使用 `docker/ops/release-preflight.sh` 持锁并检查至少 512 MiB 可用内存和 2 GiB 可用磁盘。

| 服务 | 内存上限 | 主要并发约束 |
| --- | --- | --- |
| 博客后端 | 768 MiB，堆 384 MiB，直接内存 64 MiB | HTTP 32 线程、32 等待；连接池 5；AI 工作线程 2、队列 8；总准入 10 |
| 独立 Agent | 512 MiB，堆 256 MiB，直接内存 32 MiB | HTTP 24；Run 2/队列 8；工具 4/队列 12；旧接口 2/队列 8；每会话 1 |
| MySQL / RabbitMQ | 512 / 256 MiB | 保留既有数据服务约束 |
| 关键状态 / 可丢缓存 Redis | 96 / 64 MiB | 数据内存分别 48 / 32 MiB |
| 前台 / 管理台 Nginx | 64 / 32 MiB | 静态资源和反向代理 |

这些是各进程的保护上限，不能相加后当作物理内存预留或已验证容量。发布时记录主机可用内存、各进程 RSS、容器 OOM 状态及实际 `vmstat` 换入换出速率；在目标并发下阅读路径必须仍然成功。队列满应拒绝新 AI 任务，禁止无界排队或启动替代线程。可用内存不足 256 MiB 或换页总速率达到 4096 KiB/s 时，容量监测会告警；扩容或降低任务并发后再继续放量。

## Redis 两个故障域

- `redis` 是关键状态实例：Token 撤销、浏览量 active/pending 批次、点赞去重及业务计数均使用默认业务模板，`noeviction`，AOF `appendfsync always`，数据卷持久化。达到容量时写入失败，不能回报成功或退化为放行。
- `redis-cache` 只承载 Spring Cache、文章详情与重建锁，`allkeys-lru`，不持久化、不映射公网端口；缓存丢失可回源。单独 database 不能隔离 Redis 内存淘汰，因此使用独立实例。
- 本地单 Redis 开发方式兼容保留；Compose 显式设置 `BLOG_CACHE_REDIS_ENABLED=true` 和独立缓存地址。独立工厂由 holder 管理，不会替换 Spring Boot 的业务连接工厂。
- 文章缓存返回前先检查数据库公开状态；写入提交后再清缓存和更新索引。缓存不可达时读路径回源；关键状态不可达时业务报错。
- 浏览量经 Lua 原子移至 pending，再在数据库唯一批次账本与原子增量同一事务中提交；ACK 丢失可重试，不能重复累计。发布前执行 `backend/src/main/resources/db/manual/V20261022__view_count_batches.sql`。

迁移已有 Redis 时，先保存 RDB 和受保护的离线备份，再在原实例在线启用 AOF，并等待初次 rewrite 完成和持久化状态正常。验证 AOF 文件已写入同一数据卷后才能重建容器。不能在只存在 RDB 的数据卷上直接以空 AOF 启动，也不能 `FLUSHDB` 或批量删除业务前缀。旧缓存可自然过期；回滚应用时仍保留关键状态实例的 noeviction 和持久化设置。Compose 显式设置 MySQL driver/dialect 和 `ddl-auto=validate`，不允许缺失私有配置时继承开发环境的 H2/create-drop；发布时保留已有私有生产 profile 的外部只读挂载。

## 日志和告警

Compose 对每个容器配置 `json-file max-size=10m,max-file=3`；配置只对新建或重建的容器生效，必须逐个核验运行容器的 `HostConfig.LogConfig`。不要直接截断 Docker 管理的日志文件。现有日志先备份，再在维护窗口按服务替换容器，数据卷保持不变。[Docker json-file 文档](https://docs.docker.com/engine/logging/drivers/json-file/)

两个 Java 进程的文件日志单文件 10 MB、保留 7 天、总量 70 MB；systemd 管理的 Agent 同时写 journald，发布时核验主机既有 journal 配额，不修改其他应用的全局日志设置。审计/错误证据须在清理前导出到受保护目录，不能把凭据或完整供应商响应放入公开轨迹。

将 `docker/ops/myblog-health.py` 安装到 `/opt/myblog-ops/`，配套 service/timer 安装到 `/etc/systemd/system/` 后启用 timer。它每 5 分钟采集磁盘、inode、可用内存和实际换页；阈值分别为 85%、85%、256 MiB、4096 KiB/s。状态保存在 `/var/lib/myblog-health/state.json`，仅告警集合变化或恢复时输出 journal 事件；未配置邮件或外部消息。可用 `journalctl -u myblog-health.service` 查看告警，不能仅凭 swap 已占用量认定持续换页。

验收使用独立临时容器触发缓存淘汰、关键状态重启恢复、日志轮转和容量阈值，不对生产故障注入。验证结果与线上应用状态分别记录，未部署的配置不能写成已经生效。
