package com.isaac.job_matching.application;

import java.util.UUID;

/**
 * Event published when a job application is submitted.
 */
public record ApplicationSubmittedEvent(
        UUID applicationId,
        UUID jobId,
        UUID userId,
        String coverLetter,
        String resumeUrl) {
}