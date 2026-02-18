package com.myblog.service.admin;

import com.myblog.dto.admin.DashboardStatsDTO;
import com.myblog.entity.Article;
import com.myblog.entity.Comment;
import com.myblog.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仪表盘服务
 * 提供后台管理首页的统计数据
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 获取仪表盘统计数据
     */
    public DashboardStatsDTO getStats() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // 总计统计
        long totalArticles = articleRepository.count();
        long totalComments = commentRepository.count();
        long totalUsers = userRepository.count();
        long totalViews = getTotalViews();

        // 今日统计
        long todayArticles = articleRepository.countByCreatedAtBetween(todayStart, todayEnd);
        long todayComments = commentRepository.countByCreatedAtBetween(todayStart, todayEnd);
        long todayUsers = userRepository.countByCreatedAtBetween(todayStart, todayEnd);
        long todayViews = getTodayViews();

        // 趋势数据（最近7天）
        List<DashboardStatsDTO.TrendItem> viewTrend = getViewTrend(7);
        List<DashboardStatsDTO.TrendItem> articleTrend = getArticleTrend(7);

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
     * 获取今日访问量
     * TODO: 可以通过Redis实现更精确的统计
     */
    private long getTodayViews() {
        // 暂时返回0，后续可以通过Redis实现
        return 0L;
    }

    /**
     * 获取访问趋势
     */
    private List<DashboardStatsDTO.TrendItem> getViewTrend(int days) {
        // TODO: 实现真实的访问量统计，需要添加访问日志表
        List<DashboardStatsDTO.TrendItem> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            trend.add(DashboardStatsDTO.TrendItem.builder()
                    .date(date.format(formatter))
                    .count(0L) // 暂时返回0
                    .build());
        }
        return trend;
    }

    /**
     * 获取文章发布趋势
     */
    private List<DashboardStatsDTO.TrendItem> getArticleTrend(int days) {
        List<DashboardStatsDTO.TrendItem> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            
            long count = articleRepository.countByCreatedAtBetween(startOfDay, endOfDay);
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
        List<Article> articles = articleRepository.findTopByViewCount(pageable);
        
        return articles.stream()
                .map(article -> {
                    // 获取文章评论数
                    long commentCount = commentRepository.countByArticle(article);
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
        List<Comment> comments = commentRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
        
        return comments.stream()
                .map(comment -> {
                    // 获取评论作者名称（注册用户或游客）
                    String authorName = comment.getUser() != null 
                            ? comment.getUser().getNickname() 
                            : comment.getGuestName();
                    
                    return DashboardStatsDTO.RecentComment.builder()
                            .id(comment.getId())
                            .content(truncateContent(comment.getContent(), 50))
                            .authorName(authorName != null ? authorName : "匿名")
                            .articleTitle(comment.getArticle().getTitle())
                            .createdAt(comment.getCreatedAt().format(formatter))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取分类统计
     */
    private List<DashboardStatsDTO.CategoryStats> getCategoryStats() {
        return categoryRepository.findAll().stream()
                .map(category -> {
                    // 统计该分类下的文章数量
                    long articleCount = articleRepository.countByCategoryId(category.getId());
                    return DashboardStatsDTO.CategoryStats.builder()
                            .id(category.getId())
                            .name(category.getName())
                            .articleCount(articleCount)
                            .build();
                })
                .collect(Collectors.toList());
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
