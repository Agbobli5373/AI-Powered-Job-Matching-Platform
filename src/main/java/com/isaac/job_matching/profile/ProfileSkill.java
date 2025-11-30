package com.isaac.job_matching.profile;

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
 * Entity representing an association between a Profile and a Skill.
 * 
 * <p>
 * This is a many-to-many relationship with an additional attribute
 * (proficiency level).
 */
@Entity
@Table(name = "profile_skills")
@IdClass(ProfileSkillId.class)
public class ProfileSkill {

    @Id
    @Column(name = "profile_id")
    private UUID profileId;

    @Id
    @Column(name = "skill_id")
    private UUID skillId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", insertable = false, updatable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", insertable = false, updatable = false)
    private Skill skill;

    @Column(name = "proficiency_level", nullable = false, length = 20)
    @NotNull(message = "Proficiency level is required")
    @Enumerated(EnumType.STRING)
    private ProficiencyLevel proficiencyLevel;

    protected ProfileSkill() {
        // JPA constructor
    }

    public ProfileSkill(Skill skill, ProficiencyLevel proficiencyLevel) {
        this.skill = skill;
        this.skillId = skill.getId();
        this.proficiencyLevel = proficiencyLevel;
    }

    // Getters

    public UUID getProfileId() {
        return profileId;
    }

    public UUID getSkillId() {
        return skillId;
    }

    public Profile getProfile() {
        return profile;
    }

    public Skill getSkill() {
        return skill;
    }

    public ProficiencyLevel getProficiencyLevel() {
        return proficiencyLevel;
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

    // Setters

    void setProfile(Profile profile) {
        this.profile = profile;
        this.profileId = profile != null ? profile.getId() : null;
    }

    public void setProficiencyLevel(ProficiencyLevel proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProfileSkill that = (ProfileSkill) o;
        return profileId != null && profileId.equals(that.profileId)
                && skillId != null && skillId.equals(that.skillId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ProfileSkill{profileId=" + profileId + ", skillId=" + skillId
                + ", proficiency=" + proficiencyLevel + "}";
    }
}
