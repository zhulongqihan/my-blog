package com.myblog.dto;

import java.time.LocalDateTime;

/** 归档页只需要的文章字段，避免加载完整文章实体和关联集合。 */
public interface ArchiveArticleProjection {
    Long getId();

    String getTitle();

    LocalDateTime getPublishedAt();

    LocalDateTime getCreatedAt();

    String getCategoryName();
}
