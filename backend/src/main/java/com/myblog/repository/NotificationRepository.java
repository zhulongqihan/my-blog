package com.myblog.repository;

import com.myblog.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 查询用户的通知（含全站广播 recipientId=null） */
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :userId OR n.recipientId IS NULL ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdOrBroadcast(@Param("userId") Long userId, Pageable pageable);

    /** 查询用户的未读通知数 */
    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.recipientId = :userId OR n.recipientId IS NULL) AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    /** 查询管理员通知（所有定向给管理员的 + 全站广播） */
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :adminId OR n.recipientId IS NULL ORDER BY n.createdAt DESC")
    Page<Notification> findAdminNotifications(@Param("adminId") Long adminId, Pageable pageable);

    /** 批量标记为已读（包含定向通知和广播通知） */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE (n.recipientId = :userId OR n.recipientId IS NULL) AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);

    /** 查询最近的全站广播通知 */
    List<Notification> findByRecipientIdIsNullOrderByCreatedAtDesc(Pageable pageable);
}
