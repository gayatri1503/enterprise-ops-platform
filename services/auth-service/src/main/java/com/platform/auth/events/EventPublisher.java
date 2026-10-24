package com.platform.auth.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Builds the standard event envelope from docs/events.md and sends it. */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public EventPublisher(RabbitTemplate rabbitTemplate, @Value("${events.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    public void publish(String eventType, String routingKey, String entityId, String actorId,
                        Long departmentId, Map<String, Object> payload) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", eventType);
        envelope.put("version", 1);
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("producer", "auth-service");
        envelope.put("entityId", entityId);
        envelope.put("actorId", actorId);
        envelope.put("departmentId", departmentId == null ? null : String.valueOf(departmentId));
        envelope.put("payload", payload);

        // docs/events.md rule: publish only AFTER the DB transaction commits.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(routingKey, envelope);
                }
            });
        } else {
            send(routingKey, envelope);
        }
    }

    private void send(String routingKey, Map<String, Object> envelope) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, envelope);
        } catch (Exception e) {
            // An audit event must never break login. Log it and move on.
            log.warn("Could not publish {} event: {}", routingKey, e.getMessage());
        }
    }
}