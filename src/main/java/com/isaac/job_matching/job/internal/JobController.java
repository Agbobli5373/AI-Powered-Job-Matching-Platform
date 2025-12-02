package com.isaac.job_matching.job.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isaac.job_matching.company.Company;
import com.isaac.job_matching.company.CompanyService;
import com.isaac.job_matching.job.EmploymentType;
import com.isaac.job_matching.job.ExperienceLevel;
import com.isaac.job_matching.job.Job;
import com.isaac.job_matching.job.JobService;
import com.isaac.job_matching.job.JobSkill;
import com.isaac.job_matching.job.JobStatus;
import com.isaac.job_matching.job.RemoteOption;
import com.isaac.job_matching.job.SkillImportance;
import com.isaac.job_matching.shared.Location;
import com.isaac.job_matching.shared.Money;
import com.isaac.job_matching.shared.Skill;
import com.isaac.job_matching.shared.SkillRepository;
import com.isaac.job_matching.shared.exception.EntityNotFoundException;
import com.isaac.job_matching.shared.exception.ValidationException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * REST controller for job management endpoints.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Jobs", description = "Job posting management endpoints")
public class JobController {

    private final JobService jobService;
    private final CompanyService companyService;
    private final SkillRepository skillRepository;

    public JobController(
            JobService jobService,
            CompanyService companyService,
            SkillRepository skillRepository) {
        this.jobService = jobService;
        this.companyService = companyService;
        this.skillRepository = skillRepository;
    }

    // ================ Public Endpoints ================

    @GetMapping("/jobs")
    @Operation(summary = "Search jobs", description = "Search active job postings with filters")
    public ResponseEntity<Page<JobResponse>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RemoteOption remoteOption,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @RequestParam(required = false) EmploymentType employmentType,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<Job> jobs;

        if (keyword != null && !keyword.isBlank()) {
            jobs = jobService.searchJobs(keyword, pageable);
        } else if (remoteOption != null) {
            jobs = jobService.findByRemoteOption(remoteOption, pageable);
        } else if (experienceLevel != null) {
            jobs = jobService.findByExperienceLevel(experienceLevel, pageable);
        } else if (employmentType != null) {
            jobs = jobService.findByEmploymentType(employmentType, pageable);
        } else {
            jobs = jobService.getActiveJobs(pageable);
        }

        return ResponseEntity.ok(jobs.map(this::toJobResponse));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Get job details", description = "Get detailed information about a job posting")
    public ResponseEntity<JobDetailResponse> getJob(@PathVariable UUID id) {
        Job job = jobService.getJobById(id);
        Company company = companyService.getCompanyById(job.getCompanyId());
        return ResponseEntity.ok(toJobDetailResponse(job, company));
    }

    // ================ Employer Endpoints ================

    @GetMapping("/employer/jobs")
    @Operation(summary = "Get employer's jobs", description = "Get all jobs posted by the current employer")
    public ResponseEntity<Page<JobResponse>> getEmployerJobs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Job.StatusName status,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Company company = companyService.getCompanyByUserId(userId);

        Page<Job> jobs;
        if (status != null) {
            jobs = jobService.getJobsByCompanyAndStatus(company.getId(), status, pageable);
        } else {
            jobs = jobService.getJobsByCompany(company.getId(), pageable);
        }

        return ResponseEntity.ok(jobs.map(this::toJobResponse));
    }

    @PostMapping("/jobs")
    @Operation(summary = "Create job posting", description = "Create a new job posting as draft")
    public ResponseEntity<JobResponse> createJob(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateJobRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Company company = companyService.getCompanyByUserId(userId);

        Job job = jobService.createJob(
                company.getId(),
                request.title(),
                request.description(),
                request.remoteOption(),
                request.experienceLevel(),
                request.employmentType());

        // Set optional fields
        if (request.location() != null) {
            job = jobService.updateJobLocation(job.getId(), request.location());
        }

        if (request.salaryMin() != null || request.salaryMax() != null) {
            String currency = request.salaryCurrency() != null ? request.salaryCurrency() : "USD";
            Money min = request.salaryMin() != null ? new Money(request.salaryMin(), currency) : null;
            Money max = request.salaryMax() != null ? new Money(request.salaryMax(), currency) : null;
            job = jobService.updateJobSalary(job.getId(), min, max, request.salaryVisible());
        }

        if (request.deadline() != null) {
            job = jobService.updateJobDeadline(job.getId(), request.deadline());
        }

        // Add skills
        if (request.skills() != null && !request.skills().isEmpty()) {
            for (JobSkillInput skillInput : request.skills()) {
                Skill skill = skillRepository.findByNameIgnoreCase(skillInput.skillName())
                        .orElseGet(() -> skillRepository.save(new Skill(skillInput.skillName())));
                job = jobService.addSkill(job.getId(), skill.getId(), skillInput.importance());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toJobResponse(job));
    }

    @PutMapping("/jobs/{id}")
    @Operation(summary = "Update job posting", description = "Update an existing job posting")
    public ResponseEntity<JobResponse> updateJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Job job = jobService.getJobById(id);
        Company company = companyService.getCompanyByUserId(userId);

        if (!job.isOwnedByCompany(company.getId())) {
            throw new ValidationException("You can only update your own job postings");
        }

        // Update basic info
        if (request.title() != null || request.description() != null) {
            job = jobService.updateJobBasicInfo(id, request.title(), request.description());
        }

        // Update location
        if (request.location() != null) {
            job = jobService.updateJobLocation(id, request.location());
        }

        // Update remote option
        if (request.remoteOption() != null) {
            job = jobService.updateJobRemoteOption(id, request.remoteOption());
        }

        // Update salary
        if (request.salaryMin() != null || request.salaryMax() != null || request.salaryVisible() != null) {
            String currency = job.getSalaryCurrency();
            Money min = request.salaryMin() != null ? new Money(request.salaryMin(), currency) : job.getSalaryMin();
            Money max = request.salaryMax() != null ? new Money(request.salaryMax(), currency) : job.getSalaryMax();
            job = jobService.updateJobSalary(id, min, max, request.salaryVisible());
        }

        // Update experience level
        if (request.experienceLevel() != null) {
            job = jobService.updateJobExperienceLevel(id, request.experienceLevel());
        }

        // Update employment type
        if (request.employmentType() != null) {
            job = jobService.updateJobEmploymentType(id, request.employmentType());
        }

        // Update deadline
        if (request.deadline() != null) {
            job = jobService.updateJobDeadline(id, request.deadline());
        }

        // Update skills (replace all)
        if (request.skills() != null) {
            List<JobSkill> skills = request.skills().stream()
                    .map(input -> {
                        Skill skill = skillRepository.findByNameIgnoreCase(input.skillName())
                                .orElseGet(() -> skillRepository.save(new Skill(input.skillName())));
                        return new JobSkill(skill, input.importance());
                    })
                    .toList();
            job = jobService.setSkills(id, skills);
        }

        return ResponseEntity.ok(toJobResponse(job));
    }

    @DeleteMapping("/jobs/{id}")
    @Operation(summary = "Delete job posting", description = "Delete a draft job posting")
    public ResponseEntity<Void> deleteJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Job job = jobService.getJobById(id);
        Company company = companyService.getCompanyByUserId(userId);

        if (!job.isOwnedByCompany(company.getId())) {
            throw new ValidationException("You can only delete your own job postings");
        }

        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    // ================ Status Transition Endpoints ================

    @PostMapping("/jobs/{id}/publish")
    @Operation(summary = "Publish job", description = "Publish a draft job to make it visible")
    public ResponseEntity<JobResponse> publishJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        validateJobOwnership(jwt, id);
        Job job = jobService.publishJob(id);
        return ResponseEntity.ok(toJobResponse(job));
    }

    @PostMapping("/jobs/{id}/pause")
    @Operation(summary = "Pause job", description = "Temporarily hide an active job")
    public ResponseEntity<JobResponse> pauseJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody(required = false) PauseJobRequest request) {

        validateJobOwnership(jwt, id);
        String reason = request != null ? request.reason() : null;
        Job job = jobService.pauseJob(id, reason);
        return ResponseEntity.ok(toJobResponse(job));
    }

    @PostMapping("/jobs/{id}/resume")
    @Operation(summary = "Resume job", description = "Resume a paused job")
    public ResponseEntity<JobResponse> resumeJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        validateJobOwnership(jwt, id);
        Job job = jobService.resumeJob(id);
        return ResponseEntity.ok(toJobResponse(job));
    }

    @PostMapping("/jobs/{id}/close")
    @Operation(summary = "Close job", description = "Close a job posting")
    public ResponseEntity<JobResponse> closeJob(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody CloseJobRequest request) {

        validateJobOwnership(jwt, id);
        Job job = jobService.closeJob(id, request.reason());
        return ResponseEntity.ok(toJobResponse(job));
    }

    // ================ Statistics Endpoints ================

    @GetMapping("/employer/jobs/stats")
    @Operation(summary = "Get job statistics", description = "Get statistics about employer's jobs")
    public ResponseEntity<JobStatsResponse> getJobStats(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Company company = companyService.getCompanyByUserId(userId);
        UUID companyId = company.getId();

        return ResponseEntity.ok(new JobStatsResponse(
                jobService.countJobsByCompany(companyId),
                jobService.countJobsByCompanyAndStatus(companyId, Job.StatusName.ACTIVE),
                jobService.countJobsByCompanyAndStatus(companyId, Job.StatusName.DRAFT),
                jobService.countJobsByCompanyAndStatus(companyId, Job.StatusName.PAUSED),
                jobService.countJobsByCompanyAndStatus(companyId, Job.StatusName.CLOSED)));
    }

    // ================ Helper Methods ================

    private void validateJobOwnership(Jwt jwt, UUID jobId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Job job = jobService.getJobById(jobId);
        Company company = companyService.getCompanyByUserId(userId);

        if (!job.isOwnedByCompany(company.getId())) {
            throw new ValidationException("You can only manage your own job postings");
        }
    }

    private JobResponse toJobResponse(Job job) {
        Company company = null;
        try {
            company = companyService.getCompanyById(job.getCompanyId());
        } catch (EntityNotFoundException e) {
            // Company might be deleted
        }

        return new JobResponse(
                job.getId(),
                job.getTitle(),
                company != null ? toCompanySummary(company) : null,
                job.getLocation(),
                job.getRemoteOption(),
                job.isSalaryVisible() ? job.getFormattedSalaryRange() : "Competitive",
                job.getExperienceLevel(),
                job.getEmploymentType(),
                job.getStatusName().name(),
                job.getPostedAt(),
                job.getDeadline());
    }

    private JobDetailResponse toJobDetailResponse(Job job, Company company) {
        List<JobSkillResponse> skills = job.getSkills().stream()
                .map(js -> new JobSkillResponse(js.getSkillName(), js.getImportance()))
                .toList();

        return new JobDetailResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                toCompanySummary(company),
                job.getLocation(),
                job.getRemoteOption(),
                job.isSalaryVisible() ? job.getFormattedSalaryRange() : "Competitive",
                job.getExperienceLevel(),
                job.getEmploymentType(),
                job.getStatusName().name(),
                job.getPostedAt(),
                job.getDeadline(),
                skills);
    }

    private CompanySummary toCompanySummary(Company company) {
        return new CompanySummary(
                company.getId(),
                company.getName(),
                company.getLogoUrl(),
                company.getIndustry(),
                company.isVerified());
    }

    // ================ Request/Response Records ================

    public record CreateJobRequest(
            @NotBlank(message = "Title is required") @Size(max = 255, message = "Title must not exceed 255 characters") String title,

            @NotBlank(message = "Description is required") String description,

            Location location,

            @NotNull(message = "Remote option is required") RemoteOption remoteOption,

            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String salaryCurrency,
            Boolean salaryVisible,

            @NotNull(message = "Experience level is required") ExperienceLevel experienceLevel,

            @NotNull(message = "Employment type is required") EmploymentType employmentType,

            List<JobSkillInput> skills,
            LocalDate deadline) {
    }

    public record UpdateJobRequest(
            @Size(max = 255, message = "Title must not exceed 255 characters") String title,
            String description,
            Location location,
            RemoteOption remoteOption,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            Boolean salaryVisible,
            ExperienceLevel experienceLevel,
            EmploymentType employmentType,
            List<JobSkillInput> skills,
            LocalDate deadline) {
    }

    public record JobSkillInput(
            @NotBlank(message = "Skill name is required") String skillName,

            @NotNull(message = "Importance is required") SkillImportance importance) {
    }

    public record PauseJobRequest(String reason) {
    }

    public record CloseJobRequest(
            @NotNull(message = "Close reason is required") JobStatus.CloseReason reason) {
    }

    public record JobResponse(
            UUID id,
            String title,
            CompanySummary company,
            Location location,
            RemoteOption remoteOption,
            String salaryRange,
            ExperienceLevel experienceLevel,
            EmploymentType employmentType,
            String status,
            java.time.Instant postedAt,
            LocalDate deadline) {
    }

    public record JobDetailResponse(
            UUID id,
            String title,
            String description,
            CompanySummary company,
            Location location,
            RemoteOption remoteOption,
            String salaryRange,
            ExperienceLevel experienceLevel,
            EmploymentType employmentType,
            String status,
            java.time.Instant postedAt,
            LocalDate deadline,
            List<JobSkillResponse> skills) {
    }

    public record JobSkillResponse(
            String name,
            SkillImportance importance) {
    }

    public record CompanySummary(
            UUID id,
            String name,
            String logoUrl,
            String industry,
            boolean verified) {
    }

    public record JobStatsResponse(
            long total,
            long active,
            long draft,
            long paused,
            long closed) {
    }
}
