package com.platform.analytics.repository;

import com.platform.analytics.domain.WorkItemKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkItemKpiRepository extends JpaRepository<WorkItemKpi, Long> {
    Optional<WorkItemKpi> findByDepartmentId(Long departmentId);
}
