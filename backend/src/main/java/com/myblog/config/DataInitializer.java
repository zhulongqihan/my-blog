package com.myblog.config;

import com.myblog.entity.Article;
import com.myblog.entity.Category;
import com.myblog.entity.Tag;
import com.myblog.entity.User;
import com.myblog.repository.ArticleRepository;
import com.myblog.repository.CategoryRepository;
import com.myblog.repository.CommentRepository;
import com.myblog.repository.TagRepository;
import com.myblog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${blog.demo.reset-content:false}")
    private boolean resetDemoContent;

    @Value("${blog.admin.initial-password:}")
    private String initialAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        ensureAdminUser();
        ensureDefaultCategories();
        ensureDefaultTags();

        if (resetDemoContent && articleRepository.count() > 0) {
            resetDemoArticles();
        }

        if (articleRepository.count() == 0) {
            seedArticles();
        }
    }

    private void ensureAdminUser() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        String password = initialAdminPassword == null ? "" : initialAdminPassword.trim();
        if (password.isEmpty()) {
            throw new IllegalStateException(
                    "管理员账号尚未初始化，请通过 BLOG_ADMIN_INITIAL_PASSWORD 提供初始密码"
            );
        }

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode(password))
                .email("admin@myblog.com")
                .nickname("博主")
                .bio("记录技术、写作与生活观察的长期主义实践者")
                .role(User.Role.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("✅ 管理员账号创建成功 - 用户名: admin");
    }

    private void ensureDefaultCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        Arrays.asList(
                Category.builder().name("技术").description("系统设计、后端工程与工具实践").icon("💻").sortOrder(1).build(),
                Category.builder().name("生活").description("关于节奏、观察和阶段感受").icon("🌿").sortOrder(2).build(),
                Category.builder().name("随笔").description("带一点个人语气的长文记录").icon("✏️").sortOrder(3).build(),
                Category.builder().name("教程").description("适合复盘和查阅的技术文章").icon("📚").sortOrder(4).build()
        ).forEach(categoryRepository::save);
        log.info("✅ 默认分类创建成功");
    }

    private void ensureDefaultTags() {
        if (tagRepository.count() > 0) {
            return;
        }

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

    private void resetDemoArticles() {
        List<Article> existingArticles = articleRepository.findAll();
        if (!existingArticles.isEmpty()) {
            existingArticles.forEach(article -> article.getTags().clear());
            articleRepository.saveAll(existingArticles);
            articleRepository.flush();
        }

        commentRepository.deleteAllInBatch();
        articleRepository.deleteAllInBatch();
        log.info("♻️ 已按配置清空示例文章与评论数据，将重新写入新的时间线内容");
    }

    private void seedArticles() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            log.warn("⚠️ 未找到管理员账号，跳过示例文章写入");
            return;
        }

        Category techCategory = categoryRepository.findByName("技术").orElse(null);
        Category lifeCategory = categoryRepository.findByName("生活").orElse(null);
        Category essayCategory = categoryRepository.findByName("随笔").orElse(lifeCategory);
        Category tutorialCategory = categoryRepository.findByName("教程").orElse(techCategory);

        Tag javaTag = tagRepository.findByName("Java").orElse(null);
        Tag springTag = tagRepository.findByName("Spring Boot").orElse(null);
        Tag reactTag = tagRepository.findByName("React").orElse(null);
        Tag mysqlTag = tagRepository.findByName("MySQL").orElse(null);
        Tag dockerTag = tagRepository.findByName("Docker").orElse(null);
        Tag frontendTag = tagRepository.findByName("前端").orElse(null);
        Tag backendTag = tagRepository.findByName("后端").orElse(null);

        List<Article> articles = new ArrayList<>(List.of(
                article(admin, essayCategory, true, 326, publishAt(2025, 12, 1, 20, 15), "博客系统技术架构全解析", "把一套个人博客从页面、接口、部署、缓存和通知链路完整做通之后，我终于能比较平静地回头讲一讲：哪些设计是必要的，哪些其实只是当时的执念。", cover("studio-grid"), """
                        # 博客系统技术架构全解析

                        这套博客最初只是一个很朴素的想法：我想有一个地方，能把平时写的技术文章、生活记录和做项目时的判断都放进去，而且它不只是一个静态页面集合，而是一套真的能长期维护的系统。

                        真正开始做以后，我才意识到，个人项目最难的从来不是把首页搭起来，而是当你一点点往里加功能时，系统会不会很快失去秩序。文章、评论、标签、后台、缓存、通知、部署，这些东西在需求清单上看起来像平铺直叙的条目，落到工程里却会互相牵动。

                        我后来给自己定了一个很简单的原则：一切围绕“长期能维护”来做。不是为了把技术栈堆满，而是为了让这套系统在三个月后、半年后，回头看时仍然能顺着脉络读懂。

                        ## 为什么是前后端分离，而不是单体页面

                        这不是为了追时髦。更现实的原因是，我希望前台和后台的体验可以明显不同。前台更强调阅读、节奏和视觉氛围，后台则更看重信息密度、反馈速度和管理效率。把它们放在同一个模板体系里，最终往往会谁都不够像样。

                        所以前台我用了 React 负责交互和页面组织，后台管理则单独用 Vue 3 和 Element Plus。这个决定让代码量变多了，但换来的好处非常实际：我不需要为了后台表格去牺牲前台的设计，也不需要把前台的动效逻辑强行塞进后台。

                        ## 后端真正承担的，是“把内容变成系统”

                        后端层面我最看重的不是接口数量，而是边界感。文章、分类、标签、评论、通知、日志，每个模块都要有自己的职责，不然做着做着就会变成一个到处粘逻辑的控制器工程。

                        在这套项目里，Spring Boot 帮我解决了大量工程基础问题，但真正决定项目质量的，其实是那些看起来不那么显眼的安排：DTO 怎么收口、缓存放在服务层还是控制器层、哪些操作需要异步、哪些事件应该写日志、哪些功能必须允许游客访问。

                        这些问题没有哪一个单独拿出来很“惊艳”，但就是这些小决定，最后决定了一套系统能不能稳稳站住。

                        ## 前台体验为什么花了这么多精力

                        我不想把博客做成一个只有功能清单的网站。文章本身是内容，但阅读体验同样是内容的一部分。读者能不能舒服地往下读，能不能在某个细节处感受到作者是认真打磨过界面的，这些事情会悄悄影响一整篇文章的停留时间。

                        所以前台我花了不少时间做一些看起来不那么“必要”的东西：阅读进度条、文章卡片的轻微倾斜、书签、阅读统计、命令面板、分享卡片导出、小羊桌宠、创意工坊。它们里有些是为了好玩，有些是为了体验完整，但共同点是都在试图把“技术博客”做得更有人味一点。

                        ## 这套架构里最值得保留的，不是某个组件，而是方法

                        如果现在让我重新从头做一遍，我会保留的不是某一段代码，而是这一套推进方法：先把主链路跑通，再慢慢给它加上更好的结构、更合理的拆分和更可解释的边界。项目是这样长出来的，而不是一开始就被设计得很完美。

                        这也是为什么我现在愿意把它写成一篇长文。因为系统真正留下来的，不只是运行结果，还有做这些选择时形成的判断方式。对我来说，这比“做了多少功能”更重要。
                        """, tagsOf(javaTag, reactTag, dockerTag, backendTag)),
                article(admin, lifeCategory, false, 214, publishAt(2025, 9, 22, 21, 5), "极简主义设计的魅力", "极简不等于清空一切，它更像是一种克制的取舍：知道什么该出现，什么不该抢镜。做完这个博客之后，我对这件事有了更具体的体会。", cover("warm-journal"), """
                        # 极简主义设计的魅力

                        很多人第一次聊极简设计，通常会把它理解成“元素少一点”“留白多一点”或者“配色淡一点”。这些都没错，但都还停留在表面。真正让我越来越喜欢极简风格的，是它背后那种很克制的判断：不是能放什么，而是应该留下什么。

                        做这个博客的时候，我一开始其实也走过弯路。总觉得既然是个人项目，就该把喜欢的动效、喜欢的颜色、喜欢的装饰都放进去。结果页面一多，视觉上的热闹感很快就变成了噪音。并不是内容真的更丰富了，而是每个模块都在争夺注意力，最后谁都没有得到完整表达。

                        后来我慢慢学会往回收。标题不再追求夸张，色彩减少到几个稳定的主色，卡片阴影和动画幅度都压下来，页面里的留白开始承担“让读者喘口气”的作用。也就是从那个时候开始，我第一次真正理解：极简并不是删掉细节，而是把细节放在更合适的位置。

                        ## 真正的重点，是让信息有呼吸感

                        页面里最容易被忽视的，不是颜色，也不是字体，而是信息之间的距离。段落和段落之间、标题和正文之间、图片区和文字区之间，只要这些节奏处理不好，再漂亮的视觉元素也会显得拥挤。

                        我后来给自己的一个提醒是：不要把页面当成容器，要把它当成阅读节奏本身的一部分。用户往下滚动的时候，其实是在一段一段接受信息。如果每一段都挤在一起，就算内容不复杂，也会让人觉得累。

                        ## 极简不意味着冷淡

                        这可能是最容易被误解的一点。很多极简页面看起来干净，但也容易显得没有温度。尤其做个人博客，如果画面只剩下秩序，没有一点情绪，最后会很像模板，而不是一个人在真实地表达。

                        所以我更喜欢的是“有气味的极简”。它可以克制，但不冷；可以安静，但不板着脸。比如大地色系就是我很喜欢的一种选择，它不像纯黑白那样强势，却有一种适合阅读、适合停留的温度。

                        ## 对独立开发者来说，极简是一种保护

                        项目越往后做，你越会发现精力是稀缺的。功能要做，接口要调，部署要管，细节要补，如果视觉层面也一直堆新东西，很容易把自己拖进“永远做不完”的状态。

                        极简有时候像是一种自我保护。它逼着你先把主线抓住，把最重要的结构搭好，然后只在真正值得出现的地方增加装饰。这样做的结果，不只是页面更稳，也会让你自己在维护时更轻松。

                        ## 我现在理解的极简，是一种长期主义

                        好看的页面很多，但真正能在不断迭代后仍然保持完整气质的页面并不多。极简之所以让我着迷，不是因为它总能让画面看起来“高级”，而是因为它天然适合长期维护。你可以往里加内容、加功能、加故事，但只要核心节奏不乱，整个系统就不会很快失控。
                        """, tagsOf(frontendTag, reactTag)),
                article(admin, tutorialCategory, false, 268, publishAt(2025, 10, 8, 9, 40), "Spring Boot 3.x 新特性解析", "升级到 Spring Boot 3.x 之后，我真正感受到的变化不只是版本号，而是整套基础设施在 Jakarta、AOT 和配置方式上都明显往前走了一步。", cover("spring-layers"), """
                        # Spring Boot 3.x 新特性解析

                        每次谈框架升级，大家最先问的往往都是“值不值得”。这个问题其实很正常，因为升级从来不只是把版本号改一下，它往往会牵出依赖调整、配置迁移、接口兼容甚至整套部署环境的重新检查。

                        我把博客后端迁到 Spring Boot 3.x 的时候，也不是抱着“越新越好”的心态，而是想看看它到底在真实项目里能不能带来更清晰的工程体验。

                        ## 第一感受：Jakarta 的确是一次断层式变化

                        最明显的变化当然是包名迁移。很多以前熟悉的 javax 相关内容都换到了 jakarta 命名空间下。表面看只是 import 改一下，真正麻烦的地方在于你会连带检查一整串依赖是不是都已经跟上版本。

                        这件事一开始很烦，但真正改完之后，我反而觉得它有一点好处：它逼着你认真清理那些已经过期、却一直留在项目里的东西。很多过去“先跑起来再说”的依赖组合，到这个节点就必须做一次彻底梳理。

                        ## 第二感受：配置方式比以前更规整了

                        Spring Security 在 6.x 之后取消了过去很多老写法，这一开始会让人有点不适应。以前习惯继承配置类的方式突然不能用了，换成了更加组件化的配置写法。

                        但真正用一阵子以后，你会发现这种方式其实更适合中大型项目。每个 Bean 的职责更清晰，安全链路也不再依赖那种藏在继承结构里的魔法。尤其是当你要把 JWT 认证、黑名单过滤、游客接口和后台权限拆开时，新方式明显更容易看懂。

                        ## 第三感受：对原生编译和启动速度的重视更强了

                        我这次虽然没有把博客直接打成 GraalVM 原生镜像，但 Spring Boot 3.x 对这件事的支持已经明显是往主线能力里收了。对很多团队来说，这意味着未来如果真的要追求更快冷启动或者更轻量的部署形态，框架已经把路提前铺出来了。

                        即使暂时不用原生编译，这种方向本身也很重要，因为它说明整个生态开始更认真地面对云环境里的启动成本和资源成本问题了。
                        """, tagsOf(javaTag, springTag, backendTag)),
                article(admin, essayCategory, false, 189, publishAt(2025, 10, 26, 18, 25), "廿日乡居惊岁短，一程风露赴金陵", "从乡下住下来的二十天，到重新动身去金陵，那种时间被忽然收紧的感觉，很难用三两句短话讲完，所以我想把它写成一篇慢一点的长文。", null, """
                        # 廿日乡居惊岁短，一程风露赴金陵

                        在乡下住到第二十天的时候，我忽然觉得日子开始收口了。

                        前些天还觉得时间很宽，早晨可以慢一点起，午后可以随手拿本书坐在窗边，傍晚看天色一点点暗下来，也不急着做什么。可一旦心里知道自己快要动身，所有原本松弛的东西都会变得有边界。哪怕同样是院子里的风、饭后的路、夜里亮着的一盏灯，也会因为“快结束了”而变得格外清楚。

                        我一直觉得，人在一个地方待久了，和它建立联系的方式不是靠景点，而是靠那些看起来完全不值得记下的小事。比如门前那条土路什么时候会起风，厨房窗台上下午几点会照进一块正方形的光，入夜以后远处偶尔传来的狗叫声，会不会打断正在发散的念头。

                        这些细节平时并不显眼，可一旦临近离开，它们就像忽然被擦亮了一样。你会意识到，原来自己已经悄悄适应了这里的秩序，也把一部分心情留在了这种安静的重复里。

                        收拾东西那天，天气不算坏，风却有点硬。院门打开又关上，声音比平时更响。我拎起包时甚至有一点荒唐的念头，觉得自己像是在和一段并不喧闹的生活告别。它没有发生什么轰轰烈烈的故事，也没有给人什么戏剧性的转折，只是用一种很慢的方式，把人往里放了一阵子。

                        去金陵的路上，风露确实重。车窗外的景色不停往后退，树、田、桥、站牌都像被压成了匆匆掠过的影子。那种移动感和前二十天形成了很鲜明的对照：前面是慢慢沉下去的日常，后面是重新被时刻表、路线和目的地牵着往前走。

                        但我也知道，离开不是把前面的生活剪掉，而是把它带进新的阶段里。很多时候，人并不是在某个地方“完成了什么”才离开，而是因为正好到了继续上路的时候。那些留下来的清晨、饭香、风声、灯影，不会真的被丢下，它们会在以后的某个瞬间又突然回到你身边。
                        """, tagsOf(frontendTag)),
                article(admin, lifeCategory, false, 247, publishAt(2025, 11, 3, 22, 10), "研究生这一年，我怎么平衡课程、项目和持续输出", "真正有用的平衡，不是把每天安排得满满当当，而是学会让不同类型的事情互相供给，而不是互相打架。", cover("warm-journal"), """
                        # 研究生这一年，我怎么平衡课程、项目和持续输出

                        研究生这一年里，我最常听到的问题之一就是：你到底是怎么把课程、项目、面试准备、博客更新这些事情同时推进下去的？

                        这个问题每次都让我有点尴尬，因为真实答案远没有听起来那么整齐。我并没有一套近乎完美的时间管理系统，也不是每天都能保持高效率。更多时候，我只是不断试错，最后慢慢摸出了一种自己能长期承受的节奏。

                        ## 先承认一件事：人不可能每天都全能

                        我以前最大的误区，就是总觉得一天里应该把所有重要的事情都照顾到。上午上课，下午做项目，晚上写文章，再抽空运动和复盘，好像只有这样才算“没有浪费时间”。

                        但现实很快证明，这种安排只会把人拉得很散。你每件事都碰了一点，却没有哪件真正推进。到最后最强烈的感受不是充实，而是心里一直悬着一口气。

                        后来我改了思路，不再要求每天什么都推进，而是接受不同的日子有不同的主任务。某些天以输入为主，适合上课、看资料、补知识；某些天以输出为主，适合写代码、写文档、写文章；还有一些天只是用来把前两天散落的东西收起来。

                        ## 我给输出留了一个很低的起步单位

                        很多拖延，其实不是因为懒，而是因为脑子里把一件事想得太完整了。比如写文章，总觉得必须有一整个晚上才能开头；做项目，总觉得必须有完整思路才值得打开编辑器。

                        但真正把事情慢慢做起来以后，我发现更有用的办法是把起步单位压得非常小。写不完一篇，就先写一个段落；做不完整个模块，就先把一个接口打通；整理不出完整思路，就先把关键判断记下来。

                        一旦开始的成本变小，很多事情就不会卡在“我应该找个更好的时机再做”这一步。
                        """, tagsOf(frontendTag, reactTag)),
                article(admin, essayCategory, false, 193, publishAt(2025, 11, 18, 20, 30), "AI 时代的普通人写作：为什么我还是坚持把想法写成长文", "在可以快速生成文本的时代，我反而更愿意花时间写长文，因为长文会逼着我对自己的判断负责。", cover("warm-journal"), """
                        # AI 时代的普通人写作：为什么我还是坚持把想法写成长文

                        这两年写作变得前所未有地容易。你只要给出一个主题、几个关键词，模型就可以在很短时间里吐出一篇结构完整、语言顺滑、看起来像模像样的文章。很多时候，真正困难的部分甚至不再是“写不写得出来”，而是“你到底想说什么”。

                        也正因为这样，我反而越来越愿意把一些想法写成长文。不是为了显得郑重，也不是为了和短内容对着干，而是因为我发现，只有当一件事被写得足够长、足够具体时，我才知道自己是否真的想明白了它。

                        ## 长文逼人面对自己的含糊

                        很多念头在脑子里都是顺的。你觉得自己已经知道观点在哪里，也知道大概要往哪个方向说。可一旦真的把它展开，问题就开始冒出来：这个判断的依据是什么？这个结论是不是跳得太快了？这段话到底是经验，还是情绪？

                        长文最残酷也最有价值的地方，就在这里。它会把那些平时可以靠语气混过去的模糊地带一点点逼出来。你没法一直用漂亮句子掩饰思路上的缺口，也没法靠几个标签把复杂问题省略掉。

                        ## 我不是排斥 AI，我只是更清楚它适合放在哪一层

                        现在我也会用 AI 帮我整理资料、归纳要点、提示结构，甚至在写到一半时拿来检查自己有没有遗漏明显的逻辑断层。这些帮助都很实际，也确实提高了效率。

                        但我始终不太愿意把“最终判断”这件事交出去。尤其是那些和项目取舍、成长体验、阶段观察相关的内容，如果最后只是顺着一套非常流畅的模板写出来，看上去可能很完整，实际却没有真正说出自己在意的东西。
                        """, tagsOf(frontendTag)),
                article(admin, techCategory, false, 291, publishAt(2025, 12, 20, 17, 55), "Redis 在高并发场景下的实战方案", "Redis 最容易被低估的地方，不是缓存读写速度，而是它在分布式锁、削峰、限流和排行榜这些场景里提供的工程杠杆。", cover("redis-map"), """
                        # Redis 在高并发场景下的实战方案

                        一提到 Redis，很多人第一反应还是“缓存”。这当然没错，但如果你只把它当成一个更快的 Key-Value 存储，实际上会错过它在高并发场景里真正有意思的那部分能力。它的价值并不只是把数据库前面加一道挡板，而是让很多本来会让系统变得僵硬的问题，有了更轻巧的处理方式。

                        我在博客项目里真正开始依赖 Redis，是从点赞、浏览量、限流和热门榜这些需求同时出现以后。它们有一个共同点：对一致性有要求，但又没必要把每次变化都立刻硬写进数据库。只要这条边界判断清楚，Redis 的优势就会非常明显。

                        ## 点赞这种小功能，最容易暴露并发问题

                        单看页面，一个点赞按钮没什么特别。但只要用户在短时间里连续点击，或者同一个请求被重试，后端就必须认真面对“到底应该加一次还是多次”这个问题。数据库当然也能处理，但如果每次都靠数据库去做状态判断、写入和回滚，链路会很重。

                        我最后选择的是 Redis + Lua 的组合。原因很简单：它能把“是否已点赞”“执行增减”“返回最新状态”这几步放在同一个原子过程里完成。这样做的好处不是写法更炫，而是边界更干净。业务上你只关心结果，底层的一致性由 Redis 这一侧兜住。
                        """, tagsOf(javaTag, mysqlTag, backendTag)),
                article(admin, techCategory, false, 279, publishAt(2026, 1, 7, 19, 20), "低配云服务器上部署 AI 应用，我做了哪些取舍", "真正让人变成熟的不是配置拉满，而是当机器只有那么一点资源时，你还能不能分清哪些东西必须在线，哪些东西可以慢一点。", cover("low-cost-cloud"), """
                        # 低配云服务器上部署 AI 应用，我做了哪些取舍

                        很多技术方案写出来都很漂亮，真正一落到低配服务器上，就会立刻暴露出它到底是面向展示、面向生产，还是只是面向想象。尤其是这段时间把博客和独立 AI Agent 项目都部署到线上之后，我对“取舍”这件事有了非常具体的体会。

                        一台配置紧巴巴的机器，其实会逼着你把问题看得更清楚。什么必须常驻？什么可以按需启？什么该独立出去？什么不值得为了看起来完整而硬塞进来？这些问题在高配环境里容易被掩盖，在低配环境里却会第一时间跳出来。

                        ## 我先拆的是边界，而不是优化参数

                        一开始最容易犯的错误，是把所有服务都往同一个组合里堆，以为只要部署方式统一，系统就算更“完整”。但真正做下来以后你会发现，资源紧张的时候，统一并不一定比分离更好。

                        所以我没有一开始就把博客和 AI Agent 完全绑死在一起，而是让博客继续保持一套稳定的 Compose 编排，把 AI Agent 先以更独立的形式运行。这样做的代价是入口没有那么统一，换来的好处却非常明确：任何一侧出问题，都不会立刻把另一侧一起带下去。
                        """, tagsOf(dockerTag, backendTag)),
                article(admin, techCategory, false, 261, publishAt(2026, 1, 25, 21, 45), "AI 爆火之后，普通开发者应该怎样建立自己的工具箱", "与其追每一个新名词，不如先把那些你会反复用到的能力层稳稳搭起来。工具箱的意义，不是装得多，而是拿出来就能用。", cover("agent-compass"), """
                        # AI 爆火之后，普通开发者应该怎样建立自己的工具箱

                        过去一年，关于 AI 的新词几乎是按周更新的。Agent、工作流、MCP、知识库、向量检索、模型路由，每次打开信息流都像在看一场不断换题的考试。对普通开发者来说，最容易出现的结果不是落后，而是被这些名词拖进持续性的焦虑里。

                        我自己后来慢慢想明白一件事：普通开发者真正需要建立的，不是“对所有热词都能聊两句”的表面熟悉，而是一套自己真的会反复使用、能讲清楚、能在项目里落下来的工具箱。

                        ## 工具箱不是收藏夹，它是分层能力

                        我现在更愿意把它拆成几层来理解。最底下是模型调用和基本接口层，你至少要知道一次请求是怎么发出去的，流式输出怎么接，错误要怎么处理。再往上一层是产品交互层，涉及结果怎么展示、怎么续问、怎么复制、怎么避免用户等得没耐心。

                        再上去才是工程化层和场景层。比如 API Key 放在哪里、请求失败怎么降级、日志怎么记录、提示词怎么和业务语境结合、什么样的需求适合做成一个真正的工具入口。这些能力不会让你在第一眼看上去特别炫，但它们决定了一个 AI 功能是不是能真正留下来。
                        """, tagsOf(reactTag, backendTag, dockerTag)),
                article(admin, techCategory, true, 338, publishAt(2026, 2, 6, 20, 5), "我为什么做了一个航旅稳定性 AI Agent 项目", "如果只是把大模型接进页面里，我很难说服自己那是一个真正成立的作品。这个项目更像是我把博客里那些零散判断收束成系统之后的结果。", cover("agent-compass"), """
                        # 我为什么做了一个航旅稳定性 AI Agent 项目

                        过去半年里，我越来越清楚一件事：如果简历里只是写“做过 AI 功能”“接过大模型接口”，其实并不能说明太多问题。大家都会接接口，大家都能做一个聊天框，真正拉开差距的，是你有没有把这件事推进成一个边界清楚、可以演示、也能解释得明白的系统。

                        于是我做了航旅稳定性 AI Agent 项目。它不是一个泛用聊天页，也不是把几个提示词拼在一起的概念演示，而是明确围绕航旅预订链路里那些会让人头疼的问题来展开：搜索异常、下单失败、出票延迟、供应商波动、巡检记录、故障复盘。

                        ## 我为什么选了这个题，而不是更“通用”的方向

                        原因很简单。越通用的 AI 产品，越容易做成一句话能说清楚、真正做出来却没有边界的东西。你看起来什么都能答一点，但真正进入具体问题时，系统往往又缺少稳定的任务结构。

                        我更希望这个项目能体现的是：面对一个相对专业、相对具体的业务主题，我怎样把任务拆开，把信息组织起来，再把结果变成可以继续追问和复盘的产物。航旅稳定性正好满足这个条件。它足够专业，能体现建模；又足够真实，不会沦为泛泛而谈。
                        """, tagsOf(javaTag, backendTag, dockerTag)),
                article(admin, techCategory, true, 347, publishAt(2026, 2, 24, 19, 50), "从提示词到工作流：我对这波 AI Agent 热潮的几个判断", "Agent 这个词真正有意思的地方，不在于它听起来多新，而在于它迫使开发者同时面对任务拆解、状态管理、流程编排和结果可解释性。", cover("agent-compass"), """
                        # 从提示词到工作流：我对这波 AI Agent 热潮的几个判断

                        如果把这一轮 AI 热潮的变化概括得更具体一点，我觉得最核心的转折并不是模型突然会做更多事了，而是大家开始不满足于“问一句、回一句”的交互方式，转而期待系统自己完成更长的任务链。这种期待，就是 Agent 重新爆火的土壤。

                        但我越来越觉得，围绕 Agent 的很多讨论其实还停留在概念层，真正值得认真看的，是它把哪些工程问题重新推到了台前。

                        ## 提示词当然重要，但它已经不是全部

                        很多人第一次接触 Agent，会下意识把它理解成“更复杂的提示词”。这并不算错，但只说到这里，几乎等于什么都没说。因为一旦任务开始跨步骤、跨状态、跨上下文，系统面对的就不再只是怎么向模型提问，而是怎么组织流程、如何保存中间结果、失败后是否重试、何时应该把控制权还给用户。

                        也就是说，从这个阶段开始，你真正做的已经不是 prompt 调优，而是工作流设计。
                        """, tagsOf(javaTag, backendTag, dockerTag)),
                article(admin, essayCategory, false, 201, publishAt(2026, 3, 4, 21, 0), "从博客到作品集：我怎样把内容线做成可讲述的项目线", "作品集不是把项目链接堆在一起，而是要让别人看见：这些文章、功能、系统和取舍之间，其实连着同一条内容线。", cover("studio-grid"), """
                        # 从博客到作品集：我怎样把内容线做成可讲述的项目线

                        我以前做项目和写博客时，常常把它们分得很开。文章是文章，系统是系统，作品集是最后整理材料时才临时搭出来的一块拼图。这样做当然也能把东西交出去，但回头看，总觉得少了一点更连贯的叙述感。

                        后来我慢慢意识到，真正有辨识度的作品集不是一堆页面链接，而是一条别人顺着读下去就能理解的内容线。它要让人看见：你为什么会做这个项目，项目里解决了哪些问题，文章里怎样展开这些判断，系统上线后又是如何把这些想法真正落地。

                        ## 我开始把“内容”也当成项目的一部分

                        这件事对我影响很大。以前我总觉得项目才是硬成果，文章只是附属说明。现在我反而更愿意把两者放在同一个叙述系统里看。文章负责把思考过程讲透，项目负责提供运行中的证据，文档负责让人快速理解使用方式和设计边界。
                        """, tagsOf(frontendTag, reactTag, backendTag)),
                article(admin, essayCategory, true, 238, publishAt(2025, 9, 6, 8, 30), "欢迎来到我的博客", "这个站点不是为了把所有内容都写得很正式，而是想留下一种长期更新、长期积累、也长期暴露真实取舍的写作方式。", cover("warm-journal"), """
                        # 欢迎来到我的博客

                        如果把这个博客说成一个“个人网站”，当然也没什么问题，但我更愿意把它理解成一个正在缓慢生长的地方。它不只是拿来展示结果，也不只是为了放项目链接，而是想把一些平时很容易散掉的思考、过程和阶段变化真正留下来。

                        我一直觉得，个人表达最难的地方不是开始，而是持续。你可能会在某个阶段很想记录、很想分享、很想把做过的事情讲出来，但这种冲动很容易被日常琐事打断。真正珍贵的，往往不是一次热情很高的爆发，而是你有没有能力在很多平常的日子里，仍然愿意把想法整理成别人也看得懂的东西。

                        这个博客想做的，就是承接这种持续性。这里会有技术文章，也会有项目复盘，会有生活里的观察，也会有一些不那么急着给结论的长文。它们未必都很完整，未必都很“标准”，但我希望它们至少足够诚实。
                        """, tagsOf(frontendTag, reactTag))
        ));

        applyRandomTimeline(articles, publishAt(2025, 9, 1, 8, 0), publishAt(2026, 3, 31, 22, 0));

        articleRepository.saveAll(articles);
        log.info("✅ 示例文章创建成功 - 共 {} 篇，发布时间跨度覆盖 2025/09 到 2026/03", articleRepository.count());
    }

    private Article article(User author, Category category, boolean featured, int viewCount, LocalDateTime publishedAt, String title, String summary, String coverImage, String content, List<Tag> tags) {
        Article article = Article.builder()
                .title(title)
                .summary(summary)
                .content(content)
                .coverImage(coverImage)
                .author(author)
                .category(category)
                .published(true)
                .featured(featured)
                .viewCount(viewCount)
                .likeCount(Math.max(0, viewCount / 18))
                .publishedAt(publishedAt)
                .createdAt(publishedAt)
                .updatedAt(publishedAt)
                .build();
        article.setTags(tags);
        return article;
    }

    private void applyRandomTimeline(List<Article> articles, LocalDateTime start, LocalDateTime end) {
        List<YearMonth> monthRange = new ArrayList<>();
        YearMonth current = YearMonth.from(start);
        YearMonth endMonth = YearMonth.from(end);
        while (!current.isAfter(endMonth)) {
            monthRange.add(current);
            current = current.plusMonths(1);
        }

        List<YearMonth> assignedMonths = new ArrayList<>(monthRange);
        while (assignedMonths.size() < articles.size()) {
            assignedMonths.add(monthRange.get(ThreadLocalRandom.current().nextInt(monthRange.size())));
        }

        List<LocalDateTime> timestamps = new ArrayList<>();
        for (YearMonth month : assignedMonths) {
            LocalDateTime monthStart = month.atDay(1).atTime(8, 0);
            LocalDateTime monthEnd = month.atEndOfMonth().atTime(22, 0);
            LocalDateTime effectiveStart = month.equals(YearMonth.from(start)) ? start : monthStart;
            LocalDateTime effectiveEnd = month.equals(YearMonth.from(end)) ? end : monthEnd;
            timestamps.add(randomDateTimeBetween(effectiveStart, effectiveEnd));
        }

        timestamps.sort(Comparator.reverseOrder());

        for (int index = 0; index < articles.size(); index++) {
            LocalDateTime timestamp = timestamps.get(index);
            Article article = articles.get(index);
            article.setPublishedAt(timestamp);
            article.setCreatedAt(timestamp);
            article.setUpdatedAt(timestamp);
        }
    }

    private LocalDateTime randomDateTimeBetween(LocalDateTime start, LocalDateTime end) {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        long startEpoch = start.atZone(zoneId).toInstant().toEpochMilli();
        long endEpoch = end.atZone(zoneId).toInstant().toEpochMilli();
        long randomEpoch = ThreadLocalRandom.current().nextLong(startEpoch, endEpoch + 1);
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(randomEpoch), zoneId);
    }

    private LocalDateTime publishAt(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    private String cover(String name) {
        return "/article-covers/" + name + ".svg";
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
