package com.isaac.job_matching.auth;

import java.util.UUID;

/**
 * Record representing an authenticated user's information.
 * 
 * <p>
 * This is typically extracted from a validated JWT token and contains
 * the essential user information needed for authorization decisions.
 * 
 * @param id    the user's unique identifier
 * @param email the user's email address
 * @param role  the user's role (JOB_SEEKER, EMPLOYER, ADMIN)
 */
public record AuthenticatedUser(
        UUID id,
        String email,
        String role) {

    /**
     * Checks if this user is a job seeker.
     * 
     * @return true if the user has JOB_SEEKER role
     */
    public boolean isJobSeeker() {
        return "JOB_SEEKER".equals(role);
    }

    /**
     * Checks if this user is an employer.
     * 
     * @return true if the user has EMPLOYER role
     */
    public boolean isEmployer() {
        return "EMPLOYER".equals(role);
    }

    /**
     * Checks if this user is an admin.
     * 
     * @return true if the user has ADMIN role
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * Gets the Spring Security role name (prefixed with ROLE_).
     * 
     * @return the role name for Spring Security
     */
    public String getSpringSecurityRole() {
        return "ROLE_" + role;
    }
}
