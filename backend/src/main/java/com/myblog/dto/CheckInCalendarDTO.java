package com.myblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 签到日历 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInCalendarDTO {
    /** 月份 (yyyy-MM) */
    private String month;
    /** 该月总天数 */
    private Integer totalDays;
    /** 已签到的天列表（如 [1, 2, 3, 6, 7]） */
    private List<Integer> signedDays;
    /** 本月签到总天数 */
    private Integer signCount;
    /** 当前连续签到天数 */
    private Integer consecutiveDays;
}
