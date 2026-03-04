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
                
                Tag javaTag = tagRepository.findByName("Java").orElse(null);
                Tag reactTag = tagRepository.findByName("React").orElse(null);
                Tag frontendTag = tagRepository.findByName("前端").orElse(null);
                
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
                
                log.info("✅ 示例文章创建成功 - 共 {} 篇", articleRepository.count());
            }
        }
    }
}
