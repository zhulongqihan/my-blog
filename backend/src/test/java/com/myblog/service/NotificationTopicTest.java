package com.myblog.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myblog.entity.Notification;
import com.myblog.repository.NotificationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationTopicTest {
    @Mock NotificationRepository repository;
    @Mock SimpMessagingTemplate messaging;
    @InjectMocks NotificationService service;

    @Test
    void commentGoesOnlyToAdministrativeTopic() {
        when(repository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        service.sendCommentNotification("visitor", 1L, "article", "comment");
        verify(messaging).convertAndSend(eq("/topic/admin/notifications"), any(Object.class));
        verify(messaging, never()).convertAndSend(eq("/topic/notifications"), any(Object.class));
    }

    @Test
    void publicSystemAnnouncementStillGoesToPublicTopic() {
        when(repository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        service.sendSystemNotification("notice", "message", "admin");
        verify(messaging).convertAndSend(eq("/topic/notifications"), any(Object.class));
    }
}
