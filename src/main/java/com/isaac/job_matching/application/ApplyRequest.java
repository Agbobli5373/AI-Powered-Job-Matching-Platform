package com.isaac.job_matching.application;

/**
 * Request to apply to a job.
 */
public record ApplyRequest(
        String resumeId,
        String coverLetter) {
}