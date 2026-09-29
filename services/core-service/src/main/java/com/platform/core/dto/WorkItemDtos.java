package com.platform.core.dto;

import com.platform.core.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

public final class WorkItemDtos {

    private WorkItemDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @NotNull Priority priority) {}

    public record StatusRequest(@NotNull WorkStatus status) {}

    public record AssignRequest(@NotNull Long assigneeId) {}

    public record CommentRequest(@NotBlank @Size(max = 2000) String body) {}

    public record WorkItemResponse(Long id, String title, String description, WorkStatus status,
                                   Priority priority, Long creatorId, Long assigneeId, Long departmentId,
                                   Instant createdAt, Instant updatedAt, Instant completedAt) {
        public static WorkItemResponse from(WorkItem w) {
            return new WorkItemResponse(w.getId(), w.getTitle(), w.getDescription(), w.getStatus(),
                    w.getPriority(), w.getCreatorId(), w.getAssigneeId(), w.getDepartmentId(),
                    w.getCreatedAt(), w.getUpdatedAt(), w.getCompletedAt());
        }
    }

    public record CommentResponse(Long id, Long workItemId, Long authorId, String body, Instant createdAt) {
        public static CommentResponse from(WorkComment c) {
            return new CommentResponse(c.getId(), c.getWorkItemId(), c.getAuthorId(), c.getBody(), c.getCreatedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}