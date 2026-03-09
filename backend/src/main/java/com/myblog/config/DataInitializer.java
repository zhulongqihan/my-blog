package com.myblog.config;

import com.myblog.entity.Article;
import com.myblog.entity.Category;
import com.myblog.entity.Tag;
import com.myblog.entity.User;
import com.myblog.repository.ArticleRepository;
import com.myblog.repository.CategoryRepository;
import com.myblog.repository.TagRepository;
import com.myblog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 创建管理员用户
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@myblog.com")
                    .nickname("博主")
                    .bio("这是一个热爱编程的博主")
                    .role(User.Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("✅ 管理员账号创建成功 - 用户名: admin, 密码: admin123");
        }

        // 创建默认分类
        if (categoryRepository.count() == 0) {
            Arrays.asList(
                Category.builder().name("技术").description("技术相关文章").icon("💻").sortOrder(1).build(),
                Category.builder().name("生活").description("生活感悟").icon("🌈").sortOrder(2).build(),
                Category.builder().name("随笔").description("随想随记").icon("✏️").sortOrder(3).build(),
                Category.builder().name("教程").description("技术教程").icon("📚").sortOrder(4).build()
            ).forEach(categoryRepository::save);
            log.info("✅ 默认分类创建成功");
        }

        // 创建默认标签
        if (tagRepository.count() == 0) {
            Arrays.asList(
                Tag.builder().name("Java").color("#f89820").build(),
                Tag.builder().name("Spring Boot").color("#6db33f").build(),
                Tag.builder().name("React").color("#61dafb").build(),
                Tag.builder().name("TypeScript").color("#3178c6").build(),
                Tag.builder().name("MySQL").color("#4479a1").build(),
                Tag.builder().name("Docker").color("#2496ed").build(),
                Tag.builder().name("前端").color("#e91e63").build(),
                Tag.builder().name("后端").color("#9c27b0").build()
            ).forEach(tagRepository::save);
            log.info("✅ 默认标签创建成功");
        }

        // 创建示例文章
        if (articleRepository.count() == 0) {
            User admin = userRepository.findByUsername("admin").orElse(null);
            if (admin != null) {
                Category techCategory = categoryRepository.findByName("技术").orElse(null);
                Category lifeCategory = categoryRepository.findByName("生活").orElse(null);
                Category essayCategory = categoryRepository.findByName("随笔").orElse(null);
                
                Tag javaTag = tagRepository.findByName("Java").orElse(null);
                Tag reactTag = tagRepository.findByName("React").orElse(null);
                Tag frontendTag = tagRepository.findByName("前端").orElse(null);
                Tag dockerTag = tagRepository.findByName("Docker").orElse(null);
                Tag backendTag = tagRepository.findByName("后端").orElse(null);
                
                // 文章1：欢迎文章
                Article welcomeArticle = Article.builder()
                    .title("博客系统技术架构全解析")
                    .summary("从 Spring Boot 后端到 React 前端，完整剖析这套个人博客系统的技术选型、架构设计与工程实践。")
                    .content("# 博客系统技术架构全解析\n\n" +
                            "搭建一套完整的个人博客系统，远比看上去要复杂。这篇文章将从技术选型、架构设计、性能优化等多个维度，分享整个系统的建设思路。\n\n" +
                            "## 后端架构\n\n" +
                            "后端基于 **Spring Boot 3.2**，选用这个版本有几方面考量：\n\n" +
                            "- Java 17 长期支持版本，稳定且语法特性够用\n" +
                            "- Spring Security 6 的新配置方式更清晰\n" +
                            "- 对 GraalVM 原生编译的支持日趋成熟\n\n" +
                            "### 核心技术栈\n\n" +
                            "| 模块 | 技术方案 | 选型理由 |\n" +
                            "|------|---------|----------|\n" +
                            "| 安全认证 | Spring Security + JWT | 无状态、易扩展 |\n" +
                            "| 数据层 | Spring Data JPA + MySQL 8.0 | 开发效率高 |\n" +
                            "| 缓存 | Redis 7 + Redisson | 分布式锁支持 |\n" +
                            "| 消息队列 | RabbitMQ | 异步解耦操作 |\n\n" +
                            "### 点赞系统设计\n\n" +
                            "点赞功能看似简单，实际涉及并发控制：\n\n" +
                            "```java\n" +
                            "// 使用 Redis Lua 脚本保证原子性\n" +
                            "String luaScript = \"\"\"\n" +
                            "    local isMember = redis.call('SISMEMBER', KEYS[1], ARGV[1])\n" +
                            "    if isMember == 1 then\n" +
                            "        redis.call('SREM', KEYS[1], ARGV[1])\n" +
                            "        redis.call('DECR', KEYS[2])\n" +
                            "        return 0\n" +
                            "    else\n" +
                            "        redis.call('SADD', KEYS[1], ARGV[1])\n" +
                            "        redis.call('INCR', KEYS[2])\n" +
                            "        return 1\n" +
                            "    end\n" +
                            "\"\"\";\n" +
                            "```\n\n" +
                            "## 前端架构\n\n" +
                            "前端使用 **React 19 + TypeScript**，搭配 Vite 构建：\n\n" +
                            "```typescript\n" +
                            "// 自定义 Hook 管理文章状态\n" +
                            "function useArticle(id: string) {\n" +
                            "  const [article, setArticle] = useState<Article | null>(null);\n" +
                            "  const [loading, setLoading] = useState(true);\n" +
                            "  \n" +
                            "  useEffect(() => {\n" +
                            "    articleApi.getById(Number(id))\n" +
                            "      .then(res => setArticle(res.data))\n" +
                            "      .finally(() => setLoading(false));\n" +
                            "  }, [id]);\n" +
                            "  \n" +
                            "  return { article, loading };\n" +
                            "}\n" +
                            "```\n\n" +
                            "### 特色功能\n\n" +
                            "- **阅读进度追踪** — 自动记录每篇文章的阅读位置\n" +
                            "- **成就系统** — 阅读里程碑、时段成就解锁\n" +
                            "- **AI 创意工坊** — 集成大语言模型辅助写作\n" +
                            "- **像素小羊** — 可互动的 CSS box-shadow 像素宠物\n\n" +
                            "## 部署方案\n\n" +
                            "整个系统通过 Docker Compose 一键部署：\n\n" +
                            "```bash\n" +
                            "# 构建并启动所有服务\n" +
                            "docker compose up -d --build\n\n" +
                            "# 包含以下容器\n" +
                            "# - Nginx (反向代理 + 前端静态资源)\n" +
                            "# - Spring Boot 应用\n" +
                            "# - MySQL 8.0\n" +
                            "# - Redis 7\n" +
                            "# - RabbitMQ\n" +
                            "```\n\n" +
                            "## 写在后面\n\n" +
                            "造轮子的过程本身就是一种学习。每个看似简单的功能背后，都藏着值得思考的技术选型和实现细节。欢迎翻阅其他文章，看看还有哪些有趣的技术实践。")
                    .author(admin)
                    .category(techCategory)
                    .published(true)
                    .featured(true)
                    .viewCount(0)
                    .likeCount(0)
                    .publishedAt(LocalDateTime.now().minusDays(7))
                    .build();
                
                if (javaTag != null && reactTag != null) {
                    List<Tag> tags1 = new ArrayList<>();
                    tags1.add(javaTag);
                    tags1.add(reactTag);
                    welcomeArticle.setTags(tags1);
                }
                articleRepository.save(welcomeArticle);
                
                // 文章2：关于设计
                Article designArticle = Article.builder()
                    .title("Web 排版与色彩心理学实践")
                    .summary("从版式设计到色彩搭配，聊聊独立开发者在前端视觉层面的思考方法论。不追求花哨，只追求读起来舒服。")
                    .content("# Web 排版与色彩心理学实践\n\n" +
                            "做独立项目最头疼的环节之一，就是视觉设计。作为一个后端出身的开发者，这几年在前端排版和色彩方面踩了不少坑，也摸索出一些实用的方法。\n\n" +
                            "## 为什么技术博客也需要设计\n\n" +
                            "先说一个反直觉的结论：技术文章的阅读完成率和排版质量强相关。\n\n" +
                            "原因很简单 — 当一篇文章的行高不对、字号太小、段落间距紧凑时，读者的大脑会不自觉地产生\"这篇文章很难\"的错觉。反过来，合理的排版让人觉得内容\"易于消化\"，即使技术含量一样。\n\n" +
                            "## 色彩选择的逻辑\n\n" +
                            "### 大地色系的科学依据\n\n" +
                            "这个博客的配色方案基于色彩心理学中的\"自然色\"理论：\n\n" +
                            "- **米白背景 `#F8F8F8`** — 暖色基底降低视觉刺激，比冷色调的纯白 `#FFF` 更适合长时间阅读\n" +
                            "- **炭灰文字 `#2D2D2D`** — 比纯黑 `#000` 少 18% 对比度，减轻视觉疲劳\n" +
                            "- **铁锈强调 `#8B7355`** — 暖色调的强调色引导注意力，不抢夺正文视线\n\n" +
                            "### 色彩的情绪映射\n\n" +
                            "不同色系给读者的心理暗示差别非常大：\n\n" +
                            "| 色系 | 心理暗示 | 适合场景 |\n" +
                            "|------|---------|----------|\n" +
                            "| 冷灰蓝 | 专业、理性 | 企业产品文档 |\n" +
                            "| 暖棕大地 | 温暖、沉静 | 个人博客、笔记 |\n" +
                            "| 亮色渐变 | 活泼、年轻 | 社交产品 |\n" +
                            "| 深色主题 | 沉浸、极客 | 技术社区 |\n\n" +
                            "本站选择大地色系，是因为个人博客最重要的体验是\"坐下来安静读完一篇文章\"。\n\n" +
                            "## 排版的几个关键参数\n\n" +
                            "### 行高与字号\n\n" +
                            "中文排版和英文有本质区别。中文字形方正、笔画密集，需要更大的行高来\"透气\"：\n\n" +
                            "```css\n" +
                            ".article-content {\n" +
                            "  font-size: 1rem;        /* 16px 基准 */\n" +
                            "  line-height: 1.85;      /* 中文推荐 1.8-2.0 */\n" +
                            "  letter-spacing: 0.02em; /* 轻微字间距 */\n" +
                            "  max-width: 720px;       /* 每行 38-42 个汉字 */\n" +
                            "}\n" +
                            "```\n\n" +
                            "### 字体选择策略\n\n" +
                            "遵循一个原则：**标题用衬线 / 正文用无衬线**。\n\n" +
                            "- **标题**：Playfair Display + 思源宋体 — 衬线字体提供\"正式感\"\n" +
                            "- **正文**：Inter + 系统中文字体 — 无衬线字体保证可读性\n" +
                            "- **代码**：JetBrains Mono — 等宽字体，连字功能让运算符更清晰\n\n" +
                            "### 段落节奏\n\n" +
                            "好的文章排版有\"节奏感\"—— 段落长度交替变化，避免大段文字墙：\n\n" +
                            "1. 开头段落控制在 2-3 行\n" +
                            "2. 信息密集处用列表拆分\n" +
                            "3. 每 3-4 段插入一个视觉断点（代码块、表格、引用）\n\n" +
                            "## 响应式不只是缩放\n\n" +
                            "移动端排版不是把桌面版等比缩小那么简单：\n\n" +
                            "```css\n" +
                            "@media (max-width: 768px) {\n" +
                            "  .article-content {\n" +
                            "    font-size: 0.94rem;  /* 略小但不低于 15px */\n" +
                            "    line-height: 1.75;   /* 移动端行高可以略低 */\n" +
                            "    padding: 0 1rem;     /* 保留呼吸空间 */\n" +
                            "  }\n" +
                            "  \n" +
                            "  pre { font-size: 0.8rem; } /* 代码块缩小 */\n" +
                            "}\n" +
                            "```\n\n" +
                            "## 最后说几句\n\n" +
                            "设计是一门权衡的艺术。对独立开发者来说，不需要追求视觉上的惊艳，但需要在排版、色彩和间距上做到\"不犯错\"。当读者没有注意到设计的存在，只是自然而然地读完了文章 —— 这就是设计的成功。")
                    .author(admin)
                    .category(lifeCategory)
                    .published(true)
                    .featured(true)
                    .viewCount(0)
                    .likeCount(0)
                    .publishedAt(LocalDateTime.now().minusDays(5))
                    .build();
                
                if (frontendTag != null) {
                    List<Tag> tags2 = new ArrayList<>();
                    tags2.add(frontendTag);
                    designArticle.setTags(tags2);
                }
                articleRepository.save(designArticle);
                
                // 文章3：技术分享
                Article techArticle = Article.builder()
                    .title("Redis 在高并发场景下的实战方案")
                    .summary("结合真实业务场景，梳理 Redis 在分布式锁、接口限流、缓存击穿防护等方面的工程实践。")
                    .content("# Redis 在高并发场景下的实战方案\n\n" +
                            "Redis 已经是后端工程的标配基础设施。但多数时候我们只用到了 GET/SET，远没发挥出它在并发控制方面的潜力。这篇文章结合博客系统的实际需求，整理几个核心场景。\n\n" +
                            "## 场景一：分布式锁控制点赞\n\n" +
                            "点赞操作需要原子性 —— 如果同一个用户在极短时间内连续点击，可能产生重复数据。常见方案是 Redisson 分布式锁：\n\n" +
                            "```java\n" +
                            "RLock lock = redissonClient.getLock(\"like:\" + articleId + \":\" + visitorId);\n" +
                            "try {\n" +
                            "    if (lock.tryLock(2, 5, TimeUnit.SECONDS)) {\n" +
                            "        // Lua 脚本保证 SISMEMBER + SADD/SREM + INCR/DECR 的原子性\n" +
                            "        Long result = redisTemplate.execute(likeScript, keys, args);\n" +
                            "        return result != null && result == 1;\n" +
                            "    }\n" +
                            "} finally {\n" +
                            "    if (lock.isHeldByCurrentThread()) lock.unlock();\n" +
                            "}\n" +
                            "```\n\n" +
                            "### 为什么用 Lua 脚本\n\n" +
                            "单独用 Redisson 锁不够，因为判断\"是否已点赞\"和\"执行点赞\"之间仍然有时间窗。Lua 脚本在 Redis 服务端执行，天然单线程，真正做到了原子操作。\n\n" +
                            "## 场景二：接口限流\n\n" +
                            "对外暴露的 API 需要防刷。基于滑动窗口的限流比固定窗口更平滑：\n\n" +
                            "```java\n" +
                            "String key = \"rate:\" + ip + \":\" + uri;\n" +
                            "long now = System.currentTimeMillis();\n" +
                            "long windowMs = 60_000; // 1 分钟窗口\n\n" +
                            "// 移除窗口外的请求记录\n" +
                            "redisTemplate.opsForZSet().removeRangeByScore(key, 0, now - windowMs);\n" +
                            "// 统计窗口内的请求数\n" +
                            "Long count = redisTemplate.opsForZSet().zCard(key);\n" +
                            "if (count != null && count >= maxRequests) {\n" +
                            "    throw new RateLimitExceededException();\n" +
                            "}\n" +
                            "// 记录本次请求\n" +
                            "redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);\n" +
                            "redisTemplate.expire(key, Duration.ofMinutes(2));\n" +
                            "```\n\n" +
                            "这里使用 Sorted Set，以时间戳作为 score，天然支持按时间范围清理。\n\n" +
                            "## 场景三：缓存击穿防护\n\n" +
                            "热点文章的缓存过期瞬间，大量请求穿透到数据库，可能导致雪崩。两层防护：\n\n" +
                            "1. **互斥锁** — 只允许一个线程回源查库\n" +
                            "2. **逻辑过期** — 缓存永不物理过期，通过逻辑时间戳判断是否需要更新\n\n" +
                            "```java\n" +
                            "public Article getArticleWithCache(Long id) {\n" +
                            "    String key = \"article:\" + id;\n" +
                            "    String json = redisTemplate.opsForValue().get(key);\n" +
                            "    \n" +
                            "    if (json != null) {\n" +
                            "        CacheWrapper wrapper = parse(json);\n" +
                            "        if (!wrapper.isExpired()) return wrapper.getData();\n" +
                            "        // 逻辑过期，异步更新\n" +
                            "        CompletableFuture.runAsync(() -> refreshCache(id, key));\n" +
                            "        return wrapper.getData(); // 先返回旧数据\n" +
                            "    }\n" +
                            "    return loadAndCache(id, key);\n" +
                            "}\n" +
                            "```\n\n" +
                            "## 场景四：排行榜\n\n" +
                            "Redis 的 Sorted Set 天然适合排行榜：\n\n" +
                            "```bash\n" +
                            "# 文章热度排行（score = 浏览量 * 1 + 点赞量 * 5）\n" +
                            "ZADD article:hot 150 \"article:1\"\n" +
                            "ZADD article:hot 420 \"article:2\"\n" +
                            "\n" +
                            "# 取 Top 10\n" +
                            "ZREVRANGE article:hot 0 9 WITHSCORES\n" +
                            "```\n\n" +
                            "## 运维层面的注意事项\n\n" +
                            "### 内存控制\n\n" +
                            "- 给所有 key 设置合理的 TTL\n" +
                            "- 监控 `info memory` 中的 `used_memory` 增长趋势\n" +
                            "- 使用 `maxmemory-policy allkeys-lru` 作为兜底策略\n\n" +
                            "### 持久化选择\n\n" +
                            "- 点赞数据：AOF + 每秒 fsync，保证不丢数据\n" +
                            "- 限流计数：纯内存即可，重启后自动恢复\n" +
                            "- 缓存数据：RDB 快照足够，丢了可以重建\n\n" +
                            "## 总结\n\n" +
                            "Redis 的价值不仅是\"快\"，更在于它提供了丰富的数据结构来解决并发场景下的各种工程问题。关键在于理解每种数据结构的特性，选对了结构，实现就是水到渠成的事。")
                    .author(admin)
                    .category(techCategory)
                    .published(true)
                    .featured(false)
                    .viewCount(0)
                    .likeCount(0)
                    .publishedAt(LocalDateTime.now().minusDays(3))
                    .build();
                
                if (javaTag != null) {
                    List<Tag> tags3 = new ArrayList<>();
                    tags3.add(javaTag);
                    techArticle.setTags(tags3);
                }
                articleRepository.save(techArticle);

                articleRepository.save(buildArticle(
                    admin,
                    techCategory,
                    false,
                    0,
                    2,
                    "我为什么做了一个航旅稳定性 AI Agent 项目",
                    "从博客里的 AI 创意工坊到独立 Agent 项目，这篇文章完整介绍 AeroFlow Sentinel 的定位、架构和为什么我决定把它做成可演示的作品。",
                    """
        # 我为什么做了一个航旅稳定性 AI Agent 项目

        最近这半年，我越来越清楚一件事：如果简历里写“会用大模型”“做过 AI 功能”，其实并不能真正说明问题。真正能拉开差距的，是你有没有把这些能力做成一个独立运行、可复现、可解释的系统。

        于是我做了 AeroFlow Sentinel。

        它不是一个泛泛的聊天网页，而是一个聚焦在航旅预订链路稳定性分析的 AI Agent 项目。它能围绕搜索、下单、出票、退改签、供应商网关等环节进行多轮问答，也能通过 Supervisor、Planner、Executor 的多 Agent 协作方式输出巡检报告。

        ## 这个项目为什么和博客有关

        很多人会把“写文章”和“做项目”分开理解，但对我来说，这两件事本来就是同一件事的两面。

        - 博客负责把方法论讲清楚
        - 项目负责把方法论跑起来
        - 文档负责让别人看得懂我为什么这样设计

        过去我在博客里写过 Redis 高并发、部署、前端体验、AI 集成等内容。继续往前走一步，就是把这些零散思考收束成一个完整的工程项目。

        ## 这个 Agent 解决的真实问题

        我不想做一个“什么都能聊两句，但什么都不深入”的大模型壳子，所以一开始就限定了业务范围：只做航旅预订链路稳定性相关分析。

        这样做有三个好处：

        1. 问题边界清晰，容易构建高质量提示词和工作流
        2. 能体现领域建模能力，而不只是调 API
        3. 方便把知识库、巡检手册和系统输出连接起来

        ## 架构上我关心的不是炫技，而是落地

        这个项目的后端基于 Java 17、Spring Boot、Spring AI Alibaba，前端则保持轻量，优先解决流式输出、模式切换、对话上下文和报告展示。

        我更在意的是这些问题：

        - 在低配 ECS 上能不能稳定跑起来
        - 向量库不可用时有没有降级方案
        - 多 Agent 流程是不是能被普通人理解
        - 文档和演示能不能支撑面试沟通

        所以你会看到我在这个项目里做了几件很务实的事：

        - 保留本地 Markdown 手册兜底检索
        - 支持 Demo 模式，减少部署门槛
        - 巡检结果统一输出 Markdown，便于复盘和留痕
        - 把线上演示做成一个可直接打开的页面，而不是停留在代码仓库里

        ## 我最看重的收获

        这个项目真正有价值的地方，并不是“又接了一个模型”。

        而是它逼着我把很多模糊的概念具体化：

        - 什么叫一个可解释的 Agent 工作流
        - 什么叫一个有边界的业务场景
        - 什么叫真实可运行的演示系统
        - 什么叫在简历里写得出、面试里讲得明白

        ## 最后

        如果说博客是我公开思考的地方，那这个 Agent 项目更像是我把思考继续推进后的工程结果。

        我很喜欢这种关系：不是为了凑作品而做项目，而是因为写着写着，发现有些问题必须通过一个真正的系统来回答。
        """,
                    tagsOf(javaTag, dockerTag, backendTag)
                ));

                articleRepository.save(buildArticle(
                    admin,
                    techCategory,
                    false,
                    0,
                    1,
                    "低配云服务器上部署 AI 应用，我做了哪些取舍",
                    "从 2G 内存到容器资源限制，聊聊个人项目在真实服务器上部署 AI 应用时，哪些地方应该克制，哪些地方不能省。",
                    """
        # 低配云服务器上部署 AI 应用，我做了哪些取舍

        很多教程默认你有一台配置不错的云服务器，有足够的内存、磁盘和网络带宽。但现实往往不是这样。个人项目、学生项目、作品集项目，大多数时候跑在一台低配机器上。

        我最近同时维护博客和一个独立 AI Agent 项目，这件事让我重新理解了“工程取舍”四个字。

        ## 第一原则：先区分什么必须在线，什么可以分离

        如果所有能力都硬塞到同一个进程、同一套容器里，最后往往是两边都不稳定。

        所以我做的第一件事就是拆边界：

        - 博客继续保持 Docker Compose 管理
        - AI Agent 先独立运行，避免拖垮博客主站
        - 公网访问上允许不同端口，而不是一开始就追求最完美的域名形态

        这不是“妥协”，而是优先级管理。

        ## 第二原则：资源限制要主动写死

        很多人部署项目时怕出问题，不敢设资源上限。但对低配机器来说，不设上限才更危险。

        我更倾向于在 Docker Compose 里明确写出内存上限：

        - MySQL 控制在可接受范围
        - Redis 用小内存并配合淘汰策略
        - RabbitMQ 只承担必要职责
        - 后端 JVM 明确指定 Xms/Xmx

        这样做的好处不是省那几十兆，而是避免某个服务异常膨胀后把整台机器拖死。

        ## 第三原则：演示优先级高于“理论最优”

        个人项目常见的误区是：总想一步到位做成企业级架构。

        但如果你的目标之一是对外展示，那么一个稳定、能打开、能跑通核心链路的 Demo，往往比一套复杂但经常启动失败的系统更有价值。

        所以在 AI Agent 项目里，我做了这些偏向演示友好的设计：

        - Demo profile 默认关闭昂贵依赖
        - Milvus 不可用时自动降级
        - 输出使用 Markdown，便于展示结果
        - 页面优先突出任务链路，而不是堆砌参数

        ## 第四原则：安全边界不能因为低配就省略

        预算有限不代表可以把 Key 暴露在前端里。

        这也是我为什么坚持把 DashScope 的 Authorization 放在 Nginx 层，由服务端注入，而不是让前端直接带着 Key 请求。

        资源可以紧一点，但安全边界不能顺手删掉。

        ## 第五原则：把复杂度留给未来，把可用性留给现在

        我现在没有把 AI Agent 也立即塞进博客的 Docker Compose，不是因为不会，而是因为这一步对“当前可用性”的帮助不如对“未来复杂度”的增加来得明显。

        真正值得做的演进顺序应该是：

        1. 先跑稳
        2. 再统一入口
        3. 再做子域名和守护进程
        4. 最后再考虑更漂亮的部署形态

        ## 结语

        低配机器最锻炼人的地方，不是技术炫耀空间少，而是逼着你认真回答一个问题：什么是这个系统真正重要的部分？

        当资源有限时，架构会变得诚实。你必须学会删减、排序、分层，这些能力比“我会多少组件”更接近真实工程。
        """,
                    tagsOf(dockerTag, backendTag)
                ));

                articleRepository.save(buildArticle(
                    admin,
                    techCategory,
                    false,
                    0,
                    0,
                    "AI 爆火之后，普通开发者应该怎样建立自己的工具箱",
                    "不是追着每个新名词跑，而是搭一套自己真的会用、能复用、能解释的 AI 工具链。",
                    """
        # AI 爆火之后，普通开发者应该怎样建立自己的工具箱

        过去一段时间，AI 领域最容易制造焦虑的一件事，就是新名词更新得太快了。

        今天是 Agent，明天是 MCP，后天是工作流，再过两周又会有新的框架和新的叙事。对普通开发者来说，如果只是跟着热度跑，很容易陷入一种“什么都知道一点，但没有真正沉淀”的状态。

        我现在越来越相信，普通开发者真正需要的不是追热点，而是建立一套自己的 AI 工具箱。

        ## 先把工具箱分层

        我把它拆成四层：

        ### 第一层：模型调用层

        这是最基础的一层。你至少要知道：

        - 什么是聊天补全接口
        - 什么是流式输出
        - 什么是系统提示词、用户消息、多轮上下文

        这一层解决的是“你能不能把模型先接起来”。

        ### 第二层：产品交互层

        接起来只是开始，真正的差异往往体现在交互上：

        - 返回太慢时怎么做流式展示
        - 内容太长时怎么结构化输出
        - 结果怎么复制、保存、继续追问
        - 用户如何知道系统现在在做什么

        这一层决定你的 AI 功能是不是像个真正能用的产品，而不是一个调试面板。

        ### 第三层：工程化层

        这部分经常被忽略，但恰恰最重要：

        - Key 放在哪里
        - 如何控制请求超时和错误提示
        - 如何记录日志和排障
        - 如何在服务器上稳定部署

        没有这一层，项目就很难从“能跑一次”变成“可以长期展示”。

        ### 第四层：场景建模层

        这是我现在最重视的一层。真正有辨识度的项目，不是“我接了一个模型”，而是“我针对某个问题建了一个合理的任务边界”。

        比如博客里的 AI 创意工坊，关注的是前端方案生成；而独立 Agent 项目关注的是航旅稳定性分析。这就是场景建模。

        ## 我的工具箱里有什么

        就当前阶段来说，我会优先保留这些能力：

        - 一个稳定的大模型服务商入口
        - 一套可复用的 SSE 前端解析逻辑
        - 一种服务端保护 Key 的代理方式
        - 一套适合展示的结构化输出模板
        - 一个能够承载特定业务场景的项目外壳

        注意，这里面没有“把所有最热门框架都装一遍”。

        ## 最容易犯的错误

        ### 错误一：过早追求全能

        越想做“万能 AI 平台”，越容易做出一个定位模糊的产品。普通开发者更适合先做窄场景、深一点。

        ### 错误二：只会 Demo，不会解释

        一个按钮能跑出结果不代表你真的理解它。你最好能讲清楚：

        - 为什么选这个模型
        - 为什么选这种输出结构
        - 为什么这么部署
        - 如果失败了怎么降级

        ### 错误三：把新名词当能力

        会说 MCP、RAG、Agent 并不等于会用。能在自己的项目里做出边界清晰、体验完整、可上线的东西，才更像能力。

        ## 我现在的判断

        对大多数普通开发者来说，最值得投入的方向不是无限追新，而是：

        - 把模型调用做扎实
        - 把交互体验做好
        - 把部署和安全边界守住
        - 把业务场景收窄并讲清楚

        当这些基础能力稳定之后，再往 Agent、工作流、知识库这些更复杂的方向扩展，才会更顺。

        ## 最后

        AI 的热闹还会持续很久，但真正会留下来的，往往不是那些一眼看起来最炫的东西，而是那些真的能被反复使用、反复讲述、反复演示的能力模块。

        工具箱的意义就在这里：它不是让你追上所有变化，而是让你在变化里保持自己的节奏。
        """,
                    tagsOf(reactTag, backendTag, dockerTag)
                ));

                articleRepository.save(buildArticle(
                    admin,
                    lifeCategory,
                    false,
                    0,
                    0,
                    "研究生这一年，我怎么平衡课程、项目和持续输出",
                    "不是完美的时间管理，而是用一套足够朴素的节奏，把学习、项目和写作慢慢叠起来。",
                    """
        # 研究生这一年，我怎么平衡课程、项目和持续输出

        很多时候，真正让人焦虑的不是忙，而是你明明很忙，却看不到这些投入最后汇成了什么。

        研究生这段时间，我一直在做几件事：上课、写项目、准备面试、维护博客、折腾 AI 方向的新东西。它们每一件单看都合理，但堆在一起时，很容易让人觉得生活像一个不断拉扯的页面。

        我后来慢慢意识到，自己需要的不是更激进的效率系统，而是一种能持续下去的节奏。

        ## 我先接受了一件事：不可能每天都同时推进所有事

        如果总想着一天里课程、项目、写作、运动、社交、面试准备全部兼顾，最后通常是哪件都只做了一点点，还会因为“不够完整”而自责。

        后来我改成了更现实的分配方式：

        - 有些天重点是输入，比如上课、看资料、梳理思路
        - 有些天重点是输出，比如写代码、写文章、做项目页
        - 有些天重点只是把之前的进度接上，不追求大推进

        这听起来不高级，但比“每天全都要”有效得多。

        ## 我给自己留了一个很小的输出单位

        持续输出最难的地方，在于你总觉得要有一整块完整时间才能开始。

        但现实里，大块时间并不总能出现。

        所以我给自己定的最低单位很小：

        - 写一个段落
        - 改一个模块
        - 把一个想法记到文档里
        - 修掉一个真实问题

        这件事带来的变化非常大。因为一旦“开始”的门槛降低了，很多事情就不会在心理上无限拖延。

        ## 博客和项目其实在帮我节省时间

        以前我以为写博客会额外占时间，后来发现它反而在帮我整理时间。

        因为当你把一件事写出来之后：

        - 你对它的理解会更清楚
        - 以后面试时可以直接复用表达
        - 做相关项目时也更容易知道自己要强调什么

        换句话说，写作不是和项目竞争时间，而是在给项目和表达做压缩存储。

        ## 我尽量让“同一条内容线”服务多个目标

        比如我最近在做 AI Agent 项目，这件事本身可以拆成很多层：

        - 写一篇文章，讲为什么做它
        - 改博客入口，让两个项目形成关联
        - 梳理部署过程，变成文档
        - 最后把它沉淀成简历里的一个完整项目

        这样一来，我做的不是四件分散的事，而是在同一条线上推进四个结果。

        这比同时并行做四条完全不同的线轻松很多。

        ## 我现在最看重的是“节奏感”

        我越来越不相信那种把自己压到极限的高强度叙事。

        对我来说，真正重要的是：

        - 本周有没有在往前走
        - 这个月有没有留下能复用的东西
        - 这段时间的努力有没有形成清晰的作品

        只要答案是肯定的，就不需要因为某一天不够满分而否定自己。

        ## 最后

        课程、项目和输出从来不是彼此独立的三件事。关键是你能不能找到一种方式，让它们互相供给，而不是互相消耗。

        我还远没做到特别从容，但至少现在比以前更清楚：持续成长，很多时候不是靠爆发，而是靠一种不那么戏剧化、但可以重复很多天的稳定节奏。
        """,
                    tagsOf(frontendTag)
                ));

                articleRepository.save(buildArticle(
                    admin,
                    essayCategory != null ? essayCategory : lifeCategory,
                    false,
                    0,
                    0,
                    "AI 时代的普通人写作：为什么我还是坚持把想法写成长文",
                    "在短内容越来越快的环境里，我反而越来越相信长文的价值，因为它是把模糊判断变成清晰结构的一种方式。",
                    """
        # AI 时代的普通人写作：为什么我还是坚持把想法写成长文

        这两年最常见的一种说法是：AI 让写作越来越便宜了。

        这句话没错。但我越来越在意它的另一面：当写作成本下降之后，真正稀缺的反而不是“能不能写出来”，而是“你到底有没有想清楚”。

        所以我反而更愿意写长文。

        ## 长文不是为了显得深刻，而是为了逼自己把问题讲明白

        很多念头在脑子里看起来是顺的，一旦真的写成几千字，你就会发现它哪里含糊、哪里跳步、哪里只是情绪，哪里才是真正的判断。

        对我来说，写长文最大的价值不是输出给别人看，而是把自己的想法从“差不多明白”推进到“可以复述、可以讨论、可以落地”。

        ## 短内容时代更需要慢一点的思考容器

        短内容很适合提醒、刺激、抛钩子，但并不总适合承载复杂问题。

        比如这些问题：

        - 一个项目为什么要这样设计
        - 一个技术选择背后的取舍是什么
        - 一段阶段性的成长究竟发生了什么变化

        如果只用几十秒的视频或几百字的碎片表达，很多时候只能留下态度，留不下结构。

        而我更想保留结构。

        ## AI 不会让写作失去意义，反而会重新定义意义

        我现在也会用 AI 帮助自己整理资料、生成初稿、检查表达盲区。但这并没有让我觉得“那我就不用写了”。

        相反，它让我更清楚什么部分是机器可以补的，什么部分是必须自己承担的。

        比如：

        - 素材归纳，AI 可以加速
        - 结构拆解，AI 可以辅助
        - 语言润色，AI 可以参与
        - 但真正的判断、取舍、立场，还是得自己来

        写作的价值，正在从“手工生成文字”转向“对问题负责”。

        ## 我为什么把博客当成长期容器

        对我来说，博客不是一个资讯流，而更像一个沉淀系统。

        这里的文章不一定都追热点，也不一定都立刻有流量，但它们会慢慢构成一个更稳定的东西：

        - 我思考问题的路径
        - 我做项目时的判断依据
        - 我在不同阶段关心的主题

        它们会把一个人从“会发内容”变成“有持续输出轨迹的人”。

        ## 最后

        在 AI 时代继续写长文，也许看起来有点逆势而行。但我反而觉得，这是我能给自己保留的一种确定性。

        因为世界会越来越快，工具会越来越强，而一个人愿不愿意把自己的经验、判断和困惑认真写下来，仍然是一件很私人、也很珍贵的事。
        """,
                    tagsOf(frontendTag)
                ));

                articleRepository.save(buildArticle(
                    admin,
                    techCategory,
                    false,
                    0,
                    0,
                    "从提示词到工作流：我对这波 AI Agent 热潮的几个判断",
                    "Agent 不是一句提示词的放大版，它真正有意义的地方，在于任务拆解、状态管理和结果可解释性。",
                    """
        # 从提示词到工作流：我对这波 AI Agent 热潮的几个判断

        如果把过去一年的 AI 热潮浓缩成一句话，大概就是：大家已经不满足于“问一句、答一句”了，开始期待模型自己完成更长、更复杂的任务链。

        这就是 Agent 这个词重新爆火的背景。

        但我越来越觉得，围绕 Agent 的讨论里，真正值得关注的不是名字，而是三个更具体的问题：

        - 任务有没有被正确拆开
        - 系统内部状态有没有被管理好
        - 最后的输出能不能被人理解和复盘

        ## 提示词当然重要，但它不是全部

        很多人第一次接触 Agent，会把它理解成“提示词写得更长一点”。这不完全错，但也不够。

        提示词决定了系统如何理解任务，但当任务变复杂时，光有提示词不够，还需要：

        - 把目标分解为多个阶段
        - 在阶段之间传递上下文
        - 根据中间结果调整下一步
        - 在失败时决定重试、降级还是终止

        这时你真正面对的，已经不是“怎么问模型”，而是“怎么设计一个工作流”。

        ## 我为什么开始更关注工作流设计

        因为一旦进入真实场景，问题就会立刻变得具体：

        - 哪一步应该由模型做，哪一步应该由规则做
        - 哪一步必须联网，哪一步可以本地完成
        - 哪一步输出给用户，哪一步只作为内部推理

        这些都不是单个 prompt 能解决的。

        所以我在做独立 Agent 项目时，更在意的是把系统拆成 Supervisor、Planner、Executor 这样的角色。它们不一定必须叫这个名字，但背后对应的是不同的职责。

        ## 真正的门槛是“可解释”

        我觉得未来一段时间里，Agent 项目最容易拉开差距的地方，不是能不能完成任务，而是能不能把完成过程说清楚。

        尤其在运维、巡检、分析、知识助手这类场景里，用户通常不只关心结果，还关心：

        - 为什么是这个结论
        - 中间参考了哪些信息
        - 有没有遗漏的风险点
        - 如果我不接受这个结果，还能怎么继续问

        这意味着 Agent 的输出最好天然适合结构化展示，比如 Markdown、分段结论、步骤式报告，而不是一整坨看似流畅但难以追踪的文字。

        ## 我对接下来的一点判断

        ### 第一，窄场景会比大而全更有生命力

        通用 Agent 听起来很酷，但真正容易做出成果的，还是那些业务边界清晰、评价标准明确的场景。

        ### 第二，工程化会越来越重要

        未来大家比拼的不会只是“接没接模型”，而是：

        - 你的系统稳不稳定
        - 有没有降级路径
        - 出错后怎么排查
        - 部署成本高不高

        ### 第三，能写清楚的人会更占优势

        Agent 项目复杂度高，越复杂的东西，越需要文档、文章、复盘和演示去帮助别人理解。这也是我为什么一边做项目，一边继续写博客。

        ## 结语

        我不觉得 Agent 是一个短期热词，但我也不认为它的价值只是包装一个新概念。

        对我来说，它真正有意思的地方，在于它逼着开发者同时面对提示词、流程设计、状态管理、交互体验和工程落地这些问题。

        这也是为什么我会继续对这个方向保持投入：它不是单点技能，而是一条能把很多能力真正串起来的主线。
        """,
                    tagsOf(javaTag, backendTag, dockerTag)
                ));
                
                log.info("✅ 示例文章创建成功 - 共 {} 篇", articleRepository.count());
            }
        }
    }

            private Article buildArticle(
                User author,
                Category category,
                boolean featured,
                int viewCount,
                int daysAgo,
                String title,
                String summary,
                String content,
                List<Tag> tags
            ) {
            Article article = Article.builder()
                .title(title)
                .summary(summary)
                .content(content)
                .author(author)
                .category(category)
                .published(true)
                .featured(featured)
                .viewCount(viewCount)
                .likeCount(0)
                .publishedAt(LocalDateTime.now().minusDays(daysAgo))
                .build();
            article.setTags(tags);
            return article;
            }

            private List<Tag> tagsOf(Tag... tags) {
            List<Tag> result = new ArrayList<>();
            for (Tag tag : tags) {
                if (tag != null) {
                result.add(tag);
                }
            }
            return result;
            }
}
