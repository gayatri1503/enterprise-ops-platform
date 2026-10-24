package com.platform.user;

import com.platform.user.domain.UserProfile;
import com.platform.user.events.UserEventListener;
import com.platform.user.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserEventListenerTest {

    @Autowired UserEventListener listener;
    @Autowired UserProfileRepository profiles;
    @MockBean  RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("USER_REGISTERED event creates a new profile")
    void onUserRegistered_createsProfile() {
        UserEventListener.UserEvent event = event("USER_REGISTERED", "10",
                new UserEventListener.UserEvent.Payload("jane@example.com", "Jane Doe", "USER", 1L, true));

        listener.onUserEvent(event);

        UserProfile saved = profiles.findById(10L).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getFullName()).isEqualTo("Jane Doe");
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Repeated USER_REGISTERED event (idempotency) updates existing profile")
    void onUserRegistered_idempotent() {
        UserEventListener.UserEvent first = event("USER_REGISTERED", "11",
                new UserEventListener.UserEvent.Payload("bob@example.com", "Bob Old", "USER", 1L, true));
        listener.onUserEvent(first);

        UserEventListener.UserEvent second = event("USER_REGISTERED", "11",
                new UserEventListener.UserEvent.Payload("bob@example.com", "Bob New", "MANAGER", 1L, true));
        listener.onUserEvent(second);

        UserProfile saved = profiles.findById(11L).orElseThrow();
        assertThat(saved.getFullName()).isEqualTo("Bob New");
        assertThat(saved.getRole()).isEqualTo("MANAGER");
    }

    @Test
    @DisplayName("Non-USER_REGISTERED event types are ignored")
    void onOtherEvent_ignored() {
        UserEventListener.UserEvent event = event("USER_LOGIN_SUCCESS", "99",
                new UserEventListener.UserEvent.Payload("ghost@example.com", "Ghost", "USER", 1L, true));
        listener.onUserEvent(event);

        assertThat(profiles.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("Disabled user profile is correctly stored")
    void onUserRegistered_disabledUser() {
        UserEventListener.UserEvent event = event("USER_REGISTERED", "20",
                new UserEventListener.UserEvent.Payload("disabled@example.com", "Disabled User", "USER", 1L, false));
        listener.onUserEvent(event);

        UserProfile saved = profiles.findById(20L).orElseThrow();
        assertThat(saved.isEnabled()).isFalse();
    }

    private UserEventListener.UserEvent event(String type, String entityId,
                                               UserEventListener.UserEvent.Payload payload) {
        return new UserEventListener.UserEvent(java.util.UUID.randomUUID().toString(), type, entityId, payload);
    }
}
