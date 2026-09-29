package com.platform.auth.domain;

import static com.platform.auth.domain.Permission.*;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    ADMIN(EnumSet.allOf(Permission.class)),
    MANAGER(EnumSet.of(USER_READ, WORK_CREATE, WORK_EDIT, WORK_ASSIGN, ANALYTICS_VIEW, AUDIT_VIEW)),
    USER(EnumSet.of(WORK_CREATE, WORK_EDIT, ANALYTICS_VIEW)),
    AUDITOR(EnumSet.of(USER_READ, ANALYTICS_VIEW, ANALYTICS_GLOBAL, AUDIT_VIEW));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}