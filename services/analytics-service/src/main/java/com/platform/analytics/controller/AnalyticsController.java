package com.platform.analytics.controller;

import com.platform.analytics.dto.SummaryResponse;
import com.platform.analytics.security.CurrentUser;
import com.platform.analytics.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * GET /api/analytics/summary
     * Access: ANALYTICS_VIEW (ADMIN, MANAGER, USER, AUDITOR all have this permission).
     * Scoping: ADMIN/AUDITOR → global; MANAGER/USER → own department.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS_VIEW')")
    public SummaryResponse summary(@AuthenticationPrincipal Jwt jwt) {
        return analyticsService.summary(CurrentUser.from(jwt));
    }
}
