package com.myblog.controller.admin;

import com.myblog.dto.NotificationResponse;
import com.myblog.entity.User;
import com.myblog.service.NotificationService;
import com.myblog.websocket.WebSocketEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员通知接口
 * 
 * 提供：
 * - 通知列表（分页 + 按时间排序）
 * - 未读通知数
 * - 标记已读（单条 / 全部）
 * - 发送系统公告
 * - WebSocket 状态监控
 */
@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final WebSocketEventListener eventListener;

    /**
     * 获取管理员通知列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<NotificationResponse> notifications = notificationService
                .getUserNotifications(user.getId(), 
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", Map.of(
                        "content", notifications.getContent(),
                        "totalElements", notifications.getTotalElements(),
                        "totalPages", notifications.getTotalPages(),
                        "number", notifications.getNumber()
                )
        ));
    }

    /**
     * 获取未读通知数
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("code", 200, "data", count));
    }

    /**
     * 标记单条通知已读
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "标记已读成功"));
    }

    /**
     * 标记所有通知已读
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@AuthenticationPrincipal User user) {
        int count = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("code", 200, "message", "已标记 " + count + " 条为已读"));
    }

    /**
     * 发送系统公告（广播给所有用户）
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> sendBroadcast(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "系统公告");
        String content = body.get("content");
        
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "内容不能为空"));
        }

        NotificationResponse response = notificationService.sendSystemNotification(
                title, content, user.getNickname() != null ? user.getNickname() : user.getUsername());
        
        return ResponseEntity.ok(Map.of("code", 200, "data", response));
    }

    /**
     * 获取 WebSocket 状态
     */
    @GetMapping("/ws-stats")
    public ResponseEntity<Map<String, Object>> getWebSocketStats() {
        Map<String, Object> stats = notificationService.getWebSocketStats();
        stats = new java.util.LinkedHashMap<>(stats);
        stats.put("onlineCount", eventListener.getOnlineCount());
        stats.put("onlineUsers", eventListener.getOnlineUsers());
        
        return ResponseEntity.ok(Map.of("code", 200, "data", stats));
    }
}
