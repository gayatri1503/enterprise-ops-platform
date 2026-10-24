package com.platform.user;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceApplicationTests {

    // Prevent RabbitMQ auto-configuration wiring failures in context load
    @MockBean RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }
}
