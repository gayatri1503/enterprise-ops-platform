package com.platform.analytics;

import com.platform.analytics.domain.WorkItemKpi;
import com.platform.analytics.events.AnalyticsEventListener;
import com.platform.analytics.events.AnalyticsEventListener.EventEnvelope;
import com.platform.analytics.repository.ProcessedEventRepository;
import com.platform.analytics.repository.WorkItemKpiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalyticsEventListenerTest {

    @Autowired AnalyticsEventListener listener;
    @Autowired WorkItemKpiRepository kpiRepo;
    @Autowired ProcessedEventRepository processedRepo;

    @MockBean RabbitTemplate rabbitTemplate;

    // ─── WORK_ITEM_CREATED ────────────────────────────────────────────────────

    @Test
    @DisplayName("WORK_ITEM_CREATED increments totalCreated for dept and global")
    void workItemCreated_incrementsBothCounters() {
        listener.onEvent(event("WORK_ITEM_CREATED", "7", Map.of("title", "Bug fix", "priority", "HIGH")));

        WorkItemKpi dept   = kpiFor(7L);
        WorkItemKpi global = kpiFor(WorkItemKpi.GLOBAL_DEPT_ID);

        assertThat(dept.getTotalCreated()).isEqualTo(1);
        assertThat(global.getTotalCreated()).isEqualTo(1);
    }

    // ─── WORK_ITEM_STATUS_CHANGED ─────────────────────────────────────────────

    @Test
    @DisplayName("Status change NEW→IN_PROGRESS increments inProgress counter")
    void statusChange_newToInProgress() {
        listener.onEvent(event("WORK_ITEM_CREATED", "5", Map.of()));
        listener.onEvent(event("WORK_ITEM_STATUS_CHANGED", "5",
                Map.of("fromStatus", "NEW", "toStatus", "IN_PROGRESS")));

        assertThat(kpiFor(5L).getTotalInProgress()).isEqualTo(1);
        assertThat(kpiFor(5L).getTotalCompleted()).isEqualTo(0);
    }

    @Test
    @DisplayName("Status change IN_PROGRESS→COMPLETED increments completed, decrements inProgress")
    void statusChange_inProgressToCompleted() {
        listener.onEvent(event("WORK_ITEM_CREATED", "5", Map.of()));
        listener.onEvent(event("WORK_ITEM_STATUS_CHANGED", "5",
                Map.of("fromStatus", "NEW", "toStatus", "IN_PROGRESS")));
        listener.onEvent(event("WORK_ITEM_STATUS_CHANGED", "5",
                Map.of("fromStatus", "IN_PROGRESS", "toStatus", "COMPLETED")));

        WorkItemKpi kpi = kpiFor(5L);
        assertThat(kpi.getTotalCompleted()).isEqualTo(1);
        assertThat(kpi.getTotalInProgress()).isEqualTo(0);
    }

    @Test
    @DisplayName("BLOCKED <-> IN_PROGRESS transitions keep counters consistent")
    void statusChange_blockedBidirectional() {
        listener.onEvent(event("WORK_ITEM_CREATED", "5", Map.of()));
        listener.onEvent(event("WORK_ITEM_STATUS_CHANGED", "5",
                Map.of("fromStatus", "NEW", "toStatus", "IN_PROGRESS")));
        listener.onEvent(event("WORK_ITEM_STATUS_CHANGED", "5",
                Map.of("fromStatus", "IN_PROGRESS", "toStatus", "BLOCKED")));
        listener.onEvent(event("WORK_ITEM_STATUS_CHANGED", "5",
                Map.of("fromStatus", "BLOCKED", "toStatus", "IN_PROGRESS")));

        WorkItemKpi kpi = kpiFor(5L);
        assertThat(kpi.getTotalBlocked()).isEqualTo(0);
        assertThat(kpi.getTotalInProgress()).isEqualTo(1);
    }

    // ─── USER_LOGIN_SUCCESS ───────────────────────────────────────────────────

    @Test
    @DisplayName("USER_LOGIN_SUCCESS increments loginCount for dept and global")
    void loginSuccess_incrementsLoginCount() {
        listener.onEvent(event("USER_LOGIN_SUCCESS", "3", Map.of("ip", "127.0.0.1")));

        assertThat(kpiFor(3L).getLoginCount()).isEqualTo(1);
        assertThat(kpiFor(WorkItemKpi.GLOBAL_DEPT_ID).getLoginCount()).isEqualTo(1);
    }

    // ─── Idempotency ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Duplicate eventId is silently ignored (idempotency)")
    void idempotency_duplicateDropped() {
        String id = UUID.randomUUID().toString();
        EventEnvelope e = new EventEnvelope(id, "WORK_ITEM_CREATED", "1", "1", "9", Map.of());

        listener.onEvent(e);
        listener.onEvent(e); // replay

        assertThat(kpiFor(9L).getTotalCreated()).isEqualTo(1); // not 2
        assertThat(processedRepo.existsByEventId(id)).isTrue();
    }

    @Test
    @DisplayName("Null eventId is dropped without throwing")
    void nullEventId_droppedSafely() {
        EventEnvelope e = new EventEnvelope(null, "WORK_ITEM_CREATED", "1", "1", "9", Map.of());
        // should not throw
        listener.onEvent(e);
        // no KPI row created
        assertThat(kpiRepo.findByDepartmentId(9L)).isEmpty();
    }

    @Test
    @DisplayName("Null eventType is dropped without throwing or corrupting KPIs")
    void nullEventType_droppedSafely() {
        EventEnvelope e = new EventEnvelope(UUID.randomUUID().toString(), null, "1", "1", "9", Map.of());
        // should not throw, must not attempt a DB save with null event_type
        listener.onEvent(e);
        // no KPI row created, no processed_event row created
        assertThat(kpiRepo.findByDepartmentId(9L)).isEmpty();
        assertThat(processedRepo.count()).isEqualTo(0);
    }

    // ─── Unknown event types ──────────────────────────────────────────────────

    @Test
    @DisplayName("Unknown event types are stored for idempotency but do not change KPIs")
    void unknownEventType_ignored() {
        listener.onEvent(event("USER_REGISTERED", "2", Map.of("email", "x@x.com")));
        // no crash, no KPI change for dept 2
        assertThat(kpiRepo.findByDepartmentId(2L)).isEmpty();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private EventEnvelope event(String type, String deptId, Map<String, Object> payload) {
        return new EventEnvelope(UUID.randomUUID().toString(), type, "42", "1", deptId, payload);
    }

    private WorkItemKpi kpiFor(Long deptId) {
        return kpiRepo.findByDepartmentId(deptId)
                .orElseThrow(() -> new AssertionError("No KPI row for dept " + deptId));
    }
}
