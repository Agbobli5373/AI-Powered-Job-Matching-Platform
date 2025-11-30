package com.isaac.job_matching.user;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a new user registers on the platform.
 * 
 * <p>
 * This event is published after successful user registration and can be
 * consumed by other modules for:
 * <ul>
 * <li>Sending welcome/verification emails (Notification module)</li>
 * <li>Creating default profile for job seekers (Profile module)</li>
 * <li>Analytics tracking (Analytics module)</li>
 * </ul>
 * 
 * @param userId    the unique identifier of the newly registered user
 * @param email     the user's email address
 * @param role      the user's assigned role (JOB_SEEKER, EMPLOYER)
 * @param timestamp when the registration occurred
 */
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String role,
        Instant timestamp) {

    /**
     * Creates a UserRegisteredEvent from a User entity.
     * 
     * @param user the registered user
     * @return the event instance
     */
    public static UserRegisteredEvent from(User user) {
        return new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getRoleAsString(),
                Instant.now());
    }
}
