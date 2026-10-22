package com.myblog.service;

import com.myblog.entity.Article;

/** A database change; public caches are invalidated only after its transaction commits. */
public record ArticlePublicationChanged(
        Article article, boolean previouslyPublished, boolean deleted) {}
