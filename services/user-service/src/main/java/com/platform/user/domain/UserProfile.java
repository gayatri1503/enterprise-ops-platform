package com.platform.user.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** user-service's own copy of a user, kept up to date by USER_REGISTERED events. */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    private Long id; // same id as in auth-service, so NOT generated here

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserProfile() {}

    public UserProfile(Long id) {
        this.id = id;
    }

    public void apply(String email, String fullName, String role, Long departmentId, boolean enabled) {
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.departmentId = departmentId;
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public Long getDepartmentId() { return departmentId; }
    public boolean isEnabled() { return enabled; }
}