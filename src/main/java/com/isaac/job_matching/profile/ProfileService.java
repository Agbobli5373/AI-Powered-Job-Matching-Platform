package com.isaac.job_matching.profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isaac.job_matching.profile.internal.ProfileRepository;
import com.isaac.job_matching.shared.Skill;
import com.isaac.job_matching.shared.SkillRepository;
import com.isaac.job_matching.shared.exception.EntityNotFoundException;
import com.isaac.job_matching.user.UserRegisteredEvent;

/**
 * Public service API for profile management operations.
 * 
 * <p>
 * This is the main entry point for the Profile module, providing:
 * <ul>
 * <li>Profile CRUD operations</li>
 * <li>Work experience management</li>
 * <li>Education management</li>
 * <li>Skills management</li>
 * <li>Profile search visibility control</li>
 * </ul>
 * 
 * <p>
 * Listens for UserRegisteredEvent to automatically create profiles
 * for new job seeker users.
 */
@Service
@Transactional
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProfileService(
            ProfileRepository profileRepository,
            SkillRepository skillRepository,
            ApplicationEventPublisher eventPublisher) {
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.eventPublisher = eventPublisher;
    }

    // ================ Event Listeners ================

    /**
     * Creates a profile when a job seeker registers.
     * 
     * @param event the user registered event
     */
    @ApplicationModuleListener
    public void onUserRegistered(UserRegisteredEvent event) {
        if ("JOB_SEEKER".equals(event.role())) {
            createProfileForUser(event.userId());
        }
    }

    // ================ Profile CRUD ================

    /**
     * Creates a new profile for a user.
     * 
     * @param userId the user ID
     * @return the created profile
     */
    public Profile createProfileForUser(UUID userId) {
        // Check if profile already exists
        if (profileRepository.existsByUserId(userId)) {
            return profileRepository.findByUserId(userId).orElseThrow();
        }

        Profile profile = new Profile(userId);
        return profileRepository.save(profile);
    }

    /**
     * Gets a profile by ID.
     * 
     * @param id the profile ID
     * @return the profile
     * @throws EntityNotFoundException if profile not found
     */
    @Transactional(readOnly = true)
    public Profile getProfileById(UUID id) {
        return profileRepository.findByIdWithAllAssociations(id)
                .orElseThrow(() -> new EntityNotFoundException("Profile", id));
    }

    /**
     * Gets a profile by user ID.
     * 
     * @param userId the user ID
     * @return the profile
     * @throws EntityNotFoundException if profile not found
     */
    @Transactional(readOnly = true)
    public Profile getProfileByUserId(UUID userId) {
        return profileRepository.findByUserIdWithAllAssociations(userId)
                .orElseThrow(() -> new EntityNotFoundException("Profile", "userId", userId.toString()));
    }

    /**
     * Gets a profile by user ID if it exists.
     * 
     * @param userId the user ID
     * @return Optional containing the profile if found
     */
    @Transactional(readOnly = true)
    public Optional<Profile> findProfileByUserId(UUID userId) {
        return profileRepository.findByUserIdWithAllAssociations(userId);
    }

    /**
     * Updates a profile's basic information.
     * 
     * @param profileId the profile ID
     * @param profile   the updated profile data
     * @return the updated profile
     * @throws EntityNotFoundException if profile not found
     */
    public Profile updateProfile(UUID profileId, Profile profile) {
        Profile existingProfile = getProfileById(profileId);
        // The controller/mapper will have updated the profile fields
        Profile savedProfile = profileRepository.save(existingProfile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.BASIC_INFO));

        return savedProfile;
    }

    /**
     * Updates a profile retrieved by its ID after modifications.
     * 
     * @param profile the modified profile entity
     * @return the saved profile
     */
    public Profile saveProfile(Profile profile) {
        return profileRepository.save(profile);
    }

    // ================ Work Experience Management ================

    /**
     * Adds a work experience to a profile.
     * 
     * @param profileId  the profile ID
     * @param experience the work experience to add
     * @return the updated profile
     */
    public Profile addWorkExperience(UUID profileId, WorkExperience experience) {
        Profile profile = getProfileById(profileId);
        profile.addWorkExperience(experience);
        Profile savedProfile = profileRepository.save(profile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.WORK_EXPERIENCE));

        return savedProfile;
    }

    /**
     * Updates a work experience in a profile.
     * 
     * @param profileId    the profile ID
     * @param experienceId the work experience ID
     * @param experience   the updated work experience
     * @return the updated profile
     * @throws EntityNotFoundException if profile or experience not found
     */
    public Profile updateWorkExperience(UUID profileId, UUID experienceId, WorkExperience experience) {
        Profile profile = getProfileById(profileId);
        WorkExperience existing = profile.getWorkExperiences().stream()
                .filter(we -> we.getId().equals(experienceId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("WorkExperience", experienceId));

        // Update existing experience fields
        existing.setCompany(experience.getCompany());
        existing.setRole(experience.getRole());
        existing.setStartDate(experience.getStartDate());
        existing.setEndDate(experience.getEndDate());
        existing.setDescription(experience.getDescription());
        existing.setLocation(experience.getLocation());

        Profile savedProfile = profileRepository.save(profile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.WORK_EXPERIENCE));

        return savedProfile;
    }

    /**
     * Removes a work experience from a profile.
     * 
     * @param profileId    the profile ID
     * @param experienceId the work experience ID
     * @return the updated profile
     * @throws EntityNotFoundException if profile or experience not found
     */
    public Profile removeWorkExperience(UUID profileId, UUID experienceId) {
        Profile profile = getProfileById(profileId);
        WorkExperience experience = profile.getWorkExperiences().stream()
                .filter(we -> we.getId().equals(experienceId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("WorkExperience", experienceId));

        profile.removeWorkExperience(experience);
        Profile savedProfile = profileRepository.save(profile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.WORK_EXPERIENCE));

        return savedProfile;
    }

    // ================ Education Management ================

    /**
     * Adds an education entry to a profile.
     * 
     * @param profileId the profile ID
     * @param education the education to add
     * @return the updated profile
     */
    public Profile addEducation(UUID profileId, Education education) {
        Profile profile = getProfileById(profileId);
        profile.addEducation(education);
        Profile savedProfile = profileRepository.save(profile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.EDUCATION));

        return savedProfile;
    }

    /**
     * Updates an education entry in a profile.
     * 
     * @param profileId   the profile ID
     * @param educationId the education ID
     * @param education   the updated education
     * @return the updated profile
     * @throws EntityNotFoundException if profile or education not found
     */
    public Profile updateEducation(UUID profileId, UUID educationId, Education education) {
        Profile profile = getProfileById(profileId);
        Education existing = profile.getEducations().stream()
                .filter(e -> e.getId().equals(educationId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Education", educationId));

        // Update existing education fields
        existing.setInstitution(education.getInstitution());
        existing.setDegree(education.getDegree());
        existing.setField(education.getField());
        existing.setGraduationYear(education.getGraduationYear());

        Profile savedProfile = profileRepository.save(profile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.EDUCATION));

        return savedProfile;
    }

    /**
     * Removes an education entry from a profile.
     * 
     * @param profileId   the profile ID
     * @param educationId the education ID
     * @return the updated profile
     * @throws EntityNotFoundException if profile or education not found
     */
    public Profile removeEducation(UUID profileId, UUID educationId) {
        Profile profile = getProfileById(profileId);
        Education education = profile.getEducations().stream()
                .filter(e -> e.getId().equals(educationId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Education", educationId));

        profile.removeEducation(education);
        Profile savedProfile = profileRepository.save(profile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.EDUCATION));

        return savedProfile;
    }

    // ================ Skills Management ================

    /**
     * Adds a skill to a profile.
     * 
     * @param profileId        the profile ID
     * @param skillId          the skill ID
     * @param proficiencyLevel the proficiency level
     * @return the updated profile
     * @throws EntityNotFoundException if profile or skill not found
     */
    public Profile addSkill(UUID profileId, UUID skillId, ProficiencyLevel proficiencyLevel) {
        Profile profile = getProfileById(profileId);
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new EntityNotFoundException("Skill", skillId));

        // Check if skill already exists
        boolean alreadyHasSkill = profile.getSkills().stream()
                .anyMatch(ps -> ps.getSkillId().equals(skillId));

        if (alreadyHasSkill) {
            // Update proficiency level instead
            profile.getSkills().stream()
                    .filter(ps -> ps.getSkillId().equals(skillId))
                    .findFirst()
                    .ifPresent(ps -> ps.setProficiencyLevel(proficiencyLevel));
        } else {
            ProfileSkill profileSkill = new ProfileSkill(skill, proficiencyLevel);
            profile.addSkill(profileSkill);
        }

        Profile savedProfile = profileRepository.save(profile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.SKILLS));

        return savedProfile;
    }

    /**
     * Removes a skill from a profile.
     * 
     * @param profileId the profile ID
     * @param skillId   the skill ID
     * @return the updated profile
     * @throws EntityNotFoundException if profile or skill not found
     */
    public Profile removeSkill(UUID profileId, UUID skillId) {
        Profile profile = getProfileById(profileId);
        ProfileSkill skill = profile.getSkills().stream()
                .filter(ps -> ps.getSkillId().equals(skillId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("ProfileSkill", skillId));

        profile.removeSkill(skill);
        Profile savedProfile = profileRepository.save(profile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.SKILLS));

        return savedProfile;
    }

    /**
     * Sets all skills for a profile, replacing existing skills.
     * 
     * @param profileId the profile ID
     * @param skills    list of profile skills to set
     * @return the updated profile
     */
    public Profile setSkills(UUID profileId, List<ProfileSkill> skills) {
        Profile profile = getProfileById(profileId);
        profile.clearSkills();

        for (ProfileSkill skill : skills) {
            profile.addSkill(skill);
        }

        Profile savedProfile = profileRepository.save(profile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.SKILLS));

        return savedProfile;
    }

    // ================ Profile Visibility ================

    /**
     * Sets the searchability of a profile.
     * 
     * @param profileId  the profile ID
     * @param searchable whether the profile should be searchable
     * @return the updated profile
     */
    public Profile setSearchable(UUID profileId, boolean searchable) {
        Profile profile = getProfileById(profileId);
        profile.setSearchable(searchable);
        Profile savedProfile = profileRepository.save(profile);

        eventPublisher.publishEvent(
                ProfileUpdatedEvent.from(savedProfile, ProfileUpdatedEvent.UpdateType.PREFERENCES));

        return savedProfile;
    }

    // ================ Statistics ================

    /**
     * Gets count of searchable profiles.
     * 
     * @return count of searchable profiles
     */
    @Transactional(readOnly = true)
    public long countSearchable() {
        return profileRepository.countSearchable();
    }

    /**
     * Gets count of complete profiles.
     * 
     * @return count of complete profiles
     */
    @Transactional(readOnly = true)
    public long countComplete() {
        return profileRepository.countComplete();
    }
}
