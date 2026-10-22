package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myblog.entity.*;
import com.myblog.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CommentServiceAuthorizationTest {
    @Mock CommentRepository comments;
    @Mock ArticleRepository articles;
    @Mock MQProducerService mq;
    @InjectMocks CommentService service;

    private User user(long id, User.Role role) {
        return User.builder().id(id).role(role).build();
    }

    @Test
    void ordinaryUserCannotDeleteGuestComment() {
        var c = Comment.builder().id(1L).build();
        when(comments.findById(1L)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.deleteComment(1L, user(2, User.Role.USER)))
                .isInstanceOf(AccessDeniedException.class);
        verify(comments, never()).delete(any());
    }

    @Test
    void ordinaryUserCannotDeleteAnotherUsersComment() {
        var c = Comment.builder().id(1L).user(user(3, User.Role.USER)).build();
        when(comments.findById(1L)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.deleteComment(1L, user(2, User.Role.USER)))
                .isInstanceOf(AccessDeniedException.class);
        verify(comments, never()).delete(any());
    }

    @Test
    void ownerCanDeleteOwnComment() {
        var c = Comment.builder().id(1L).user(user(2, User.Role.USER)).build();
        when(comments.findById(1L)).thenReturn(Optional.of(c));
        service.deleteComment(1L, user(2, User.Role.USER));
        verify(comments).delete(c);
    }

    @Test
    void adminCanDeleteGuestComment() {
        var c = Comment.builder().id(1L).build();
        when(comments.findById(1L)).thenReturn(Optional.of(c));
        service.deleteComment(1L, user(2, User.Role.ADMIN));
        verify(comments).delete(c);
    }

    @Test
    void anonymousDeletionIsDeniedByServiceAsWell() {
        assertThatThrownBy(() -> service.deleteComment(1L, null))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(comments);
    }
}
