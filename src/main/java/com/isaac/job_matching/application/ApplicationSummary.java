package com.isaac.job_matching.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary of an application for listing views.
 */
public record ApplicationSummary(
        UUID id,
        UUID jobId,
        String jobTitle,
        String companyName,
        String status,
        Instant appliedAt,
        Instant updatedAt) {
}