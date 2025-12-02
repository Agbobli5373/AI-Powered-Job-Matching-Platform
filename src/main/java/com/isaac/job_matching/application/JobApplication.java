package com.isaac.job_matching.application;

import com.isaac.job_matching.shared.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Job application entity representing a job seeker's application to a job.
 * This is an aggregate root in the application module.
 */
@Entity
@Table(name = "applications", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "job_id", "user_id" })
})
public class JobApplication extends BaseEntity {

    @NotNull
    @Column(name = "job_id")
    private UUID jobId;

    @NotNull
    @Column(name = "user_id")
    private UUID userId;

    @NotNull
    @Column(name = "status")
    private String status;

    @Column(name = "cover_letter", length = 5000)
    private String coverLetter;

    @Column(name = "resume_url", length = 500)
    private String resumeUrl;

    @NotNull
    @Column(name = "applied_at")
    private Instant appliedAt;

    @NotNull
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected JobApplication() {
        // JPA default constructor
    }

    public JobApplication(UUID jobId, UUID userId, String coverLetter, String resumeUrl) {
        this.jobId = jobId;
        this.userId = userId;
        this.status = "PENDING";
        this.coverLetter = coverLetter;
        this.resumeUrl = resumeUrl;
        this.appliedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Business methods

    public void updateStatus(String newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void withdraw() {
        if (canWithdraw()) {
            this.status = "WITHDRAWN";
            this.updatedAt = Instant.now();
        } else {
            throw new IllegalStateException("Cannot withdraw application in current status: " + status);
        }
    }

    public boolean canWithdraw() {
        return "PENDING".equals(status) || "REVIEWED".equals(status);
    }

    // Getters

    public UUID getJobId() {
        return jobId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}