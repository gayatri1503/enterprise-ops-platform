package com.platform.analytics.events;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /** Matches docs/events.md queue table: "analytics.events" binds work-item.# and auth.# */
    public static final String QUEUE = "analytics.events";

    @Bean
    public TopicExchange platformEventsExchange(@Value("${events.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    public Queue analyticsEventsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding workItemBinding(Queue analyticsEventsQueue, TopicExchange platformEventsExchange) {
        return BindingBuilder.bind(analyticsEventsQueue).to(platformEventsExchange).with("work-item.#");
    }

    @Bean
    public Binding authBinding(Queue analyticsEventsQueue, TopicExchange platformEventsExchange) {
        return BindingBuilder.bind(analyticsEventsQueue).to(platformEventsExchange).with("auth.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
