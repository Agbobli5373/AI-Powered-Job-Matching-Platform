package com.isaac.job_matching.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed view of an application.
 */
public record ApplicationDetails(
        UUID id,
        UUID jobId,
        String jobTitle,
        String companyName,
        String status,
        String coverLetter,
        String resumeUrl,
        Instant appliedAt,
        Instant updatedAt) {
}