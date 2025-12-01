package com.isaac.job_matching.job;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new job is posted (published).
 * 
 * <p>
 * This event is published when a job transitions from DRAFT to ACTIVE status.
 * Consumers can use this event to:
 * <ul>
 *   <li>Index the job in Elasticsearch for search</li>
 *   <li>Send notifications to matching candidates</li>
 *   <li>Update analytics/metrics</li>
 * </ul>
 */
public record JobPostedEvent(
    UUID jobId,
    UUID companyId,
    String title,
    String experienceLevel,
    String employmentType,
    String remoteOption,
    String location,
    Instant postedAt
) {
    
    /**
     * Creates a JobPostedEvent from a Job entity.
     * 
     * @param job the job that was posted
     * @return the event
     */
    public static JobPostedEvent from(Job job) {
        String locationStr = job.getLocation() != null ? job.getLocation().format() : null;
        return new JobPostedEvent(
            job.getId(),
            job.getCompanyId(),
            job.getTitle(),
            job.getExperienceLevel().name(),
            job.getEmploymentType().name(),
            job.getRemoteOption().name(),
            locationStr,
            job.getPostedAt()
        );
    }
}
