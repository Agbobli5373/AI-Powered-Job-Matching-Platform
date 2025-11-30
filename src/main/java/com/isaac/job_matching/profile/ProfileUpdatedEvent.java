package com.isaac.job_matching.profile;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a profile is updated.
 * 
 * <p>
 * This event is published after profile updates and can be consumed by:
 * <ul>
 * <li>Search module - for re-indexing the profile</li>
 * <li>Matching module - for recalculating job matches</li>
 * <li>Analytics module - for tracking profile activity</li>
 * </ul>
 * 
 * @param profileId  the profile ID
 * @param userId     the user ID
 * @param updateType the type of update (BASIC_INFO, WORK_EXPERIENCE, EDUCATION,
 *                   SKILLS)
 * @param timestamp  when the update occurred
 */
public record ProfileUpdatedEvent(
        UUID profileId,
        UUID userId,
        UpdateType updateType,
        Instant timestamp) {

    /**
     * Types of profile updates.
     */
    public enum UpdateType {
        BASIC_INFO,
        WORK_EXPERIENCE,
        EDUCATION,
        SKILLS,
        PREFERENCES
    }

    /**
     * Creates a ProfileUpdatedEvent from a Profile entity.
     * 
     * @param profile    the updated profile
     * @param updateType the type of update
     * @return the event instance
     */
    public static ProfileUpdatedEvent from(Profile profile, UpdateType updateType) {
        return new ProfileUpdatedEvent(
                profile.getId(),
                profile.getUserId(),
                updateType,
                Instant.now());
    }
}
