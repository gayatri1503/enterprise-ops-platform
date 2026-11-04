package com.platform.notification.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.notification.domain.ProcessedEvent;
import com.platform.notification.events.NotificationEventListener.PlatformEvent;
import com.platform.notification.repository.ProcessedEventRepository;
import com.platform.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationService, processedEventRepository, new ObjectMapper());
    }

    @Test
    void onEvent_ignoresDuplicateEvents_forIdempotency() {
        String eventId = "evt-uuid-1";
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        PlatformEvent event = new PlatformEvent(
                eventId, "WORK_ITEM_ASSIGNED", 1, "2026-11-04T12:00:00Z",
                "core-service", "10", "1", "1", Map.of("assigneeId", 20L)
        );

        listener.onEvent(event);

        verify(notificationService, never()).createNotification(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void onEvent_createsNotification_onWorkItemAssigned() {
        String eventId = "evt-assigned-1";
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        PlatformEvent event = new PlatformEvent(
                eventId, "WORK_ITEM_ASSIGNED", 1, "2026-11-04T12:00:00Z",
                "core-service", "10", "1", "1", Map.of("assigneeId", 20L, "title", "Upgrade Java")
        );

        listener.onEvent(event);

        verify(notificationService).createNotification(
                eq(20L),
                eq("WORK_ITEM_ASSIGNED"),
                eq("Work Item Assigned"),
                contains("Upgrade Java"),
                eq("10"),
                anyString()
        );
        verify(processedEventRepository).save(argThat(pe -> pe.getEventId().equals(eventId)));
    }

    @Test
    void onEvent_createsNotifications_onWorkItemStatusChanged() {
        String eventId = "evt-status-1";
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        PlatformEvent event = new PlatformEvent(
                eventId, "WORK_ITEM_STATUS_CHANGED", 1, "2026-11-04T12:00:00Z",
                "core-service", "10", "1", "1",
                Map.of("assigneeId", 20L, "creatorId", 30L, "fromStatus", "NEW", "toStatus", "IN_PROGRESS", "title", "Task A")
        );

        listener.onEvent(event);

        verify(notificationService).createNotification(
                eq(20L),
                eq("WORK_ITEM_STATUS_CHANGED"),
                eq("Work Item Status Changed"),
                contains("Task A"),
                eq("10"),
                anyString()
        );

        verify(notificationService).createNotification(
                eq(30L),
                eq("WORK_ITEM_STATUS_CHANGED"),
                eq("Work Item Status Changed"),
                contains("Task A"),
                eq("10"),
                anyString()
        );
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void onEvent_createsNotification_onWorkItemCreated() {
        String eventId = "evt-created-1";
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        PlatformEvent event = new PlatformEvent(
                eventId, "WORK_ITEM_CREATED", 1, "2026-11-04T12:00:00Z",
                "core-service", "15", "1", "1",
                Map.of("assigneeId", 40L, "title", "New Server Provisioning")
        );

        listener.onEvent(event);

        verify(notificationService).createNotification(
                eq(40L),
                eq("WORK_ITEM_CREATED"),
                eq("New Work Item Assigned"),
                contains("New Server Provisioning"),
                eq("15"),
                anyString()
        );
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }
}
