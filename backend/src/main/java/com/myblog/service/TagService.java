package com.myblog.service;

import com.myblog.entity.Tag;
import com.myblog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签服务层
 * 
 * 缓存策略同 CategoryService：
 *   - 列表缓存2小时
 *   - 写操作清除缓存
 *   - Cache Aside Pattern
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Cacheable(value = "tags", key = "'all'")
    public List<Tag> findAll() {
        log.info("[Cache MISS] 标签列表 - 从数据库加载");
        return tagRepository.findAll();
    }

    @CacheEvict(value = "tags", allEntries = true)
    public Tag create(Tag tag) {
        log.info("[Cache EVICT] 标签缓存已清除 - 新建标签: {}", tag.getName());
        return tagRepository.save(tag);
    }

    @CacheEvict(value = "tags", allEntries = true)
    public Tag update(Long id, Tag tag) {
        Tag existing = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        existing.setName(tag.getName());
        existing.setColor(tag.getColor());
        log.info("[Cache EVICT] 标签缓存已清除 - 更新标签: {}", existing.getName());
        return tagRepository.save(existing);
    }

    @CacheEvict(value = "tags", allEntries = true)
    public void delete(Long id) {
        log.info("[Cache EVICT] 标签缓存已清除 - 删除标签 id={}", id);
        tagRepository.deleteById(id);
    }
}
