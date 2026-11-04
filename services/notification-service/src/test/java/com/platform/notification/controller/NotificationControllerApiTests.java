package com.platform.notification.controller;

import com.platform.notification.domain.Notification;
import com.platform.notification.repository.NotificationRepository;
import com.platform.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ping_isPublic_returns200() throws Exception {
        mockMvc.perform(get("/api/notifications/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void list_returnsOnlyCurrentUserNotifications() throws Exception {
        // Given notifications for User 10 and User 20
        Notification n1 = notificationRepository.save(new Notification(10L, "TYPE_A", "User 10 Notification", "Body 1", "101", "{}"));
        Notification n2 = notificationRepository.save(new Notification(20L, "TYPE_B", "User 20 Notification", "Body 2", "102", "{}"));

        // When User 10 requests notifications
        mockMvc.perform(get("/api/notifications")
                        .with(jwt().jwt(j -> j.subject("10").claim("roles", List.of("USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(n1.getId().intValue())))
                .andExpect(jsonPath("$.content[0].title", is("User 10 Notification")));
    }

    @Test
    void getUnreadCount_returnsCountForCurrentUser() throws Exception {
        notificationRepository.save(new Notification(10L, "TYPE_A", "Unread 1", "Body 1", "101", "{}"));
        notificationRepository.save(new Notification(10L, "TYPE_B", "Unread 2", "Body 2", "102", "{}"));
        Notification readNotif = new Notification(10L, "TYPE_C", "Already Read", "Body 3", "103", "{}");
        readNotif.markAsRead();
        notificationRepository.save(readNotif);

        // User 20 unread should not affect user 10
        notificationRepository.save(new Notification(20L, "TYPE_A", "User 20 Unread", "Body", "201", "{}"));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(jwt().jwt(j -> j.subject("10").claim("roles", List.of("USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(2)));
    }

    @Test
    void markAsRead_whenOwnedByUser_succeeds() throws Exception {
        Notification n = notificationRepository.save(new Notification(10L, "TYPE_A", "Unread", "Body", "101", "{}"));

        mockMvc.perform(patch("/api/notifications/" + n.getId() + "/read")
                        .with(jwt().jwt(j -> j.subject("10").claim("roles", List.of("USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read", is(true)))
                .andExpect(jsonPath("$.readAt", notNullValue()));
    }

    @Test
    void markAsRead_whenOwnedByAnotherUser_returns403Forbidden() throws Exception {
        // Notification belongs to user 20
        Notification n = notificationRepository.save(new Notification(20L, "TYPE_A", "Private Notification", "Secret", "101", "{}"));

        // User 10 attempts to mark user 20's notification as read
        mockMvc.perform(patch("/api/notifications/" + n.getId() + "/read")
                        .with(jwt().jwt(j -> j.subject("10").claim("roles", List.of("USER")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                .andExpect(jsonPath("$.message", containsString("Cannot access notification of another user")));
    }

    @Test
    void markAllAsRead_marksAllUnreadForCurrentUser() throws Exception {
        notificationRepository.save(new Notification(10L, "TYPE_A", "Unread 1", "Body 1", "101", "{}"));
        notificationRepository.save(new Notification(10L, "TYPE_B", "Unread 2", "Body 2", "102", "{}"));
        notificationRepository.save(new Notification(20L, "TYPE_C", "User 20 Unread", "Body 3", "201", "{}"));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .with(jwt().jwt(j -> j.subject("10").claim("roles", List.of("USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount", is(2)));

        // User 20's notification must remain unread
        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(jwt().jwt(j -> j.subject("20").claim("roles", List.of("USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(1)));
    }
}
