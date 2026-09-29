package com.platform.core.controller;

import com.platform.core.domain.Priority;
import com.platform.core.domain.WorkStatus;
import com.platform.core.dto.WorkItemDtos.*;
import com.platform.core.exception.ApiException;
import com.platform.core.security.CurrentUser;
import com.platform.core.service.WorkItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/work-items")
public class WorkItemController {

    private static final Set<String> SORTABLE = Set.of("id", "title", "status", "priority", "createdAt", "updatedAt");

    private final WorkItemService service;

    public WorkItemController(WorkItemService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_WORK_CREATE')")
    public ResponseEntity<WorkItemResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody CreateRequest req) {
        var created = service.create(CurrentUser.from(jwt), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkItemResponse.from(created));
    }

    @GetMapping
    public PageResponse<WorkItemResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) WorkStatus status,
            @RequestParam(required = false) Priority priority,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // Whitelist sort fields so clients can't sort on arbitrary columns.
        pageable.getSort().forEach(order -> {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SORT", "Cannot sort by " + order.getProperty());
            }
        });
        return PageResponse.of(service.list(CurrentUser.from(jwt), status, priority, pageable), WorkItemResponse::from);
    }

    @GetMapping("/{id}")
    public WorkItemResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return WorkItemResponse.from(service.get(CurrentUser.from(jwt), id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_WORK_EDIT')")
    public WorkItemResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @Valid @RequestBody StatusRequest req) {
        return WorkItemResponse.from(service.changeStatus(CurrentUser.from(jwt), id, req.status()));
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAuthority('PERM_WORK_ASSIGN')")
    public WorkItemResponse assign(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                   @Valid @RequestBody AssignRequest req) {
        return WorkItemResponse.from(service.assign(CurrentUser.from(jwt), id, req.assigneeId()));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('PERM_WORK_EDIT')")
    public ResponseEntity<CommentResponse> addComment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                      @Valid @RequestBody CommentRequest req) {
        var saved = service.addComment(CurrentUser.from(jwt), id, req.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(saved));
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> listComments(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.listComments(CurrentUser.from(jwt), id).stream().map(CommentResponse::from).toList();
    }
}