package com.myblog.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 仪表盘统计数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    /**
     * 总访问量
     */
    private Long totalViews;

    /**
     * 文章总数
     */
    private Long totalArticles;

    /**
     * 评论总数
     */
    private Long totalComments;

    /**
     * 用户总数
     */
    private Long totalUsers;

    /**
     * 今日访问量
     */
    private Long todayViews;

    /**
     * 今日新增文章数
     */
    private Long todayArticles;

    /**
     * 今日新增评论数
     */
    private Long todayComments;

    /**
     * 今日新增用户数
     */
    private Long todayUsers;

    /**
     * 访问趋势（最近7天）
     */
    private List<TrendItem> viewTrend;

    /**
     * 文章发布趋势（最近7天）
     */
    private List<TrendItem> articleTrend;

    /**
     * 热门文章（Top 5）
     */
    private List<PopularArticle> popularArticles;

    /**
     * 最新评论（最近5条）
     */
    private List<RecentComment> recentComments;

    /**
     * 分类文章统计
     */
    private List<CategoryStats> categoryStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularArticle {
        private Long id;
        private String title;
        private Long viewCount;
        private Long commentCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentComment {
        private Long id;
        private String content;
        private String authorName;
        private String articleTitle;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStats {
        private Long id;
        private String name;
        private Long articleCount;
    }
}
