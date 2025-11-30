package com.isaac.job_matching.user;

import java.time.Instant;
import java.util.UUID;

import com.isaac.job_matching.shared.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * User entity representing a platform user account.
 * 
 * <p>
 * This is the aggregate root for the User module. Users can have one of three
 * roles:
 * JOB_SEEKER, EMPLOYER, or ADMIN.
 * 
 * <p>
 * User lifecycle:
 * <ol>
 * <li>User registers → status PENDING</li>
 * <li>Email verified → status ACTIVE</li>
 * <li>Admin suspends → status SUSPENDED</li>
 * </ol>
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "email", unique = true, nullable = false, length = 255)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 50)
    @NotNull(message = "Role is required")
    private String role;

    @Column(name = "status", nullable = false, length = 50)
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected User() {
        // JPA constructor
    }

    /**
     * Creates a new user with the specified email, password hash, and role.
     * 
     * @param email        the user's email address
     * @param passwordHash the bcrypt hashed password (null for OAuth users)
     * @param role         the user's role
     */
    public User(String email, String passwordHash, UserRole role) {
        this.email = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
        this.role = role.name();
        this.status = UserStatus.PENDING;
        this.emailVerified = false;
    }

    /**
     * Creates a new user for OAuth authentication (no password).
     * 
     * @param email the user's email address
     * @param role  the user's role
     * @return new User instance
     */
    public static User forOAuth(String email, UserRole role) {
        User user = new User(email, null, role);
        user.emailVerified = true;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    // Getters

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return UserRole.fromString(role);
    }

    public String getRoleAsString() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    // Business methods

    /**
     * Verifies the user's email and activates the account.
     */
    public void verifyEmail() {
        if (this.emailVerified) {
            return;
        }
        this.emailVerified = true;
        if (this.status == UserStatus.PENDING) {
            this.status = UserStatus.ACTIVE;
        }
    }

    /**
     * Records a successful login.
     */
    public void recordLogin() {
        this.lastLoginAt = Instant.now();
    }

    /**
     * Suspends the user account.
     * 
     * @throws IllegalStateException if the account cannot be suspended
     */
    public void suspend() {
        if (!this.status.canTransitionTo(UserStatus.SUSPENDED)) {
            throw new IllegalStateException("Cannot suspend user in status: " + status);
        }
        this.status = UserStatus.SUSPENDED;
    }

    /**
     * Reactivates a suspended user account.
     * 
     * @throws IllegalStateException if the account cannot be reactivated
     */
    public void reactivate() {
        if (!this.status.canTransitionTo(UserStatus.ACTIVE)) {
            throw new IllegalStateException("Cannot reactivate user in status: " + status);
        }
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Updates the user's password hash.
     * 
     * @param newPasswordHash the new bcrypt hashed password
     */
    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    /**
     * Checks if this user is a job seeker.
     * 
     * @return true if the user has JOB_SEEKER role
     */
    public boolean isJobSeeker() {
        return getRole().isJobSeeker();
    }

    /**
     * Checks if this user is an employer.
     * 
     * @return true if the user has EMPLOYER role
     */
    public boolean isEmployer() {
        return getRole().isEmployer();
    }

    /**
     * Checks if this user is an admin.
     * 
     * @return true if the user has ADMIN role
     */
    public boolean isAdmin() {
        return getRole().isAdmin();
    }

    /**
     * Checks if this user can log in (active status).
     * 
     * @return true if the user can authenticate
     */
    public boolean canLogin() {
        return status.canLogin();
    }

    /**
     * Checks if this user was registered via OAuth (no password).
     * 
     * @return true if this is an OAuth user
     */
    public boolean isOAuthUser() {
        return passwordHash == null;
    }

    @Override
    public String toString() {
        return "User{id=" + getId() + ", email='" + email + "', role=" + role + ", status=" + status + "}";
    }
}
