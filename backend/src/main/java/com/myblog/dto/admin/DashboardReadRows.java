package com.myblog.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Scalar dashboard projections: never hydrate article bodies or lazy entity graphs. */
public final class DashboardReadRows {
    private DashboardReadRows() {}

    public interface PopularArticle {
        Long getId();
        String getTitle();
        Integer getViewCount();
    }

    public interface RecentComment {
        Long getId();
        Long getUserId();
        String getContent();
        String getNickname();
        String getGuestName();
        String getArticleTitle();
        LocalDateTime getCreatedAt();
    }

    public interface CategoryCount {
        Long getId();
        String getName();
        Long getArticleCount();
    }

    public interface DailyCount {
        LocalDate getDay();
        Long getCount();
    }
}
