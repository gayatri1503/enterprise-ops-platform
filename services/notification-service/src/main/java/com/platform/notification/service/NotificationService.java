package com.platform.notification.service;

import com.platform.notification.domain.Notification;
import com.platform.notification.dto.NotificationDtos.*;
import com.platform.notification.exception.ApiException;
import com.platform.notification.repository.NotificationRepository;
import com.platform.notification.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(CurrentUser user, Boolean unreadOnly, Pageable pageable) {
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(user.id(), pageable)
                    .map(NotificationResponse::from);
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.id(), pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(CurrentUser user) {
        long count = notificationRepository.countByUserIdAndReadAtIsNull(user.id());
        return new UnreadCountResponse(count);
    }

    @Transactional
    public NotificationResponse markAsRead(CurrentUser user, Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Notification not found"));

        if (!notification.getUserId().equals(user.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Cannot access notification of another user");
        }

        notification.markAsRead();
        Notification saved = notificationRepository.save(notification);
        log.info("Marked notification {} as read for user {}", id, user.id());
        return NotificationResponse.from(saved);
    }

    @Transactional
    public BatchReadResponse markAllAsRead(CurrentUser user) {
        int count = notificationRepository.markAllAsReadForUser(user.id());
        log.info("Marked {} notifications as read for user {}", count, user.id());
        return new BatchReadResponse(count);
    }

    @Transactional
    public Notification createNotification(Long userId, String type, String title, String message, String entityId, String payload) {
        Notification notification = new Notification(userId, type, title, message, entityId, payload);
        Notification saved = notificationRepository.save(notification);
        log.info("Created notification {} of type {} for user {}", saved.getId(), type, userId);
        return saved;
    }
}
