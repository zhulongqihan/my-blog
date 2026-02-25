package com.myblog.task;

import com.myblog.service.ArticleService;
import com.myblog.service.CategoryService;
import com.myblog.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 缓存预热任务
 * 
 * Spring Boot 启动完成后自动执行，提前加载热点数据到缓存
 * 
 * 设计思路：
 *   - 分类和标签是每个页面都要加载的数据 → 必须预热
 *   - 精选文章和热门文章是首页数据 → 建议预热
 *   - 具体文章按需加载（访问时缓存），不预热
 * 
 * 面试考点：
 *   - 缓存预热避免冷启动时大量请求穿透到DB
 *   - ApplicationRunner 在所有Bean初始化完成后执行
 *   - 调用 @Cacheable 方法 → 自动回填缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupTask implements ApplicationRunner {

    private final CategoryService categoryService;
    private final TagService tagService;
    private final ArticleService articleService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[CacheWarmup] 开始缓存预热...");
        
        try {
            // 预热分类列表
            categoryService.findAll();
            log.info("[CacheWarmup] ✓ 分类列表已预热");
            
            // 预热标签列表
            tagService.findAll();
            log.info("[CacheWarmup] ✓ 标签列表已预热");
            
            // 预热精选文章
            articleService.getFeaturedArticles();
            log.info("[CacheWarmup] ✓ 精选文章已预热");
            
            // 预热热门文章 Top5
            articleService.getPopularArticles(5);
            log.info("[CacheWarmup] ✓ 热门文章已预热");
            
            log.info("[CacheWarmup] 缓存预热完成！");
        } catch (Exception e) {
            log.warn("[CacheWarmup] 缓存预热失败（不影响正常使用）: {}", e.getMessage());
        }
    }
}
