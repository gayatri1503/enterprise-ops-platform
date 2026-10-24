package com.platform.user;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
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

import com.platform.user.domain.UserProfile;
import com.platform.user.repository.UserProfileRepository;

import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";

    @Autowired MockMvc mvc;
    @Autowired UserProfileRepository profileRepository;
    @MockBean  RabbitTemplate rabbitTemplate;

    @BeforeEach
    void seed() {
        UserProfile admin = new UserProfile(1L);
        admin.apply("admin@example.com", "Admin User", "ADMIN", 1L, true);
        profileRepository.save(admin);

        UserProfile user = new UserProfile(2L);
        user.apply("user@example.com", "Normal User", "USER", 1L, true);
        profileRepository.save(user);
    }

    // ─── /api/users/me ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me returns JWT claims including fullName from local profile")
    void me_returnsClaims() throws Exception {
        String token = buildToken(2L, "user@example.com", List.of("USER"),
                List.of("WORK_CREATE", "WORK_EDIT", "ANALYTICS_VIEW"), 1L);

        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.fullName").value("Normal User"))  // from seeded profile
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.departmentId").value(1))
                .andExpect(jsonPath("$.permissions", hasItems("WORK_CREATE", "WORK_EDIT")));
    }

    @Test
    @DisplayName("GET /me returns null fullName when profile not yet synced (first login race)")
    void me_profileNotYetSynced_fullNameIsNull() throws Exception {
        // Token subject 99 has no UserProfile row yet
        String token = buildToken(99L, "new@example.com", List.of("USER"),
                List.of("WORK_CREATE"), 1L);

        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.fullName").value(nullValue())); // profile not yet synced
    }

    @Test
    @DisplayName("GET /me without token returns 401")
    void me_noToken_returns401() throws Exception {
        mvc.perform(get("/api/users/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /me with tampered token returns 401")
    void me_tamperedToken_returns401() throws Exception {
        String token = buildToken(2L, "user@example.com", List.of("USER"), List.of(), 1L);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tampered)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ─── /api/admin/users ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/users with ADMIN token returns paginated user list")
    void adminUsers_adminToken_returnsPage() throws Exception {
        String token = buildToken(1L, "admin@example.com", List.of("ADMIN"),
                List.of("USER_MANAGE", "USER_READ", "WORK_CREATE", "WORK_EDIT", "WORK_ASSIGN",
                        "ANALYTICS_VIEW", "ANALYTICS_GLOBAL", "AUDIT_VIEW"), 1L);

        mvc.perform(get("/api/admin/users?page=0&size=10")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET /admin/users with USER token (no USER_MANAGE) returns 403")
    void adminUsers_userToken_returns403() throws Exception {
        String token = buildToken(2L, "user@example.com", List.of("USER"),
                List.of("WORK_CREATE", "WORK_EDIT", "ANALYTICS_VIEW"), 1L);

        mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /admin/users without token returns 401")
    void adminUsers_noToken_returns401() throws Exception {
        mvc.perform(get("/api/admin/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/users page size is capped at 100")
    void adminUsers_pageSizeCappedAt100() throws Exception {
        String token = buildToken(1L, "admin@example.com", List.of("ADMIN"),
                List.of("USER_MANAGE"), 1L);

        mvc.perform(get("/api/admin/users?page=0&size=9999")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(lessThanOrEqualTo(100)));
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private static String buildToken(Long sub, String email, List<String> roles,
                                     List<String> perms, Long deptId) throws Exception {
        byte[] secret = SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(sub))
                .claim("email", email)
                .claim("roles", roles)
                .claim("perms", perms)
                .claim("deptId", deptId)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 900_000))
                .jwtID(java.util.UUID.randomUUID().toString())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret));
        return jwt.serialize();
    }
}
