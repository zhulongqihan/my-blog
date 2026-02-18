package com.myblog.controller.admin;

import com.myblog.common.annotation.Log;
import com.myblog.common.result.PageResult;
import com.myblog.common.result.Result;
import com.myblog.entity.Comment;
import com.myblog.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端评论控制器
 * 基础路径：/api/admin/comments
 * 权限要求：ADMIN角色
 *
 * 接口列表：
 *   GET    /api/admin/comments              - 分页查询评论
 *   PUT    /api/admin/comments/{id}/approve - 审核通过
 *   DELETE /api/admin/comments/{id}         - 删除单条评论
 *   DELETE /api/admin/comments/batch        - 批量删除
 */
@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommentController {

    private final CommentRepository commentRepository;

    /**
     * 分页查询所有评论（按创建时间倒序）
     * GET /api/admin/comments?page=1&size=10
     */
    @GetMapping
    public Result<PageResult<Comment>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Comment> commentPage = commentRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page - 1, size));
        return Result.success(new PageResult<>(
                commentPage.getContent(),
                commentPage.getTotalElements(),
                (long) page,
                (long) size
        ));
    }

    /**
     * 审核通过评论
     * PUT /api/admin/comments/1/approve
     */
    @PutMapping("/{id}/approve")
    @Log(module = "评论管理", operationType = "UPDATE", description = "审核通过评论")
    public Result<String> approve(@PathVariable Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在，id=" + id));
        comment.setApproved(true);
        commentRepository.save(comment);
        return Result.success("审核通过");
    }

    /**
     * 删除单条评论
     * DELETE /api/admin/comments/1
     */
    @DeleteMapping("/{id}")
    @Log(module = "评论管理", operationType = "DELETE", description = "删除评论")
    public Result<String> delete(@PathVariable Long id) {
        commentRepository.deleteById(id);
        return Result.success("删除成功");
    }

    /**
     * 批量删除评论
     * DELETE /api/admin/comments/batch
     * Body: [1, 2, 3]
     */
    @DeleteMapping("/batch")
    @Log(module = "评论管理", operationType = "DELETE", description = "批量删除评论")
    public Result<String> batchDelete(@RequestBody List<Long> ids) {
        commentRepository.deleteAllById(ids);
        return Result.success("批量删除成功");
    }
}
