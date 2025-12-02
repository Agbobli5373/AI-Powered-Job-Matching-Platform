package com.isaac.job_matching.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Response for application operations.
 */
public record ApplicationResponse(
        UUID id,
        UUID jobId,
        String status,
        Instant appliedAt) {
}