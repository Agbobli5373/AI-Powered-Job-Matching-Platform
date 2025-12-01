package com.isaac.job_matching.job.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.isaac.job_matching.job.Job;
import com.isaac.job_matching.job.JobSkill;
import com.isaac.job_matching.job.RemoteOption;
import com.isaac.job_matching.shared.exception.ValidationException;

/**
 * Internal service for validating job postings.
 * 
 * <p>
 * This service contains validation logic for:
 * <ul>
 *   <li>Job creation and update validation</li>
 *   <li>Publication readiness checks</li>
 *   <li>Business rule enforcement</li>
 * </ul>
 */
@Service
public class JobValidationService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MIN_DESCRIPTION_LENGTH = 50;
    private static final int MAX_SKILLS = 20;

    /**
     * Validates a job for creation.
     * 
     * @param title the job title
     * @param description the job description
     * @param skills the required/preferred skills
     * @throws ValidationException if validation fails
     */
    public void validateForCreation(String title, String description, List<JobSkill> skills) {
        List<String> errors = new ArrayList<>();

        // Title validation
        if (title == null || title.isBlank()) {
            errors.add("Title is required");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            errors.add("Title must not exceed " + MAX_TITLE_LENGTH + " characters");
        }

        // Description validation
        if (description == null || description.isBlank()) {
            errors.add("Description is required");
        } else if (description.length() < MIN_DESCRIPTION_LENGTH) {
            errors.add("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
        }

        // Skills validation
        if (skills != null && skills.size() > MAX_SKILLS) {
            errors.add("Cannot have more than " + MAX_SKILLS + " skills");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    /**
     * Validates a job for publication.
     * 
     * @param job the job to validate
     * @throws ValidationException if validation fails
     */
    public void validateForPublication(Job job) {
        List<String> errors = job.validateForPublication();
        
        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    /**
     * Validates job update data.
     * 
     * @param title the new title (optional)
     * @param description the new description (optional)
     * @param skills the new skills list (optional)
     * @throws ValidationException if validation fails
     */
    public void validateForUpdate(String title, String description, List<JobSkill> skills) {
        List<String> errors = new ArrayList<>();

        // Title validation (if provided)
        if (title != null) {
            if (title.isBlank()) {
                errors.add("Title cannot be empty");
            } else if (title.length() > MAX_TITLE_LENGTH) {
                errors.add("Title must not exceed " + MAX_TITLE_LENGTH + " characters");
            }
        }

        // Description validation (if provided)
        if (description != null) {
            if (description.isBlank()) {
                errors.add("Description cannot be empty");
            } else if (description.length() < MIN_DESCRIPTION_LENGTH) {
                errors.add("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
            }
        }

        // Skills validation (if provided)
        if (skills != null && skills.size() > MAX_SKILLS) {
            errors.add("Cannot have more than " + MAX_SKILLS + " skills");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    /**
     * Validates salary range.
     * 
     * @param min minimum salary
     * @param max maximum salary
     * @throws ValidationException if validation fails
     */
    public void validateSalaryRange(BigDecimal min, BigDecimal max) {
        List<String> errors = new ArrayList<>();

        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Minimum salary cannot be negative");
        }

        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Maximum salary cannot be negative");
        }

        if (min != null && max != null && min.compareTo(max) > 0) {
            errors.add("Minimum salary cannot exceed maximum salary");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("; ", errors));
        }
    }

    /**
     * Validates deadline.
     * 
     * @param deadline the application deadline
     * @throws ValidationException if validation fails
     */
    public void validateDeadline(LocalDate deadline) {
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            throw new ValidationException("Deadline must be in the future");
        }
    }

    /**
     * Validates that a non-remote job has a location.
     * 
     * @param remoteOption the remote option
     * @param hasLocation whether location is provided
     * @throws ValidationException if validation fails
     */
    public void validateLocationRequirement(RemoteOption remoteOption, boolean hasLocation) {
        if (remoteOption != RemoteOption.REMOTE && !hasLocation) {
            throw new ValidationException("Location is required for non-remote jobs");
        }
    }

    /**
     * Validates that skills list contains at least one MUST_HAVE skill.
     * 
     * @param skills the skills list
     * @throws ValidationException if validation fails
     */
    public void validateRequiredSkills(List<JobSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            throw new ValidationException("At least one skill is required");
        }

        boolean hasMustHave = skills.stream().anyMatch(JobSkill::isMustHave);
        if (!hasMustHave) {
            throw new ValidationException("At least one MUST_HAVE skill is required");
        }
    }

    /**
     * Checks if job can be edited.
     * 
     * @param job the job to check
     * @throws ValidationException if job cannot be edited
     */
    public void validateEditable(Job job) {
        if (!job.isEditable()) {
            throw new ValidationException("Job cannot be edited in current status: " + job.getStatusName());
        }
    }

    /**
     * Checks if job can be published.
     * 
     * @param job the job to check
     * @throws ValidationException if job cannot be published
     */
    public void validateCanPublish(Job job) {
        if (job.getStatusName() != Job.StatusName.DRAFT && job.getStatusName() != Job.StatusName.PAUSED) {
            throw new ValidationException("Only DRAFT or PAUSED jobs can be published");
        }
    }

    /**
     * Checks if job can be paused.
     * 
     * @param job the job to check
     * @throws ValidationException if job cannot be paused
     */
    public void validateCanPause(Job job) {
        if (job.getStatusName() != Job.StatusName.ACTIVE) {
            throw new ValidationException("Only ACTIVE jobs can be paused");
        }
    }

    /**
     * Checks if job can be closed.
     * 
     * @param job the job to check
     * @throws ValidationException if job cannot be closed
     */
    public void validateCanClose(Job job) {
        if (job.getStatusName() == Job.StatusName.CLOSED) {
            throw new ValidationException("Job is already closed");
        }
    }
}
