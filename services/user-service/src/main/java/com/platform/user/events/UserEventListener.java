package com.platform.user.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.platform.user.domain.UserProfile;
import com.platform.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    /** The part of the envelope from docs/events.md that we care about. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserEvent(String eventId, String eventType, String entityId, Payload payload) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Payload(String email, String fullName, String role, Long departmentId, Boolean enabled) {}
    }

    private final UserProfileRepository profiles;

    public UserEventListener(UserProfileRepository profiles) {
        this.profiles = profiles;
    }

    /** Insert-or-update by user id, so a repeated event changes nothing. */
    @RabbitListener(queues = RabbitConfig.QUEUE)
    @Transactional
    public void onUserEvent(UserEvent event) {
        if (!"USER_REGISTERED".equals(event.eventType())) return;

        Long id = Long.valueOf(event.entityId());
        UserEvent.Payload p = event.payload();
        UserProfile profile = profiles.findById(id).orElseGet(() -> new UserProfile(id));
        profile.apply(p.email(), p.fullName(), p.role(), p.departmentId(), p.enabled() == null || p.enabled());
        profiles.save(profile);
        log.info("Synced user profile {} ({})", id, p.email());
    }
}