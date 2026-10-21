package com.platform.user.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE = "user.events";

    @Bean
    public TopicExchange platformEventsExchange(@Value("${events.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    /** Durable: messages wait here even while user-service is stopped. */
    @Bean
    public Queue userEventsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding userEventsBinding(Queue userEventsQueue, TopicExchange platformEventsExchange) {
        return BindingBuilder.bind(userEventsQueue).to(platformEventsExchange).with("auth.user-registered");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}