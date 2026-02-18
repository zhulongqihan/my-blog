package com.myblog.controller.admin;

import com.myblog.common.annotation.Log;
import com.myblog.common.result.Result;
import com.myblog.entity.Category;
import com.myblog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端分类控制器
 * 基础路径：/api/admin/categories
 * 权限要求：ADMIN角色
 *
 * 接口列表：
 *   GET    /api/admin/categories       - 获取所有分类
 *   POST   /api/admin/categories       - 新建分类
 *   PUT    /api/admin/categories/{id}  - 更新分类
 *   DELETE /api/admin/categories/{id}  - 删除分类
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;

    /**
     * 获取所有分类
     */
    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(categoryRepository.findAll());
    }

    /**
     * 新建分类
     * POST /api/admin/categories
     * Body: {"name": "Java", "description": "Java相关文章"}
     */
    @PostMapping
    @Log(module = "分类管理", operationType = "CREATE", description = "新建分类")
    public Result<Category> create(@RequestBody Category category) {
        // 确保id为空，防止误更新
        category.setId(null);
        return Result.success(categoryRepository.save(category));
    }

    /**
     * 更新分类
     * PUT /api/admin/categories/1
     * Body: {"name": "Java进阶", "description": "..."}
     */
    @PutMapping("/{id}")
    @Log(module = "分类管理", operationType = "UPDATE", description = "更新分类")
    public Result<Category> update(@PathVariable Long id, @RequestBody Category category) {
        // 验证分类是否存在
        categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在，id=" + id));
        category.setId(id);
        return Result.success(categoryRepository.save(category));
    }

    /**
     * 删除分类
     * DELETE /api/admin/categories/1
     */
    @DeleteMapping("/{id}")
    @Log(module = "分类管理", operationType = "DELETE", description = "删除分类")
    public Result<String> delete(@PathVariable Long id) {
        categoryRepository.deleteById(id);
        return Result.success("删除成功");
    }
}
