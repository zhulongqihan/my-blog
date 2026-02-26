package com.myblog.controller.admin;

import com.myblog.common.annotation.Log;
import com.myblog.common.result.Result;
import com.myblog.service.MQMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台 - MQ监控控制器
 * 
 * 提供 RabbitMQ 运行状态查看、测试消息发送等功能
 */
@RestController
@RequestMapping("/api/admin/mq")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMQController {

    private final MQMonitorService mqMonitorService;

    /**
     * 获取 MQ 综合状态
     * 包括：连接状态、队列信息、交换机信息
     */
    @GetMapping("/stats")
    @Log(module = "MQ监控", operationType = "QUERY", description = "查看MQ状态")
    public Result<Map<String, Object>> getStats() {
        return Result.success(mqMonitorService.getMQStats());
    }

    /**
     * 检查 MQ 连接状态
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        boolean connected = mqMonitorService.isConnected();
        return Result.success(Map.of(
                "connected", connected,
                "status", connected ? "UP" : "DOWN"
        ));
    }

    /**
     * 发送测试消息
     * 
     * @param queueName 目标队列名称
     */
    @PostMapping("/test/{queueName}")
    @Log(module = "MQ监控", operationType = "CREATE", description = "发送MQ测试消息")
    public Result<String> sendTestMessage(@PathVariable String queueName) {
        try {
            mqMonitorService.sendTestMessage(queueName);
            return Result.success("测试消息已发送到队列: " + queueName);
        } catch (Exception e) {
            return Result.error("发送失败: " + e.getMessage());
        }
    }
}
