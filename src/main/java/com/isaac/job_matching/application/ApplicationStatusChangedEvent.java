package com.isaac.job_matching.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an application status changes.
 */
public record ApplicationStatusChangedEvent(
        UUID applicationId,
        UUID jobId,
        UUID userId,
        String oldStatus,
        String newStatus,
        Instant changedAt) {
}