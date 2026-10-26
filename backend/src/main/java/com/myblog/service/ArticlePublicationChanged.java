package com.myblog.service;

import com.myblog.entity.Article;
import com.myblog.entity.Tag;
import java.time.LocalDateTime;
import java.util.List;

/** Immutable values captured inside the write transaction; never share managed entities with workers. */
public record ArticlePublicationChanged(
        Long articleId,
        boolean published,
        boolean previouslyPublished,
        boolean deleted,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        List<Long> tagIds) {
    public ArticlePublicationChanged {
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
    }

    public ArticlePublicationChanged(Article article, boolean previouslyPublished, boolean deleted) {
        this(article.getId(), Boolean.TRUE.equals(article.getPublished()), previouslyPublished, deleted,
                article.getPublishedAt(), article.getUpdatedAt(),
                !deleted && Boolean.TRUE.equals(article.getPublished()) && !previouslyPublished
                        && article.getTags() != null
                        ? article.getTags().stream().map(Tag::getId).toList() : List.of());
    }
}
