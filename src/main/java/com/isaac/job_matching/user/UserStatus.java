package com.isaac.job_matching.user;

/**
 * Enumeration of possible user account statuses.
 * 
 * <p>
 * Status transitions:
 * <ul>
 * <li>PENDING → ACTIVE (after email verification)</li>
 * <li>ACTIVE → SUSPENDED (by admin action)</li>
 * <li>SUSPENDED → ACTIVE (by admin action)</li>
 * </ul>
 */
public enum UserStatus {

    /**
     * Account created but email not yet verified.
     * Users in this status cannot fully use the platform.
     */
    PENDING("Pending email verification"),

    /**
     * Account is active and verified.
     * Full platform access based on role.
     */
    ACTIVE("Active and verified"),

    /**
     * Account has been suspended by an administrator.
     * No platform access until reactivated.
     */
    SUSPENDED("Suspended by administrator");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    /**
     * Gets a human-readable description of this status.
     * 
     * @return status description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if the user can log in with this status.
     * 
     * @return true if the user can authenticate
     */
    public boolean canLogin() {
        return this == ACTIVE;
    }

    /**
     * Checks if the user can transition to the given status.
     * 
     * @param newStatus the target status
     * @return true if the transition is allowed
     */
    public boolean canTransitionTo(UserStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == ACTIVE;
            case ACTIVE -> newStatus == SUSPENDED;
            case SUSPENDED -> newStatus == ACTIVE;
        };
    }
}
