package com.platform.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AnalyticsServiceApplicationTests {

    @MockBean RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {}
}
