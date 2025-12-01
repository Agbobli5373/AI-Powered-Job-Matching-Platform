package com.isaac.job_matching.job;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a job is closed.
 * 
 * <p>
 * This event is published when a job transitions to CLOSED status.
 * Consumers can use this event to:
 * <ul>
 *   <li>Remove the job from search index</li>
 *   <li>Notify applicants if position was filled</li>
 *   <li>Update analytics/metrics</li>
 * </ul>
 */
public record JobClosedEvent(
    UUID jobId,
    UUID companyId,
    String title,
    String closeReason,
    Instant closedAt
) {
    
    /**
     * Creates a JobClosedEvent from a Job entity.
     * 
     * @param job the job that was closed
     * @param reason the reason for closing
     * @return the event
     */
    public static JobClosedEvent from(Job job, JobStatus.CloseReason reason) {
        return new JobClosedEvent(
            job.getId(),
            job.getCompanyId(),
            job.getTitle(),
            reason.name(),
            Instant.now()
        );
    }
}
