package com.isaac.job_matching.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.isaac.job_matching.profile.internal.ProfileRepository;
import com.isaac.job_matching.shared.Skill;
import com.isaac.job_matching.shared.SkillRepository;
import com.isaac.job_matching.shared.exception.EntityNotFoundException;
import com.isaac.job_matching.user.UserRegisteredEvent;

/**
 * Unit tests for ProfileService.
 * 
 * <p>
 * Test Categories:
 * <ul>
 * <li>Profile Creation - event-driven and manual creation</li>
 * <li>Profile Lookup - by ID and user ID</li>
 * <li>Profile Update - basic info updates</li>
 * <li>Work Experience Management - add, update, remove</li>
 * <li>Education Management - add, update, remove</li>
 * <li>Skills Management - add, update, remove, set all</li>
 * <li>Profile Visibility - searchability toggle</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceUnitTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(profileRepository, skillRepository, eventPublisher);
    }

    // ==================== Profile Creation Tests ====================

    @Nested
    @DisplayName("Profile Creation Tests")
    class ProfileCreationTests {

        @Test
        @DisplayName("Should create profile on UserRegisteredEvent for job seeker")
        void shouldCreateProfileOnUserRegisteredEvent() {
            // Given
            UUID userId = UUID.randomUUID();
            UserRegisteredEvent event = new UserRegisteredEvent(userId, "jobseeker@example.com", "JOB_SEEKER", java.time.Instant.now());

            when(profileRepository.existsByUserId(userId)).thenReturn(false);
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            profileService.onUserRegistered(event);

            // Then
            ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepository).save(profileCaptor.capture());

            Profile savedProfile = profileCaptor.getValue();
            assertThat(savedProfile.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should not create profile for employer on UserRegisteredEvent")
        void shouldNotCreateProfileForEmployer() {
            // Given
            UUID userId = UUID.randomUUID();
            UserRegisteredEvent event = new UserRegisteredEvent(userId, "employer@example.com", "EMPLOYER", java.time.Instant.now());

            // When
            profileService.onUserRegistered(event);

            // Then
            verify(profileRepository, never()).save(any(Profile.class));
        }

        @Test
        @DisplayName("Should create profile for user manually")
        void shouldCreateProfileForUserManually() {
            // Given
            UUID userId = UUID.randomUUID();

            when(profileRepository.existsByUserId(userId)).thenReturn(false);
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.createProfileForUser(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            verify(profileRepository).save(any(Profile.class));
        }

        @Test
        @DisplayName("Should return existing profile if already exists")
        void shouldReturnExistingProfile() {
            // Given
            UUID userId = UUID.randomUUID();
            Profile existingProfile = new Profile(userId);

            when(profileRepository.existsByUserId(userId)).thenReturn(true);
            when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

            // When
            Profile result = profileService.createProfileForUser(userId);

            // Then
            assertThat(result).isSameAs(existingProfile);
            verify(profileRepository, never()).save(any(Profile.class));
        }
    }

    // ==================== Profile Lookup Tests ====================

    @Nested
    @DisplayName("Profile Lookup Tests")
    class ProfileLookupTests {

        @Test
        @DisplayName("Should get profile by ID successfully")
        void shouldGetProfileById() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));

            // When
            Profile result = profileService.getProfileById(profileId);

            // Then
            assertThat(result).isSameAs(profile);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when profile not found by ID")
        void shouldThrowExceptionWhenProfileNotFoundById() {
            // Given
            UUID profileId = UUID.randomUUID();

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> profileService.getProfileById(profileId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Profile");
        }

        @Test
        @DisplayName("Should get profile by user ID successfully")
        void shouldGetProfileByUserId() {
            // Given
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);

            when(profileRepository.findByUserIdWithAllAssociations(userId)).thenReturn(Optional.of(profile));

            // When
            Profile result = profileService.getProfileByUserId(userId);

            // Then
            assertThat(result).isSameAs(profile);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when profile not found by user ID")
        void shouldThrowExceptionWhenProfileNotFoundByUserId() {
            // Given
            UUID userId = UUID.randomUUID();

            when(profileRepository.findByUserIdWithAllAssociations(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> profileService.getProfileByUserId(userId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Profile");
        }

        @Test
        @DisplayName("Should find profile by user ID optionally")
        void shouldFindProfileByUserIdOptionally() {
            // Given
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);

            when(profileRepository.findByUserIdWithAllAssociations(userId)).thenReturn(Optional.of(profile));

            // When
            Optional<Profile> result = profileService.findProfileByUserId(userId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(profile);
        }

        @Test
        @DisplayName("Should return empty optional when profile not found")
        void shouldReturnEmptyWhenProfileNotFound() {
            // Given
            UUID userId = UUID.randomUUID();

            when(profileRepository.findByUserIdWithAllAssociations(userId)).thenReturn(Optional.empty());

            // When
            Optional<Profile> result = profileService.findProfileByUserId(userId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== Profile Update Tests ====================

    @Nested
    @DisplayName("Profile Update Tests")
    class ProfileUpdateTests {

        @Test
        @DisplayName("Should save profile and publish event")
        void shouldSaveProfileAndPublishEvent() {
            // Given
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);

            when(profileRepository.save(profile)).thenReturn(profile);

            // When
            Profile result = profileService.saveProfile(profile);

            // Then
            assertThat(result).isSameAs(profile);
            verify(profileRepository).save(profile);
        }
    }

    // ==================== Work Experience Management Tests ====================

    @Nested
    @DisplayName("Work Experience Management Tests")
    class WorkExperienceTests {

        @Test
        @DisplayName("Should add work experience to profile")
        void shouldAddWorkExperience() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            WorkExperience experience = new WorkExperience("Company Inc", "Developer", LocalDate.now().minusYears(1));

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.addWorkExperience(profileId, experience);

            // Then
            assertThat(result.getWorkExperiences()).hasSize(1);
            assertThat(result.getWorkExperiences().get(0).getCompany()).isEqualTo("Company Inc");
            verify(eventPublisher).publishEvent(any(ProfileUpdatedEvent.class));
        }

        @Test
        @DisplayName("Should update existing work experience")
        void shouldUpdateWorkExperience() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            WorkExperience existingExperience = new WorkExperience("Old Company", "Developer", LocalDate.now().minusYears(2));
            profile.addWorkExperience(existingExperience);
            UUID experienceId = existingExperience.getId();

            WorkExperience updatedExperience = new WorkExperience("New Company", "Senior Developer", LocalDate.now().minusYears(1));
            updatedExperience.setDescription("Updated description");

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.updateWorkExperience(profileId, experienceId, updatedExperience);

            // Then
            WorkExperience updated = result.getWorkExperiences().get(0);
            assertThat(updated.getCompany()).isEqualTo("New Company");
            assertThat(updated.getRole()).isEqualTo("Senior Developer");
            assertThat(updated.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent work experience")
        void shouldThrowExceptionWhenUpdatingNonExistentExperience() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            UUID nonExistentExperienceId = UUID.randomUUID();
            WorkExperience updatedExperience = new WorkExperience("Company", "Role", LocalDate.now());

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));

            // When/Then
            assertThatThrownBy(() -> profileService.updateWorkExperience(profileId, nonExistentExperienceId, updatedExperience))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("WorkExperience");
        }

        @Test
        @DisplayName("Should remove work experience from profile")
        void shouldRemoveWorkExperience() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            WorkExperience experience = new WorkExperience("Company", "Developer", LocalDate.now().minusYears(1));
            profile.addWorkExperience(experience);
            UUID experienceId = experience.getId();

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.removeWorkExperience(profileId, experienceId);

            // Then
            assertThat(result.getWorkExperiences()).isEmpty();
            verify(eventPublisher).publishEvent(any(ProfileUpdatedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when removing non-existent work experience")
        void shouldThrowExceptionWhenRemovingNonExistentExperience() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            UUID nonExistentExperienceId = UUID.randomUUID();

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));

            // When/Then
            assertThatThrownBy(() -> profileService.removeWorkExperience(profileId, nonExistentExperienceId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ==================== Education Management Tests ====================

    @Nested
    @DisplayName("Education Management Tests")
    class EducationTests {

        @Test
        @DisplayName("Should add education to profile")
        void shouldAddEducation() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            Education education = new Education("MIT", "BS", "Computer Science", 2020);

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.addEducation(profileId, education);

            // Then
            assertThat(result.getEducations()).hasSize(1);
            assertThat(result.getEducations().get(0).getInstitution()).isEqualTo("MIT");
            verify(eventPublisher).publishEvent(any(ProfileUpdatedEvent.class));
        }

        @Test
        @DisplayName("Should update existing education")
        void shouldUpdateEducation() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            Education existingEducation = new Education("Old University", "BA", "History", 2019);
            profile.addEducation(existingEducation);
            UUID educationId = existingEducation.getId();

            Education updatedEducation = new Education("New University", "MS", "Computer Science", 2022);

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.updateEducation(profileId, educationId, updatedEducation);

            // Then
            Education updated = result.getEducations().get(0);
            assertThat(updated.getInstitution()).isEqualTo("New University");
            assertThat(updated.getDegree()).isEqualTo("MS");
            assertThat(updated.getField()).isEqualTo("Computer Science");
            assertThat(updated.getGraduationYear()).isEqualTo(2022);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent education")
        void shouldThrowExceptionWhenUpdatingNonExistentEducation() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            UUID nonExistentEducationId = UUID.randomUUID();
            Education updatedEducation = new Education("University", "BS", "CS", 2020);

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));

            // When/Then
            assertThatThrownBy(() -> profileService.updateEducation(profileId, nonExistentEducationId, updatedEducation))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Education");
        }

        @Test
        @DisplayName("Should remove education from profile")
        void shouldRemoveEducation() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            Education education = new Education("University", "BS", "CS", 2020);
            profile.addEducation(education);
            UUID educationId = education.getId();

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.removeEducation(profileId, educationId);

            // Then
            assertThat(result.getEducations()).isEmpty();
            verify(eventPublisher).publishEvent(any(ProfileUpdatedEvent.class));
        }
    }

    // ==================== Skills Management Tests ====================

    @Nested
    @DisplayName("Skills Management Tests")
    class SkillsTests {

        @Test
        @DisplayName("Should add skill to profile")
        void shouldAddSkill() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID skillId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            Skill skill = createSkill(skillId, "Java", "Programming");

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.addSkill(profileId, skillId, ProficiencyLevel.INTERMEDIATE);

            // Then
            assertThat(result.getSkills()).hasSize(1);
            assertThat(result.getSkills().get(0).getProficiencyLevel()).isEqualTo(ProficiencyLevel.INTERMEDIATE);
            verify(eventPublisher).publishEvent(any(ProfileUpdatedEvent.class));
        }

        @Test
        @DisplayName("Should update proficiency level for existing skill")
        void shouldUpdateProficiencyLevel() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID skillId = UUID.randomUUID();
            Skill skill = createSkill(skillId, "Java", "Programming");
            Profile profile = new Profile(userId);
            ProfileSkill existingSkill = new ProfileSkill(skill, ProficiencyLevel.BEGINNER);
            profile.addSkill(existingSkill);

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(skillRepository.findById(skillId)).thenReturn(Optional.of(skill));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.addSkill(profileId, skillId, ProficiencyLevel.EXPERT);

            // Then
            assertThat(result.getSkills()).hasSize(1);
            assertThat(result.getSkills().get(0).getProficiencyLevel()).isEqualTo(ProficiencyLevel.EXPERT);
        }

        @Test
        @DisplayName("Should throw exception when adding non-existent skill")
        void shouldThrowExceptionWhenAddingNonExistentSkill() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID nonExistentSkillId = UUID.randomUUID();
            Profile profile = new Profile(userId);

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(skillRepository.findById(nonExistentSkillId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> profileService.addSkill(profileId, nonExistentSkillId, ProficiencyLevel.INTERMEDIATE))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Skill");
        }

        @Test
        @DisplayName("Should remove skill from profile")
        void shouldRemoveSkill() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID skillId = UUID.randomUUID();
            Skill skill = createSkill(skillId, "Java", "Programming");
            Profile profile = new Profile(userId);
            ProfileSkill profileSkill = new ProfileSkill(skill, ProficiencyLevel.INTERMEDIATE);
            profile.addSkill(profileSkill);

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.removeSkill(profileId, skillId);

            // Then
            assertThat(result.getSkills()).isEmpty();
            verify(eventPublisher).publishEvent(any(ProfileUpdatedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when removing non-existent skill")
        void shouldThrowExceptionWhenRemovingNonExistentSkill() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID nonExistentSkillId = UUID.randomUUID();
            Profile profile = new Profile(userId);

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));

            // When/Then
            assertThatThrownBy(() -> profileService.removeSkill(profileId, nonExistentSkillId))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Should set all skills replacing existing")
        void shouldSetAllSkills() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            
            // Add existing skill
            UUID existingSkillId = UUID.randomUUID();
            Skill existingSkill = createSkill(existingSkillId, "Python", "Programming");
            profile.addSkill(new ProfileSkill(existingSkill, ProficiencyLevel.BEGINNER));

            // New skills to set
            UUID newSkillId = UUID.randomUUID();
            Skill newSkill = createSkill(newSkillId, "Java", "Programming");
            List<ProfileSkill> newSkills = List.of(new ProfileSkill(newSkill, ProficiencyLevel.EXPERT));

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.setSkills(profileId, newSkills);

            // Then
            assertThat(result.getSkills()).hasSize(1);
            verify(eventPublisher).publishEvent(any(ProfileUpdatedEvent.class));
        }

        private Skill createSkill(UUID id, String name, String category) {
            // Using reflection to set the ID since Skill extends BaseEntity
            Skill skill = new Skill(name, category);
            try {
                java.lang.reflect.Field idField = skill.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(skill, id);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set skill ID", e);
            }
            return skill;
        }
    }

    // ==================== Profile Visibility Tests ====================

    @Nested
    @DisplayName("Profile Visibility Tests")
    class VisibilityTests {

        @Test
        @DisplayName("Should set profile as searchable")
        void shouldSetProfileAsSearchable() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            profile.setSearchable(false);

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.setSearchable(profileId, true);

            // Then
            assertThat(result.isSearchable()).isTrue();
            verify(eventPublisher).publishEvent(any(ProfileUpdatedEvent.class));
        }

        @Test
        @DisplayName("Should set profile as not searchable")
        void shouldSetProfileAsNotSearchable() {
            // Given
            UUID profileId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Profile profile = new Profile(userId);
            profile.setSearchable(true);

            when(profileRepository.findByIdWithAllAssociations(profileId)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Profile result = profileService.setSearchable(profileId, false);

            // Then
            assertThat(result.isSearchable()).isFalse();
        }
    }

    // ==================== Statistics Tests ====================

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should count searchable profiles")
        void shouldCountSearchableProfiles() {
            // Given
            when(profileRepository.countSearchable()).thenReturn(50L);

            // When
            long count = profileService.countSearchable();

            // Then
            assertThat(count).isEqualTo(50L);
        }

        @Test
        @DisplayName("Should count complete profiles")
        void shouldCountCompleteProfiles() {
            // Given
            when(profileRepository.countComplete()).thenReturn(30L);

            // When
            long count = profileService.countComplete();

            // Then
            assertThat(count).isEqualTo(30L);
        }
    }
}
