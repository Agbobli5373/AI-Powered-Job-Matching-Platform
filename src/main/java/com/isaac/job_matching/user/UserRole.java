package com.isaac.job_matching.user;

/**
 * Sealed interface representing the different user roles in the platform.
 * 
 * <p>
 * Uses Java sealed types (JDK 25) to restrict implementations to known roles,
 * enabling exhaustive pattern matching in switch expressions.
 * 
 * <p>
 * Roles:
 * <ul>
 * <li>{@link JobSeeker} - Users looking for jobs, can create profiles and
 * apply</li>
 * <li>{@link Employer} - Company representatives who post jobs and review
 * candidates</li>
 * <li>{@link Admin} - Platform administrators with full access</li>
 * </ul>
 */
public sealed interface UserRole permits UserRole.JobSeeker, UserRole.Employer, UserRole.Admin {

    /**
     * Returns the string representation for database storage.
     * 
     * @return role name as string
     */
    String name();

    /**
     * Job seeker role - users looking for employment opportunities.
     */
    record JobSeeker() implements UserRole {
        @Override
        public String name() {
            return "JOB_SEEKER";
        }
    }

    /**
     * Employer role - company representatives managing job postings.
     */
    record Employer() implements UserRole {
        @Override
        public String name() {
            return "EMPLOYER";
        }
    }

    /**
     * Admin role - platform administrators with elevated privileges.
     */
    record Admin() implements UserRole {
        @Override
        public String name() {
            return "ADMIN";
        }
    }

    /**
     * Creates a UserRole from its string name.
     * 
     * @param name the role name (JOB_SEEKER, EMPLOYER, ADMIN)
     * @return the corresponding UserRole instance
     * @throws IllegalArgumentException if the name is not recognized
     */
    static UserRole fromString(String name) {
        return switch (name.toUpperCase()) {
            case "JOB_SEEKER" -> new JobSeeker();
            case "EMPLOYER" -> new Employer();
            case "ADMIN" -> new Admin();
            default -> throw new IllegalArgumentException("Unknown role: " + name);
        };
    }

    /**
     * Checks if this role is a job seeker.
     * 
     * @return true if this is a JobSeeker role
     */
    default boolean isJobSeeker() {
        return this instanceof JobSeeker;
    }

    /**
     * Checks if this role is an employer.
     * 
     * @return true if this is an Employer role
     */
    default boolean isEmployer() {
        return this instanceof Employer;
    }

    /**
     * Checks if this role is an admin.
     * 
     * @return true if this is an Admin role
     */
    default boolean isAdmin() {
        return this instanceof Admin;
    }
}
