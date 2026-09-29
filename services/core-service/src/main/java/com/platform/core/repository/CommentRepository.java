package com.platform.core.repository;

import com.platform.core.domain.WorkComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<WorkComment, Long> {
    List<WorkComment> findByWorkItemIdOrderByCreatedAtAsc(Long workItemId);
}