package com.platform.auth.dto;

import com.platform.auth.domain.Permission;
import com.platform.auth.domain.User;

import java.util.List;

public record UserSummary(Long id, String email, String fullName, String role,
                          Long departmentId, List<String> permissions) {

    public static UserSummary from(User u) {
        return new UserSummary(
                u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.getDepartmentId(),
                u.getRole().getPermissions().stream().map(Permission::name).sorted().toList());
    }
}