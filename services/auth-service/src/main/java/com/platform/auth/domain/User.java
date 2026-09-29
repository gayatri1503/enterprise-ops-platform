package com.platform.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {} // required by JPA

    public User(String email, String passwordHash, String fullName, Role role, Long departmentId) {
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.departmentId = departmentId;
    }

    public void recordFailedLogin() { failedLoginAttempts++; }
    public void resetFailedLogins() { failedLoginAttempts = 0; }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public Long getDepartmentId() { return departmentId; }
    public boolean isEnabled() { return enabled; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
}