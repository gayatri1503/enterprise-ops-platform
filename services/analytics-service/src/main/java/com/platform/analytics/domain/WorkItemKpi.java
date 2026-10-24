package com.platform.analytics.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One row per department holding running counters for the /analytics/summary endpoint.
 * departmentId = 0 is the global (all-departments) aggregate used by ADMIN and AUDITOR.
 */
@Entity
@Table(name = "work_item_kpis",
       indexes = @Index(name = "idx_kpi_dept", columnList = "department_id", unique = true))
public class WorkItemKpi {

    public static final long GLOBAL_DEPT_ID = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 0 = global aggregate; any other value = per-department. */
    @Column(name = "department_id", nullable = false, unique = true)
    private Long departmentId;

    @Column(name = "total_created", nullable = false)
    private long totalCreated = 0;

    @Column(name = "total_completed", nullable = false)
    private long totalCompleted = 0;

    @Column(name = "total_in_progress", nullable = false)
    private long totalInProgress = 0;

    @Column(name = "total_blocked", nullable = false)
    private long totalBlocked = 0;

    @Column(name = "login_count", nullable = false)
    private long loginCount = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected WorkItemKpi() {}

    public WorkItemKpi(Long departmentId) {
        this.departmentId = departmentId;
    }

    public void recordCreated() {
        totalCreated++;
        totalInProgress = Math.max(0, totalInProgress); // no-op guard
        touch();
    }

    /**
     * Adjusts in-progress / blocked / completed counters when a status transition arrives.
     * We increment the target bucket and decrement the source bucket.
     */
    public void recordStatusChange(String fromStatus, String toStatus) {
        decrement(fromStatus);
        increment(toStatus);
        if ("COMPLETED".equals(toStatus)) totalCompleted++;
        touch();
    }

    public void recordLogin() {
        loginCount++;
        touch();
    }

    private void increment(String status) {
        switch (status) {
            case "IN_PROGRESS" -> totalInProgress++;
            case "BLOCKED"     -> totalBlocked++;
            // NEW → nothing extra; COMPLETED handled above
        }
    }

    private void decrement(String status) {
        switch (status) {
            case "NEW"         -> { /* first transition out of NEW: nothing to subtract */ }
            case "IN_PROGRESS" -> totalInProgress = Math.max(0, totalInProgress - 1);
            case "BLOCKED"     -> totalBlocked     = Math.max(0, totalBlocked - 1);
        }
    }

    private void touch() { updatedAt = Instant.now(); }

    public Long getDepartmentId()   { return departmentId; }
    public long getTotalCreated()   { return totalCreated; }
    public long getTotalCompleted() { return totalCompleted; }
    public long getTotalInProgress(){ return totalInProgress; }
    public long getTotalBlocked()   { return totalBlocked; }
    public long getLoginCount()     { return loginCount; }
    public Instant getUpdatedAt()   { return updatedAt; }
}
