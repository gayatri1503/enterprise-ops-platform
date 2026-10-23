package com.platform.user.controller;

import com.platform.user.repository.UserProfileRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    public record MeResponse(Long id, String email, String fullName, String role,
                              List<String> permissions, Long departmentId) {}

    private final UserProfileRepository profiles;

    public UserController(UserProfileRepository profiles) {
        this.profiles = profiles;
    }

    /**
     * "Who am I?"
     * Core identity fields (id, email, role, perms, deptId) come from the verified JWT —
     * no DB round-trip needed and they are always fresh.
     * fullName comes from the local user_profiles table (populated by USER_REGISTERED events).
     * If the profile hasn't arrived yet (race on first login), fullName falls back to null
     * rather than blocking the request.
     */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<String> perms = jwt.getClaimAsStringList("perms");
        Object dept = jwt.getClaim("deptId");

        Long userId = Long.valueOf(jwt.getSubject());

        String fullName = profiles.findById(userId)
                .map(p -> p.getFullName())
                .orElse(null); // profile arrives via event; may not exist yet on first login

        return new MeResponse(
                userId,
                jwt.getClaimAsString("email"),
                fullName,
                (roles == null || roles.isEmpty()) ? "USER" : roles.get(0),
                perms == null ? List.of() : perms,
                dept instanceof Number n ? n.longValue() : null);
    }
}
