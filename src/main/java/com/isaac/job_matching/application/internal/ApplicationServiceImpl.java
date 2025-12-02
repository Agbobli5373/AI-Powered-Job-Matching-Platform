package com.isaac.job_matching.application.internal;

import com.isaac.job_matching.application.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of ApplicationService.
 */
@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationWorkflow applicationWorkflow;
    private final ApplicationEventPublisher eventPublisher;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
            ApplicationWorkflow applicationWorkflow,
            ApplicationEventPublisher eventPublisher) {
        this.applicationRepository = applicationRepository;
        this.applicationWorkflow = applicationWorkflow;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ApplicationResponse applyToJob(UUID jobId, ApplyRequest request) {
        // Get current user from security context (simplified for now)
        UUID userId = getCurrentUserId();

        // Check if already applied
        if (applicationRepository.existsByJobIdAndUserId(jobId, userId)) {
            throw new IllegalStateException("Already applied to this job");
        }

        // Create application
        JobApplication application = new JobApplication(jobId, userId, request.coverLetter(), request.resumeId());
        application = applicationRepository.save(application);

        // Publish event
        eventPublisher.publishEvent(new ApplicationSubmittedEvent(
                application.getId(),
                jobId,
                userId,
                request.coverLetter(),
                request.resumeId()));

        return new ApplicationResponse(
                application.getId(),
                application.getJobId(),
                application.getStatus(),
                application.getAppliedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationSummary> getMyApplications(String status, Pageable pageable) {
        UUID userId = getCurrentUserId();
        List<JobApplication> applications;

        if (status != null && !status.isEmpty()) {
            applications = applicationRepository.findByUserIdAndStatus(userId, status);
        } else {
            applications = applicationRepository.findByUserId(userId);
        }

        // Convert to summaries (simplified - would need job details in real
        // implementation)
        List<ApplicationSummary> summaries = applications.stream()
                .map(app -> new ApplicationSummary(
                        app.getId(),
                        app.getJobId(),
                        "Job Title", // Would fetch from job
                        "Company Name", // Would fetch from company
                        app.getStatus(),
                        app.getAppliedAt(),
                        app.getUpdatedAt()))
                .toList();

        return new PageImpl<>(summaries, pageable, summaries.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDetails getApplication(UUID applicationId) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        // Verify ownership
        UUID userId = getCurrentUserId();
        if (!application.getUserId().equals(userId)) {
            throw new IllegalStateException("Access denied");
        }

        return new ApplicationDetails(
                application.getId(),
                application.getJobId(),
                "Job Title", // Would fetch from job
                "Company Name", // Would fetch from company
                application.getStatus(),
                application.getCoverLetter(),
                application.getResumeUrl(),
                application.getAppliedAt(),
                application.getUpdatedAt());
    }

    @Override
    public void withdrawApplication(UUID applicationId) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        // Verify ownership
        UUID userId = getCurrentUserId();
        if (!application.getUserId().equals(userId)) {
            throw new IllegalStateException("Access denied");
        }

        String oldStatus = application.getStatus();
        application.withdraw();
        applicationRepository.save(application);

        // Publish event
        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(
                applicationId,
                application.getJobId(),
                userId,
                oldStatus,
                application.getStatus(),
                application.getUpdatedAt()));
    }

    @Override
    public void updateApplicationStatus(UUID applicationId, String newStatus) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        // Verify workflow allows transition
        if (!applicationWorkflow.canTransition(application.getStatus(), newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition from " + application.getStatus() + " to " + newStatus);
        }

        String oldStatus = application.getStatus();
        application.updateStatus(newStatus);
        applicationRepository.save(application);

        // Publish event
        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(
                applicationId,
                application.getJobId(),
                application.getUserId(),
                oldStatus,
                newStatus,
                application.getUpdatedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationSummary> getJobApplications(UUID jobId, Pageable pageable) {
        // Verify job ownership (employer check would go here)
        List<JobApplication> applications = applicationRepository.findByJobId(jobId);

        // Convert to summaries
        List<ApplicationSummary> summaries = applications.stream()
                .map(app -> new ApplicationSummary(
                        app.getId(),
                        app.getJobId(),
                        "Job Title", // Would fetch from job
                        "Company Name", // Would fetch from company
                        app.getStatus(),
                        app.getAppliedAt(),
                        app.getUpdatedAt()))
                .toList();

        return new PageImpl<>(summaries, pageable, summaries.size());
    }

    // TODO: Implement proper user context retrieval
    protected UUID getCurrentUserId() {
        // This would come from Spring Security context
        return UUID.randomUUID(); // Placeholder
    }
}