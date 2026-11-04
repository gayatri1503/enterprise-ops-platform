package com.platform.notification.events;

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

    public static final String QUEUE = "notification.events";

    @Bean
    public TopicExchange platformEventsExchange(@Value("${events.exchange:platform.events}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue notificationEventsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding assignedBinding(Queue notificationEventsQueue, TopicExchange platformEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(platformEventsExchange).with("work-item.assigned");
    }

    @Bean
    public Binding statusChangedBinding(Queue notificationEventsQueue, TopicExchange platformEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(platformEventsExchange).with("work-item.status-changed");
    }

    @Bean
    public Binding createdBinding(Queue notificationEventsQueue, TopicExchange platformEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(platformEventsExchange).with("work-item.created");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
