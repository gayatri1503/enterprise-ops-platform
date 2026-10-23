package com.platform.core.service;

import com.platform.core.domain.*;
import com.platform.core.dto.WorkItemDtos.CreateRequest;
import com.platform.core.events.EventPublisher;
import com.platform.core.exception.ApiException;
import com.platform.core.repository.CommentRepository;
import com.platform.core.repository.WorkItemRepository;
import com.platform.core.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class WorkItemService {

    private final WorkItemRepository items;
    private final CommentRepository comments;
    private final EventPublisher events;

    public WorkItemService(WorkItemRepository items, CommentRepository comments, EventPublisher events) {
        this.items = items;
        this.comments = comments;
        this.events = events;
    }

    @Transactional
    public WorkItem create(CurrentUser user, CreateRequest req) {
        WorkItem item = items.save(new WorkItem(
                req.title().trim(), req.description(), req.priority(), user.id(), user.departmentId()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", item.getTitle());
        payload.put("priority", item.getPriority().name());
        payload.put("status", item.getStatus().name());
        payload.put("assigneeId", item.getAssigneeId()); // null when unassigned
        events.publish("WORK_ITEM_CREATED", "work-item.created",
                String.valueOf(item.getId()), String.valueOf(user.id()), item.getDepartmentId(), payload);
        return item;
    }

    @Transactional(readOnly = true)
    public Page<WorkItem> list(CurrentUser user, WorkStatus status, Priority priority, Pageable pageable) {
        return items.findAll(WorkItemSpecs.visibleAndFiltered(user, status, priority), pageable);
    }

    @Transactional(readOnly = true)
    public WorkItem get(CurrentUser user, Long id) {
        return loadVisible(user, id);
    }

    @Transactional
    public WorkItem changeStatus(CurrentUser user, Long id, WorkStatus next) {
        WorkItem item = loadVisible(user, id);
        if (!item.getStatus().canMoveTo(next)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "Cannot move from " + item.getStatus() + " to " + next);
        }
        WorkStatus from = item.getStatus();
        item.changeStatus(next); // dirty checking saves it on commit

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fromStatus", from.name());
        payload.put("toStatus", next.name());
        payload.put("completedAt", item.getCompletedAt() == null ? null : item.getCompletedAt().toString());
        events.publish("WORK_ITEM_STATUS_CHANGED", "work-item.status-changed",
                String.valueOf(item.getId()), String.valueOf(user.id()), item.getDepartmentId(), payload);
        return item;
    }

    @Transactional
    public WorkItem assign(CurrentUser user, Long id, Long assigneeId) {
        WorkItem item = loadVisible(user, id);
        // MANAGER may only assign within their own department (API contract: "WORK_ASSIGN: own dept")
        if ("MANAGER".equals(user.role()) && !Objects.equals(user.departmentId(), item.getDepartmentId())) {
            throw new AccessDeniedException("Managers can only assign within their own department");
        }
        Long previous = item.getAssigneeId();
        if (Objects.equals(previous, assigneeId)) {
            return item; // nothing changed, so no event
        }
        item.assignTo(assigneeId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assigneeId", assigneeId);
        payload.put("previousAssigneeId", previous); // null if it was unassigned
        events.publish("WORK_ITEM_ASSIGNED", "work-item.assigned",
                String.valueOf(item.getId()), String.valueOf(user.id()), item.getDepartmentId(), payload);
        return item;
    }

    @Transactional
    public WorkComment addComment(CurrentUser user, Long id, String body) {
        loadVisible(user, id);
        return comments.save(new WorkComment(id, user.id(), body.trim()));
    }

    @Transactional(readOnly = true)
    public List<WorkComment> listComments(CurrentUser user, Long id) {
        loadVisible(user, id);
        return comments.findByWorkItemIdOrderByCreatedAtAsc(id);
    }

    private WorkItem loadVisible(CurrentUser user, Long id) {
        WorkItem item = items.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Work item not found"));
        if (!canSee(user, item)) {
            throw new AccessDeniedException("Outside your data scope");
        }
        return item;
    }

    private boolean canSee(CurrentUser user, WorkItem item) {
        return switch (user.scope()) {
            case ALL -> true;
            case DEPARTMENT -> Objects.equals(user.departmentId(), item.getDepartmentId());
            case OWN -> user.id().equals(item.getCreatorId()) || user.id().equals(item.getAssigneeId());
        };
    }
}