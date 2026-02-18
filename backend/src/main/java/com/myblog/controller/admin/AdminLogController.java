package com.myblog.controller.admin;

import com.myblog.common.result.PageResult;
import com.myblog.common.result.Result;
import com.myblog.entity.OperationLog;
import com.myblog.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端操作日志控制器
 * 基础路径：/api/admin/logs
 * 权限要求：ADMIN角色
 *
 * 接口列表：
 *   GET /api/admin/logs  - 分页查询操作日志（按操作时间倒序）
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final OperationLogRepository operationLogRepository;

    /**
     * 分页查询操作日志
     * GET /api/admin/logs?page=1&size=20
     */
    @GetMapping
    public Result<PageResult<OperationLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OperationLog> logPage = operationLogRepository.findAll(
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "operationTime"))
        );
        return Result.success(new PageResult<>(
                logPage.getContent(),
                logPage.getTotalElements(),
                (long) page,
                (long) size
        ));
    }
}
