package com.platform.analytics.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.platform.analytics.domain.ProcessedEvent;
import com.platform.analytics.domain.WorkItemKpi;
import com.platform.analytics.repository.ProcessedEventRepository;
import com.platform.analytics.repository.WorkItemKpiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Consumes all events from the analytics.events queue (work-item.# and auth.#).
 * Idempotency: each eventId is stored in processed_events; duplicates are silently dropped.
 */
@Component
public class AnalyticsEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventListener.class);

    /** Partial view of the standard event envelope from docs/events.md. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventEnvelope(String eventId, String eventType,
                                String entityId, String actorId, String departmentId,
                                Map<String, Object> payload) {}

    private final ProcessedEventRepository processedEvents;
    private final WorkItemKpiRepository kpis;

    public AnalyticsEventListener(ProcessedEventRepository processedEvents,
                                  WorkItemKpiRepository kpis) {
        this.processedEvents = processedEvents;
        this.kpis = kpis;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    @Transactional
    public void onEvent(EventEnvelope event) {
        if (event.eventId() == null) {
            log.warn("Received event with null eventId — dropping");
            return;
        }
        if (event.eventType() == null) {
            log.warn("Received event {} with null eventType — dropping", event.eventId());
            return;
        }
        // Idempotency check: ignore if already processed
        if (processedEvents.existsByEventId(event.eventId())) {
            log.debug("Duplicate event {} ({}) — skipped", event.eventId(), event.eventType());
            return;
        }
        processedEvents.save(new ProcessedEvent(event.eventId(), event.eventType()));

        switch (event.eventType()) {
            case "WORK_ITEM_CREATED"       -> handleCreated(event);
            case "WORK_ITEM_STATUS_CHANGED" -> handleStatusChanged(event);
            case "USER_LOGIN_SUCCESS"       -> handleLoginSuccess(event);
            default -> log.debug("Unhandled event type: {}", event.eventType());
        }
    }

    private void handleCreated(EventEnvelope event) {
        Long deptId = parseDept(event.departmentId());
        kpiFor(deptId).recordCreated();
        kpiFor(WorkItemKpi.GLOBAL_DEPT_ID).recordCreated();
    }

    private void handleStatusChanged(EventEnvelope event) {
        if (event.payload() == null) return;
        String from = str(event.payload().get("fromStatus"));
        String to   = str(event.payload().get("toStatus"));
        if (from == null || to == null) return;

        Long deptId = parseDept(event.departmentId());
        kpiFor(deptId).recordStatusChange(from, to);
        kpiFor(WorkItemKpi.GLOBAL_DEPT_ID).recordStatusChange(from, to);
    }

    private void handleLoginSuccess(EventEnvelope event) {
        Long deptId = parseDept(event.departmentId());
        kpiFor(deptId).recordLogin();
        kpiFor(WorkItemKpi.GLOBAL_DEPT_ID).recordLogin();
    }

    /** Load-or-create a KPI row for the given departmentId within this transaction. */
    private WorkItemKpi kpiFor(Long deptId) {
        return kpis.findByDepartmentId(deptId)
                .orElseGet(() -> kpis.save(new WorkItemKpi(deptId)));
    }

    private static Long parseDept(String value) {
        if (value == null) return WorkItemKpi.GLOBAL_DEPT_ID;
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { return WorkItemKpi.GLOBAL_DEPT_ID; }
    }

    private static String str(Object o) {
        return o instanceof String s ? s : null;
    }
}
