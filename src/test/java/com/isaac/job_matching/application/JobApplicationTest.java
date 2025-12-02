package com.isaac.job_matching.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JobApplication entity.
 */
class JobApplicationTest {

    @Test
    void shouldCreateApplicationWithPendingStatus() {
        // Given
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String coverLetter = "I am very interested in this position";
        String resumeUrl = "resume.pdf";

        // When
        JobApplication application = new JobApplication(jobId, userId, coverLetter, resumeUrl);

        // Then
        assertThat(application).isNotNull();
        assertThat(application.getJobId()).isEqualTo(jobId);
        assertThat(application.getUserId()).isEqualTo(userId);
        assertThat(application.getStatus()).isEqualTo("PENDING");
        assertThat(application.getCoverLetter()).isEqualTo(coverLetter);
        assertThat(application.getResumeUrl()).isEqualTo(resumeUrl);
        assertThat(application.getAppliedAt()).isNotNull();
        assertThat(application.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldUpdateStatus() {
        // Given
        JobApplication application = new JobApplication(UUID.randomUUID(), UUID.randomUUID(), "Cover", "resume.pdf");
        Instant originalUpdatedAt = application.getUpdatedAt();

        // When
        application.updateStatus("REVIEWED");

        // Then
        assertThat(application.getStatus()).isEqualTo("REVIEWED");
        assertThat(application.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void shouldAllowWithdrawalFromPendingStatus() {
        // Given
        JobApplication application = new JobApplication(UUID.randomUUID(), UUID.randomUUID(), "Cover", "resume.pdf");
        application.updateStatus("PENDING");

        // When
        application.withdraw();

        // Then
        assertThat(application.getStatus()).isEqualTo("WITHDRAWN");
    }

    @Test
    void shouldAllowWithdrawalFromReviewedStatus() {
        // Given
        JobApplication application = new JobApplication(UUID.randomUUID(), UUID.randomUUID(), "Cover", "resume.pdf");
        application.updateStatus("REVIEWED");

        // When
        application.withdraw();

        // Then
        assertThat(application.getStatus()).isEqualTo("WITHDRAWN");
    }

    @Test
    void shouldNotAllowWithdrawalFromAcceptedStatus() {
        // Given
        JobApplication application = new JobApplication(UUID.randomUUID(), UUID.randomUUID(), "Cover", "resume.pdf");
        application.updateStatus("ACCEPTED");

        // When & Then
        assertThatThrownBy(() -> application.withdraw())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot withdraw application in current status");
    }

    @Test
    void shouldNotAllowWithdrawalFromRejectedStatus() {
        // Given
        JobApplication application = new JobApplication(UUID.randomUUID(), UUID.randomUUID(), "Cover", "resume.pdf");
        application.updateStatus("REJECTED");

        // When & Then
        assertThatThrownBy(() -> application.withdraw())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot withdraw application in current status");
    }

    @Test
    void shouldNotAllowWithdrawalFromWithdrawnStatus() {
        // Given
        JobApplication application = new JobApplication(UUID.randomUUID(), UUID.randomUUID(), "Cover", "resume.pdf");
        application.updateStatus("WITHDRAWN");

        // When & Then
        assertThatThrownBy(() -> application.withdraw())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot withdraw application in current status");
    }

    @Test
    void shouldIdentifyWithdrawableStatuses() {
        // Given
        JobApplication pendingApp = new JobApplication(UUID.randomUUID(), UUID.randomUUID(), "Cover", "resume.pdf");
        pendingApp.updateStatus("PENDING");

        JobApplication reviewedApp = new JobApplication(UUID.randomUUID(), UUID.randomUUID(), "Cover", "resume.pdf");
        reviewedApp.updateStatus("REVIEWED");

        JobApplication acceptedApp = new JobApplication(UUID.randomUUID(), UUID.randomUUID(), "Cover", "resume.pdf");
        acceptedApp.updateStatus("ACCEPTED");

        // Then
        assertThat(pendingApp.canWithdraw()).isTrue();
        assertThat(reviewedApp.canWithdraw()).isTrue();
        assertThat(acceptedApp.canWithdraw()).isFalse();
    }
}