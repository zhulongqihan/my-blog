package com.myblog.controller;

import com.myblog.dto.ApiResponse;
import com.myblog.entity.Category;
import com.myblog.entity.Tag;
import com.myblog.service.CategoryService;
import com.myblog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryTagController {

    private final CategoryService categoryService;
    private final TagService tagService;

    // ========== Category APIs ==========

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findAll()));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(ApiResponse.success("分类创建成功", categoryService.create(category)));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Category>> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return ResponseEntity.ok(ApiResponse.success("分类更新成功", categoryService.update(id, category)));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("分类删除成功", null));
    }

    // ========== Tag APIs ==========

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<Tag>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.success(tagService.findAll()));
    }

    @PostMapping("/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Tag>> createTag(@RequestBody Tag tag) {
        return ResponseEntity.ok(ApiResponse.success("标签创建成功", tagService.create(tag)));
    }

    @PutMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Tag>> updateTag(@PathVariable Long id, @RequestBody Tag tag) {
        return ResponseEntity.ok(ApiResponse.success("标签更新成功", tagService.update(id, tag)));
    }

    @DeleteMapping("/tags/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("标签删除成功", null));
    }
}
