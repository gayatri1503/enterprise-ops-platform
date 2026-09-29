package com.platform.core.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "work_comments", indexes = @Index(name = "idx_wc_item", columnList = "work_item_id"))
public class WorkComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_item_id", nullable = false)
    private Long workItemId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected WorkComment() {}

    public WorkComment(Long workItemId, Long authorId, String body) {
        this.workItemId = workItemId;
        this.authorId = authorId;
        this.body = body;
    }

    public Long getId() { return id; }
    public Long getWorkItemId() { return workItemId; }
    public Long getAuthorId() { return authorId; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}