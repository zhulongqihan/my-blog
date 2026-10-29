package com.myblog.service.admin;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.dto.admin.DashboardStatsDTO;
import com.myblog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表盘服务
 * 提供后台管理首页的统计数据
 * 
 * 缓存策略：
 *   - 统计数据缓存5分钟（dashboardStats）
 *   - 今日访问量读取Redis日计数，与统计快照一起缓存
 *   - 访问趋势使用Redis记录的日访问量
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取仪表盘统计数据（缓存5分钟）
     */
    @Cacheable(value = "dashboardStats", key = "'overview'")
    public DashboardStatsDTO getStats() {
        log.info("[Cache MISS] 仪表盘统计 - 从数据库加载");
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(today, LocalTime.MAX);

        // 总计统计
        long totalArticles = articleRepository.count();
        long totalComments = commentRepository.count();
        long totalUsers = userRepository.count();
        long totalViews = getTotalViews();

        // 今日统计
        long todayComments = commentRepository.countByCreatedAtBetween(todayStart, todayEnd);
        long todayUsers = userRepository.countByCreatedAtBetween(todayStart, todayEnd);

        // 趋势数据（最近7天）
        List<DashboardStatsDTO.TrendItem> viewTrend = getViewTrend(today, 7);
        List<DashboardStatsDTO.TrendItem> articleTrend = getArticleTrend(today, 7);
        long todayViews = viewTrend.get(viewTrend.size() - 1).getCount();
        long todayArticles = articleTrend.get(articleTrend.size() - 1).getCount();

        // 热门文章
        List<DashboardStatsDTO.PopularArticle> popularArticles = getPopularArticles(5);

        // 最新评论
        List<DashboardStatsDTO.RecentComment> recentComments = getRecentComments(5);

        // 分类统计
        List<DashboardStatsDTO.CategoryStats> categoryStats = getCategoryStats();

        return DashboardStatsDTO.builder()
                .totalViews(totalViews)
                .totalArticles(totalArticles)
                .totalComments(totalComments)
                .totalUsers(totalUsers)
                .todayViews(todayViews)
                .todayArticles(todayArticles)
                .todayComments(todayComments)
                .todayUsers(todayUsers)
                .viewTrend(viewTrend)
                .articleTrend(articleTrend)
                .popularArticles(popularArticles)
                .recentComments(recentComments)
                .categoryStats(categoryStats)
                .build();
    }

    /**
     * 获取总访问量
     */
    private long getTotalViews() {
        Long total = articleRepository.sumViewCount();
        return total != null ? total : 0L;
    }

    /**
     * 获取访问趋势（从Redis日访问量读取，7天数据）
     */
    private List<DashboardStatsDTO.TrendItem> getViewTrend(LocalDate today, int days) {
        List<DashboardStatsDTO.TrendItem> trend = new ArrayList<>();
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MM-dd");
        List<LocalDate> dates = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date);
            keys.add(RedisKeyPrefix.DAILY_VIEW_COUNT + date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        // Pipeline GET instead of MGET: retain WRONGTYPE errors rather than presenting corrupt counters as zero.
        // SessionCallback keeps this template's existing key and JSON value serializers.
        List<Object> values = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                for (String key : keys) operations.opsForValue().get((K) key);
                return null;
            }
        });
        if (values == null || values.size() != days) {
            throw new IllegalStateException("Daily view counts unavailable");
        }
        for (int i = 0; i < dates.size(); i++) {
            Object value = values.get(i);
            long count = value != null ? ((Number) value).longValue() : 0L;
            trend.add(DashboardStatsDTO.TrendItem.builder()
                    .date(dates.get(i).format(displayFormatter))
                    .count(count)
                    .build());
        }
        return trend;
    }

    /**
     * 获取文章创建趋势（保留包含草稿的统计口径）
     */
    private List<DashboardStatsDTO.TrendItem> getArticleTrend(LocalDate today, int days) {
        List<DashboardStatsDTO.TrendItem> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        Map<LocalDate, Long> counts = new HashMap<>();
        articleRepository.countDashboardDays(today.minusDays(days - 1).atStartOfDay(),
                today.plusDays(1).atStartOfDay()).forEach(row -> counts.put(row.getDay(), row.getCount()));

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = counts.getOrDefault(date, 0L);
            trend.add(DashboardStatsDTO.TrendItem.builder()
                    .date(date.format(formatter))
                    .count(count)
                    .build());
        }
        return trend;
    }

    /**
     * 获取热门文章
     */
    private List<DashboardStatsDTO.PopularArticle> getPopularArticles(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        var articles = articleRepository.findDashboardPopular(pageable);
        Map<Long, Long> commentCounts = new HashMap<>();
        if (!articles.isEmpty()) for (Object[] count : commentRepository.countForArticles(
                articles.stream().map(com.myblog.dto.admin.DashboardReadRows.PopularArticle::getId).toList())) {
            commentCounts.put(((Number) count[0]).longValue(), ((Number) count[1]).longValue());
        }

        return articles.stream()
                .map(article -> {
                    // 获取文章评论数
                    long commentCount = commentCounts.getOrDefault(article.getId(), 0L);
                    return DashboardStatsDTO.PopularArticle.builder()
                            .id(article.getId())
                            .title(article.getTitle())
                            .viewCount(article.getViewCount() != null ? article.getViewCount().longValue() : 0L)
                            .commentCount(commentCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取最新评论
     */
    private List<DashboardStatsDTO.RecentComment> getRecentComments(int limit) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        Pageable pageable = PageRequest.of(0, limit);
        var comments = commentRepository.findDashboardRecent(pageable);
        
        return comments.stream()
                .map(comment -> {
                    // 获取评论作者名称（注册用户或游客）
                    String authorName = comment.getUserId() != null
                            ? comment.getNickname()
                            : comment.getGuestName();
                    
                    return DashboardStatsDTO.RecentComment.builder()
                            .id(comment.getId())
                            .content(truncateContent(comment.getContent(), 50))
                            .authorName(authorName != null ? authorName : "匿名")
                            .articleTitle(comment.getArticleTitle())
                            .createdAt(comment.getCreatedAt().format(formatter))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取分类统计
     */
    private List<DashboardStatsDTO.CategoryStats> getCategoryStats() {
        return categoryRepository.countDashboardArticles().stream()
                .map(category -> {
                    // 统计该分类下的文章数量
                    long articleCount = category.getArticleCount();
                    return DashboardStatsDTO.CategoryStats.builder()
                            .id(category.getId())
                            .name(category.getName())
                            .articleCount(articleCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取UV趋势（基于 HyperLogLog 日UV + Redis 日PV）
     *
     * @param days 天数
     * @return Map 包含 dates、uvCounts、pvCounts 三个列表
     */
    public Map<String, Object> getUvTrend(int days) {
        List<String> dates = new ArrayList<>();
        List<Long> uvCounts = new ArrayList<>();
        List<Long> pvCounts = new ArrayList<>();

        DateTimeFormatter display = DateTimeFormatter.ofPattern("MM-dd");
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE;

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.format(display));

            // UV — HyperLogLog PFCOUNT
            String uvKey = RedisKeyPrefix.STATS_UV_DAILY + date.format(iso);
            Long uv = stringRedisTemplate.opsForHyperLogLog().size(uvKey);
            uvCounts.add(uv != null ? uv : 0L);

            // PV — String INCR 计数
            String pvKey = RedisKeyPrefix.DAILY_VIEW_COUNT + date.format(iso);
            Object pvVal = redisTemplate.opsForValue().get(pvKey);
            pvCounts.add(pvVal != null ? ((Number) pvVal).longValue() : 0L);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("uvCounts", uvCounts);
        result.put("pvCounts", pvCounts);
        return result;
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }
}
