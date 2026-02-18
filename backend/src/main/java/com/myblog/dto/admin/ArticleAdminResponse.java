package com.myblog.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端文章列表响应体
 * 包含文章基础信息 + 统计数据（评论数、浏览量）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleAdminResponse {

    private Long id;

    /** 文章标题 */
    private String title;

    /** 文章摘要 */
    private String summary;

    /** 所属分类名称 */
    private String categoryName;

    /** 标签名称列表 */
    private List<String> tagNames;

    /** 作者昵称 */
    private String authorName;

    /** 浏览量 */
    private Integer viewCount;

    /** 评论数 */
    private Long commentCount;

    /** 是否已发布 */
    private Boolean published;

    /** 是否置顶 */
    private Boolean featured;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
