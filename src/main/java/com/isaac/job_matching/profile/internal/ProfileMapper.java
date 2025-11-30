package com.isaac.job_matching.profile.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.isaac.job_matching.profile.Availability;
import com.isaac.job_matching.profile.Education;
import com.isaac.job_matching.profile.ProficiencyLevel;
import com.isaac.job_matching.profile.Profile;
import com.isaac.job_matching.profile.ProfileSkill;
import com.isaac.job_matching.profile.RemotePreference;
import com.isaac.job_matching.profile.WorkExperience;
import com.isaac.job_matching.shared.Location;
import com.isaac.job_matching.shared.Money;
import com.isaac.job_matching.shared.Skill;

/**
 * Mapper for converting between Profile entities and DTOs.
 * 
 * <p>
 * This is an internal class that handles the mapping between:
 * <ul>
 * <li>Profile entities and API response DTOs</li>
 * <li>API request DTOs and Profile entities</li>
 * </ul>
 */
@Component
public class ProfileMapper {

    // ================ Response DTOs ================

    /**
     * Converts a Profile entity to a ProfileResponse DTO.
     * 
     * @param profile the profile entity
     * @return the response DTO
     */
    public ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getFullName(),
                profile.getBio(),
                toLocationResponse(profile.getLocation()),
                toMoneyResponse(profile.getSalaryExpectationMin()),
                toMoneyResponse(profile.getSalaryExpectationMax()),
                profile.getAvailability() != null ? profile.getAvailability().name() : null,
                profile.getRemotePreference() != null ? profile.getRemotePreference().name() : null,
                profile.isProfileComplete(),
                profile.isSearchable(),
                profile.getHeadline(),
                profile.getTotalExperienceYears(),
                profile.getWorkExperiences().stream().map(this::toWorkExperienceResponse).toList(),
                profile.getEducations().stream().map(this::toEducationResponse).toList(),
                profile.getSkills().stream().map(this::toSkillResponse).toList(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }

    /**
     * Converts a WorkExperience entity to a response DTO.
     */
    public WorkExperienceResponse toWorkExperienceResponse(WorkExperience experience) {
        return new WorkExperienceResponse(
                experience.getId(),
                experience.getCompany(),
                experience.getRole(),
                experience.getStartDate(),
                experience.getEndDate(),
                experience.isCurrent(),
                experience.getDescription(),
                toLocationResponse(experience.getLocation()),
                experience.getDateRange().durationInYears());
    }

    /**
     * Converts an Education entity to a response DTO.
     */
    public EducationResponse toEducationResponse(Education education) {
        return new EducationResponse(
                education.getId(),
                education.getInstitution(),
                education.getDegree(),
                education.getField(),
                education.getGraduationYear(),
                education.getFormatted());
    }

    /**
     * Converts a ProfileSkill entity to a response DTO.
     */
    public ProfileSkillResponse toSkillResponse(ProfileSkill skill) {
        return new ProfileSkillResponse(
                skill.getSkillId(),
                skill.getSkillName(),
                skill.getSkillCategory(),
                skill.getProficiencyLevel().name());
    }

    /**
     * Converts a Location to a response DTO.
     */
    public LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationResponse(
                location.city(),
                location.state(),
                location.country(),
                location.latitude(),
                location.longitude());
    }

    /**
     * Converts a Money to a response DTO.
     */
    public MoneyResponse toMoneyResponse(Money money) {
        if (money == null) {
            return null;
        }
        return new MoneyResponse(money.amount(), money.currency());
    }

    // ================ Entity Updates from Requests ================

    /**
     * Updates a Profile entity from an update request.
     * 
     * @param profile the profile to update
     * @param request the update request
     */
    public void updateFromRequest(Profile profile, UpdateProfileRequest request) {
        if (request.firstName() != null) {
            profile.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            profile.setLastName(request.lastName());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.location() != null) {
            profile.setLocation(toLocation(request.location()));
        }
        if (request.salaryExpectationMin() != null || request.salaryExpectationMax() != null) {
            profile.setSalaryExpectation(
                    request.salaryExpectationMin() != null ? toMoney(request.salaryExpectationMin()) : null,
                    request.salaryExpectationMax() != null ? toMoney(request.salaryExpectationMax()) : null);
        }
        if (request.availability() != null) {
            profile.setAvailability(Availability.valueOf(request.availability()));
        }
        if (request.remotePreference() != null) {
            profile.setRemotePreference(RemotePreference.valueOf(request.remotePreference()));
        }
        if (request.searchable() != null) {
            profile.setSearchable(request.searchable());
        }
    }

    /**
     * Creates a WorkExperience entity from a request.
     * 
     * @param request the create request
     * @return the work experience entity
     */
    public WorkExperience toWorkExperience(CreateWorkExperienceRequest request) {
        WorkExperience experience = new WorkExperience(
                request.company(),
                request.role(),
                request.startDate(),
                request.endDate());
        experience.setDescription(request.description());
        if (request.location() != null) {
            experience.setLocation(toLocation(request.location()));
        }
        return experience;
    }

    /**
     * Updates a WorkExperience entity from a request.
     * 
     * @param experience the entity to update
     * @param request    the update request
     */
    public void updateFromRequest(WorkExperience experience, UpdateWorkExperienceRequest request) {
        if (request.company() != null) {
            experience.setCompany(request.company());
        }
        if (request.role() != null) {
            experience.setRole(request.role());
        }
        if (request.startDate() != null) {
            experience.setStartDate(request.startDate());
        }
        // endDate can be null (current position)
        experience.setEndDate(request.endDate());
        if (request.description() != null) {
            experience.setDescription(request.description());
        }
        if (request.location() != null) {
            experience.setLocation(toLocation(request.location()));
        }
    }

    /**
     * Creates an Education entity from a request.
     * 
     * @param request the create request
     * @return the education entity
     */
    public Education toEducation(CreateEducationRequest request) {
        return new Education(
                request.institution(),
                request.degree(),
                request.field(),
                request.graduationYear());
    }

    /**
     * Updates an Education entity from a request.
     * 
     * @param education the entity to update
     * @param request   the update request
     */
    public void updateFromRequest(Education education, UpdateEducationRequest request) {
        if (request.institution() != null) {
            education.setInstitution(request.institution());
        }
        if (request.degree() != null) {
            education.setDegree(request.degree());
        }
        if (request.field() != null) {
            education.setField(request.field());
        }
        education.setGraduationYear(request.graduationYear());
    }

    /**
     * Creates a ProfileSkill entity from a request and skill.
     * 
     * @param skill   the skill entity
     * @param request the request containing proficiency level
     * @return the profile skill entity
     */
    public ProfileSkill toProfileSkill(Skill skill, AddSkillRequest request) {
        ProficiencyLevel level = ProficiencyLevel.valueOf(request.proficiencyLevel());
        return new ProfileSkill(skill, level);
    }

    // ================ Value Object Conversions ================

    private Location toLocation(LocationRequest request) {
        if (request == null) {
            return null;
        }
        return new Location(
                request.city(),
                request.state(),
                request.country(),
                request.latitude(),
                request.longitude());
    }

    private Money toMoney(MoneyRequest request) {
        if (request == null) {
            return null;
        }
        return new Money(request.amount(), request.currency());
    }

    // ================ Response DTOs ================

    public record ProfileResponse(
            UUID id,
            UUID userId,
            String firstName,
            String lastName,
            String fullName,
            String bio,
            LocationResponse location,
            MoneyResponse salaryExpectationMin,
            MoneyResponse salaryExpectationMax,
            String availability,
            String remotePreference,
            boolean profileComplete,
            boolean searchable,
            String headline,
            int totalExperienceYears,
            List<WorkExperienceResponse> workExperiences,
            List<EducationResponse> educations,
            List<ProfileSkillResponse> skills,
            java.time.Instant createdAt,
            java.time.Instant updatedAt) {
    }

    public record WorkExperienceResponse(
            UUID id,
            String company,
            String role,
            LocalDate startDate,
            LocalDate endDate,
            boolean current,
            String description,
            LocationResponse location,
            int durationYears) {
    }

    public record EducationResponse(
            UUID id,
            String institution,
            String degree,
            String field,
            Integer graduationYear,
            String formatted) {
    }

    public record ProfileSkillResponse(
            UUID skillId,
            String skillName,
            String category,
            String proficiencyLevel) {
    }

    public record LocationResponse(
            String city,
            String state,
            String country,
            Double latitude,
            Double longitude) {
    }

    public record MoneyResponse(
            java.math.BigDecimal amount,
            String currency) {
    }

    // ================ Request DTOs ================

    public record UpdateProfileRequest(
            String firstName,
            String lastName,
            String bio,
            LocationRequest location,
            MoneyRequest salaryExpectationMin,
            MoneyRequest salaryExpectationMax,
            String availability,
            String remotePreference,
            Boolean searchable) {
    }

    public record CreateWorkExperienceRequest(
            String company,
            String role,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            LocationRequest location) {
    }

    public record UpdateWorkExperienceRequest(
            String company,
            String role,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            LocationRequest location) {
    }

    public record CreateEducationRequest(
            String institution,
            String degree,
            String field,
            Integer graduationYear) {
    }

    public record UpdateEducationRequest(
            String institution,
            String degree,
            String field,
            Integer graduationYear) {
    }

    public record AddSkillRequest(
            UUID skillId,
            String proficiencyLevel) {
    }

    public record LocationRequest(
            String city,
            String state,
            String country,
            Double latitude,
            Double longitude) {
    }

    public record MoneyRequest(
            java.math.BigDecimal amount,
            String currency) {
    }
}
