package com.platform.core.service;

import com.platform.core.domain.*;
import com.platform.core.dto.WorkItemDtos.CreateRequest;
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

import java.util.List;
import java.util.Objects;

@Service
public class WorkItemService {

    private final WorkItemRepository items;
    private final CommentRepository comments;

    public WorkItemService(WorkItemRepository items, CommentRepository comments) {
        this.items = items;
        this.comments = comments;
    }

    @Transactional
    public WorkItem create(CurrentUser user, CreateRequest req) {
        return items.save(new WorkItem(
                req.title().trim(), req.description(), req.priority(), user.id(), user.departmentId()));
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
        item.changeStatus(next); // dirty checking saves it on commit
        return item;
    }

    @Transactional
    public WorkItem assign(CurrentUser user, Long id, Long assigneeId) {
        WorkItem item = loadVisible(user, id);
        // TODO (with user-service): verify the assignee exists and belongs to the same department.
        item.assignTo(assigneeId);
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