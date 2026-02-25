package com.myblog.service;

import com.myblog.entity.Category;
import com.myblog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务层
 * 
 * 缓存策略：
 *   - 分类列表缓存2小时（分类是低频变更数据，缓存命中率极高）
 *   - 任何写操作清除缓存，下次读取重新加载
 *   - 这就是经典的 Cache Aside Pattern（旁路缓存模式）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 获取所有分类（缓存2小时）
     * 面试说法：使用 @Cacheable 实现 Cache Aside 读策略
     *   1. 先查 Redis 缓存
     *   2. 缓存命中 → 直接返回（不走DB）
     *   3. 缓存未命中 → 查DB → 结果写入缓存 → 返回
     */
    @Cacheable(value = "categories", key = "'all'")
    public List<Category> findAll() {
        log.info("[Cache MISS] 分类列表 - 从数据库加载");
        return categoryRepository.findAll();
    }

    /**
     * 创建分类 → 清除分类缓存
     * 面试说法：使用 @CacheEvict 实现 Cache Aside 写策略
     *   1. 先写DB
     *   2. 再删缓存（而不是更新缓存，避免并发问题）
     */
    @CacheEvict(value = "categories", allEntries = true)
    public Category create(Category category) {
        log.info("[Cache EVICT] 分类缓存已清除 - 新建分类: {}", category.getName());
        return categoryRepository.save(category);
    }

    /**
     * 更新分类 → 清除分类缓存
     */
    @CacheEvict(value = "categories", allEntries = true)
    public Category update(Long id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setIcon(category.getIcon());
        existing.setSortOrder(category.getSortOrder());
        log.info("[Cache EVICT] 分类缓存已清除 - 更新分类: {}", existing.getName());
        return categoryRepository.save(existing);
    }

    /**
     * 删除分类 → 清除分类缓存
     */
    @CacheEvict(value = "categories", allEntries = true)
    public void delete(Long id) {
        log.info("[Cache EVICT] 分类缓存已清除 - 删除分类 id={}", id);
        categoryRepository.deleteById(id);
    }
}
