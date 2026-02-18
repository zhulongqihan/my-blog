package com.myblog.controller.admin;

import com.myblog.common.annotation.Log;
import com.myblog.common.result.PageResult;
import com.myblog.common.result.Result;
import com.myblog.dto.admin.ArticleAdminResponse;
import com.myblog.dto.admin.ArticleQueryRequest;
import com.myblog.service.admin.AdminArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端文章控制器
 * 基础路径：/api/admin/articles
 * 权限要求：ADMIN角色
 *
 * 接口列表：
 *   GET    /api/admin/articles               - 分页查询文章（支持关键词/分类/状态筛选）
 *   PUT    /api/admin/articles/{id}/publish   - 发布文章
 *   PUT    /api/admin/articles/{id}/unpublish - 取消发布
 *   PUT    /api/admin/articles/{id}/top       - 置顶文章
 *   PUT    /api/admin/articles/{id}/untop     - 取消置顶
 *   DELETE /api/admin/articles/batch          - 批量删除
 */
@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminArticleController {

    private final AdminArticleService adminArticleService;

    /**
     * 分页查询文章列表
     * GET /api/admin/articles?page=1&size=10&keyword=xxx&published=true
     */
    @GetMapping
    public Result<PageResult<ArticleAdminResponse>> list(ArticleQueryRequest request) {
        return Result.success(adminArticleService.getArticles(request));
    }

    /**
     * 发布文章
     * PUT /api/admin/articles/1/publish
     */
    @PutMapping("/{id}/publish")
    @Log(module = "文章管理", operationType = "UPDATE", description = "发布文章")
    public Result<String> publish(@PathVariable Long id) {
        adminArticleService.togglePublish(id, true);
        return Result.success("发布成功");
    }

    /**
     * 取消发布（变为草稿）
     * PUT /api/admin/articles/1/unpublish
     */
    @PutMapping("/{id}/unpublish")
    @Log(module = "文章管理", operationType = "UPDATE", description = "取消发布文章")
    public Result<String> unpublish(@PathVariable Long id) {
        adminArticleService.togglePublish(id, false);
        return Result.success("已撤回为草稿");
    }

    /**
     * 置顶文章
     * PUT /api/admin/articles/1/top
     */
    @PutMapping("/{id}/top")
    @Log(module = "文章管理", operationType = "UPDATE", description = "置顶文章")
    public Result<String> top(@PathVariable Long id) {
        adminArticleService.toggleFeatured(id, true);
        return Result.success("置顶成功");
    }

    /**
     * 取消置顶
     * PUT /api/admin/articles/1/untop
     */
    @PutMapping("/{id}/untop")
    @Log(module = "文章管理", operationType = "UPDATE", description = "取消置顶文章")
    public Result<String> untop(@PathVariable Long id) {
        adminArticleService.toggleFeatured(id, false);
        return Result.success("已取消置顶");
    }

    /**
     * 批量删除文章
     * DELETE /api/admin/articles/batch
     * Body: [1, 2, 3]
     */
    @DeleteMapping("/batch")
    @Log(module = "文章管理", operationType = "DELETE", description = "批量删除文章")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        adminArticleService.batchDelete(ids);
        return Result.success("删除成功");
    }
}
