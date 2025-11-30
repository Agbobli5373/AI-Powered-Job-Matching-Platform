package com.isaac.job_matching.profile;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key class for ProfileSkill entity.
 */
public class ProfileSkillId implements Serializable {

    private UUID profileId;
    private UUID skillId;

    public ProfileSkillId() {
    }

    public ProfileSkillId(UUID profileId, UUID skillId) {
        this.profileId = profileId;
        this.skillId = skillId;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public void setProfileId(UUID profileId) {
        this.profileId = profileId;
    }

    public UUID getSkillId() {
        return skillId;
    }

    public void setSkillId(UUID skillId) {
        this.skillId = skillId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProfileSkillId that = (ProfileSkillId) o;
        return Objects.equals(profileId, that.profileId)
                && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profileId, skillId);
    }
}
