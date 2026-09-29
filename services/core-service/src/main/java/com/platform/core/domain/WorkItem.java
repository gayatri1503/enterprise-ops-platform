package com.platform.core.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "work_items", indexes = {
        @Index(name = "idx_wi_status", columnList = "status"),
        @Index(name = "idx_wi_dept", columnList = "department_id"),
        @Index(name = "idx_wi_creator", columnList = "creator_id"),
        @Index(name = "idx_wi_assignee", columnList = "assignee_id")
})
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkStatus status = WorkStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    protected WorkItem() {}

    public WorkItem(String title, String description, Priority priority, Long creatorId, Long departmentId) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.creatorId = creatorId;
        this.departmentId = departmentId;
    }

    public void changeStatus(WorkStatus next) {
        this.status = next;
        this.updatedAt = Instant.now();
        this.completedAt = (next == WorkStatus.COMPLETED) ? this.updatedAt : null;
    }

    public void assignTo(Long assigneeId) {
        this.assigneeId = assigneeId;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public WorkStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public Long getCreatorId() { return creatorId; }
    public Long getAssigneeId() { return assigneeId; }
    public Long getDepartmentId() { return departmentId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
}