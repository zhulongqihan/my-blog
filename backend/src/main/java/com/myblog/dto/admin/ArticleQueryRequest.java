package com.myblog.dto.admin;

import lombok.Data;

/**
 * 管理端文章查询请求参数
 * 支持关键词搜索、分类筛选、发布状态筛选、排序
 */
@Data
public class ArticleQueryRequest {

    /** 当前页码，从1开始 */
    private int page = 1;

    /** 每页条数 */
    private int size = 10;

    /** 搜索关键词（标题/内容模糊匹配） */
    private String keyword;

    /** 分类ID筛选 */
    private Long categoryId;

    /** 发布状态：true=已发布，false=草稿，null=全部 */
    private Boolean published;

    /** 排序字段，默认按创建时间 */
    private String sortField = "createdAt";

    /** 排序方向：desc / asc */
    private String sortOrder = "desc";
}
