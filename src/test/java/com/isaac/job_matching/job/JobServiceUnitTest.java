package com.isaac.job_matching.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.isaac.job_matching.job.internal.JobValidationService;
import com.isaac.job_matching.shared.Location;
import com.isaac.job_matching.shared.Money;
import com.isaac.job_matching.shared.Skill;
import com.isaac.job_matching.shared.SkillRepository;
import com.isaac.job_matching.shared.exception.EntityNotFoundException;
import com.isaac.job_matching.shared.exception.ValidationException;

/**
 * Unit tests for JobService.
 * 
 * <p>
 * Test Categories:
 * <ul>
 * <li>Job Creation - creating new job postings</li>
 * <li>Job Lookup - finding jobs by ID, company, etc.</li>
 * <li>Job Update - updating job fields</li>
 * <li>Status Transitions - publish, pause, resume, close</li>
 * <li>Skills Management - adding/removing skills</li>
 * <li>Search Operations - keyword, filters</li>
 * <li>Statistics - counts and aggregations</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JobServiceUnitTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private JobValidationService validationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepository, skillRepository, validationService, eventPublisher);
    }

    // ==================== Test Utilities ====================

    private void setEntityId(Object entity, UUID id) {
        try {
            Class<?> clazz = entity.getClass();
            Field idField = null;

            while (clazz != null && idField == null) {
                try {
                    idField = clazz.getDeclaredField("id");
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }

            if (idField != null) {
                idField.setAccessible(true);
                idField.set(entity, id);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set entity ID", e);
        }
    }

    private Job createJob(UUID companyId, String title) {
        Job job = new Job(companyId, title, "A detailed description with at least 50 characters for testing.",
                RemoteOption.REMOTE, ExperienceLevel.MID, EmploymentType.FULL_TIME);
        setEntityId(job, UUID.randomUUID());
        return job;
    }

    private Job createPublishableJob(UUID companyId, String title) {
        Job job = createJob(companyId, title);
        // Add a required skill for publication
        Skill skill = new Skill("Java", "Programming");
        setEntityId(skill, UUID.randomUUID());
        JobSkill jobSkill = new JobSkill(skill, SkillImportance.MUST_HAVE);
        job.addSkill(jobSkill);
        return job;
    }

    private Skill createSkill(String name) {
        Skill skill = new Skill(name, "Programming");
        setEntityId(skill, UUID.randomUUID());
        return skill;
    }

    // ==================== Job Creation Tests ====================

    @Nested
    @DisplayName("Job Creation Tests")
    class JobCreationTests {

        @Test
        @DisplayName("Should create job successfully")
        void shouldCreateJob() {
            // Given
            UUID companyId = UUID.randomUUID();
            String title = "Software Engineer";
            String description = "A detailed job description that is at least 50 characters long for validation.";

            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
                Job saved = invocation.getArgument(0);
                setEntityId(saved, UUID.randomUUID());
                return saved;
            });

            // When
            Job result = jobService.createJob(companyId, title, description,
                    RemoteOption.HYBRID, ExperienceLevel.SENIOR, EmploymentType.FULL_TIME);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCompanyId()).isEqualTo(companyId);
            assertThat(result.getTitle()).isEqualTo(title);
            assertThat(result.getDescription()).isEqualTo(description);
            assertThat(result.getRemoteOption()).isEqualTo(RemoteOption.HYBRID);
            assertThat(result.getExperienceLevel()).isEqualTo(ExperienceLevel.SENIOR);
            assertThat(result.getEmploymentType()).isEqualTo(EmploymentType.FULL_TIME);
            assertThat(result.getStatusName()).isEqualTo(Job.StatusName.DRAFT);

            verify(validationService).validateForCreation(title, description, null);
            verify(jobRepository).save(any(Job.class));
        }

        @Test
        @DisplayName("Should create job with different remote options")
        void shouldCreateJobWithDifferentRemoteOptions() {
            // Given
            UUID companyId = UUID.randomUUID();

            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
                Job saved = invocation.getArgument(0);
                setEntityId(saved, UUID.randomUUID());
                return saved;
            });

            // When - Remote
            Job remoteJob = jobService.createJob(companyId, "Remote Dev", "Remote description with more than 50 chars",
                    RemoteOption.REMOTE, ExperienceLevel.MID, EmploymentType.FULL_TIME);
            assertThat(remoteJob.getRemoteOption()).isEqualTo(RemoteOption.REMOTE);

            // When - Onsite
            Job onsiteJob = jobService.createJob(companyId, "Onsite Dev", "Onsite description with more than 50 chars",
                    RemoteOption.ONSITE, ExperienceLevel.MID, EmploymentType.FULL_TIME);
            assertThat(onsiteJob.getRemoteOption()).isEqualTo(RemoteOption.ONSITE);
        }

        @Test
        @DisplayName("Should create job with different employment types")
        void shouldCreateJobWithDifferentEmploymentTypes() {
            // Given
            UUID companyId = UUID.randomUUID();

            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
                Job saved = invocation.getArgument(0);
                setEntityId(saved, UUID.randomUUID());
                return saved;
            });

            // When - Contract
            Job contractJob = jobService.createJob(companyId, "Contract Dev", "Contract description with 50+ chars",
                    RemoteOption.REMOTE, ExperienceLevel.MID, EmploymentType.CONTRACT);
            assertThat(contractJob.getEmploymentType()).isEqualTo(EmploymentType.CONTRACT);

            // When - Part-time
            Job partTimeJob = jobService.createJob(companyId, "Part-time Dev", "Part-time description with 50+ chars",
                    RemoteOption.REMOTE, ExperienceLevel.ENTRY, EmploymentType.PART_TIME);
            assertThat(partTimeJob.getEmploymentType()).isEqualTo(EmploymentType.PART_TIME);
        }
    }

    // ==================== Job Lookup Tests ====================

    @Nested
    @DisplayName("Job Lookup Tests")
    class JobLookupTests {

        @Test
        @DisplayName("Should get job by ID successfully")
        void shouldGetJobById() {
            // Given
            UUID jobId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            Job job = createJob(companyId, "Test Job");
            setEntityId(job, jobId);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));

            // When
            Job result = jobService.getJobById(jobId);

            // Then
            assertThat(result).isSameAs(job);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when job not found by ID")
        void shouldThrowExceptionWhenJobNotFoundById() {
            // Given
            UUID jobId = UUID.randomUUID();

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> jobService.getJobById(jobId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Job");
        }

        @Test
        @DisplayName("Should find job by ID optionally")
        void shouldFindJobByIdOptionally() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));

            // When
            Optional<Job> result = jobService.findJobById(jobId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(job);
        }

        @Test
        @DisplayName("Should return empty optional when job not found")
        void shouldReturnEmptyWhenJobNotFound() {
            // Given
            UUID jobId = UUID.randomUUID();

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.empty());

            // When
            Optional<Job> result = jobService.findJobById(jobId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should get jobs by company")
        void shouldGetJobsByCompany() {
            // Given
            UUID companyId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            List<Job> jobs = List.of(
                    createJob(companyId, "Job 1"),
                    createJob(companyId, "Job 2"));
            Page<Job> page = new PageImpl<>(jobs, pageable, jobs.size());

            when(jobRepository.findByCompanyId(companyId, pageable)).thenReturn(page);

            // When
            Page<Job> result = jobService.getJobsByCompany(companyId, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("Should get jobs by company and status")
        void shouldGetJobsByCompanyAndStatus() {
            // Given
            UUID companyId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            Job activeJob = createJob(companyId, "Active Job");
            List<Job> jobs = List.of(activeJob);
            Page<Job> page = new PageImpl<>(jobs, pageable, jobs.size());

            when(jobRepository.findByCompanyIdAndStatusName(companyId, Job.StatusName.ACTIVE, pageable))
                    .thenReturn(page);

            // When
            Page<Job> result = jobService.getJobsByCompanyAndStatus(companyId, Job.StatusName.ACTIVE, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    // ==================== Job Update Tests ====================

    @Nested
    @DisplayName("Job Update Tests")
    class JobUpdateTests {

        @Test
        @DisplayName("Should update job basic info")
        void shouldUpdateJobBasicInfo() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Original Title");
            setEntityId(job, jobId);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.updateJobBasicInfo(jobId, "New Title", "New Description with more than 50 chars");

            // Then
            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getDescription()).isEqualTo("New Description with more than 50 chars");
            verify(validationService).validateEditable(job);
            verify(validationService).validateForUpdate("New Title", "New Description with more than 50 chars", null);
        }

        @Test
        @DisplayName("Should preserve null fields on update")
        void shouldPreserveNullFieldsOnUpdate() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Original Title");
            setEntityId(job, jobId);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.updateJobBasicInfo(jobId, null, null);

            // Then
            assertThat(result.getTitle()).isEqualTo("Original Title"); // preserved
        }

        @Test
        @DisplayName("Should update job location")
        void shouldUpdateJobLocation() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);
            Location newLocation = Location.of("San Francisco", "CA", "USA");

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.updateJobLocation(jobId, newLocation);

            // Then
            assertThat(result.getLocation()).isNotNull();
            assertThat(result.getLocation().city()).isEqualTo("San Francisco");
            verify(validationService).validateEditable(job);
        }

        @Test
        @DisplayName("Should update job salary")
        void shouldUpdateJobSalary() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Money min = new Money(new BigDecimal("100000"), "USD");
            Money max = new Money(new BigDecimal("150000"), "USD");

            // When
            Job result = jobService.updateJobSalary(jobId, min, max, true);

            // Then
            assertThat(result.getSalaryMin().amount()).isEqualByComparingTo(new BigDecimal("100000"));
            assertThat(result.getSalaryMax().amount()).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(result.isSalaryVisible()).isTrue();
        }

        @Test
        @DisplayName("Should update job deadline")
        void shouldUpdateJobDeadline() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);
            LocalDate deadline = LocalDate.now().plusMonths(1);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.updateJobDeadline(jobId, deadline);

            // Then
            assertThat(result.getDeadline()).isEqualTo(deadline);
            verify(validationService).validateDeadline(deadline);
        }

        @Test
        @DisplayName("Should delete draft job")
        void shouldDeleteDraftJob() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Draft Job");
            setEntityId(job, jobId);
            assertThat(job.getStatusName()).isEqualTo(Job.StatusName.DRAFT);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));

            // When
            jobService.deleteJob(jobId);

            // Then
            verify(jobRepository).delete(job);
        }

        @Test
        @DisplayName("Should not allow deleting non-draft job")
        void shouldNotAllowDeletingNonDraftJob() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createPublishableJob(UUID.randomUUID(), "Active Job");
            setEntityId(job, jobId);
            job.publish(); // Make it active

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));

            // When/Then
            assertThatThrownBy(() -> jobService.deleteJob(jobId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Only DRAFT jobs can be deleted");

            verify(jobRepository, never()).delete(any(Job.class));
        }
    }

    // ==================== Status Transitions Tests ====================

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should publish draft job")
        void shouldPublishDraftJob() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createPublishableJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.publishJob(jobId);

            // Then
            assertThat(result.getStatusName()).isEqualTo(Job.StatusName.ACTIVE);
            assertThat(result.getPostedAt()).isNotNull();

            verify(validationService).validateCanPublish(job);
            verify(validationService).validateForPublication(job);
            verify(eventPublisher).publishEvent(any(JobPostedEvent.class));
        }

        @Test
        @DisplayName("Should pause active job")
        void shouldPauseActiveJob() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createPublishableJob(UUID.randomUUID(), "Active Job");
            setEntityId(job, jobId);
            job.publish(); // Make it active

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.pauseJob(jobId, "Position filled temporarily");

            // Then
            assertThat(result.getStatusName()).isEqualTo(Job.StatusName.PAUSED);
            verify(validationService).validateCanPause(job);
        }

        @Test
        @DisplayName("Should resume paused job")
        void shouldResumePausedJob() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createPublishableJob(UUID.randomUUID(), "Paused Job");
            setEntityId(job, jobId);
            job.publish();
            job.pause("Temp pause");

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.resumeJob(jobId);

            // Then
            assertThat(result.getStatusName()).isEqualTo(Job.StatusName.ACTIVE);
            verify(eventPublisher).publishEvent(any(JobPostedEvent.class));
        }

        @Test
        @DisplayName("Should not resume non-paused job")
        void shouldNotResumeNonPausedJob() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Draft Job");
            setEntityId(job, jobId);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));

            // When/Then
            assertThatThrownBy(() -> jobService.resumeJob(jobId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Only PAUSED jobs can be resumed");
        }

        @Test
        @DisplayName("Should close job with reason FILLED")
        void shouldCloseJobWithFilledReason() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createPublishableJob(UUID.randomUUID(), "Active Job");
            setEntityId(job, jobId);
            job.publish();

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.closeJob(jobId, JobStatus.CloseReason.FILLED);

            // Then
            assertThat(result.getStatusName()).isEqualTo(Job.StatusName.CLOSED);
            verify(validationService).validateCanClose(job);

            ArgumentCaptor<JobClosedEvent> eventCaptor = ArgumentCaptor.forClass(JobClosedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().closeReason()).isEqualTo(JobStatus.CloseReason.FILLED.name());
        }

        @Test
        @DisplayName("Should close job with reason CANCELLED")
        void shouldCloseJobWithCancelledReason() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createPublishableJob(UUID.randomUUID(), "Active Job");
            setEntityId(job, jobId);
            job.publish();

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.closeJob(jobId, JobStatus.CloseReason.CANCELLED);

            // Then
            assertThat(result.getStatusName()).isEqualTo(Job.StatusName.CLOSED);

            ArgumentCaptor<JobClosedEvent> eventCaptor = ArgumentCaptor.forClass(JobClosedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().closeReason()).isEqualTo(JobStatus.CloseReason.CANCELLED.name());
        }
    }

    // ==================== Skills Management Tests ====================

    @Nested
    @DisplayName("Skills Management Tests")
    class SkillsManagementTests {

        @Test
        @DisplayName("Should add skill to job")
        void shouldAddSkillToJob() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);

            Skill skill = createSkill("Java");

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(skillRepository.findById(skill.getId())).thenReturn(Optional.of(skill));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.addSkill(jobId, skill.getId(), SkillImportance.MUST_HAVE);

            // Then
            assertThat(result.getSkills()).hasSize(1);
            assertThat(result.getSkills().get(0).getSkillName()).isEqualTo("Java");
            assertThat(result.getSkills().get(0).isMustHave()).isTrue();
            verify(validationService).validateEditable(job);
        }

        @Test
        @DisplayName("Should update skill importance if already exists")
        void shouldUpdateSkillImportanceIfExists() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);

            Skill skill = createSkill("Java");
            JobSkill existingSkill = new JobSkill(skill, SkillImportance.NICE_TO_HAVE);
            job.addSkill(existingSkill);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(skillRepository.findById(skill.getId())).thenReturn(Optional.of(skill));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.addSkill(jobId, skill.getId(), SkillImportance.MUST_HAVE);

            // Then
            assertThat(result.getSkills()).hasSize(1);
            assertThat(result.getSkills().get(0).isMustHave()).isTrue();
        }

        @Test
        @DisplayName("Should remove skill from job")
        void shouldRemoveSkillFromJob() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);

            Skill skill = createSkill("Java");
            JobSkill jobSkill = new JobSkill(skill, SkillImportance.MUST_HAVE);
            job.addSkill(jobSkill);

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.removeSkill(jobId, skill.getId());

            // Then
            assertThat(result.getSkills()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when removing non-existent skill")
        void shouldThrowExceptionWhenRemovingNonExistentSkill() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);

            UUID nonExistentSkillId = UUID.randomUUID();

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));

            // When/Then
            assertThatThrownBy(() -> jobService.removeSkill(jobId, nonExistentSkillId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("JobSkill");
        }

        @Test
        @DisplayName("Should throw exception when skill not found")
        void shouldThrowExceptionWhenSkillNotFound() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);

            UUID skillId = UUID.randomUUID();

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(skillRepository.findById(skillId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> jobService.addSkill(jobId, skillId, SkillImportance.MUST_HAVE))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Skill");
        }

        @Test
        @DisplayName("Should set all skills for job")
        void shouldSetAllSkillsForJob() {
            // Given
            UUID jobId = UUID.randomUUID();
            Job job = createJob(UUID.randomUUID(), "Test Job");
            setEntityId(job, jobId);

            // Add existing skill
            Skill existingSkill = createSkill("Python");
            job.addSkill(new JobSkill(existingSkill, SkillImportance.NICE_TO_HAVE));

            // New skills to set
            Skill javaSkill = createSkill("Java");
            Skill springSkill = createSkill("Spring");
            List<JobSkill> newSkills = List.of(
                    new JobSkill(javaSkill, SkillImportance.MUST_HAVE),
                    new JobSkill(springSkill, SkillImportance.NICE_TO_HAVE));

            when(jobRepository.findByIdWithSkills(jobId)).thenReturn(Optional.of(job));
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Job result = jobService.setSkills(jobId, newSkills);

            // Then
            assertThat(result.getSkills()).hasSize(2);
            verify(validationService).validateForUpdate(null, null, newSkills);
        }
    }

    // ==================== Search Tests ====================

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        @Test
        @DisplayName("Should get active jobs")
        void shouldGetActiveJobs() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Job> jobs = List.of(createJob(UUID.randomUUID(), "Active Job"));
            Page<Job> page = new PageImpl<>(jobs, pageable, jobs.size());

            when(jobRepository.findActiveJobs(pageable)).thenReturn(page);

            // When
            Page<Job> result = jobService.getActiveJobs(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should search jobs by keyword")
        void shouldSearchJobsByKeyword() {
            // Given
            String keyword = "Java";
            Pageable pageable = PageRequest.of(0, 10);
            List<Job> jobs = List.of(createJob(UUID.randomUUID(), "Java Developer"));
            Page<Job> page = new PageImpl<>(jobs, pageable, jobs.size());

            when(jobRepository.searchByKeyword(keyword, pageable)).thenReturn(page);

            // When
            Page<Job> result = jobService.searchJobs(keyword, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).contains("Java");
        }

        @Test
        @DisplayName("Should find jobs by remote option")
        void shouldFindByRemoteOption() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Job> jobs = List.of(createJob(UUID.randomUUID(), "Remote Job"));
            Page<Job> page = new PageImpl<>(jobs, pageable, jobs.size());

            when(jobRepository.findActiveByRemoteOption(RemoteOption.REMOTE, pageable)).thenReturn(page);

            // When
            Page<Job> result = jobService.findByRemoteOption(RemoteOption.REMOTE, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should find jobs by experience level")
        void shouldFindByExperienceLevel() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Job> jobs = List.of(createJob(UUID.randomUUID(), "Senior Position"));
            Page<Job> page = new PageImpl<>(jobs, pageable, jobs.size());

            when(jobRepository.findActiveByExperienceLevel(ExperienceLevel.SENIOR, pageable)).thenReturn(page);

            // When
            Page<Job> result = jobService.findByExperienceLevel(ExperienceLevel.SENIOR, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should find jobs by employment type")
        void shouldFindByEmploymentType() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Job> jobs = List.of(createJob(UUID.randomUUID(), "Contract Position"));
            Page<Job> page = new PageImpl<>(jobs, pageable, jobs.size());

            when(jobRepository.findActiveByEmploymentType(EmploymentType.CONTRACT, pageable)).thenReturn(page);

            // When
            Page<Job> result = jobService.findByEmploymentType(EmploymentType.CONTRACT, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should find jobs by skill")
        void shouldFindBySkill() {
            // Given
            UUID skillId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            List<Job> jobs = List.of(createJob(UUID.randomUUID(), "Java Position"));
            Page<Job> page = new PageImpl<>(jobs, pageable, jobs.size());

            when(jobRepository.findActiveBySkill(skillId, pageable)).thenReturn(page);

            // When
            Page<Job> result = jobService.findBySkill(skillId, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    // ==================== Statistics Tests ====================

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should count active jobs")
        void shouldCountActiveJobs() {
            // Given
            when(jobRepository.countByStatusName(Job.StatusName.ACTIVE)).thenReturn(50L);

            // When
            long count = jobService.countActiveJobs();

            // Then
            assertThat(count).isEqualTo(50L);
        }

        @Test
        @DisplayName("Should count jobs by company")
        void shouldCountJobsByCompany() {
            // Given
            UUID companyId = UUID.randomUUID();
            when(jobRepository.countByCompanyId(companyId)).thenReturn(10L);

            // When
            long count = jobService.countJobsByCompany(companyId);

            // Then
            assertThat(count).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should count jobs by company and status")
        void shouldCountJobsByCompanyAndStatus() {
            // Given
            UUID companyId = UUID.randomUUID();
            when(jobRepository.countByCompanyIdAndStatusName(companyId, Job.StatusName.ACTIVE)).thenReturn(5L);

            // When
            long count = jobService.countJobsByCompanyAndStatus(companyId, Job.StatusName.ACTIVE);

            // Then
            assertThat(count).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should check if company has active jobs")
        void shouldCheckIfCompanyHasActiveJobs() {
            // Given
            UUID companyId = UUID.randomUUID();
            when(jobRepository.hasActiveJobs(companyId)).thenReturn(true);

            // When
            boolean result = jobService.hasActiveJobs(companyId);

            // Then
            assertThat(result).isTrue();
        }
    }

    // ==================== Scheduled Tasks Tests ====================

    @Nested
    @DisplayName("Scheduled Tasks Tests")
    class ScheduledTasksTests {

        @Test
        @DisplayName("Should close expired jobs")
        void shouldCloseExpiredJobs() {
            // Given
            Job expiredJob1 = createPublishableJob(UUID.randomUUID(), "Expired Job 1");
            setEntityId(expiredJob1, UUID.randomUUID());
            expiredJob1.publish();

            Job expiredJob2 = createPublishableJob(UUID.randomUUID(), "Expired Job 2");
            setEntityId(expiredJob2, UUID.randomUUID());
            expiredJob2.publish();

            List<Job> expiredJobs = List.of(expiredJob1, expiredJob2);

            when(jobRepository.findExpiredActiveJobs()).thenReturn(expiredJobs);
            when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            int count = jobService.closeExpiredJobs();

            // Then
            assertThat(count).isEqualTo(2);
            verify(jobRepository, org.mockito.Mockito.times(2)).save(any(Job.class));
            verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(any(JobClosedEvent.class));
        }

        @Test
        @DisplayName("Should return zero when no expired jobs")
        void shouldReturnZeroWhenNoExpiredJobs() {
            // Given
            when(jobRepository.findExpiredActiveJobs()).thenReturn(List.of());

            // When
            int count = jobService.closeExpiredJobs();

            // Then
            assertThat(count).isEqualTo(0);
            verify(jobRepository, never()).save(any(Job.class));
        }
    }
}
