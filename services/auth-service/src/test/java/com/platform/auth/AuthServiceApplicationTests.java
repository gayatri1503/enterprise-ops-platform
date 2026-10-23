package com.platform.auth;

import com.platform.auth.events.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    // Prevent the DataSeeder from blowing up without RabbitMQ
    @MockBean EventPublisher eventPublisher;

    @Test
    void contextLoads() {
    }
}
