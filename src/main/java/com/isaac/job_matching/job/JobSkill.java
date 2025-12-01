package com.isaac.job_matching.job;

import java.util.UUID;

import com.isaac.job_matching.shared.Skill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing an association between a Job and a required/preferred Skill.
 * 
 * <p>
 * This is a many-to-many relationship with an additional attribute
 * (importance level: MUST_HAVE or NICE_TO_HAVE).
 */
@Entity
@Table(name = "job_skills")
@IdClass(JobSkillId.class)
public class JobSkill {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Id
    @Column(name = "skill_id")
    private UUID skillId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", insertable = false, updatable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", insertable = false, updatable = false)
    private Skill skill;

    @Column(name = "importance", nullable = false, length = 20)
    @NotNull(message = "Skill importance is required")
    @Enumerated(EnumType.STRING)
    private SkillImportance importance;

    protected JobSkill() {
        // JPA constructor
    }

    /**
     * Creates a new JobSkill association.
     * 
     * @param skill the skill
     * @param importance the importance level (MUST_HAVE or NICE_TO_HAVE)
     */
    public JobSkill(Skill skill, SkillImportance importance) {
        this.skill = skill;
        this.skillId = skill.getId();
        this.importance = importance;
    }

    // Getters

    public UUID getJobId() {
        return jobId;
    }

    public UUID getSkillId() {
        return skillId;
    }

    public Job getJob() {
        return job;
    }

    public Skill getSkill() {
        return skill;
    }

    public SkillImportance getImportance() {
        return importance;
    }

    /**
     * Gets the skill name.
     * 
     * @return skill name
     */
    public String getSkillName() {
        return skill != null ? skill.getName() : null;
    }

    /**
     * Gets the skill category.
     * 
     * @return skill category
     */
    public String getSkillCategory() {
        return skill != null ? skill.getCategory() : null;
    }

    /**
     * Checks if this is a required skill.
     * 
     * @return true if MUST_HAVE
     */
    public boolean isMustHave() {
        return importance == SkillImportance.MUST_HAVE;
    }

    /**
     * Checks if this is a preferred but not required skill.
     * 
     * @return true if NICE_TO_HAVE
     */
    public boolean isNiceToHave() {
        return importance == SkillImportance.NICE_TO_HAVE;
    }

    // Setters

    void setJob(Job job) {
        this.job = job;
        this.jobId = job != null ? job.getId() : null;
    }

    public void setImportance(SkillImportance importance) {
        this.importance = importance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobSkill that = (JobSkill) o;
        return jobId != null && jobId.equals(that.jobId)
                && skillId != null && skillId.equals(that.skillId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "JobSkill{jobId=" + jobId + ", skillId=" + skillId 
                + ", importance=" + importance + "}";
    }
}
