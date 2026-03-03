package com.myblog.controller;

import com.myblog.dto.ApiResponse;
import com.myblog.dto.CheckInCalendarDTO;
import com.myblog.dto.CheckInStatsDTO;
import com.myblog.entity.User;
import com.myblog.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

/**
 * 读者签到控制器 — BitMap 签到 + 连续签到统计
 */
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * 签到打卡
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CheckInStatsDTO>> checkIn(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("签到成功", checkInService.checkIn(user.getId())));
    }

    /**
     * 签到统计
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<CheckInStatsDTO>> getStats(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        if (month == null) month = YearMonth.now();
        return ResponseEntity.ok(ApiResponse.success(checkInService.getStats(user.getId(), month)));
    }

    /**
     * 签到日历（某月签到详情）
     */
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<CheckInCalendarDTO>> getCalendar(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return ResponseEntity.ok(ApiResponse.success(checkInService.getCalendar(user.getId(), month)));
    }
}
