package com.myblog.controller.admin;

import com.myblog.common.result.Result;
import com.myblog.dto.AiTraceResponse;
import com.myblog.service.AiTraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiController {

    private final AiTraceService traceService;

    @GetMapping("/traces")
    public Result<Map<String, Object>> getTraces(@RequestParam(defaultValue = "50") int limit) {
        List<AiTraceResponse> traces = traceService.recent(limit);
        long success = traces.stream().filter(item -> "success".equals(item.getStatus())).count();
        long totalLatency = traces.stream().mapToLong(AiTraceResponse::getTotalLatencyMs).sum();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("traces", traces);
        data.put("total", traces.size());
        data.put("successRate", traces.isEmpty() ? 0 : Math.round(success * 10000.0 / traces.size()) / 100.0);
        data.put("avgLatencyMs", traces.isEmpty() ? 0 : Math.round(totalLatency * 1.0 / traces.size()));
        return Result.success(data);
    }
}
