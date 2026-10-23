package com.platform.analytics;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.platform.analytics.domain.WorkItemKpi;
import com.platform.analytics.events.AnalyticsEventListener;
import com.platform.analytics.repository.WorkItemKpiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalyticsControllerTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";

    @Autowired MockMvc mvc;
    @Autowired WorkItemKpiRepository kpiRepo;
    @Autowired AnalyticsEventListener listener;
    @MockBean  RabbitTemplate rabbitTemplate;

    // ─── /api/analytics/summary ───────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN gets global summary (zero when no events)")
    void summary_admin_globalScope_empty() throws Exception {
        mvc.perform(get("/api/analytics/summary")
                        .header("Authorization", "Bearer " + token("ADMIN",
                                List.of("ANALYTICS_VIEW", "ANALYTICS_GLOBAL"), 1L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreated").value(0))
                .andExpect(jsonPath("$.departmentId").doesNotExist()); // null → omitted
    }

    @Test
    @DisplayName("ADMIN sees global aggregate after events arrive")
    void summary_admin_globalAggregate() throws Exception {
        // Seed two departments via the listener
        listener.onEvent(envelope("WORK_ITEM_CREATED", "1", Map.of()));
        listener.onEvent(envelope("WORK_ITEM_CREATED", "2", Map.of()));

        mvc.perform(get("/api/analytics/summary")
                        .header("Authorization", "Bearer " + token("ADMIN",
                                List.of("ANALYTICS_VIEW", "ANALYTICS_GLOBAL"), 1L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreated").value(2));
    }

    @Test
    @DisplayName("MANAGER gets department-scoped summary only")
    void summary_manager_deptScoped() throws Exception {
        listener.onEvent(envelope("WORK_ITEM_CREATED", "1", Map.of())); // dept 1
        listener.onEvent(envelope("WORK_ITEM_CREATED", "2", Map.of())); // dept 2

        mvc.perform(get("/api/analytics/summary")
                        .header("Authorization", "Bearer " + token("MANAGER",
                                List.of("ANALYTICS_VIEW"), 1L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreated").value(1))   // only dept 1
                .andExpect(jsonPath("$.departmentId").value(1));
    }

    @Test
    @DisplayName("AUDITOR gets global summary (read-only)")
    void summary_auditor_globalScope() throws Exception {
        listener.onEvent(envelope("WORK_ITEM_CREATED", "3", Map.of()));

        mvc.perform(get("/api/analytics/summary")
                        .header("Authorization", "Bearer " + token("AUDITOR",
                                List.of("ANALYTICS_VIEW", "ANALYTICS_GLOBAL"), 3L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreated").value(1));
    }

    @Test
    @DisplayName("No token returns 401")
    void summary_noToken_returns401() throws Exception {
        mvc.perform(get("/api/analytics/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER without ANALYTICS_VIEW gets 403")
    void summary_missingPermission_returns403() throws Exception {
        // Build a token with no ANALYTICS_VIEW perm
        mvc.perform(get("/api/analytics/summary")
                        .header("Authorization", "Bearer " + token("USER", List.of(), 1L))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String token(String role, List<String> perms, Long deptId) throws Exception {
        byte[] secret = SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("42")
                .claim("roles", List.of(role))
                .claim("perms", perms)
                .claim("deptId", deptId)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 900_000))
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret));
        return jwt.serialize();
    }

    private AnalyticsEventListener.EventEnvelope envelope(String type, String deptId,
                                                           Map<String, Object> payload) {
        return new AnalyticsEventListener.EventEnvelope(
                UUID.randomUUID().toString(), type, "1", "1", deptId, payload);
    }
}
