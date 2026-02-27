package com.myblog.service;

import com.myblog.dto.NotificationResponse;
import com.myblog.entity.Notification;
import com.myblog.entity.Notification.NotificationType;
import com.myblog.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 通知服务
 * 
 * 职责：
 * 1. 创建通知并持久化到数据库
 * 2. 通过 WebSocket 实时推送通知
 * 3. 管理通知的已读状态
 * 
 * 面试亮点：
 * - SimpMessagingTemplate 实现 WebSocket 消息推送
 * - convertAndSendToUser() 实现用户定向推送
 * - convertAndSend() 实现全站广播
 * - 数据持久化 + 实时推送双保障
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 发送评论通知给管理员（被 MQ 消费者调用）
     */
    @Transactional
    public void sendCommentNotification(String commenterName, Long articleId, 
                                         String articleTitle, String commentContent) {
        Notification notification = Notification.builder()
                .type(NotificationType.COMMENT)
                .title("新评论通知")
                .content(commenterName + " 评论了文章《" + articleTitle + "》：" + 
                         (commentContent.length() > 50 ? commentContent.substring(0, 50) + "..." : commentContent))
                .senderName(commenterName)
                .relatedId(articleId)
                .relatedType("article")
                .recipientId(null)  // 广播给所有管理员
                .build();

        notification = notificationRepository.save(notification);
        NotificationResponse response = NotificationResponse.fromEntity(notification);

        // 通过 WebSocket 广播到 /topic/notifications（管理员订阅）
        messagingTemplate.convertAndSend("/topic/notifications", response);
        log.info("[通知] 评论通知已推送: article={}, commenter={}", articleTitle, commenterName);
    }

    /**
     * 发送系统公告（管理员调用）
     */
    @Transactional
    public NotificationResponse sendSystemNotification(String title, String content, String senderName) {
        Notification notification = Notification.builder()
                .type(NotificationType.SYSTEM)
                .title(title)
                .content(content)
                .senderName(senderName)
                .recipientId(null)  // 全站广播
                .build();

        notification = notificationRepository.save(notification);
        NotificationResponse response = NotificationResponse.fromEntity(notification);

        // 广播到全站
        messagingTemplate.convertAndSend("/topic/notifications", response);
        log.info("[通知] 系统公告已推送: title={}", title);

        return response;
    }

    /**
     * 发送定向通知给特定用户
     */
    @Transactional
    public void sendToUser(String username, Notification notification) {
        notification = notificationRepository.save(notification);
        NotificationResponse response = NotificationResponse.fromEntity(notification);

        // 点对点推送：/user/{username}/queue/notifications
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", response);
        log.info("[通知] 定向通知已推送给: {}", username);
    }

    /**
     * 获取用户通知列表（分页）
     */
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrBroadcast(userId, pageable)
                .map(NotificationResponse::fromEntity);
    }

    /**
     * 获取未读通知数
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * 标记单条通知已读
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    /**
     * 标记用户所有通知已读
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    /**
     * 获取 WebSocket 状态信息
     */
    public Map<String, Object> getWebSocketStats() {
        long totalNotifications = notificationRepository.count();
        return Map.of(
                "totalNotifications", totalNotifications,
                "status", "running"
        );
    }
}
