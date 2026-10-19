package com.platform.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    public record MeResponse(Long id, String email, String role,
                             List<String> permissions, Long departmentId) {}

    /** "Who am I?" Answered straight from the verified JWT, no database needed. */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<String> perms = jwt.getClaimAsStringList("perms");
        Object dept = jwt.getClaim("deptId");
        return new MeResponse(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                (roles == null || roles.isEmpty()) ? "USER" : roles.get(0),
                perms == null ? List.of() : perms,
                dept instanceof Number n ? n.longValue() : null);
    }
}