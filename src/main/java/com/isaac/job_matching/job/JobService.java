package com.isaac.job_matching.job;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isaac.job_matching.job.internal.JobValidationService;
import com.isaac.job_matching.shared.Location;
import com.isaac.job_matching.shared.Money;
import com.isaac.job_matching.shared.Skill;
import com.isaac.job_matching.shared.SkillRepository;
import com.isaac.job_matching.shared.exception.EntityNotFoundException;
import com.isaac.job_matching.shared.exception.ValidationException;

/**
 * Public service API for job management operations.
 * 
 * <p>
 * This is the main entry point for the Job module, providing:
 * <ul>
 *   <li>Job CRUD operations</li>
 *   <li>Job status transitions (publish, pause, close)</li>
 *   <li>Job search and filtering</li>
 *   <li>Skills management for jobs</li>
 * </ul>
 * 
 * <p>
 * Publishes events for job lifecycle changes:
 * <ul>
 *   <li>{@link JobPostedEvent} - when a job is published</li>
 *   <li>{@link JobClosedEvent} - when a job is closed</li>
 * </ul>
 */
@Service
@Transactional
public class JobService {

    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final JobValidationService validationService;
    private final ApplicationEventPublisher eventPublisher;

    public JobService(
            JobRepository jobRepository,
            SkillRepository skillRepository,
            JobValidationService validationService,
            ApplicationEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.validationService = validationService;
        this.eventPublisher = eventPublisher;
    }

    // ================ Job CRUD ================

    /**
     * Creates a new job posting (as draft).
     * 
     * @param companyId the company ID
     * @param title the job title
     * @param description the job description
     * @param remoteOption the remote work option
     * @param experienceLevel the required experience level
     * @param employmentType the employment type
     * @return the created job
     */
    public Job createJob(UUID companyId, String title, String description,
                         RemoteOption remoteOption, ExperienceLevel experienceLevel,
                         EmploymentType employmentType) {
        validationService.validateForCreation(title, description, null);
        
        Job job = new Job(companyId, title, description, remoteOption, experienceLevel, employmentType);
        return jobRepository.save(job);
    }

    /**
     * Gets a job by ID.
     * 
     * @param jobId the job ID
     * @return the job
     * @throws EntityNotFoundException if job not found
     */
    @Transactional(readOnly = true)
    public Job getJobById(UUID jobId) {
        return jobRepository.findByIdWithSkills(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job", jobId));
    }

    /**
     * Gets a job by ID if it exists.
     * 
     * @param jobId the job ID
     * @return Optional containing the job if found
     */
    @Transactional(readOnly = true)
    public Optional<Job> findJobById(UUID jobId) {
        return jobRepository.findByIdWithSkills(jobId);
    }

    /**
     * Updates a job's basic information.
     * 
     * @param jobId the job ID
     * @param title the new title (or null to keep existing)
     * @param description the new description (or null to keep existing)
     * @return the updated job
     * @throws EntityNotFoundException if job not found
     * @throws ValidationException if job cannot be edited
     */
    public Job updateJobBasicInfo(UUID jobId, String title, String description) {
        Job job = getJobById(jobId);
        validationService.validateEditable(job);
        validationService.validateForUpdate(title, description, null);

        if (title != null) {
            job.setTitle(title);
        }
        if (description != null) {
            job.setDescription(description);
        }

        return jobRepository.save(job);
    }

    /**
     * Updates a job's location.
     * 
     * @param jobId the job ID
     * @param location the new location
     * @return the updated job
     */
    public Job updateJobLocation(UUID jobId, Location location) {
        Job job = getJobById(jobId);
        validationService.validateEditable(job);
        
        job.setLocation(location);
        return jobRepository.save(job);
    }

    /**
     * Updates a job's remote option.
     * 
     * @param jobId the job ID
     * @param remoteOption the new remote option
     * @return the updated job
     */
    public Job updateJobRemoteOption(UUID jobId, RemoteOption remoteOption) {
        Job job = getJobById(jobId);
        validationService.validateEditable(job);
        
        job.setRemoteOption(remoteOption);
        return jobRepository.save(job);
    }

    /**
     * Updates a job's salary range.
     * 
     * @param jobId the job ID
     * @param min minimum salary
     * @param max maximum salary
     * @param visible whether salary is visible to candidates
     * @return the updated job
     */
    public Job updateJobSalary(UUID jobId, Money min, Money max, Boolean visible) {
        Job job = getJobById(jobId);
        validationService.validateEditable(job);
        
        if (min != null || max != null) {
            validationService.validateSalaryRange(
                min != null ? min.amount() : null,
                max != null ? max.amount() : null
            );
            job.setSalaryRange(min, max);
        }
        
        if (visible != null) {
            job.setSalaryVisible(visible);
        }

        return jobRepository.save(job);
    }

    /**
     * Updates a job's experience level.
     * 
     * @param jobId the job ID
     * @param experienceLevel the new experience level
     * @return the updated job
     */
    public Job updateJobExperienceLevel(UUID jobId, ExperienceLevel experienceLevel) {
        Job job = getJobById(jobId);
        validationService.validateEditable(job);
        
        job.setExperienceLevel(experienceLevel);
        return jobRepository.save(job);
    }

    /**
     * Updates a job's employment type.
     * 
     * @param jobId the job ID
     * @param employmentType the new employment type
     * @return the updated job
     */
    public Job updateJobEmploymentType(UUID jobId, EmploymentType employmentType) {
        Job job = getJobById(jobId);
        validationService.validateEditable(job);
        
        job.setEmploymentType(employmentType);
        return jobRepository.save(job);
    }

    /**
     * Updates a job's deadline.
     * 
     * @param jobId the job ID
     * @param deadline the new deadline
     * @return the updated job
     */
    public Job updateJobDeadline(UUID jobId, LocalDate deadline) {
        Job job = getJobById(jobId);
        validationService.validateEditable(job);
        validationService.validateDeadline(deadline);
        
        job.setDeadline(deadline);
        return jobRepository.save(job);
    }

    /**
     * Deletes a job.
     * Only draft jobs can be deleted. Active/paused jobs should be closed instead.
     * 
     * @param jobId the job ID
     * @throws EntityNotFoundException if job not found
     * @throws ValidationException if job cannot be deleted
     */
    public void deleteJob(UUID jobId) {
        Job job = getJobById(jobId);
        
        if (job.getStatusName() != Job.StatusName.DRAFT) {
            throw new ValidationException("Only DRAFT jobs can be deleted. Close the job instead.");
        }
        
        jobRepository.delete(job);
    }

    // ================ Skills Management ================

    /**
     * Adds a skill to a job.
     * 
     * @param jobId the job ID
     * @param skillId the skill ID
     * @param importance the skill importance
     * @return the updated job
     */
    public Job addSkill(UUID jobId, UUID skillId, SkillImportance importance) {
        Job job = getJobById(jobId);
        validationService.validateEditable(job);
        
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new EntityNotFoundException("Skill", skillId));

        // Check if skill already exists
        if (job.hasSkill(skillId)) {
            // Update importance instead
            job.getSkills().stream()
                    .filter(js -> js.getSkillId().equals(skillId))
                    .findFirst()
                    .ifPresent(js -> js.setImportance(importance));
        } else {
            JobSkill jobSkill = new JobSkill(skill, importance);
            job.addSkill(jobSkill);
        }

        return jobRepository.save(job);
    }

    /**
     * Removes a skill from a job.
     * 
     * @param jobId the job ID
     * @param skillId the skill ID
     * @return the updated job
     */
    public Job removeSkill(UUID jobId, UUID skillId) {
        Job job = getJobById(jobId);
        validationService.validateEditable(job);
        
        JobSkill skill = job.getSkills().stream()
                .filter(js -> js.getSkillId().equals(skillId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("JobSkill", skillId));

        job.removeSkill(skill);
        return jobRepository.save(job);
    }

    /**
     * Sets all skills for a job, replacing existing skills.
     * 
     * @param jobId the job ID
     * @param skills list of job skills to set
     * @return the updated job
     */
    public Job setSkills(UUID jobId, List<JobSkill> skills) {
        Job job = getJobById(jobId);
        validationService.validateEditable(job);
        validationService.validateForUpdate(null, null, skills);
        
        job.clearSkills();
        for (JobSkill skill : skills) {
            job.addSkill(skill);
        }

        return jobRepository.save(job);
    }

    // ================ Status Transitions ================

    /**
     * Publishes a job, making it visible to job seekers.
     * 
     * @param jobId the job ID
     * @return the updated job
     * @throws EntityNotFoundException if job not found
     * @throws ValidationException if job cannot be published
     */
    public Job publishJob(UUID jobId) {
        Job job = getJobById(jobId);
        validationService.validateCanPublish(job);
        validationService.validateForPublication(job);
        
        job.publish();
        Job savedJob = jobRepository.save(job);

        eventPublisher.publishEvent(JobPostedEvent.from(savedJob));

        return savedJob;
    }

    /**
     * Pauses a job, temporarily hiding it from job seekers.
     * 
     * @param jobId the job ID
     * @param reason optional reason for pausing
     * @return the updated job
     */
    public Job pauseJob(UUID jobId, String reason) {
        Job job = getJobById(jobId);
        validationService.validateCanPause(job);
        
        job.pause(reason);
        return jobRepository.save(job);
    }

    /**
     * Resumes a paused job.
     * 
     * @param jobId the job ID
     * @return the updated job
     */
    public Job resumeJob(UUID jobId) {
        Job job = getJobById(jobId);
        
        if (job.getStatusName() != Job.StatusName.PAUSED) {
            throw new ValidationException("Only PAUSED jobs can be resumed");
        }
        
        job.publish();
        Job savedJob = jobRepository.save(job);

        // Re-publish the posted event (job is active again)
        eventPublisher.publishEvent(JobPostedEvent.from(savedJob));

        return savedJob;
    }

    /**
     * Closes a job, preventing further applications.
     * 
     * @param jobId the job ID
     * @param reason the reason for closing
     * @return the updated job
     */
    public Job closeJob(UUID jobId, JobStatus.CloseReason reason) {
        Job job = getJobById(jobId);
        validationService.validateCanClose(job);
        
        job.close(reason);
        Job savedJob = jobRepository.save(job);

        eventPublisher.publishEvent(JobClosedEvent.from(savedJob, reason));

        return savedJob;
    }

    // ================ Query Methods ================

    /**
     * Gets jobs for a company.
     * 
     * @param companyId the company ID
     * @param pageable pagination parameters
     * @return page of jobs
     */
    @Transactional(readOnly = true)
    public Page<Job> getJobsByCompany(UUID companyId, Pageable pageable) {
        return jobRepository.findByCompanyId(companyId, pageable);
    }

    /**
     * Gets jobs for a company with specific status.
     * 
     * @param companyId the company ID
     * @param status the job status
     * @param pageable pagination parameters
     * @return page of jobs
     */
    @Transactional(readOnly = true)
    public Page<Job> getJobsByCompanyAndStatus(UUID companyId, Job.StatusName status, Pageable pageable) {
        return jobRepository.findByCompanyIdAndStatusName(companyId, status, pageable);
    }

    /**
     * Gets all active jobs.
     * 
     * @param pageable pagination parameters
     * @return page of active jobs
     */
    @Transactional(readOnly = true)
    public Page<Job> getActiveJobs(Pageable pageable) {
        return jobRepository.findActiveJobs(pageable);
    }

    /**
     * Searches active jobs by keyword.
     * 
     * @param keyword the search keyword
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Transactional(readOnly = true)
    public Page<Job> searchJobs(String keyword, Pageable pageable) {
        return jobRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * Finds active jobs by remote option.
     * 
     * @param remoteOption the remote option
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Transactional(readOnly = true)
    public Page<Job> findByRemoteOption(RemoteOption remoteOption, Pageable pageable) {
        return jobRepository.findActiveByRemoteOption(remoteOption, pageable);
    }

    /**
     * Finds active jobs by experience level.
     * 
     * @param level the experience level
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Transactional(readOnly = true)
    public Page<Job> findByExperienceLevel(ExperienceLevel level, Pageable pageable) {
        return jobRepository.findActiveByExperienceLevel(level, pageable);
    }

    /**
     * Finds active jobs by employment type.
     * 
     * @param type the employment type
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Transactional(readOnly = true)
    public Page<Job> findByEmploymentType(EmploymentType type, Pageable pageable) {
        return jobRepository.findActiveByEmploymentType(type, pageable);
    }

    /**
     * Finds active jobs requiring a specific skill.
     * 
     * @param skillId the skill ID
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Transactional(readOnly = true)
    public Page<Job> findBySkill(UUID skillId, Pageable pageable) {
        return jobRepository.findActiveBySkill(skillId, pageable);
    }

    // ================ Statistics ================

    /**
     * Counts active jobs.
     * 
     * @return count of active jobs
     */
    @Transactional(readOnly = true)
    public long countActiveJobs() {
        return jobRepository.countByStatusName(Job.StatusName.ACTIVE);
    }

    /**
     * Counts jobs for a company.
     * 
     * @param companyId the company ID
     * @return count of jobs
     */
    @Transactional(readOnly = true)
    public long countJobsByCompany(UUID companyId) {
        return jobRepository.countByCompanyId(companyId);
    }

    /**
     * Counts jobs for a company by status.
     * 
     * @param companyId the company ID
     * @param status the job status
     * @return count of matching jobs
     */
    @Transactional(readOnly = true)
    public long countJobsByCompanyAndStatus(UUID companyId, Job.StatusName status) {
        return jobRepository.countByCompanyIdAndStatusName(companyId, status);
    }

    /**
     * Checks if company has any active jobs.
     * 
     * @param companyId the company ID
     * @return true if company has active jobs
     */
    @Transactional(readOnly = true)
    public boolean hasActiveJobs(UUID companyId) {
        return jobRepository.hasActiveJobs(companyId);
    }

    // ================ Scheduled Tasks Support ================

    /**
     * Closes expired jobs (deadline passed).
     * This method should be called by a scheduled task.
     * 
     * @return number of jobs closed
     */
    public int closeExpiredJobs() {
        List<Job> expiredJobs = jobRepository.findExpiredActiveJobs();
        int count = 0;
        
        for (Job job : expiredJobs) {
            job.close(JobStatus.CloseReason.EXPIRED);
            jobRepository.save(job);
            eventPublisher.publishEvent(JobClosedEvent.from(job, JobStatus.CloseReason.EXPIRED));
            count++;
        }
        
        return count;
    }
}
