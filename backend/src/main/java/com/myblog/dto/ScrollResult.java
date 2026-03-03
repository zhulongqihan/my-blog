package com.myblog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Feed 流滚动分页 DTO
 *
 * 对标黑马点评 ScrollResult — 用 lastTimestamp + offset 替代传统分页
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScrollResult<T> {
    /** 数据列表 */
    private List<T> list;
    /** 本批最小时间戳（下一次查询的 lastTimestamp） */
    private Long minTime;
    /** 相同 score 的偏移量（下一次查询的 offset） */
    private Integer offset;
}
