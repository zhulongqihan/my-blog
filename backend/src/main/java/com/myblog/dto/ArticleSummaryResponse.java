package com.myblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表摘要，避免列表、搜索和推荐接口重复传输正文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSummaryResponse {
    private Long id;
    private String title;
    private String summary;
    private String coverImage;
    private ArticleResponse.AuthorInfo author;
    private ArticleResponse.CategoryInfo category;
    private List<ArticleResponse.TagInfo> tags;
    private Integer viewCount;
    private Integer likeCount;
    private Long commentCount;
    private Boolean published;
    private Boolean featured;
    private Long uvCount;
    private Boolean liked;
    private Integer readTimeMinutes;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
