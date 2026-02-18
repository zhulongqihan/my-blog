package com.myblog.controller.admin;

import com.myblog.common.annotation.Log;
import com.myblog.common.result.Result;
import com.myblog.entity.Tag;
import com.myblog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端标签控制器
 * 基础路径：/api/admin/tags
 * 权限要求：ADMIN角色
 *
 * 接口列表：
 *   GET    /api/admin/tags       - 获取所有标签
 *   POST   /api/admin/tags       - 新建标签
 *   PUT    /api/admin/tags/{id}  - 更新标签
 *   DELETE /api/admin/tags/{id}  - 删除标签
 */
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTagController {

    private final TagRepository tagRepository;

    /**
     * 获取所有标签
     */
    @GetMapping
    public Result<List<Tag>> list() {
        return Result.success(tagRepository.findAll());
    }

    /**
     * 新建标签
     * POST /api/admin/tags
     * Body: {"name": "Spring Boot"}
     */
    @PostMapping
    @Log(module = "标签管理", operationType = "CREATE", description = "新建标签")
    public Result<Tag> create(@RequestBody Tag tag) {
        tag.setId(null);
        return Result.success(tagRepository.save(tag));
    }

    /**
     * 更新标签
     * PUT /api/admin/tags/1
     * Body: {"name": "Spring Boot 3"}
     */
    @PutMapping("/{id}")
    @Log(module = "标签管理", operationType = "UPDATE", description = "更新标签")
    public Result<Tag> update(@PathVariable Long id, @RequestBody Tag tag) {
        tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在，id=" + id));
        tag.setId(id);
        return Result.success(tagRepository.save(tag));
    }

    /**
     * 删除标签
     * DELETE /api/admin/tags/1
     */
    @DeleteMapping("/{id}")
    @Log(module = "标签管理", operationType = "DELETE", description = "删除标签")
    public Result<String> delete(@PathVariable Long id) {
        tagRepository.deleteById(id);
        return Result.success("删除成功");
    }
}
