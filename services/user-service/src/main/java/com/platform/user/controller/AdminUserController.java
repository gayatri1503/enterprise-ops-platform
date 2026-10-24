package com.platform.user.controller;

import com.platform.user.domain.UserProfile;
import com.platform.user.repository.UserProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    public record UserResponse(Long id, String email, String fullName, String role,
                               Long departmentId, boolean enabled) {
        static UserResponse from(UserProfile p) {
            return new UserResponse(p.getId(), p.getEmail(), p.getFullName(), p.getRole(),
                    p.getDepartmentId(), p.isEnabled());
        }
    }

    /** Same page shape core-service uses, so the frontend can treat both alike. */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    private final UserProfileRepository profiles;

    public AdminUserController(UserProfileRepository profiles) {
        this.profiles = profiles;
    }

    /** ADMIN only: only ADMIN has USER_MANAGE (see Role.java in auth-service). */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public PageResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        // Sort is fixed on purpose: a client-chosen sort field can crash the query.
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("email"));
        Page<UserProfile> result = profiles.findAll(pageable);
        return new PageResponse<>(
                result.getContent().stream().map(UserResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}