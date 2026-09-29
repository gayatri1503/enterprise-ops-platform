package com.platform.core.service;

import com.platform.core.domain.Priority;
import com.platform.core.domain.WorkItem;
import com.platform.core.domain.WorkStatus;
import com.platform.core.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class WorkItemSpecs {

    private WorkItemSpecs() {}

    public static Specification<WorkItem> visibleAndFiltered(CurrentUser user, WorkStatus status, Priority priority) {
        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();

            switch (user.scope()) {
                case DEPARTMENT -> where.add(cb.equal(root.get("departmentId"), user.departmentId()));
                case OWN -> where.add(cb.or(
                        cb.equal(root.get("creatorId"), user.id()),
                        cb.equal(root.get("assigneeId"), user.id())));
                case ALL -> { }
            }
            if (status != null) where.add(cb.equal(root.get("status"), status));
            if (priority != null) where.add(cb.equal(root.get("priority"), priority));

            return cb.and(where.toArray(new Predicate[0]));
        };
    }
}