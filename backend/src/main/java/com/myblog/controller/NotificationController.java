package com.myblog.controller;

import com.myblog.dto.NotificationResponse;
import com.myblog.service.NotificationService;
import com.myblog.websocket.WebSocketEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知公开接口（前台前端使用）
 * 
 * 提供在线人数查询等公开功能
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final WebSocketEventListener eventListener;

    /**
     * 获取当前在线人数
     */
    @GetMapping("/online-count")
    public ResponseEntity<Map<String, Object>> getOnlineCount() {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", Map.of(
                        "onlineCount", eventListener.getOnlineCount(),
                        "timestamp", System.currentTimeMillis()
                )
        ));
    }
}
