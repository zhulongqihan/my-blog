package com.myblog.service.admin;

import com.myblog.common.result.PageResult;
import com.myblog.dto.admin.ArticleAdminResponse;
import com.myblog.dto.admin.ArticleQueryRequest;
import com.myblog.entity.Article;
import com.myblog.repository.ArticleRepository;
import com.myblog.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端文章服务
 * 提供：分页查询、发布/取消发布、置顶、批量删除
 */
@Service
@RequiredArgsConstructor
public class AdminArticleService {

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;

    /**
     * 分页查询文章列表（管理端，可见全部文章含草稿）
     */
    public PageResult<ArticleAdminResponse> getArticles(ArticleQueryRequest request) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(request.getSortOrder())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC,
                request.getSortField()
        );
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);

        Page<Article> articlePage;
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            // 按关键词搜索（标题+内容）
            articlePage = articleRepository.searchByKeyword(request.getKeyword(), pageable);
        } else {
            articlePage = articleRepository.findAll(pageable);
        }

        List<ArticleAdminResponse> records = articlePage.getContent().stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());

        return new PageResult<>(
                records,
                articlePage.getTotalElements(),
                (long) request.getPage(),
                (long) request.getSize()
        );
    }

    /**
     * 发布 / 取消发布文章
     *
     * @param id      文章ID
     * @param publish true=发布，false=撤回草稿
     */
    @Transactional
    public void togglePublish(Long id, boolean publish) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在，id=" + id));
        article.setPublished(publish);
        articleRepository.save(article);
    }

    /**
     * 设置 / 取消置顶
     *
     * @param id       文章ID
     * @param featured true=置顶，false=取消
     */
    @Transactional
    public void toggleFeatured(Long id, boolean featured) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在，id=" + id));
        article.setFeatured(featured);
        articleRepository.save(article);
    }

    /**
     * 批量删除文章
     *
     * @param ids 要删除的文章ID列表
     */
    @Transactional
    public void batchDelete(List<Long> ids) {
        articleRepository.deleteAllById(ids);
    }

    // ---- 私有辅助方法 ----

    private ArticleAdminResponse toAdminResponse(Article article) {
        return ArticleAdminResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .categoryName(article.getCategory() != null ? article.getCategory().getName() : null)
                .tagNames(article.getTags().stream()
                        .map(t -> t.getName())
                        .collect(Collectors.toList()))
                .authorName(article.getAuthor() != null ? article.getAuthor().getNickname() : "未知")
                .viewCount(article.getViewCount())
                .commentCount(commentRepository.countByArticle(article))
                .published(article.getPublished())
                .featured(article.getFeatured())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}
