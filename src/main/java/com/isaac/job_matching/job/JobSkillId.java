package com.isaac.job_matching.job;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for JobSkill entity.
 */
public class JobSkillId implements Serializable {
    
    private UUID jobId;
    private UUID skillId;

    public JobSkillId() {
    }

    public JobSkillId(UUID jobId, UUID skillId) {
        this.jobId = jobId;
        this.skillId = skillId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getSkillId() {
        return skillId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobSkillId that = (JobSkillId) o;
        return Objects.equals(jobId, that.jobId) && 
               Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, skillId);
    }
}
