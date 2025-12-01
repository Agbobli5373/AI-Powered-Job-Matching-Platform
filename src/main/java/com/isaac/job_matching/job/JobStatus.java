package com.isaac.job_matching.job;

import java.time.Instant;

/**
 * Sealed interface representing the status of a job posting.
 * 
 * <p>Job status lifecycle:
 * <ol>
 *   <li>{@link Draft} - Initial state when job is created</li>
 *   <li>{@link Active} - Job is published and accepting applications</li>
 *   <li>{@link Paused} - Temporarily hidden (can be reactivated)</li>
 *   <li>{@link Closed} - No longer accepting applications (terminal state)</li>
 * </ol>
 * 
 * <p>State transitions:
 * <ul>
 *   <li>Draft → Active (publish)</li>
 *   <li>Active → Paused (pause)</li>
 *   <li>Active → Closed (close)</li>
 *   <li>Paused → Active (resume)</li>
 *   <li>Paused → Closed (close)</li>
 * </ul>
 */
public sealed interface JobStatus permits 
    JobStatus.Draft, 
    JobStatus.Active, 
    JobStatus.Paused, 
    JobStatus.Closed {

    /**
     * Draft status - job is being prepared, not yet visible.
     * 
     * @param createdAt timestamp when the draft was created
     */
    record Draft(Instant createdAt) implements JobStatus {
        public Draft {
            if (createdAt == null) {
                createdAt = Instant.now();
            }
        }
        
        public Draft() {
            this(Instant.now());
        }
    }

    /**
     * Active status - job is published and accepting applications.
     * 
     * @param publishedAt timestamp when the job was published
     */
    record Active(Instant publishedAt) implements JobStatus {
        public Active {
            if (publishedAt == null) {
                publishedAt = Instant.now();
            }
        }
        
        public Active() {
            this(Instant.now());
        }
    }

    /**
     * Paused status - job is temporarily hidden.
     * 
     * @param pausedAt timestamp when the job was paused
     * @param reason optional reason for pausing
     */
    record Paused(Instant pausedAt, String reason) implements JobStatus {
        public Paused {
            if (pausedAt == null) {
                pausedAt = Instant.now();
            }
        }
        
        public Paused() {
            this(Instant.now(), null);
        }
        
        public Paused(String reason) {
            this(Instant.now(), reason);
        }
    }

    /**
     * Closed status - job is no longer accepting applications.
     * 
     * @param closedAt timestamp when the job was closed
     * @param reason the reason for closing
     */
    record Closed(Instant closedAt, CloseReason reason) implements JobStatus {
        public Closed {
            if (closedAt == null) {
                closedAt = Instant.now();
            }
            if (reason == null) {
                reason = CloseReason.CANCELLED;
            }
        }
        
        public Closed(CloseReason reason) {
            this(Instant.now(), reason);
        }
    }

    /**
     * Reasons for closing a job posting.
     */
    enum CloseReason {
        /** Position was filled */
        FILLED,
        /** Posting was cancelled by employer */
        CANCELLED,
        /** Posting expired (deadline passed) */
        EXPIRED
    }

    /**
     * Gets the simple status name for database storage.
     * 
     * @return status name (DRAFT, ACTIVE, PAUSED, CLOSED)
     */
    default String name() {
        return switch (this) {
            case Draft _ -> "DRAFT";
            case Active _ -> "ACTIVE";
            case Paused _ -> "PAUSED";
            case Closed _ -> "CLOSED";
        };
    }

    /**
     * Checks if the job is currently accepting applications.
     * 
     * @return true if status is Active
     */
    default boolean isAcceptingApplications() {
        return this instanceof Active;
    }

    /**
     * Checks if the job is visible to job seekers.
     * 
     * @return true if status is Active
     */
    default boolean isVisible() {
        return this instanceof Active;
    }

    /**
     * Checks if the job can be edited.
     * 
     * @return true if status is Draft or Paused
     */
    default boolean isEditable() {
        return this instanceof Draft || this instanceof Paused;
    }

    /**
     * Checks if the job status is terminal (cannot transition further).
     * 
     * @return true if status is Closed
     */
    default boolean isTerminal() {
        return this instanceof Closed;
    }

    /**
     * Creates a JobStatus from database values.
     * 
     * @param statusName the status name
     * @param statusData optional JSON data with additional fields
     * @param postedAt the posted timestamp (for Active status)
     * @return the corresponding JobStatus
     */
    static JobStatus fromDatabase(String statusName, java.util.Map<String, Object> statusData, Instant postedAt) {
        return switch (statusName) {
            case "DRAFT" -> new Draft();
            case "ACTIVE" -> new Active(postedAt != null ? postedAt : Instant.now());
            case "PAUSED" -> {
                String reason = statusData != null ? (String) statusData.get("pauseReason") : null;
                yield new Paused(reason);
            }
            case "CLOSED" -> {
                String reasonStr = statusData != null ? (String) statusData.get("closeReason") : "CANCELLED";
                CloseReason reason = CloseReason.valueOf(reasonStr);
                yield new Closed(reason);
            }
            default -> new Draft();
        };
    }
}
