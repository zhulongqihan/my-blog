package com.myblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文章归档响应 DTO
 * 按年-月-文章层级结构返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveResponse {
    private int totalCount;
    private List<YearArchive> years;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearArchive {
        private int year;
        private int count;
        private List<MonthArchive> months;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthArchive {
        private int month;
        private String monthName;
        private List<ArticleBrief> articles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleBrief {
        private Long id;
        private String title;
        private String date;
        private String category;
    }
}
