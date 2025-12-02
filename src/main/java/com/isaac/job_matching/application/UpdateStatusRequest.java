package com.isaac.job_matching.application;

/**
 * Request to update application status.
 */
public record UpdateStatusRequest(
        String newStatus) {
}