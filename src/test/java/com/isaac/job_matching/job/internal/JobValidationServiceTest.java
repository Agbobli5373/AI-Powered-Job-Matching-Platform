package com.isaac.job_matching.job.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.isaac.job_matching.job.EmploymentType;
import com.isaac.job_matching.job.ExperienceLevel;
import com.isaac.job_matching.job.Job;
import com.isaac.job_matching.job.JobSkill;
import com.isaac.job_matching.job.RemoteOption;
import com.isaac.job_matching.job.SkillImportance;
import com.isaac.job_matching.shared.Location;
import com.isaac.job_matching.shared.Skill;
import com.isaac.job_matching.shared.exception.ValidationException;

/**
 * Unit tests for JobValidationService.
 * 
 * <p>
 * Test Categories:
 * <ul>
 * <li>Creation Validation - title, description, skills</li>
 * <li>Update Validation - field updates</li>
 * <li>Publication Validation - readiness checks</li>
 * <li>Salary Validation - range checks</li>
 * <li>Deadline Validation - future date checks</li>
 * <li>Status Validation - editable, publishable, pausable, closeable</li>
 * </ul>
 */
class JobValidationServiceTest {

    private JobValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new JobValidationService();
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

    private Job createDraftJob() {
        Job job = new Job(UUID.randomUUID(), "Test Title", 
                "A detailed description that is at least 50 characters long for testing purposes.",
                RemoteOption.REMOTE, ExperienceLevel.MID, EmploymentType.FULL_TIME);
        setEntityId(job, UUID.randomUUID());
        return job;
    }

    private Job createPublishableJob() {
        Job job = createDraftJob();
        // Add a MUST_HAVE skill
        Skill skill = new Skill("Java", "Programming");
        setEntityId(skill, UUID.randomUUID());
        JobSkill jobSkill = new JobSkill(skill, SkillImportance.MUST_HAVE);
        job.addSkill(jobSkill);
        return job;
    }

    private Job createActiveJob() {
        Job job = createPublishableJob();
        job.publish();
        return job;
    }

    private Job createPausedJob() {
        Job job = createActiveJob();
        job.pause("Temporary pause");
        return job;
    }

    private Job createClosedJob() {
        Job job = createActiveJob();
        job.close(com.isaac.job_matching.job.JobStatus.CloseReason.FILLED);
        return job;
    }

    // ==================== Creation Validation Tests ====================

    @Nested
    @DisplayName("Creation Validation Tests")
    class CreationValidationTests {

        @Test
        @DisplayName("Should pass validation with valid title and description")
        void shouldPassWithValidTitleAndDescription() {
            // Given
            String title = "Software Engineer";
            String description = "A detailed job description that is at least 50 characters long for the system.";

            // When/Then
            assertThatCode(() -> validationService.validateForCreation(title, description, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail validation with null title")
        void shouldFailWithNullTitle() {
            // Given
            String description = "A detailed job description that is at least 50 characters long for the system.";

            // When/Then
            assertThatThrownBy(() -> validationService.validateForCreation(null, description, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Title is required");
        }

        @Test
        @DisplayName("Should fail validation with blank title")
        void shouldFailWithBlankTitle() {
            // Given
            String description = "A detailed job description that is at least 50 characters long for the system.";

            // When/Then
            assertThatThrownBy(() -> validationService.validateForCreation("   ", description, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Title is required");
        }

        @Test
        @DisplayName("Should fail validation with title exceeding max length")
        void shouldFailWithTitleExceedingMaxLength() {
            // Given
            String title = "A".repeat(256); // 256 characters
            String description = "A detailed job description that is at least 50 characters long for the system.";

            // When/Then
            assertThatThrownBy(() -> validationService.validateForCreation(title, description, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Title must not exceed");
        }

        @Test
        @DisplayName("Should fail validation with null description")
        void shouldFailWithNullDescription() {
            // Given
            String title = "Software Engineer";

            // When/Then
            assertThatThrownBy(() -> validationService.validateForCreation(title, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Description is required");
        }

        @Test
        @DisplayName("Should fail validation with short description")
        void shouldFailWithShortDescription() {
            // Given
            String title = "Software Engineer";
            String description = "Too short";

            // When/Then
            assertThatThrownBy(() -> validationService.validateForCreation(title, description, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Description must be at least");
        }

        @Test
        @DisplayName("Should fail validation with too many skills")
        void shouldFailWithTooManySkills() {
            // Given
            String title = "Software Engineer";
            String description = "A detailed job description that is at least 50 characters long for the system.";
            List<JobSkill> skills = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                Skill skill = new Skill("Skill" + i, "Category");
                setEntityId(skill, UUID.randomUUID());
                skills.add(new JobSkill(skill, SkillImportance.NICE_TO_HAVE));
            }

            // When/Then
            assertThatThrownBy(() -> validationService.validateForCreation(title, description, skills))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Cannot have more than");
        }

        @Test
        @DisplayName("Should collect multiple validation errors")
        void shouldCollectMultipleValidationErrors() {
            // Given - both title and description invalid
            String title = null;
            String description = "Short";

            // When/Then
            assertThatThrownBy(() -> validationService.validateForCreation(title, description, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Title is required")
                    .hasMessageContaining("Description must be at least");
        }
    }

    // ==================== Update Validation Tests ====================

    @Nested
    @DisplayName("Update Validation Tests")
    class UpdateValidationTests {

        @Test
        @DisplayName("Should pass with null fields (preserve existing)")
        void shouldPassWithNullFields() {
            // When/Then
            assertThatCode(() -> validationService.validateForUpdate(null, null, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass with valid update values")
        void shouldPassWithValidUpdateValues() {
            // Given
            String title = "Updated Title";
            String description = "Updated description that is at least 50 characters long for the system.";

            // When/Then
            assertThatCode(() -> validationService.validateForUpdate(title, description, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail with empty title on update")
        void shouldFailWithEmptyTitleOnUpdate() {
            // When/Then
            assertThatThrownBy(() -> validationService.validateForUpdate("", null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Title cannot be empty");
        }

        @Test
        @DisplayName("Should fail with empty description on update")
        void shouldFailWithEmptyDescriptionOnUpdate() {
            // When/Then
            assertThatThrownBy(() -> validationService.validateForUpdate(null, "  ", null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Description cannot be empty");
        }
    }

    // ==================== Publication Validation Tests ====================

    @Nested
    @DisplayName("Publication Validation Tests")
    class PublicationValidationTests {

        @Test
        @DisplayName("Should pass publication validation for complete job")
        void shouldPassForCompleteJob() {
            // Given
            Job job = createPublishableJob();

            // When/Then
            assertThatCode(() -> validationService.validateForPublication(job))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail publication validation for job without required skills")
        void shouldFailForJobWithoutRequiredSkills() {
            // Given
            Job job = createDraftJob(); // No skills

            // When/Then
            assertThatThrownBy(() -> validationService.validateForPublication(job))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("required skill");
        }

        @Test
        @DisplayName("Should fail publication validation for non-remote job without location")
        void shouldFailForOnsiteJobWithoutLocation() {
            // Given
            Job job = new Job(UUID.randomUUID(), "Test Job",
                    "A detailed description that is at least 50 characters long for testing.",
                    RemoteOption.ONSITE, ExperienceLevel.MID, EmploymentType.FULL_TIME);
            setEntityId(job, UUID.randomUUID());
            // Add required skill
            Skill skill = new Skill("Java", "Programming");
            setEntityId(skill, UUID.randomUUID());
            job.addSkill(new JobSkill(skill, SkillImportance.MUST_HAVE));

            // When/Then
            assertThatThrownBy(() -> validationService.validateForPublication(job))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Location is required");
        }

        @Test
        @DisplayName("Should pass for remote job without location")
        void shouldPassForRemoteJobWithoutLocation() {
            // Given
            Job job = createPublishableJob(); // Remote job

            // When/Then
            assertThatCode(() -> validationService.validateForPublication(job))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== Salary Validation Tests ====================

    @Nested
    @DisplayName("Salary Validation Tests")
    class SalaryValidationTests {

        @Test
        @DisplayName("Should pass with valid salary range")
        void shouldPassWithValidSalaryRange() {
            // Given
            BigDecimal min = new BigDecimal("50000");
            BigDecimal max = new BigDecimal("100000");

            // When/Then
            assertThatCode(() -> validationService.validateSalaryRange(min, max))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass with null salary values")
        void shouldPassWithNullSalaryValues() {
            // When/Then
            assertThatCode(() -> validationService.validateSalaryRange(null, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass with only min salary")
        void shouldPassWithOnlyMinSalary() {
            // Given
            BigDecimal min = new BigDecimal("50000");

            // When/Then
            assertThatCode(() -> validationService.validateSalaryRange(min, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass with only max salary")
        void shouldPassWithOnlyMaxSalary() {
            // Given
            BigDecimal max = new BigDecimal("100000");

            // When/Then
            assertThatCode(() -> validationService.validateSalaryRange(null, max))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail with negative min salary")
        void shouldFailWithNegativeMinSalary() {
            // Given
            BigDecimal min = new BigDecimal("-1000");

            // When/Then
            assertThatThrownBy(() -> validationService.validateSalaryRange(min, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Minimum salary cannot be negative");
        }

        @Test
        @DisplayName("Should fail with negative max salary")
        void shouldFailWithNegativeMaxSalary() {
            // Given
            BigDecimal max = new BigDecimal("-1000");

            // When/Then
            assertThatThrownBy(() -> validationService.validateSalaryRange(null, max))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Maximum salary cannot be negative");
        }

        @Test
        @DisplayName("Should fail when min exceeds max")
        void shouldFailWhenMinExceedsMax() {
            // Given
            BigDecimal min = new BigDecimal("150000");
            BigDecimal max = new BigDecimal("100000");

            // When/Then
            assertThatThrownBy(() -> validationService.validateSalaryRange(min, max))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Minimum salary cannot exceed maximum");
        }

        @Test
        @DisplayName("Should pass when min equals max")
        void shouldPassWhenMinEqualsMax() {
            // Given - exact salary
            BigDecimal value = new BigDecimal("100000");

            // When/Then
            assertThatCode(() -> validationService.validateSalaryRange(value, value))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== Deadline Validation Tests ====================

    @Nested
    @DisplayName("Deadline Validation Tests")
    class DeadlineValidationTests {

        @Test
        @DisplayName("Should pass with future deadline")
        void shouldPassWithFutureDeadline() {
            // Given
            LocalDate futureDate = LocalDate.now().plusDays(30);

            // When/Then
            assertThatCode(() -> validationService.validateDeadline(futureDate))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass with null deadline")
        void shouldPassWithNullDeadline() {
            // When/Then
            assertThatCode(() -> validationService.validateDeadline(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail with past deadline")
        void shouldFailWithPastDeadline() {
            // Given
            LocalDate pastDate = LocalDate.now().minusDays(1);

            // When/Then
            assertThatThrownBy(() -> validationService.validateDeadline(pastDate))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Deadline must be in the future");
        }
    }

    // ==================== Location Requirement Tests ====================

    @Nested
    @DisplayName("Location Requirement Tests")
    class LocationRequirementTests {

        @Test
        @DisplayName("Should pass for remote job without location")
        void shouldPassForRemoteWithoutLocation() {
            // When/Then
            assertThatCode(() -> validationService.validateLocationRequirement(RemoteOption.REMOTE, false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass for remote job with location")
        void shouldPassForRemoteWithLocation() {
            // When/Then
            assertThatCode(() -> validationService.validateLocationRequirement(RemoteOption.REMOTE, true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass for hybrid job with location")
        void shouldPassForHybridWithLocation() {
            // When/Then
            assertThatCode(() -> validationService.validateLocationRequirement(RemoteOption.HYBRID, true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail for hybrid job without location")
        void shouldFailForHybridWithoutLocation() {
            // When/Then
            assertThatThrownBy(() -> validationService.validateLocationRequirement(RemoteOption.HYBRID, false))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Location is required for non-remote jobs");
        }

        @Test
        @DisplayName("Should pass for onsite job with location")
        void shouldPassForOnsiteWithLocation() {
            // When/Then
            assertThatCode(() -> validationService.validateLocationRequirement(RemoteOption.ONSITE, true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail for onsite job without location")
        void shouldFailForOnsiteWithoutLocation() {
            // When/Then
            assertThatThrownBy(() -> validationService.validateLocationRequirement(RemoteOption.ONSITE, false))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Location is required for non-remote jobs");
        }
    }

    // ==================== Required Skills Validation Tests ====================

    @Nested
    @DisplayName("Required Skills Validation Tests")
    class RequiredSkillsValidationTests {

        @Test
        @DisplayName("Should pass with at least one MUST_HAVE skill")
        void shouldPassWithMustHaveSkill() {
            // Given
            Skill skill = new Skill("Java", "Programming");
            setEntityId(skill, UUID.randomUUID());
            List<JobSkill> skills = List.of(new JobSkill(skill, SkillImportance.MUST_HAVE));

            // When/Then
            assertThatCode(() -> validationService.validateRequiredSkills(skills))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail with null skills list")
        void shouldFailWithNullSkillsList() {
            // When/Then
            assertThatThrownBy(() -> validationService.validateRequiredSkills(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("At least one skill is required");
        }

        @Test
        @DisplayName("Should fail with empty skills list")
        void shouldFailWithEmptySkillsList() {
            // When/Then
            assertThatThrownBy(() -> validationService.validateRequiredSkills(List.of()))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("At least one skill is required");
        }

        @Test
        @DisplayName("Should fail with only NICE_TO_HAVE skills")
        void shouldFailWithOnlyNiceToHaveSkills() {
            // Given
            Skill skill = new Skill("Java", "Programming");
            setEntityId(skill, UUID.randomUUID());
            List<JobSkill> skills = List.of(new JobSkill(skill, SkillImportance.NICE_TO_HAVE));

            // When/Then
            assertThatThrownBy(() -> validationService.validateRequiredSkills(skills))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("At least one MUST_HAVE skill is required");
        }
    }

    // ==================== Status Validation Tests ====================

    @Nested
    @DisplayName("Status Validation Tests")
    class StatusValidationTests {

        @Test
        @DisplayName("Should pass editable validation for DRAFT job")
        void shouldPassEditableForDraft() {
            // Given
            Job job = createDraftJob();

            // When/Then
            assertThatCode(() -> validationService.validateEditable(job))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass editable validation for PAUSED job")
        void shouldPassEditableForPaused() {
            // Given
            Job job = createPausedJob();

            // When/Then
            assertThatCode(() -> validationService.validateEditable(job))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail editable validation for ACTIVE job")
        void shouldFailEditableForActive() {
            // Given
            Job job = createActiveJob();

            // When/Then
            assertThatThrownBy(() -> validationService.validateEditable(job))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cannot be edited");
        }

        @Test
        @DisplayName("Should fail editable validation for CLOSED job")
        void shouldFailEditableForClosed() {
            // Given
            Job job = createClosedJob();

            // When/Then
            assertThatThrownBy(() -> validationService.validateEditable(job))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("cannot be edited");
        }

        @Test
        @DisplayName("Should pass can publish validation for DRAFT job")
        void shouldPassCanPublishForDraft() {
            // Given
            Job job = createDraftJob();

            // When/Then
            assertThatCode(() -> validationService.validateCanPublish(job))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass can publish validation for PAUSED job")
        void shouldPassCanPublishForPaused() {
            // Given
            Job job = createPausedJob();

            // When/Then
            assertThatCode(() -> validationService.validateCanPublish(job))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail can publish validation for ACTIVE job")
        void shouldFailCanPublishForActive() {
            // Given
            Job job = createActiveJob();

            // When/Then
            assertThatThrownBy(() -> validationService.validateCanPublish(job))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Only DRAFT or PAUSED jobs can be published");
        }

        @Test
        @DisplayName("Should pass can pause validation for ACTIVE job")
        void shouldPassCanPauseForActive() {
            // Given
            Job job = createActiveJob();

            // When/Then
            assertThatCode(() -> validationService.validateCanPause(job))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail can pause validation for non-ACTIVE job")
        void shouldFailCanPauseForNonActive() {
            // Given
            Job job = createDraftJob();

            // When/Then
            assertThatThrownBy(() -> validationService.validateCanPause(job))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Only ACTIVE jobs can be paused");
        }

        @Test
        @DisplayName("Should pass can close validation for non-CLOSED job")
        void shouldPassCanCloseForNonClosed() {
            // Given
            Job job = createActiveJob();

            // When/Then
            assertThatCode(() -> validationService.validateCanClose(job))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail can close validation for CLOSED job")
        void shouldFailCanCloseForClosed() {
            // Given
            Job job = createClosedJob();

            // When/Then
            assertThatThrownBy(() -> validationService.validateCanClose(job))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Job is already closed");
        }
    }
}
