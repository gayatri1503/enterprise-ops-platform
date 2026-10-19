package com.platform.auth.events;

import com.platform.auth.domain.User;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the USER_REGISTERED event. Used on register and on startup re-sync. */
public final class UserEvents {

    private UserEvents() {}

    public static void publishRegistered(EventPublisher events, User user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", user.getEmail());
        payload.put("fullName", user.getFullName());
        payload.put("role", user.getRole().name());
        payload.put("departmentId", user.getDepartmentId());
        payload.put("enabled", user.isEnabled());

        events.publish("USER_REGISTERED", "auth.user-registered",
                String.valueOf(user.getId()), String.valueOf(user.getId()),
                user.getDepartmentId(), payload);
    }
}