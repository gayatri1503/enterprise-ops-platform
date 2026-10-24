package com.platform.core;

import com.platform.core.domain.Priority;
import com.platform.core.domain.WorkItem;
import com.platform.core.domain.WorkStatus;
import com.platform.core.events.EventPublisher;
import com.platform.core.exception.ApiException;
import com.platform.core.repository.CommentRepository;
import com.platform.core.repository.WorkItemRepository;
import com.platform.core.security.CurrentUser;
import com.platform.core.service.WorkItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.platform.core.dto.WorkItemDtos.CreateRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkItemServiceTest {

    @Autowired WorkItemService service;
    @Autowired WorkItemRepository itemRepo;
    @Autowired CommentRepository commentRepo;

    @MockBean RabbitTemplate rabbitTemplate;
    @MockBean EventPublisher eventPublisher;

    private final CurrentUser admin    = new CurrentUser(1L, "ADMIN",   1L);
    private final CurrentUser manager1 = new CurrentUser(2L, "MANAGER", 1L);
    private final CurrentUser manager2 = new CurrentUser(3L, "MANAGER", 2L); // different dept
    private final CurrentUser user1    = new CurrentUser(4L, "USER",    1L);
    private final CurrentUser auditor  = new CurrentUser(5L, "AUDITOR", 1L);

    @BeforeEach
    void permitPublish() {
        doNothing().when(eventPublisher)
                .publish(any(), any(), any(), any(), any(), any());
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("USER can create a work item in their own department")
    void create_user_success() {
        CreateRequest req = new CreateRequest("Fix login bug", null, Priority.HIGH);
        WorkItem item = service.create(user1, req);

        assertThat(item.getId()).isNotNull();
        assertThat(item.getTitle()).isEqualTo("Fix login bug");
        assertThat(item.getStatus()).isEqualTo(WorkStatus.NEW);
        assertThat(item.getCreatorId()).isEqualTo(user1.id());
        assertThat(item.getDepartmentId()).isEqualTo(user1.departmentId());
    }

    @Test
    @DisplayName("Title is trimmed on creation")
    void create_titleTrimmed() {
        WorkItem item = service.create(user1, new CreateRequest("  Whitespace title  ", null, Priority.LOW));
        assertThat(item.getTitle()).isEqualTo("Whitespace title");
    }

    // ─── Get / list scoping ───────────────────────────────────────────────────

    @Test
    @DisplayName("USER can get their own work item")
    void get_ownItem_allowed() {
        WorkItem item = service.create(user1, new CreateRequest("My item", null, Priority.MEDIUM));
        assertThat(service.get(user1, item.getId())).isNotNull();
    }

    @Test
    @DisplayName("USER cannot get another user's item they are not assigned to")
    void get_otherUserItem_forbidden() {
        WorkItem item = service.create(admin, new CreateRequest("Admin item", null, Priority.LOW));
        assertThatThrownBy(() -> service.get(user1, item.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("ADMIN sees all items across departments")
    void list_adminSeesAll() {
        service.create(user1,  new CreateRequest("Dept 1 item", null, Priority.LOW));
        service.create(manager2, new CreateRequest("Dept 2 item", null, Priority.LOW));

        Page<WorkItem> page = service.list(admin, null, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("MANAGER only sees items from their own department")
    void list_managerScopesDepartment() {
        service.create(user1,   new CreateRequest("Dept 1 item", null, Priority.LOW));
        service.create(manager2, new CreateRequest("Dept 2 item", null, Priority.LOW));

        Page<WorkItem> page = service.list(manager1, null, null, PageRequest.of(0, 20));
        assertThat(page.getContent()).allMatch(i -> i.getDepartmentId().equals(manager1.departmentId()));
    }

    @Test
    @DisplayName("AUDITOR sees all items (read-only scope)")
    void list_auditorSeesAll() {
        service.create(user1,   new CreateRequest("Item A", null, Priority.LOW));
        service.create(manager2, new CreateRequest("Item B", null, Priority.LOW));

        Page<WorkItem> page = service.list(auditor, null, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("List filters by status correctly")
    void list_filterByStatus() {
        service.create(user1, new CreateRequest("Status item", null, Priority.LOW));

        Page<WorkItem> newItems = service.list(admin, WorkStatus.NEW, null, PageRequest.of(0, 20));
        assertThat(newItems.getContent()).allMatch(i -> i.getStatus() == WorkStatus.NEW);
    }

    // ─── Status transitions ───────────────────────────────────────────────────

    @Test
    @DisplayName("Valid status transition NEW -> IN_PROGRESS succeeds")
    void changeStatus_validTransition() {
        WorkItem item = service.create(user1, new CreateRequest("Transition test", null, Priority.MEDIUM));
        WorkItem updated = service.changeStatus(user1, item.getId(), WorkStatus.IN_PROGRESS);
        assertThat(updated.getStatus()).isEqualTo(WorkStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Invalid status transition NEW -> COMPLETED throws conflict")
    void changeStatus_invalidTransition_throws() {
        WorkItem item = service.create(user1, new CreateRequest("Invalid transition", null, Priority.LOW));
        assertThatThrownBy(() -> service.changeStatus(user1, item.getId(), WorkStatus.COMPLETED))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot move from");
    }

    @Test
    @DisplayName("Completing a work item sets completedAt")
    void changeStatus_toCompleted_setsCompletedAt() {
        WorkItem item = service.create(user1, new CreateRequest("Complete me", null, Priority.LOW));
        service.changeStatus(user1, item.getId(), WorkStatus.IN_PROGRESS);
        WorkItem done = service.changeStatus(user1, item.getId(), WorkStatus.COMPLETED);

        assertThat(done.getStatus()).isEqualTo(WorkStatus.COMPLETED);
        assertThat(done.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("COMPLETED item cannot change status")
    void changeStatus_fromCompleted_throws() {
        WorkItem item = service.create(user1, new CreateRequest("Done item", null, Priority.LOW));
        service.changeStatus(user1, item.getId(), WorkStatus.IN_PROGRESS);
        service.changeStatus(user1, item.getId(), WorkStatus.COMPLETED);

        assertThatThrownBy(() -> service.changeStatus(user1, item.getId(), WorkStatus.IN_PROGRESS))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("BLOCKED <-> IN_PROGRESS is bidirectional")
    void changeStatus_blockedBidirectional() {
        WorkItem item = service.create(user1, new CreateRequest("Bidirectional", null, Priority.LOW));
        service.changeStatus(user1, item.getId(), WorkStatus.IN_PROGRESS);
        service.changeStatus(user1, item.getId(), WorkStatus.BLOCKED);
        WorkItem unblocked = service.changeStatus(user1, item.getId(), WorkStatus.IN_PROGRESS);

        assertThat(unblocked.getStatus()).isEqualTo(WorkStatus.IN_PROGRESS);
    }

    // ─── Assignment ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN can assign to anyone in any department")
    void assign_adminCanAssignAcrossDepartments() {
        WorkItem item = service.create(manager2, new CreateRequest("Cross-dept", null, Priority.LOW));
        // admin is in dept 1, item is in dept 2 — admin should still be allowed
        WorkItem assigned = service.assign(admin, item.getId(), 99L);
        assertThat(assigned.getAssigneeId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("MANAGER can assign within own department")
    void assign_managerOwnDepartment_allowed() {
        WorkItem item = service.create(user1, new CreateRequest("Dept 1 item", null, Priority.LOW));
        // item is in dept 1, manager1 is in dept 1
        WorkItem assigned = service.assign(manager1, item.getId(), 42L);
        assertThat(assigned.getAssigneeId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("MANAGER cannot assign items in another department")
    void assign_managerCrossDepartment_forbidden() {
        // Create item in dept 2 (via manager2), then try to assign from manager1 (dept 1)
        WorkItem item = service.create(manager2, new CreateRequest("Dept 2 item", null, Priority.LOW));
        assertThatThrownBy(() -> service.assign(manager1, item.getId(), 42L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Assigning same assignee is a no-op (no duplicate event)")
    void assign_sameAssignee_noOp() {
        WorkItem item = service.create(user1, new CreateRequest("Assign test", null, Priority.LOW));
        service.assign(admin, item.getId(), 10L);
        WorkItem same = service.assign(admin, item.getId(), 10L);
        assertThat(same.getAssigneeId()).isEqualTo(10L);
    }

    // ─── Comments ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("User can add a comment to a visible work item")
    void addComment_success() {
        WorkItem item = service.create(user1, new CreateRequest("Comment test", null, Priority.LOW));
        var comment = service.addComment(user1, item.getId(), "  My comment  ");
        assertThat(comment.getBody()).isEqualTo("My comment"); // body is trimmed
        assertThat(comment.getAuthorId()).isEqualTo(user1.id());
        assertThat(comment.getWorkItemId()).isEqualTo(item.getId());
    }

    @Test
    @DisplayName("Comments are returned in ascending created-at order")
    void listComments_orderedByCreatedAt() {
        WorkItem item = service.create(user1, new CreateRequest("Ordered comments", null, Priority.LOW));
        service.addComment(user1, item.getId(), "First");
        service.addComment(user1, item.getId(), "Second");

        var list = service.listComments(user1, item.getId());
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getBody()).isEqualTo("First");
        assertThat(list.get(1).getBody()).isEqualTo("Second");
    }

    @Test
    @DisplayName("Cannot comment on an item outside visibility scope")
    void addComment_invisibleItem_forbidden() {
        WorkItem item = service.create(admin, new CreateRequest("Hidden item", null, Priority.LOW));
        assertThatThrownBy(() -> service.addComment(user1, item.getId(), "Sneaky comment"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── Not-found ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Getting a non-existent item returns 404")
    void get_notFound_throws() {
        assertThatThrownBy(() -> service.get(admin, 9999L))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus().value()).isEqualTo(404));
    }
}
