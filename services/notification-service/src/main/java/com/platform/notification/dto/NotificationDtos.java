package com.platform.notification.dto;

import com.platform.notification.domain.Notification;
import java.time.Instant;

public class NotificationDtos {

    public record NotificationResponse(
            Long id,
            Long userId,
            String type,
            String title,
            String message,
            String entityId,
            String payload,
            Instant readAt,
            Instant createdAt,
            boolean read
    ) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(),
                    n.getUserId(),
                    n.getType(),
                    n.getTitle(),
                    n.getMessage(),
                    n.getEntityId(),
                    n.getPayload(),
                    n.getReadAt(),
                    n.getCreatedAt(),
                    n.isRead()
            );
        }
    }

    public record UnreadCountResponse(long unreadCount) {}

    public record BatchReadResponse(int updatedCount) {}
}
