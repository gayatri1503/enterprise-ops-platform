package com.platform.notification.service;

import com.platform.notification.domain.Notification;
import com.platform.notification.dto.NotificationDtos.*;
import com.platform.notification.exception.ApiException;
import com.platform.notification.repository.NotificationRepository;
import com.platform.notification.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    private final CurrentUser user1 = new CurrentUser(100L, "USER", 1L);
    private final CurrentUser user2 = new CurrentUser(200L, "USER", 1L);

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void list_returnsAllForUser_whenUnreadOnlyFalseOrNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Notification n = new Notification(100L, "ASSIGNED", "Title", "Msg", "1", "{}");
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(100L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationResponse> result = notificationService.list(user1, false, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Title");
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(100L, pageable);
    }

    @Test
    void list_returnsOnlyUnread_whenUnreadOnlyTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        Notification n = new Notification(100L, "ASSIGNED", "Title", "Msg", "1", "{}");
        when(notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(eq(100L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationResponse> result = notificationService.list(user1, true, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(notificationRepository).findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(100L, pageable);
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByUserIdAndReadAtIsNull(100L)).thenReturn(5L);

        UnreadCountResponse resp = notificationService.getUnreadCount(user1);

        assertThat(resp.unreadCount()).isEqualTo(5L);
    }

    @Test
    void markAsRead_success_whenNotificationBelongsToUser() {
        Notification n = new Notification(100L, "ASSIGNED", "Title", "Msg", "1", "{}");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse resp = notificationService.markAsRead(user1, 1L);

        assertThat(resp.read()).isTrue();
        assertThat(resp.readAt()).isNotNull();
        verify(notificationRepository).save(n);
    }

    @Test
    void markAsRead_throwsForbidden_whenNotificationBelongsToDifferentUser() {
        Notification n = new Notification(200L, "ASSIGNED", "Title", "Msg", "1", "{}");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.markAsRead(user1, 1L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("code", "FORBIDDEN");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_throwsNotFound_whenNotificationDoesNotExist() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(user1, 999L))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "NOT_FOUND");
    }

    @Test
    void markAllAsRead_updatesAllUnreadNotifications() {
        when(notificationRepository.markAllAsReadForUser(100L)).thenReturn(3);

        BatchReadResponse resp = notificationService.markAllAsRead(user1);

        assertThat(resp.updatedCount()).isEqualTo(3);
    }

    @Test
    void createNotification_persistsNotification() {
        Notification n = new Notification(100L, "STATUS_CHANGE", "Updated", "Done", "42", "{}");
        when(notificationRepository.save(any(Notification.class))).thenReturn(n);

        Notification created = notificationService.createNotification(
                100L, "STATUS_CHANGE", "Updated", "Done", "42", "{}"
        );

        assertThat(created.getUserId()).isEqualTo(100L);
        assertThat(created.getTitle()).isEqualTo("Updated");
    }
}
