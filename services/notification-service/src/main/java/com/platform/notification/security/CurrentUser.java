package com.platform.notification.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public record CurrentUser(Long id, String role, Long departmentId) {

    public static CurrentUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        Object dept = jwt.getClaim("deptId");
        return new CurrentUser(
                Long.valueOf(jwt.getSubject()),
                (roles == null || roles.isEmpty()) ? "USER" : roles.get(0),
                dept instanceof Number n ? n.longValue() : null);
    }
}
