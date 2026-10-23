package com.platform.analytics.dto;

import com.platform.analytics.domain.WorkItemKpi;

import java.time.Instant;

/** Response body for GET /api/analytics/summary */
public record SummaryResponse(
        Long departmentId,
        long totalCreated,
        long totalCompleted,
        long totalInProgress,
        long totalBlocked,
        long loginCount,
        Instant updatedAt) {

    public static SummaryResponse from(WorkItemKpi kpi) {
        return new SummaryResponse(
                kpi.getDepartmentId() == WorkItemKpi.GLOBAL_DEPT_ID ? null : kpi.getDepartmentId(),
                kpi.getTotalCreated(),
                kpi.getTotalCompleted(),
                kpi.getTotalInProgress(),
                kpi.getTotalBlocked(),
                kpi.getLoginCount(),
                kpi.getUpdatedAt());
    }

    /** Zero-value response when no events have been received yet. */
    public static SummaryResponse empty(Long departmentId) {
        return new SummaryResponse(
                departmentId == WorkItemKpi.GLOBAL_DEPT_ID ? null : departmentId,
                0, 0, 0, 0, 0, null);
    }
}
