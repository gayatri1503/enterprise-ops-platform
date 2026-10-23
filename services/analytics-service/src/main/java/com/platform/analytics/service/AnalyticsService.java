package com.platform.analytics.service;

import com.platform.analytics.domain.WorkItemKpi;
import com.platform.analytics.dto.SummaryResponse;
import com.platform.analytics.repository.WorkItemKpiRepository;
import com.platform.analytics.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final WorkItemKpiRepository kpis;

    public AnalyticsService(WorkItemKpiRepository kpis) {
        this.kpis = kpis;
    }

    /**
     * Returns the KPI summary scoped to the caller:
     * - ADMIN / AUDITOR → global aggregate (departmentId = 0)
     * - MANAGER / USER  → their own department
     */
    @Transactional(readOnly = true)
    public SummaryResponse summary(CurrentUser user) {
        Long deptId = user.isGlobalScope()
                ? WorkItemKpi.GLOBAL_DEPT_ID
                : user.departmentId();

        return kpis.findByDepartmentId(deptId)
                .map(SummaryResponse::from)
                .orElse(SummaryResponse.empty(deptId));
    }
}
