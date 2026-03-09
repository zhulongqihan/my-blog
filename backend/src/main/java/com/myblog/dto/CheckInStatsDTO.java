package com.myblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 签到统计 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInStatsDTO {
    /** 本月签到天数 */
    private Integer signCount;
    /** 当前连续签到天数 */
    private Integer consecutiveDays;
    /** 今日是否已签到 */
    private Boolean signedToday;
    /** 触发的成就（如 LOYAL_READER, HARDCORE_FAN） */
    private String achievement;
}
