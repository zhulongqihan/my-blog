package com.myblog.service;

import com.myblog.dto.AiSource;
import com.myblog.entity.Article;
import com.myblog.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 博客内容工具的唯一实现。
 * AI Gateway 与 MCP 共同复用，避免两套检索逻辑产生不一致的回答来源。
 */
@Service
@RequiredArgsConstructor
public class BlogContentToolService {

    private final ArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public RetrievalResult retrieve(String query, Long articleId, int limit) {
        List<Article> candidates = new ArrayList<>();
        if (articleId != null) {
            articleRepository.findById(articleId)
                    .filter(article -> Boolean.TRUE.equals(article.getPublished()))
                    .ifPresent(candidates::add);
        }

        articleRepository.findByPublishedTrue(PageRequest.of(0, 80)).forEach(article -> {
            if (candidates.stream().noneMatch(item -> item.getId().equals(article.getId()))) {
                candidates.add(article);
            }
        });

        List<String> terms = tokenize(query);
        List<ScoredArticle> scored = candidates.stream()
                .map(article -> new ScoredArticle(article, score(article, terms, articleId)))
                .filter(item -> item.score > 0 || item.article.getId().equals(articleId))
                .sorted(Comparator.comparingInt(ScoredArticle::score).reversed())
                .limit(Math.max(1, limit))
                .toList();

        List<AiSource> sources = scored.stream()
                .map(item -> AiSource.builder()
                        .articleId(item.article.getId())
                        .title(item.article.getTitle())
                        .score(item.score)
                        .build())
                .toList();

        String context = scored.stream()
                .map(item -> formatContext(item.article))
                .collect(Collectors.joining("\n\n"));
        if (context.length() > 9000) {
            context = context.substring(0, 9000) + "\n[检索上下文已截断]";
        }
        return new RetrievalResult(context, sources);
    }

    @Transactional(readOnly = true)
    public Optional<Article> getPublishedArticle(Long articleId) {
        return articleRepository.findById(articleId)
                .filter(article -> Boolean.TRUE.equals(article.getPublished()));
    }

    private int score(Article article, List<String> terms, Long pinnedId) {
        int score = Objects.equals(article.getId(), pinnedId) ? 100 : 0;
        String title = lower(article.getTitle());
        String summary = lower(article.getSummary());
        String content = lower(article.getContent());
        for (String term : terms) {
            if (title.contains(term)) score += 12;
            if (summary.contains(term)) score += 5;
            if (content.contains(term)) score += 1;
        }
        return score;
    }

    private List<String> tokenize(String query) {
        return Arrays.stream(lower(query).split("[^\\p{L}\\p{N}]+"))
                .map(String::trim)
                .filter(term -> term.length() >= 2)
                .distinct()
                .limit(12)
                .toList();
    }

    private String formatContext(Article article) {
        String content = Optional.ofNullable(article.getContent()).orElse("");
        if (content.length() > 5000) content = content.substring(0, 5000) + "\n[文章正文已截断]";
        return "【文章 #" + article.getId() + "】\n标题：" + article.getTitle()
                + "\n摘要：" + Optional.ofNullable(article.getSummary()).orElse("无")
                + "\n正文：\n" + content;
    }

    private String lower(String value) {
        return Optional.ofNullable(value).orElse("").toLowerCase(Locale.ROOT);
    }

    private record ScoredArticle(Article article, int score) { }

    public record RetrievalResult(String context, List<AiSource> sources) { }
}
