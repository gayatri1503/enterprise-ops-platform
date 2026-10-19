package com.platform.auth.events;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the shared exchange every service publishes to (see docs/events.md).
 * Consumers (analytics, notification) declare their own queues and bind them to it.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange platformEventsExchange(@Value("${events.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false); // durable, not auto-delete
    }

    /** Send events as JSON (readable by any service), not Java-serialized bytes. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}