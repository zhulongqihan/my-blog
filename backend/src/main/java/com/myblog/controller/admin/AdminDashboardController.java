package com.myblog.controller.admin;

import com.myblog.common.annotation.Log;
import com.myblog.common.result.Result;
import com.myblog.dto.admin.DashboardStatsDTO;
import com.myblog.service.admin.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台 - 仪表盘控制器
 * 提供首页统计数据接口
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取仪表盘统计数据
     * 包括：总览数据、趋势图数据、热门文章、最新评论、分类统计
     */
    @GetMapping("/stats")
    @Log(module = "仪表盘", operationType = "QUERY", description = "查看统计数据")
    public Result<DashboardStatsDTO> getStats() {
        DashboardStatsDTO stats = dashboardService.getStats();
        return Result.success(stats);
    }

    /**
     * 获取总览统计数据
     * 返回简化的统计信息：文章数、评论数、用户数、访问量
     */
    @GetMapping("/overview")
    public Result<DashboardStatsDTO> getOverview() {
        DashboardStatsDTO stats = dashboardService.getStats();
        // 简化版，只返回总览数据
        return Result.success(DashboardStatsDTO.builder()
                .totalViews(stats.getTotalViews())
                .totalArticles(stats.getTotalArticles())
                .totalComments(stats.getTotalComments())
                .totalUsers(stats.getTotalUsers())
                .todayViews(stats.getTodayViews())
                .todayArticles(stats.getTodayArticles())
                .todayComments(stats.getTodayComments())
                .todayUsers(stats.getTodayUsers())
                .build());
    }
}
