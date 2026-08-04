package com.myblog.controller;

import com.myblog.entity.Article;
import com.myblog.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final ArticleRepository articleRepository;

    @Value("${site.base-url:https://cyruszhang.online}")
    private String siteBaseUrl;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap() {
        String baseUrl = siteBaseUrl.replaceAll("/$", "");
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        appendUrl(xml, baseUrl + "/", null);
        appendUrl(xml, baseUrl + "/archive", null);
        appendUrl(xml, baseUrl + "/about", null);

        for (Article article : articleRepository.findByPublishedTrueOrderByPublishedAtDesc()) {
            LocalDateTime lastModified = article.getUpdatedAt() != null
                    ? article.getUpdatedAt()
                    : article.getPublishedAt();
            appendUrl(xml, baseUrl + "/article/" + article.getId(), lastModified);
        }

        xml.append("</urlset>");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(xml.toString());
    }

    private void appendUrl(StringBuilder xml, String location, LocalDateTime lastModified) {
        xml.append("<url><loc>")
                .append(escapeXml(location))
                .append("</loc>");
        if (lastModified != null) {
            xml.append("<lastmod>").append(lastModified.toLocalDate()).append("</lastmod>");
        }
        xml.append("</url>");
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
