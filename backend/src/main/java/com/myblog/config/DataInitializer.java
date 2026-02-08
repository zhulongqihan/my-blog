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
                    .title("欢迎来到我的博客")
                    .summary("这是一个使用 Spring Boot + React 搭建的个人博客系统，采用极简主义大地色系设计，营造温暖复古的阅读体验。")
                    .content("# 欢迎来到我的博客\n\n" +
                            "## 关于这个博客\n\n" +
                            "这是一个使用现代技术栈搭建的个人博客系统：\n\n" +
                            "### 后端技术\n\n" +
                            "- **Spring Boot 3.2** - 强大的 Java 框架\n" +
                            "- **Spring Security + JWT** - 安全认证\n" +
                            "- **Spring Data JPA** - 数据持久化\n" +
                            "- **H2/MySQL** - 数据库支持\n\n" +
                            "### 前端技术\n\n" +
                            "- **React 18** - 现代化 UI 框架\n" +
                            "- **TypeScript** - 类型安全\n" +
                            "- **Vite** - 快速构建工具\n" +
                            "- **Framer Motion** - 流畅动画\n\n" +
                            "### 设计理念\n\n" +
                            "采用 **极简主义大地色系** 设计，包括：\n\n" +
                            "- 温暖的米白色背景 `#F8F8F8`\n" +
                            "- 炭灰色文字 `#2D2D2D`\n" +
                            "- 铁锈色和橄榄绿强调色\n" +
                            "- 衬线字体标题 + 无衬线正文\n" +
                            "- 复古优雅的视觉效果\n\n" +
                            "## 功能特性\n\n" +
                            "✅ 文章发布与管理  \n" +
                            "✅ 分类和标签  \n" +
                            "✅ Markdown 支持  \n" +
                            "✅ 代码语法高亮  \n" +
                            "✅ 用户认证  \n" +
                            "✅ 评论系统  \n" +
                            "✅ 响应式设计  \n\n" +
                            "```java\n" +
                            "// 示例代码\n" +
                            "@RestController\n" +
                            "@RequestMapping(\"/api/articles\")\n" +
                            "public class ArticleController {\n" +
                            "    @GetMapping\n" +
                            "    public Page<Article> getArticles(@PageableDefault Pageable pageable) {\n" +
                            "        return articleService.findAll(pageable);\n" +
                            "    }\n" +
                            "}\n" +
                            "```\n\n" +
                            "## 开始探索\n\n" +
                            "浏览导航栏可以查看：\n\n" +
                            "- **首页** - 最新文章和精选内容\n" +
                            "- **归档** - 按时间线浏览所有文章\n" +
                            "- **关于** - 了解更多关于我的信息\n\n" +
                            "感谢访问！")
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
                    .title("极简主义设计的魅力")
                    .summary("探讨极简主义设计理念在现代 Web 应用中的应用，以及如何通过色彩和排版营造舒适的阅读体验。")
                    .content("# 极简主义设计的魅力\n\n" +
                            "## 为什么选择极简设计？\n\n" +
                            "在信息爆炸的时代，**极简主义设计** 帮助我们：\n\n" +
                            "1. **减少认知负担** - 让用户专注于内容\n" +
                            "2. **提升可读性** - 清晰的层次结构\n" +
                            "3. **营造氛围** - 温暖、舒适的视觉体验\n\n" +
                            "## 大地色系的选择\n\n" +
                            "这个博客采用了精心挑选的大地色系：\n\n" +
                            "### 背景色\n" +
                            "- **米白色** `#F8F8F8` - 温暖柔和，不刺眼\n" +
                            "- 相比纯白色 `#FFFFFF`，更有质感\n\n" +
                            "### 文字色\n" +
                            "- **炭灰色** `#2D2D2D` - 比纯黑柔和\n" +
                            "- 对比度适中，长时间阅读不疲劳\n\n" +
                            "### 强调色\n" +
                            "- **铁锈色** `#8B7355` - 温暖的棕色调\n" +
                            "- **橄榄绿** `#5C6B4A` - 自然的绿色调\n\n" +
                            "## 字体的艺术\n\n" +
                            "### 标题字体\n" +
                            "使用 **Playfair Display** 衬线字体：\n" +
                            "- 优雅、古典\n" +
                            "- 适合标题和重点内容\n\n" +
                            "### 正文字体\n" +
                            "使用 **Inter** 无衬线字体：\n" +
                            "- 现代、清晰\n" +
                            "- 适合长文本阅读\n\n" +
                            "### 代码字体\n" +
                            "使用 **JetBrains Mono** 等宽字体：\n" +
                            "```css\n" +
                            "code {\n" +
                            "  font-family: 'JetBrains Mono', 'Fira Code', monospace;\n" +
                            "  background: rgba(139, 115, 85, 0.1);\n" +
                            "  padding: 0.2em 0.4em;\n" +
                            "  border-radius: 3px;\n" +
                            "}\n" +
                            "```\n\n" +
                            "## 细节的力量\n\n" +
                            "好的设计在于细节：\n\n" +
                            "- 微妙的阴影效果\n" +
                            "- 流畅的过渡动画\n" +
                            "- 恰到好处的间距\n" +
                            "- 统一的圆角设计\n\n" +
                            "这些看似微小的元素，共同营造出和谐的视觉体验。\n\n" +
                            "## 响应式设计\n\n" +
                            "在不同设备上都保持良好的阅读体验：\n\n" +
                            "```css\n" +
                            "@media (max-width: 768px) {\n" +
                            "  .container {\n" +
                            "    padding: 1rem;\n" +
                            "  }\n" +
                            "  \n" +
                            "  h1 {\n" +
                            "    font-size: 2rem;\n" +
                            "  }\n" +
                            "}\n" +
                            "```\n\n" +
                            "## 结语\n\n" +
                            "极简不是简陋，而是删繁就简后的精致。\n\n" +
                            "每一个设计决策都经过深思熟虑，目标只有一个：\n\n" +
                            "> 让读者享受纯粹的阅读体验。")
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
                    .title("Spring Boot 3.x 新特性解析")
                    .summary("深入了解 Spring Boot 3.0 带来的重大变化，包括 Java 17 基线、GraalVM 原生镜像支持等。")
                    .content("# Spring Boot 3.x 新特性解析\n\n" +
                            "Spring Boot 3.0 是一个重大版本更新，带来了许多激动人心的新特性。\n\n" +
                            "## 主要变化\n\n" +
                            "### 1. Java 17 基线\n\n" +
                            "Spring Boot 3.0 要求 **Java 17** 作为最低版本：\n\n" +
                            "```java\n" +
                            "// 使用 Java 17 的新特性\n" +
                            "public record User(String name, String email) {}\n\n" +
                            "// Switch 表达式\n" +
                            "String result = switch (status) {\n" +
                            "    case PENDING -> \"等待中\";\n" +
                            "    case APPROVED -> \"已批准\";\n" +
                            "    case REJECTED -> \"已拒绝\";\n" +
                            "};\n" +
                            "```\n\n" +
                            "### 2. Jakarta EE 9+\n\n" +
                            "从 `javax.*` 迁移到 `jakarta.*`：\n\n" +
                            "```java\n" +
                            "// 旧版本\n" +
                            "import javax.persistence.Entity;\n\n" +
                            "// 新版本\n" +
                            "import jakarta.persistence.Entity;\n" +
                            "```\n\n" +
                            "### 3. Native 支持\n\n" +
                            "使用 GraalVM 编译原生镜像：\n\n" +
                            "```bash\n" +
                            "# 构建原生镜像\n" +
                            "mvn -Pnative spring-boot:build-image\n\n" +
                            "# 启动时间大幅减少\n" +
                            "# 内存占用显著降低\n" +
                            "```\n\n" +
                            "### 4. 改进的可观测性\n\n" +
                            "```java\n" +
                            "@RestController\n" +
                            "public class UserController {\n" +
                            "    \n" +
                            "    @GetMapping(\"/users/{id}\")\n" +
                            "    @Observed(name = \"user.find\")\n" +
                            "    public User getUser(@PathVariable Long id) {\n" +
                            "        return userService.findById(id);\n" +
                            "    }\n" +
                            "}\n" +
                            "```\n\n" +
                            "## 迁移建议\n\n" +
                            "### 依赖更新\n\n" +
                            "```xml\n" +
                            "<parent>\n" +
                            "    <groupId>org.springframework.boot</groupId>\n" +
                            "    <artifactId>spring-boot-starter-parent</artifactId>\n" +
                            "    <version>3.2.2</version>\n" +
                            "</parent>\n" +
                            "```\n\n" +
                            "### 包名替换\n\n" +
                            "使用 IDE 批量替换：\n" +
                            "- `javax.` → `jakarta.`\n\n" +
                            "### 测试\n\n" +
                            "充分测试所有功能，特别是：\n" +
                            "- 数据库操作\n" +
                            "- 安全认证\n" +
                            "- HTTP 请求处理\n\n" +
                            "## 总结\n\n" +
                            "Spring Boot 3.0 是一个面向未来的版本：\n\n" +
                            "✅ 更现代的 Java 版本  \n" +
                            "✅ 更好的性能  \n" +
                            "✅ 云原生支持  \n" +
                            "✅ 改进的开发体验  \n\n" +
                            "是时候升级了！")
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
