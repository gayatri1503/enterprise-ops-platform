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

    /** Set when the account is temporarily locked after too many failed logins. Null = not locked. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

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
    public void resetFailedLogins() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }
    public void disable() { this.enabled = false; }
    public void lockUntil(Instant until) { this.lockedUntil = until; }

    /** True if a temporary lockout is currently active. */
    public boolean isLockedOut() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public Long getDepartmentId() { return departmentId; }
    public boolean isEnabled() { return enabled; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
}