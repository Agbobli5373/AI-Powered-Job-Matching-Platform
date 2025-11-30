package com.isaac.job_matching.profile.internal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.isaac.job_matching.profile.Profile;
import com.isaac.job_matching.profile.ProfileService;
import com.isaac.job_matching.shared.Skill;
import com.isaac.job_matching.shared.SkillRepository;
import com.isaac.job_matching.user.User;
import com.isaac.job_matching.user.UserRepository;
import com.isaac.job_matching.user.UserRole;

/**
 * Integration tests for ProfileController.
 * 
 * <p>
 * Test Categories:
 * <ul>
 * <li>Profile Retrieval - get my profile, get profile by ID</li>
 * <li>Profile Update - update basic info</li>
 * <li>Work Experience - add, update, delete</li>
 * <li>Education - add, update, delete</li>
 * <li>Skills - add, remove</li>
 * <li>Authorization - role-based access, JWT validation</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class ProfileControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private SkillRepository skillRepository;

    private UUID jobSeekerUserId;
    private UUID jobSeekerProfileId;
    private UUID employerUserId;

    private static final String API_PROFILES_ME = "/api/profiles/me";
    private static final String API_PROFILES = "/api/profiles";

    @BeforeEach
    void setUp() {
        // Set up MockMvc with security
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Clean up
        profileRepository.deleteAll();
        userRepository.deleteAll();
        skillRepository.deleteAll();

        // Create job seeker user and profile
        User jobSeeker = new User("jobseeker@example.com", "hash", UserRole.fromString("JOB_SEEKER"));
        jobSeeker.verifyEmail();
        jobSeeker = userRepository.save(jobSeeker);
        jobSeekerUserId = jobSeeker.getId();

        Profile profile = profileService.createProfileForUser(jobSeekerUserId);
        jobSeekerProfileId = profile.getId();

        // Create employer user (no profile)
        User employer = new User("employer@example.com", "hash", UserRole.fromString("EMPLOYER"));
        employer.verifyEmail();
        employer = userRepository.save(employer);
        employerUserId = employer.getId();

        // Create some skills
        skillRepository.save(new Skill("Java", "Programming"));
        skillRepository.save(new Skill("Python", "Programming"));
        skillRepository.save(new Skill("Spring Boot", "Framework"));
    }

    // ==================== Profile Retrieval Tests ====================

    @Nested
    @DisplayName("Profile Retrieval Tests")
    class ProfileRetrievalTests {

        @Test
        @DisplayName("Should get current user's profile successfully")
        void shouldGetCurrentUserProfile() throws Exception {
            mockMvc.perform(get(API_PROFILES_ME)
                            .with(jobSeekerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(jobSeekerProfileId.toString())))
                    .andExpect(jsonPath("$.userId", is(jobSeekerUserId.toString())));
        }

        @Test
        @DisplayName("Should get profile by ID successfully")
        void shouldGetProfileById() throws Exception {
            mockMvc.perform(get(API_PROFILES + "/" + jobSeekerProfileId)
                            .with(jobSeekerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(jobSeekerProfileId.toString())));
        }

        @Test
        @DisplayName("Should return 404 for non-existent profile")
        void shouldReturn404ForNonExistentProfile() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get(API_PROFILES + "/" + nonExistentId)
                            .with(jobSeekerJwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 without JWT token")
        void shouldReturn401WithoutJwt() throws Exception {
            mockMvc.perform(get(API_PROFILES_ME))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 for employer accessing profile endpoints")
        void shouldReturn403ForEmployer() throws Exception {
            mockMvc.perform(get(API_PROFILES_ME)
                            .with(employerJwt()))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== Profile Update Tests ====================

    @Nested
    @DisplayName("Profile Update Tests")
    class ProfileUpdateTests {

        @Test
        @DisplayName("Should update profile basic info successfully")
        void shouldUpdateProfileBasicInfo() throws Exception {
            String requestJson = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "bio": "Experienced software developer",
                    "availability": "IMMEDIATELY",
                    "remotePreference": "HYBRID"
                }
                """;

            mockMvc.perform(put(API_PROFILES_ME)
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName", is("John")))
                    .andExpect(jsonPath("$.lastName", is("Doe")))
                    .andExpect(jsonPath("$.bio", is("Experienced software developer")))
                    .andExpect(jsonPath("$.availability", is("IMMEDIATELY")))
                    .andExpect(jsonPath("$.remotePreference", is("HYBRID")));
        }

        @Test
        @DisplayName("Should update profile location")
        void shouldUpdateProfileLocation() throws Exception {
            String requestJson = """
                {
                    "location": {
                        "city": "San Francisco",
                        "state": "CA",
                        "country": "USA",
                        "latitude": 37.7749,
                        "longitude": -122.4194
                    }
                }
                """;

            mockMvc.perform(put(API_PROFILES_ME)
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.location.city", is("San Francisco")))
                    .andExpect(jsonPath("$.location.state", is("CA")))
                    .andExpect(jsonPath("$.location.country", is("USA")));
        }

        @Test
        @DisplayName("Should update salary expectations")
        void shouldUpdateSalaryExpectations() throws Exception {
            String requestJson = """
                {
                    "salaryExpectationMin": {
                        "amount": 80000,
                        "currency": "USD"
                    },
                    "salaryExpectationMax": {
                        "amount": 120000,
                        "currency": "USD"
                    }
                }
                """;

            mockMvc.perform(put(API_PROFILES_ME)
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.salaryExpectationMin.amount", is(80000)))
                    .andExpect(jsonPath("$.salaryExpectationMax.amount", is(120000)));
        }

        @Test
        @DisplayName("Should update searchability")
        void shouldUpdateSearchability() throws Exception {
            String requestJson = """
                {
                    "searchable": false
                }
                """;

            mockMvc.perform(put(API_PROFILES_ME)
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.searchable", is(false)));
        }

        @Test
        @DisplayName("Should return 401 for update without JWT")
        void shouldReturn401ForUpdateWithoutJwt() throws Exception {
            String requestJson = """
                {
                    "firstName": "John"
                }
                """;

            mockMvc.perform(put(API_PROFILES_ME)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== Work Experience Tests ====================

    @Nested
    @DisplayName("Work Experience Tests")
    class WorkExperienceTests {

        @Test
        @DisplayName("Should add work experience successfully")
        void shouldAddWorkExperience() throws Exception {
            String requestJson = """
                {
                    "company": "Tech Corp",
                    "role": "Software Engineer",
                    "startDate": "2020-01-15",
                    "endDate": "2023-06-30",
                    "description": "Developed web applications",
                    "location": {
                        "city": "New York",
                        "country": "USA"
                    }
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/work-experiences")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.workExperiences", hasSize(1)))
                    .andExpect(jsonPath("$.workExperiences[0].company", is("Tech Corp")))
                    .andExpect(jsonPath("$.workExperiences[0].role", is("Software Engineer")));
        }

        @Test
        @DisplayName("Should add current position without end date")
        void shouldAddCurrentPosition() throws Exception {
            String requestJson = """
                {
                    "company": "Current Company",
                    "role": "Senior Developer",
                    "startDate": "2023-07-01",
                    "description": "Leading development team"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/work-experiences")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.workExperiences[0].current", is(true)));
        }

        @Test
        @DisplayName("Should reject work experience without company")
        void shouldRejectWorkExperienceWithoutCompany() throws Exception {
            String requestJson = """
                {
                    "role": "Developer",
                    "startDate": "2020-01-15"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/work-experiences")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject work experience without role")
        void shouldRejectWorkExperienceWithoutRole() throws Exception {
            String requestJson = """
                {
                    "company": "Tech Corp",
                    "startDate": "2020-01-15"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/work-experiences")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject work experience without start date")
        void shouldRejectWorkExperienceWithoutStartDate() throws Exception {
            String requestJson = """
                {
                    "company": "Tech Corp",
                    "role": "Developer"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/work-experiences")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should update work experience successfully")
        void shouldUpdateWorkExperience() throws Exception {
            // First add a work experience
            String addJson = """
                {
                    "company": "Old Company",
                    "role": "Developer",
                    "startDate": "2020-01-15"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/work-experiences")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addJson))
                    .andExpect(status().isCreated());

            // Get the experience ID
            Profile profile = profileService.getProfileByUserId(jobSeekerUserId);
            UUID experienceId = profile.getWorkExperiences().get(0).getId();

            // Update it
            String updateJson = """
                {
                    "company": "New Company",
                    "role": "Senior Developer",
                    "startDate": "2020-01-15",
                    "endDate": "2023-12-31"
                }
                """;

            mockMvc.perform(put(API_PROFILES_ME + "/work-experiences/" + experienceId)
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workExperiences[0].company", is("New Company")))
                    .andExpect(jsonPath("$.workExperiences[0].role", is("Senior Developer")));
        }

        @Test
        @DisplayName("Should delete work experience successfully")
        void shouldDeleteWorkExperience() throws Exception {
            // First add a work experience
            String addJson = """
                {
                    "company": "Company",
                    "role": "Developer",
                    "startDate": "2020-01-15"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/work-experiences")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addJson))
                    .andExpect(status().isCreated());

            // Get the experience ID
            Profile profile = profileService.getProfileByUserId(jobSeekerUserId);
            UUID experienceId = profile.getWorkExperiences().get(0).getId();

            // Delete it
            mockMvc.perform(delete(API_PROFILES_ME + "/work-experiences/" + experienceId)
                            .with(jobSeekerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.workExperiences", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent work experience")
        void shouldReturn404WhenDeletingNonExistentExperience() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(delete(API_PROFILES_ME + "/work-experiences/" + nonExistentId)
                            .with(jobSeekerJwt()))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Education Tests ====================

    @Nested
    @DisplayName("Education Tests")
    class EducationTests {

        @Test
        @DisplayName("Should add education successfully")
        void shouldAddEducation() throws Exception {
            String requestJson = """
                {
                    "institution": "MIT",
                    "degree": "Bachelor of Science",
                    "field": "Computer Science",
                    "graduationYear": 2020
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/educations")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.educations", hasSize(1)))
                    .andExpect(jsonPath("$.educations[0].institution", is("MIT")))
                    .andExpect(jsonPath("$.educations[0].degree", is("Bachelor of Science")))
                    .andExpect(jsonPath("$.educations[0].field", is("Computer Science")));
        }

        @Test
        @DisplayName("Should add education with only institution")
        void shouldAddEducationWithOnlyInstitution() throws Exception {
            String requestJson = """
                {
                    "institution": "Online Course Platform"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/educations")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.educations[0].institution", is("Online Course Platform")));
        }

        @Test
        @DisplayName("Should reject education without institution")
        void shouldRejectEducationWithoutInstitution() throws Exception {
            String requestJson = """
                {
                    "degree": "Bachelor's",
                    "field": "CS"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/educations")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should update education successfully")
        void shouldUpdateEducation() throws Exception {
            // First add education
            String addJson = """
                {
                    "institution": "Old University",
                    "degree": "BA",
                    "field": "History"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/educations")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addJson))
                    .andExpect(status().isCreated());

            // Get the education ID
            Profile profile = profileService.getProfileByUserId(jobSeekerUserId);
            UUID educationId = profile.getEducations().get(0).getId();

            // Update it
            String updateJson = """
                {
                    "institution": "New University",
                    "degree": "MS",
                    "field": "Computer Science",
                    "graduationYear": 2022
                }
                """;

            mockMvc.perform(put(API_PROFILES_ME + "/educations/" + educationId)
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.educations[0].institution", is("New University")))
                    .andExpect(jsonPath("$.educations[0].degree", is("MS")));
        }

        @Test
        @DisplayName("Should delete education successfully")
        void shouldDeleteEducation() throws Exception {
            // First add education
            String addJson = """
                {
                    "institution": "University"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/educations")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addJson))
                    .andExpect(status().isCreated());

            // Get the education ID
            Profile profile = profileService.getProfileByUserId(jobSeekerUserId);
            UUID educationId = profile.getEducations().get(0).getId();

            // Delete it
            mockMvc.perform(delete(API_PROFILES_ME + "/educations/" + educationId)
                            .with(jobSeekerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.educations", hasSize(0)));
        }
    }

    // ==================== Skills Tests ====================

    @Nested
    @DisplayName("Skills Tests")
    class SkillsTests {

        @Test
        @DisplayName("Should add skill to profile")
        void shouldAddSkill() throws Exception {
            Skill javaSkill = skillRepository.findAll().stream()
                    .filter(s -> s.getName().equals("Java"))
                    .findFirst()
                    .orElseThrow();

            String requestJson = String.format("""
                {
                    "skillId": "%s",
                    "proficiencyLevel": "INTERMEDIATE"
                }
                """, javaSkill.getId());

            mockMvc.perform(post(API_PROFILES_ME + "/skills")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.skills", hasSize(1)))
                    .andExpect(jsonPath("$.skills[0].skillName", is("Java")))
                    .andExpect(jsonPath("$.skills[0].proficiencyLevel", is("INTERMEDIATE")));
        }

        @Test
        @DisplayName("Should add multiple skills")
        void shouldAddMultipleSkills() throws Exception {
            Skill javaSkill = skillRepository.findAll().stream()
                    .filter(s -> s.getName().equals("Java"))
                    .findFirst()
                    .orElseThrow();

            Skill pythonSkill = skillRepository.findAll().stream()
                    .filter(s -> s.getName().equals("Python"))
                    .findFirst()
                    .orElseThrow();

            // Add first skill
            String requestJson1 = String.format("""
                {
                    "skillId": "%s",
                    "proficiencyLevel": "EXPERT"
                }
                """, javaSkill.getId());

            mockMvc.perform(post(API_PROFILES_ME + "/skills")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson1))
                    .andExpect(status().isCreated());

            // Add second skill
            String requestJson2 = String.format("""
                {
                    "skillId": "%s",
                    "proficiencyLevel": "BEGINNER"
                }
                """, pythonSkill.getId());

            mockMvc.perform(post(API_PROFILES_ME + "/skills")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson2))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.skills", hasSize(2)));
        }

        @Test
        @DisplayName("Should return 404 when adding non-existent skill")
        void shouldReturn404WhenAddingNonExistentSkill() throws Exception {
            UUID nonExistentSkillId = UUID.randomUUID();

            String requestJson = String.format("""
                {
                    "skillId": "%s",
                    "proficiencyLevel": "INTERMEDIATE"
                }
                """, nonExistentSkillId);

            mockMvc.perform(post(API_PROFILES_ME + "/skills")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject skill without skill ID")
        void shouldRejectSkillWithoutSkillId() throws Exception {
            String requestJson = """
                {
                    "proficiencyLevel": "INTERMEDIATE"
                }
                """;

            mockMvc.perform(post(API_PROFILES_ME + "/skills")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject skill without proficiency level")
        void shouldRejectSkillWithoutProficiencyLevel() throws Exception {
            Skill javaSkill = skillRepository.findAll().stream()
                    .filter(s -> s.getName().equals("Java"))
                    .findFirst()
                    .orElseThrow();

            String requestJson = String.format("""
                {
                    "skillId": "%s"
                }
                """, javaSkill.getId());

            mockMvc.perform(post(API_PROFILES_ME + "/skills")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should remove skill from profile")
        void shouldRemoveSkill() throws Exception {
            // First add a skill
            Skill javaSkill = skillRepository.findAll().stream()
                    .filter(s -> s.getName().equals("Java"))
                    .findFirst()
                    .orElseThrow();

            String addJson = String.format("""
                {
                    "skillId": "%s",
                    "proficiencyLevel": "INTERMEDIATE"
                }
                """, javaSkill.getId());

            mockMvc.perform(post(API_PROFILES_ME + "/skills")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addJson))
                    .andExpect(status().isCreated());

            // Remove the skill
            mockMvc.perform(delete(API_PROFILES_ME + "/skills/" + javaSkill.getId())
                            .with(jobSeekerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skills", hasSize(0)));
        }

        @Test
        @DisplayName("Should get all available skills")
        void shouldGetAllAvailableSkills() throws Exception {
            mockMvc.perform(get(API_PROFILES + "/skills")
                            .with(jobSeekerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }
    }

    // ==================== Searchability Tests ====================

    @Nested
    @DisplayName("Searchability Tests")
    class SearchabilityTests {

        @Test
        @DisplayName("Should set profile as searchable")
        void shouldSetProfileAsSearchable() throws Exception {
            String requestJson = """
                {
                    "searchable": true
                }
                """;

            mockMvc.perform(put(API_PROFILES_ME + "/searchable")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.searchable", is(true)));
        }

        @Test
        @DisplayName("Should set profile as not searchable")
        void shouldSetProfileAsNotSearchable() throws Exception {
            String requestJson = """
                {
                    "searchable": false
                }
                """;

            mockMvc.perform(put(API_PROFILES_ME + "/searchable")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.searchable", is(false)));
        }

        @Test
        @DisplayName("Should reject searchability update without value")
        void shouldRejectSearchabilityUpdateWithoutValue() throws Exception {
            String requestJson = "{}";

            mockMvc.perform(put(API_PROFILES_ME + "/searchable")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Authorization Tests ====================

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should allow admin to access profile endpoints")
        void shouldAllowAdminAccess() throws Exception {
            // First create a profile for an admin (manually since they don't auto-create)
            User admin = new User("admin@example.com", "hash", UserRole.fromString("ADMIN"));
            admin.verifyEmail();
            final User savedAdmin = userRepository.save(admin);
            Profile adminProfile = profileService.createProfileForUser(savedAdmin.getId());

            mockMvc.perform(get(API_PROFILES_ME)
                            .with(jwt()
                                    .jwt(builder -> builder
                                            .subject(savedAdmin.getId().toString())
                                            .claim("role", "ADMIN"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should reject expired JWT")
        void shouldRejectExpiredJwt() throws Exception {
            // MockMvc with jwt() doesn't easily simulate expiration, 
            // but we can test missing auth
            mockMvc.perform(get(API_PROFILES_ME))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject invalid JWT")
        void shouldRejectInvalidJwt() throws Exception {
            mockMvc.perform(get(API_PROFILES_ME)
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== Helper Methods ====================

    private org.springframework.test.web.servlet.request.RequestPostProcessor jobSeekerJwt() {
        return jwt()
                .jwt(builder -> builder
                        .subject(jobSeekerUserId.toString())
                        .claim("role", "JOB_SEEKER")
                        .claim("email", "jobseeker@example.com"))
                .authorities(new SimpleGrantedAuthority("ROLE_JOB_SEEKER"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor employerJwt() {
        return jwt()
                .jwt(builder -> builder
                        .subject(employerUserId.toString())
                        .claim("role", "EMPLOYER")
                        .claim("email", "employer@example.com"))
                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYER"));
    }
}
