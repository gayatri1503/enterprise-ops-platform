package com.platform.notification.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.notification.domain.ProcessedEvent;
import com.platform.notification.repository.ProcessedEventRepository;
import com.platform.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlatformEvent(
            String eventId,
            String eventType,
            Integer version,
            String occurredAt,
            String producer,
            String entityId,
            String actorId,
            String departmentId,
            Map<String, Object> payload
    ) {}

    private final NotificationService notificationService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(NotificationService notificationService,
                                   ProcessedEventRepository processedEventRepository,
                                   ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    @Transactional
    public void onEvent(PlatformEvent event) {
        if (event == null || event.eventType() == null) {
            return;
        }

        // Idempotency check: guarantee that re-delivered events are not re-processed
        if (event.eventId() != null && processedEventRepository.existsById(event.eventId())) {
            log.info("Duplicate event {} ignored for idempotency", event.eventId());
            return;
        }

        Map<String, Object> payload = event.payload() != null ? event.payload() : Map.of();
        String jsonPayload = serializePayload(payload);

        switch (event.eventType()) {
            case "WORK_ITEM_ASSIGNED" -> handleWorkItemAssigned(event, payload, jsonPayload);
            case "WORK_ITEM_STATUS_CHANGED" -> handleWorkItemStatusChanged(event, payload, jsonPayload);
            case "WORK_ITEM_CREATED" -> handleWorkItemCreated(event, payload, jsonPayload);
            default -> log.debug("Notification service ignoring event type: {}", event.eventType());
        }

        if (event.eventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.eventId()));
        }
    }

    private void handleWorkItemAssigned(PlatformEvent event, Map<String, Object> payload, String jsonPayload) {
        Long assigneeId = toLong(payload.get("assigneeId"));
        if (assigneeId == null) {
            return;
        }
        String title = (String) payload.get("title");
        String titlePart = (title != null && !title.isBlank()) ? " (\"" + title + "\")" : "";
        String message = "You have been assigned to work item #" + event.entityId() + titlePart;

        notificationService.createNotification(
                assigneeId,
                "WORK_ITEM_ASSIGNED",
                "Work Item Assigned",
                message,
                event.entityId(),
                jsonPayload
        );
    }

    private void handleWorkItemStatusChanged(PlatformEvent event, Map<String, Object> payload, String jsonPayload) {
        String fromStatus = String.valueOf(payload.getOrDefault("fromStatus", ""));
        String toStatus = String.valueOf(payload.getOrDefault("toStatus", ""));
        String title = (String) payload.get("title");
        String titlePart = (title != null && !title.isBlank()) ? " (\"" + title + "\")" : "";
        String message = "Work item #" + event.entityId() + titlePart + " status updated from " + fromStatus + " to " + toStatus;

        Long assigneeId = toLong(payload.get("assigneeId"));
        Long creatorId = toLong(payload.get("creatorId"));
        Long actorId = toLong(event.actorId());

        if (assigneeId != null && !assigneeId.equals(actorId)) {
            notificationService.createNotification(
                    assigneeId,
                    "WORK_ITEM_STATUS_CHANGED",
                    "Work Item Status Changed",
                    message,
                    event.entityId(),
                    jsonPayload
            );
        }

        if (creatorId != null && !creatorId.equals(actorId) && !creatorId.equals(assigneeId)) {
            notificationService.createNotification(
                    creatorId,
                    "WORK_ITEM_STATUS_CHANGED",
                    "Work Item Status Changed",
                    message,
                    event.entityId(),
                    jsonPayload
            );
        }
    }

    private void handleWorkItemCreated(PlatformEvent event, Map<String, Object> payload, String jsonPayload) {
        Long assigneeId = toLong(payload.get("assigneeId"));
        if (assigneeId == null) {
            return;
        }
        String title = (String) payload.get("title");
        String titlePart = (title != null && !title.isBlank()) ? " (\"" + title + "\")" : "";
        String message = "You have been assigned to newly created work item #" + event.entityId() + titlePart;

        notificationService.createNotification(
                assigneeId,
                "WORK_ITEM_CREATED",
                "New Work Item Assigned",
                message,
                event.entityId(),
                jsonPayload
        );
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }
}
