package com.myblog.controller;

import com.myblog.websocket.WebSocketEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
