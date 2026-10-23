package com.platform.analytics.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Idempotency record: stores every eventId we have already processed so that
 * a redelivered message (RabbitMQ at-least-once) is silently ignored.
 * docs/events.md: "Consumers must be idempotent."
 */
@Entity
@Table(name = "processed_events",
       indexes = @Index(name = "idx_pe_event_id", columnList = "event_id", unique = true))
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    protected ProcessedEvent() {}

    public ProcessedEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public String getEventId() { return eventId; }
}
