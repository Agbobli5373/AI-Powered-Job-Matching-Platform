package com.isaac.job_matching.profile.internal;

import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RestController;

import com.isaac.job_matching.profile.Education;
import com.isaac.job_matching.profile.ProficiencyLevel;
import com.isaac.job_matching.profile.Profile;
import com.isaac.job_matching.profile.ProfileService;
import com.isaac.job_matching.profile.WorkExperience;
import com.isaac.job_matching.shared.Skill;
import com.isaac.job_matching.shared.SkillRepository;
import com.isaac.job_matching.shared.exception.EntityNotFoundException;
import com.isaac.job_matching.shared.exception.ForbiddenException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * REST controller for profile management endpoints.
 * 
 * <p>
 * All endpoints require authentication (JWT token).
 * 
 * <p>
 * Endpoints:
 * <ul>
 * <li>GET /api/profiles/me - Get current user's profile</li>
 * <li>PUT /api/profiles/me - Update current user's profile</li>
 * <li>GET /api/profiles/{id} - Get profile by ID</li>
 * <li>POST /api/profiles/me/work-experiences - Add work experience</li>
 * <li>PUT /api/profiles/me/work-experiences/{id} - Update work experience</li>
 * <li>DELETE /api/profiles/me/work-experiences/{id} - Delete work
 * experience</li>
 * <li>POST /api/profiles/me/educations - Add education</li>
 * <li>PUT /api/profiles/me/educations/{id} - Update education</li>
 * <li>DELETE /api/profiles/me/educations/{id} - Delete education</li>
 * <li>POST /api/profiles/me/skills - Add skill</li>
 * <li>DELETE /api/profiles/me/skills/{skillId} - Remove skill</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/profiles")
@Tag(name = "Profiles", description = "Job seeker profile management")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileMapper profileMapper;
    private final SkillRepository skillRepository;

    public ProfileController(
            ProfileService profileService,
            ProfileMapper profileMapper,
            SkillRepository skillRepository) {
        this.profileService = profileService;
        this.profileMapper = profileMapper;
        this.skillRepository = skillRepository;
    }

    // ================ Profile Endpoints ================

    @GetMapping("/me")
    @Operation(summary = "Get current user's profile", description = "Returns the profile of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);
        return ResponseEntity.ok(profileMapper.toResponse(profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user's profile", description = "Updates the profile of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileMapper.UpdateProfileRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);
        profileMapper.updateFromRequest(profile, request);
        Profile savedProfile = profileService.saveProfile(profile);
        return ResponseEntity.ok(profileMapper.toResponse(savedProfile));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get profile by ID", description = "Returns a public profile by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> getProfileById(@PathVariable UUID id) {
        Profile profile = profileService.getProfileById(id);
        return ResponseEntity.ok(profileMapper.toResponse(profile));
    }

    // ================ Work Experience Endpoints ================

    @PostMapping("/me/work-experiences")
    @Operation(summary = "Add work experience", description = "Adds a new work experience to the profile")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Work experience added"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> addWorkExperience(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateWorkExperienceRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);

        ProfileMapper.LocationRequest mapperLocation = null;
        if (request.location() != null) {
            mapperLocation = new ProfileMapper.LocationRequest(
                    request.location().city(),
                    request.location().state(),
                    request.location().country(),
                    request.location().latitude(),
                    request.location().longitude());
        }

        WorkExperience experience = profileMapper.toWorkExperience(
                new ProfileMapper.CreateWorkExperienceRequest(
                        request.company(),
                        request.role(),
                        request.startDate(),
                        request.endDate(),
                        request.description(),
                        mapperLocation));
        Profile updatedProfile = profileService.addWorkExperience(profile.getId(), experience);
        return ResponseEntity.status(HttpStatus.CREATED).body(profileMapper.toResponse(updatedProfile));
    }

    @PutMapping("/me/work-experiences/{experienceId}")
    @Operation(summary = "Update work experience", description = "Updates an existing work experience")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work experience updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Work experience not found")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> updateWorkExperience(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID experienceId,
            @Valid @RequestBody UpdateWorkExperienceRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);

        // Create updated experience
        WorkExperience experience = new WorkExperience(
                request.company(),
                request.role(),
                request.startDate(),
                request.endDate());
        experience.setDescription(request.description());
        if (request.location() != null) {
            experience.setLocation(new com.isaac.job_matching.shared.Location(
                    request.location().city(),
                    request.location().state(),
                    request.location().country(),
                    request.location().latitude(),
                    request.location().longitude()));
        }

        Profile updatedProfile = profileService.updateWorkExperience(profile.getId(), experienceId, experience);
        return ResponseEntity.ok(profileMapper.toResponse(updatedProfile));
    }

    @DeleteMapping("/me/work-experiences/{experienceId}")
    @Operation(summary = "Delete work experience", description = "Removes a work experience from the profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Work experience removed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Work experience not found")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> removeWorkExperience(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID experienceId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);
        Profile updatedProfile = profileService.removeWorkExperience(profile.getId(), experienceId);
        return ResponseEntity.ok(profileMapper.toResponse(updatedProfile));
    }

    // ================ Education Endpoints ================

    @PostMapping("/me/educations")
    @Operation(summary = "Add education", description = "Adds a new education entry to the profile")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Education added"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> addEducation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateEducationRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);
        Education education = profileMapper.toEducation(
                new ProfileMapper.CreateEducationRequest(
                        request.institution(),
                        request.degree(),
                        request.field(),
                        request.graduationYear()));
        Profile updatedProfile = profileService.addEducation(profile.getId(), education);
        return ResponseEntity.status(HttpStatus.CREATED).body(profileMapper.toResponse(updatedProfile));
    }

    @PutMapping("/me/educations/{educationId}")
    @Operation(summary = "Update education", description = "Updates an existing education entry")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Education updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Education not found")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> updateEducation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID educationId,
            @Valid @RequestBody UpdateEducationRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);

        // Create updated education
        Education education = new Education(
                request.institution(),
                request.degree(),
                request.field(),
                request.graduationYear());

        Profile updatedProfile = profileService.updateEducation(profile.getId(), educationId, education);
        return ResponseEntity.ok(profileMapper.toResponse(updatedProfile));
    }

    @DeleteMapping("/me/educations/{educationId}")
    @Operation(summary = "Delete education", description = "Removes an education entry from the profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Education removed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Education not found")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> removeEducation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID educationId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);
        Profile updatedProfile = profileService.removeEducation(profile.getId(), educationId);
        return ResponseEntity.ok(profileMapper.toResponse(updatedProfile));
    }

    // ================ Skills Endpoints ================

    @PostMapping("/me/skills")
    @Operation(summary = "Add skill", description = "Adds a skill to the profile with a proficiency level")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Skill added"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Skill not found")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> addSkill(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddSkillRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);

        ProficiencyLevel level = ProficiencyLevel.valueOf(request.proficiencyLevel());
        Profile updatedProfile = profileService.addSkill(profile.getId(), request.skillId(), level);
        return ResponseEntity.status(HttpStatus.CREATED).body(profileMapper.toResponse(updatedProfile));
    }

    @DeleteMapping("/me/skills/{skillId}")
    @Operation(summary = "Remove skill", description = "Removes a skill from the profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skill removed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Skill not found")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> removeSkill(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID skillId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);
        Profile updatedProfile = profileService.removeSkill(profile.getId(), skillId);
        return ResponseEntity.ok(profileMapper.toResponse(updatedProfile));
    }

    // ================ Searchability ================

    @PutMapping("/me/searchable")
    @Operation(summary = "Set profile searchability", description = "Controls whether the profile appears in employer searches")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Searchability updated"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ProfileMapper.ProfileResponse> setSearchable(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SearchableRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Profile profile = profileService.getProfileByUserId(userId);
        Profile updatedProfile = profileService.setSearchable(profile.getId(), request.searchable());
        return ResponseEntity.ok(profileMapper.toResponse(updatedProfile));
    }

    // ================ Skills Lookup ================

    @GetMapping("/skills")
    @Operation(summary = "Get all available skills", description = "Returns all skills that can be added to a profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skills retrieved")
    })
    public ResponseEntity<List<SkillResponse>> getAllSkills() {
        List<Skill> skills = skillRepository.findAll();
        List<SkillResponse> responses = skills.stream()
                .map(s -> new SkillResponse(s.getId(), s.getName(), s.getCategory()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    // ================ Request/Response DTOs ================

    public record CreateWorkExperienceRequest(
            @NotBlank(message = "Company is required") String company,
            @NotBlank(message = "Role is required") String role,
            @NotNull(message = "Start date is required") java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String description,
            LocationRequest location) {
    }

    public record UpdateWorkExperienceRequest(
            @NotBlank(message = "Company is required") String company,
            @NotBlank(message = "Role is required") String role,
            @NotNull(message = "Start date is required") java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String description,
            LocationRequest location) {
    }

    public record CreateEducationRequest(
            @NotBlank(message = "Institution is required") String institution,
            String degree,
            String field,
            Integer graduationYear) {
    }

    public record UpdateEducationRequest(
            @NotBlank(message = "Institution is required") String institution,
            String degree,
            String field,
            Integer graduationYear) {
    }

    public record AddSkillRequest(
            @NotNull(message = "Skill ID is required") UUID skillId,
            @NotBlank(message = "Proficiency level is required") String proficiencyLevel) {
    }

    public record SearchableRequest(
            @NotNull(message = "Searchable flag is required") Boolean searchable) {
    }

    public record LocationRequest(
            String city,
            String state,
            String country,
            Double latitude,
            Double longitude) {
    }

    public record SkillResponse(
            UUID id,
            String name,
            String category) {
    }
}
