package com.isaac.job_matching.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Public API for job application management.
 */
public interface ApplicationService {

    /**
     * Apply to a job.
     */
    ApplicationResponse applyToJob(UUID jobId, ApplyRequest request);

    /**
     * Get applications for current user.
     */
    Page<ApplicationSummary> getMyApplications(String status, Pageable pageable);

    /**
     * Get application details.
     */
    ApplicationDetails getApplication(UUID applicationId);

    /**
     * Withdraw application.
     */
    void withdrawApplication(UUID applicationId);

    /**
     * Update application status (employer only).
     */
    void updateApplicationStatus(UUID applicationId, String newStatus);

    /**
     * Get applications for a job (employer only).
     */
    Page<ApplicationSummary> getJobApplications(UUID jobId, Pageable pageable);
}