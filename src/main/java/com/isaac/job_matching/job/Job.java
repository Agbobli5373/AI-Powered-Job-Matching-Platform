package com.isaac.job_matching.job;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.isaac.job_matching.shared.BaseEntity;
import com.isaac.job_matching.shared.Location;
import com.isaac.job_matching.shared.Money;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Job entity representing a job posting from an employer.
 * 
 * <p>
 * This is the aggregate root for the Job module. Jobs are owned by
 * companies and can receive applications from job seekers.
 * 
 * <p>
 * Job lifecycle:
 * <ol>
 *   <li>DRAFT: Job created but not published</li>
 *   <li>ACTIVE: Job published and accepting applications</li>
 *   <li>PAUSED: Temporarily hidden (can be reactivated)</li>
 *   <li>CLOSED: No longer accepting applications</li>
 * </ol>
 */
@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "title", nullable = false, length = 255)
    @NotBlank(message = "Job title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Job description is required")
    private String description;

    // Location (embedded columns)
    @Column(name = "location_city", length = 100)
    private String locationCity;

    @Column(name = "location_state", length = 100)
    private String locationState;

    @Column(name = "location_country", length = 100)
    private String locationCountry;

    @Column(name = "location_latitude")
    private Double locationLatitude;

    @Column(name = "location_longitude")
    private Double locationLongitude;

    @Column(name = "remote_option", nullable = false, length = 50)
    @NotNull(message = "Remote option is required")
    @Enumerated(EnumType.STRING)
    private RemoteOption remoteOption;

    // Salary (embedded columns)
    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "salary_currency", length = 3)
    private String salaryCurrency = "USD";

    @Column(name = "salary_visible", nullable = false)
    private boolean salaryVisible = true;

    @Column(name = "experience_level", nullable = false, length = 50)
    @NotNull(message = "Experience level is required")
    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    @Column(name = "employment_type", nullable = false, length = 50)
    @NotNull(message = "Employment type is required")
    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    // Status stored as string with additional data in JSON
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private StatusName statusName = StatusName.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_data", columnDefinition = "jsonb")
    private Map<String, Object> statusData = new HashMap<>();

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "deadline")
    private LocalDate deadline;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JobSkill> skills = new ArrayList<>();

    // Transient field for rich status object
    @Transient
    private JobStatus status;

    /**
     * Internal enum for database storage of status name.
     */
    public enum StatusName {
        DRAFT, ACTIVE, PAUSED, CLOSED
    }

    protected Job() {
        // JPA constructor
    }

    /**
     * Creates a new job posting for a company.
     * 
     * @param companyId the company ID
     * @param title the job title
     * @param description the job description
     * @param remoteOption the remote work option
     * @param experienceLevel the required experience level
     * @param employmentType the employment type
     */
    public Job(UUID companyId, String title, String description,
               RemoteOption remoteOption, ExperienceLevel experienceLevel,
               EmploymentType employmentType) {
        this.companyId = companyId;
        this.title = title;
        this.description = description;
        this.remoteOption = remoteOption;
        this.experienceLevel = experienceLevel;
        this.employmentType = employmentType;
        this.statusName = StatusName.DRAFT;
        this.status = new JobStatus.Draft();
    }

    // ================ Getters ================

    public UUID getCompanyId() {
        return companyId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the job location as a Location object.
     * 
     * @return the location, or null if no location is set
     */
    public Location getLocation() {
        if (locationCity == null && locationState == null && locationCountry == null) {
            return null;
        }
        return new Location(locationCity, locationState, locationCountry, 
                           locationLatitude, locationLongitude);
    }

    public RemoteOption getRemoteOption() {
        return remoteOption;
    }

    /**
     * Gets the minimum salary as a Money object.
     * 
     * @return the minimum salary, or null if not set
     */
    public Money getSalaryMin() {
        return salaryMin != null ? new Money(salaryMin, salaryCurrency) : null;
    }

    /**
     * Gets the maximum salary as a Money object.
     * 
     * @return the maximum salary, or null if not set
     */
    public Money getSalaryMax() {
        return salaryMax != null ? new Money(salaryMax, salaryCurrency) : null;
    }

    public String getSalaryCurrency() {
        return salaryCurrency;
    }

    public boolean isSalaryVisible() {
        return salaryVisible;
    }

    public ExperienceLevel getExperienceLevel() {
        return experienceLevel;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    /**
     * Gets the job status as a rich object.
     * 
     * @return the job status
     */
    public JobStatus getStatus() {
        if (status == null) {
            status = JobStatus.fromDatabase(statusName.name(), statusData, postedAt);
        }
        return status;
    }

    /**
     * Gets the status name for simple queries.
     * 
     * @return the status name
     */
    public StatusName getStatusName() {
        return statusName;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public List<JobSkill> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    /**
     * Gets the required (MUST_HAVE) skills.
     * 
     * @return list of required skills
     */
    public List<JobSkill> getRequiredSkills() {
        return skills.stream()
                .filter(JobSkill::isMustHave)
                .toList();
    }

    /**
     * Gets the preferred (NICE_TO_HAVE) skills.
     * 
     * @return list of preferred skills
     */
    public List<JobSkill> getPreferredSkills() {
        return skills.stream()
                .filter(JobSkill::isNiceToHave)
                .toList();
    }

    // ================ Setters ================

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the job location.
     * 
     * @param location the location to set
     */
    public void setLocation(Location location) {
        if (location == null) {
            this.locationCity = null;
            this.locationState = null;
            this.locationCountry = null;
            this.locationLatitude = null;
            this.locationLongitude = null;
        } else {
            this.locationCity = location.city();
            this.locationState = location.state();
            this.locationCountry = location.country();
            this.locationLatitude = location.latitude();
            this.locationLongitude = location.longitude();
        }
    }

    public void setRemoteOption(RemoteOption remoteOption) {
        this.remoteOption = remoteOption;
    }

    /**
     * Sets the salary range.
     * 
     * @param min minimum salary
     * @param max maximum salary
     */
    public void setSalaryRange(Money min, Money max) {
        if (min != null) {
            this.salaryMin = min.amount();
            this.salaryCurrency = min.currency();
        } else {
            this.salaryMin = null;
        }
        
        if (max != null) {
            this.salaryMax = max.amount();
            if (min == null) {
                this.salaryCurrency = max.currency();
            }
        } else {
            this.salaryMax = null;
        }
    }

    public void setSalaryVisible(boolean salaryVisible) {
        this.salaryVisible = salaryVisible;
    }

    public void setExperienceLevel(ExperienceLevel experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    // ================ Skill Management ================

    /**
     * Adds a skill requirement to the job.
     * 
     * @param skill the skill to add
     */
    public void addSkill(JobSkill skill) {
        skill.setJob(this);
        skills.add(skill);
    }

    /**
     * Removes a skill requirement from the job.
     * 
     * @param skill the skill to remove
     */
    public void removeSkill(JobSkill skill) {
        skills.remove(skill);
        skill.setJob(null);
    }

    /**
     * Clears all skills from the job.
     */
    public void clearSkills() {
        skills.forEach(s -> s.setJob(null));
        skills.clear();
    }

    /**
     * Checks if the job has a specific skill requirement.
     * 
     * @param skillId the skill ID to check
     * @return true if the job requires or prefers this skill
     */
    public boolean hasSkill(UUID skillId) {
        return skills.stream().anyMatch(s -> s.getSkillId().equals(skillId));
    }

    // ================ Status Transitions ================

    /**
     * Publishes the job, making it visible to job seekers.
     * 
     * @throws IllegalStateException if job is not in DRAFT or PAUSED status
     */
    public void publish() {
        if (statusName != StatusName.DRAFT && statusName != StatusName.PAUSED) {
            throw new IllegalStateException("Can only publish jobs in DRAFT or PAUSED status");
        }
        
        this.statusName = StatusName.ACTIVE;
        this.postedAt = (this.postedAt == null) ? Instant.now() : this.postedAt;
        this.status = new JobStatus.Active(this.postedAt);
        this.statusData = new HashMap<>();
    }

    /**
     * Pauses the job, temporarily hiding it from job seekers.
     * 
     * @param reason optional reason for pausing
     * @throws IllegalStateException if job is not in ACTIVE status
     */
    public void pause(String reason) {
        if (statusName != StatusName.ACTIVE) {
            throw new IllegalStateException("Can only pause ACTIVE jobs");
        }
        
        this.statusName = StatusName.PAUSED;
        this.status = new JobStatus.Paused(reason);
        this.statusData = new HashMap<>();
        if (reason != null) {
            this.statusData.put("pauseReason", reason);
        }
    }

    /**
     * Closes the job, preventing further applications.
     * 
     * @param reason the reason for closing
     * @throws IllegalStateException if job is already closed
     */
    public void close(JobStatus.CloseReason reason) {
        if (statusName == StatusName.CLOSED) {
            throw new IllegalStateException("Job is already closed");
        }
        
        this.statusName = StatusName.CLOSED;
        this.status = new JobStatus.Closed(reason);
        this.statusData = new HashMap<>();
        this.statusData.put("closeReason", reason.name());
    }

    // ================ Business Logic ================

    /**
     * Checks if the job is owned by the specified company.
     * 
     * @param companyId the company ID to check
     * @return true if the company owns this job
     */
    public boolean isOwnedByCompany(UUID companyId) {
        return this.companyId.equals(companyId);
    }

    /**
     * Checks if the job is accepting applications.
     * 
     * @return true if job is ACTIVE and deadline hasn't passed
     */
    public boolean isAcceptingApplications() {
        if (statusName != StatusName.ACTIVE) {
            return false;
        }
        if (deadline != null && LocalDate.now().isAfter(deadline)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the job is visible to job seekers.
     * 
     * @return true if job is ACTIVE
     */
    public boolean isVisible() {
        return statusName == StatusName.ACTIVE;
    }

    /**
     * Checks if the job can be edited.
     * 
     * @return true if job is DRAFT or PAUSED
     */
    public boolean isEditable() {
        return statusName == StatusName.DRAFT || statusName == StatusName.PAUSED;
    }

    /**
     * Checks if the deadline has passed.
     * 
     * @return true if deadline exists and has passed
     */
    public boolean isDeadlinePassed() {
        return deadline != null && LocalDate.now().isAfter(deadline);
    }

    /**
     * Gets a formatted salary range string.
     * 
     * @return formatted salary range or "Not specified"
     */
    public String getFormattedSalaryRange() {
        if (salaryMin == null && salaryMax == null) {
            return "Not specified";
        }
        if (salaryMin != null && salaryMax != null) {
            return String.format("%s %,.0f - %,.0f", salaryCurrency, salaryMin, salaryMax);
        }
        if (salaryMin != null) {
            return String.format("%s %,.0f+", salaryCurrency, salaryMin);
        }
        return String.format("Up to %s %,.0f", salaryCurrency, salaryMax);
    }

    /**
     * Checks if the job has at least one MUST_HAVE skill.
     * 
     * @return true if at least one required skill exists
     */
    public boolean hasRequiredSkills() {
        return skills.stream().anyMatch(JobSkill::isMustHave);
    }

    /**
     * Validates the job is ready for publication.
     * 
     * @return list of validation errors (empty if valid)
     */
    public List<String> validateForPublication() {
        List<String> errors = new ArrayList<>();
        
        if (title == null || title.isBlank()) {
            errors.add("Title is required");
        }
        if (description == null || description.isBlank()) {
            errors.add("Description is required");
        }
        if (remoteOption == null) {
            errors.add("Remote option is required");
        }
        if (experienceLevel == null) {
            errors.add("Experience level is required");
        }
        if (employmentType == null) {
            errors.add("Employment type is required");
        }
        if (remoteOption != RemoteOption.REMOTE && getLocation() == null) {
            errors.add("Location is required for non-remote jobs");
        }
        if (!hasRequiredSkills()) {
            errors.add("At least one required skill (MUST_HAVE) is needed");
        }
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            errors.add("Deadline must be in the future");
        }
        if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
            errors.add("Minimum salary cannot exceed maximum salary");
        }
        
        return errors;
    }

    @Override
    public String toString() {
        return "Job{id=" + getId() + ", title='" + title + "', company=" + companyId
                + ", status=" + statusName + "}";
    }
}
