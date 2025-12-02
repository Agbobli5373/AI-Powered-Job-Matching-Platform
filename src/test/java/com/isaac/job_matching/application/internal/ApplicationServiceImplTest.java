package com.isaac.job_matching.application.internal;

import com.isaac.job_matching.application.*;
import com.isaac.job_matching.job.Job;
import com.isaac.job_matching.job.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApplicationServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationWorkflow applicationWorkflow;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Captor
    private ArgumentCaptor<ApplicationSubmittedEvent> submittedEventCaptor;

    @Captor
    private ArgumentCaptor<ApplicationStatusChangedEvent> statusChangedEventCaptor;

    @Test
    void shouldApplyToJobSuccessfully() {
        // Given
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ApplyRequest request = new ApplyRequest("resume-123", "I am very interested in this position");

        Job job = mock(Job.class);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobIdAndUserId(jobId, userId)).thenReturn(false);

        JobApplication savedApplication = mock(JobApplication.class);
        when(savedApplication.getId()).thenReturn(UUID.randomUUID());
        when(savedApplication.getJobId()).thenReturn(jobId);
        when(savedApplication.getStatus()).thenReturn("PENDING");
        when(savedApplication.getAppliedAt()).thenReturn(java.time.Instant.now());
        when(applicationRepository.save(any(JobApplication.class))).thenReturn(savedApplication);

        // Mock the service to return our test user ID
        ApplicationServiceImpl serviceSpy = spy(applicationService);
        doReturn(userId).when(serviceSpy).getCurrentUserId();

        // When
        ApplicationResponse response = serviceSpy.applyToJob(jobId, request);

        // Then
        assertThat(response.id()).isEqualTo(savedApplication.getId());
        assertThat(response.jobId()).isEqualTo(jobId);
        assertThat(response.status()).isEqualTo("PENDING");

        verify(applicationRepository).save(any(JobApplication.class));
        verify(eventPublisher).publishEvent(submittedEventCaptor.capture());

        ApplicationSubmittedEvent event = submittedEventCaptor.getValue();
        assertThat(event.applicationId()).isEqualTo(savedApplication.getId());
        assertThat(event.jobId()).isEqualTo(jobId);
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.coverLetter()).isEqualTo(request.coverLetter());
        assertThat(event.resumeUrl()).isEqualTo(request.resumeId());
    }

    @Test
    void shouldThrowExceptionWhenJobNotFound() {
        // Given
        UUID jobId = UUID.randomUUID();
        ApplyRequest request = new ApplyRequest("resume-123", "Cover letter");

        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        ApplicationServiceImpl serviceSpy = spy(applicationService);
        doReturn(UUID.randomUUID()).when(serviceSpy).getCurrentUserId();

        // When & Then
        assertThatThrownBy(() -> serviceSpy.applyToJob(jobId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Job not found: " + jobId);
    }

    @Test
    void shouldThrowExceptionWhenAlreadyApplied() {
        // Given
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ApplyRequest request = new ApplyRequest("resume-123", "Cover letter");

        Job job = mock(Job.class);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.existsByJobIdAndUserId(jobId, userId)).thenReturn(true);

        ApplicationServiceImpl serviceSpy = spy(applicationService);
        doReturn(userId).when(serviceSpy).getCurrentUserId();

        // When & Then
        assertThatThrownBy(() -> serviceSpy.applyToJob(jobId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already applied to this job");
    }

    @Test
    void shouldWithdrawApplicationSuccessfully() {
        // Given
        UUID applicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        JobApplication application = spy(new JobApplication(jobId, userId, "Cover", "resume.pdf"));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationServiceImpl serviceSpy = spy(applicationService);
        doReturn(userId).when(serviceSpy).getCurrentUserId();

        // When
        serviceSpy.withdrawApplication(applicationId);

        // Then
        verify(applicationRepository).save(application);
        verify(eventPublisher).publishEvent(statusChangedEventCaptor.capture());

        ApplicationStatusChangedEvent event = statusChangedEventCaptor.getValue();
        assertThat(event.applicationId()).isEqualTo(applicationId);
        assertThat(event.oldStatus()).isEqualTo("PENDING");
        assertThat(event.newStatus()).isEqualTo("WITHDRAWN");
        assertThat(application.getStatus()).isEqualTo("WITHDRAWN");
    }

    @Test
    void shouldThrowExceptionWhenWithdrawingOthersApplication() {
        // Given
        UUID applicationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        JobApplication application = mock(JobApplication.class);
        when(application.getUserId()).thenReturn(otherUserId);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        ApplicationServiceImpl serviceSpy = spy(applicationService);
        doReturn(userId).when(serviceSpy).getCurrentUserId();

        // When & Then
        assertThatThrownBy(() -> serviceSpy.withdrawApplication(applicationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Access denied");
    }

    @Test
    void shouldUpdateApplicationStatusSuccessfully() {
        // Given
        UUID applicationId = UUID.randomUUID();
        String newStatus = "REVIEWED";

        JobApplication application = mock(JobApplication.class);
        when(application.getJobId()).thenReturn(UUID.randomUUID());
        when(application.getUserId()).thenReturn(UUID.randomUUID());
        when(application.getStatus()).thenReturn("PENDING");
        when(application.getUpdatedAt()).thenReturn(java.time.Instant.now());

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationWorkflow.canTransition("PENDING", newStatus)).thenReturn(true);

        // When
        applicationService.updateApplicationStatus(applicationId, newStatus);

        // Then
        verify(application).updateStatus(newStatus);
        verify(applicationRepository).save(application);
        verify(eventPublisher).publishEvent(statusChangedEventCaptor.capture());

        ApplicationStatusChangedEvent event = statusChangedEventCaptor.getValue();
        assertThat(event.applicationId()).isEqualTo(applicationId);
        assertThat(event.oldStatus()).isEqualTo("PENDING");
        assertThat(event.newStatus()).isEqualTo(newStatus);
    }

    @Test
    void shouldThrowExceptionForInvalidStatusTransition() {
        // Given
        UUID applicationId = UUID.randomUUID();
        String newStatus = "ACCEPTED";

        JobApplication application = mock(JobApplication.class);
        when(application.getStatus()).thenReturn("PENDING");

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(applicationWorkflow.canTransition("PENDING", newStatus)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> applicationService.updateApplicationStatus(applicationId, newStatus))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid status transition from PENDING to ACCEPTED");
    }
}