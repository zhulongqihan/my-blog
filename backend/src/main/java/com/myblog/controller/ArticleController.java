package com.myblog.controller;

import com.myblog.common.annotation.RateLimit;
import com.myblog.dto.ApiResponse;
import com.myblog.dto.ArchiveResponse;
import com.myblog.dto.ArticleRequest;
import com.myblog.dto.ArticleResponse;
import com.myblog.entity.User;
import com.myblog.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @RateLimit(maxRequests = 60, timeWindow = 60, limitType = RateLimit.LimitType.IP_AND_API)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> getArticles(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(articleService.getPublishedArticles(pageable)));
    }

    @RateLimit(maxRequests = 60, timeWindow = 60, limitType = RateLimit.LimitType.IP_AND_API)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(articleService.getArticleAndIncrementView(id)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> getFeaturedArticles() {
        return ResponseEntity.ok(ApiResponse.success(articleService.getFeaturedArticles()));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> getPopularArticles(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(articleService.getPopularArticles(limit)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> getArticlesByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(articleService.getArticlesByCategory(categoryId, pageable)));
    }

    @GetMapping("/tag/{tagId}")
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> getArticlesByTag(
            @PathVariable Long tagId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(articleService.getArticlesByTag(tagId, pageable)));
    }

    @RateLimit(maxRequests = 30, timeWindow = 60, limitType = RateLimit.LimitType.IP_AND_API, prefix = "search")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> searchArticles(
            @RequestParam String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(articleService.searchArticles(keyword, pageable)));
    }

    @GetMapping("/archive")
    public ResponseEntity<ApiResponse<ArchiveResponse>> getArchive() {
        return ResponseEntity.ok(ApiResponse.success(articleService.getArchive()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success("文章创建成功", articleService.createArticle(request, currentUser)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success("文章更新成功", articleService.updateArticle(id, request, currentUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        articleService.deleteArticle(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("文章删除成功", null));
    }
}
