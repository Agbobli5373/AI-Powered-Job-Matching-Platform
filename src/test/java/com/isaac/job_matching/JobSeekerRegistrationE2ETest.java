package com.isaac.job_matching;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.job_matching.profile.internal.ProfileRepository;
import com.isaac.job_matching.shared.Skill;
import com.isaac.job_matching.shared.SkillRepository;
import com.isaac.job_matching.user.UserRepository;

/**
 * End-to-End integration test for the complete Job Seeker registration and profile creation flow.
 * 
 * <p>
 * This test simulates a full user journey:
 * <ol>
 * <li>Register a new job seeker user</li>
 * <li>Verify email address</li>
 * <li>Log in and receive tokens</li>
 * <li>Create/update profile with personal info</li>
 * <li>Add work experience entries</li>
 * <li>Add education records</li>
 * <li>Add skills to profile</li>
 * <li>Update profile information</li>
 * <li>Retrieve complete profile</li>
 * <li>Verify authorization is enforced at each step</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Job Seeker Registration & Profile Creation E2E Test")
class JobSeekerRegistrationE2ETest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private SkillRepository skillRepository;

    // Test data
    private static final String TEST_EMAIL = "e2e.jobseeker@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    
    private UUID userId;
    private UUID profileId;
    private String accessToken;
    private String refreshToken;
    private String verificationToken;
    private Skill javaSkill;
    private Skill pythonSkill;

    @BeforeEach
    void setUp() {
        // Set up MockMvc with security
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Clean up any previous test data
        profileRepository.deleteAll();
        userRepository.deleteAll();
        skillRepository.deleteAll();

        // Create skills for testing
        javaSkill = skillRepository.save(new Skill("Java", "Programming"));
        pythonSkill = skillRepository.save(new Skill("Python", "Programming"));
        skillRepository.save(new Skill("Spring Boot", "Framework"));
    }

    @Test
    @Order(1)
    @DisplayName("Complete E2E flow: Registration → Verification → Login → Profile Creation → Profile Updates")
    void completeJobSeekerJourney() throws Exception {
        // Step 1: Register new user
        registerUser();
        
        // Step 2: Verify email cannot access protected resources before verification
        verifyCannotAccessProtectedResourcesWithoutAuth();
        
        // Step 3: Verify email
        verifyEmail();
        
        // Step 4: Login
        login();
        
        // Step 5: Get initial profile (auto-created for job seeker)
        getInitialProfile();
        
        // Step 6: Update profile with personal info
        updateProfileBasicInfo();
        
        // Step 7: Add work experiences
        addWorkExperiences();
        
        // Step 8: Add education records
        addEducationRecords();
        
        // Step 9: Add skills
        addSkills();
        
        // Step 10: Update profile info again
        updateProfileDetails();
        
        // Step 11: Retrieve complete profile and verify
        verifyCompleteProfile();
        
        // Step 12: Test profile completeness
        verifyProfileCompleteness();
        
        // Step 13: Test authorization enforcement
        verifyAuthorizationEnforcement();
    }

    // ==================== Step Methods ====================

    private void registerUser() throws Exception {
        String requestJson = String.format("""
            {
                "email": "%s",
                "password": "%s",
                "role": "JOB_SEEKER"
            }
            """, TEST_EMAIL, TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL.toLowerCase()))
                .andExpect(jsonPath("$.message").exists())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        userId = UUID.fromString(response.get("id").asText());

        // Verify user was created in database
        assertThat(userRepository.findById(userId)).isPresent();
        
        // Store verification token for later use
        // In a real system, this would be sent via email
        // For testing, we'll call the auth service directly
    }

    private void verifyCannotAccessProtectedResourcesWithoutAuth() throws Exception {
        // Try to access profile without authentication
        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isUnauthorized());

        // Try with invalid token
        mockMvc.perform(get("/api/profiles/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    private void verifyEmail() throws Exception {
        // In a real system, we would extract the verification token from the email
        // For testing, we get it directly from the auth service
        var authService = (com.isaac.job_matching.auth.AuthService) 
            org.springframework.test.util.ReflectionTestUtils.getField(this, "authService");
        
        // Since we can't access auth service directly in the test context,
        // we'll simulate by calling the controller with a mock verification token
        // First, we need to get the actual token
        
        // Simulating the verification process by directly updating user status
        com.isaac.job_matching.user.User user = userRepository.findById(userId).orElseThrow();
        user.verifyEmail();
        userRepository.save(user);
        
        // Verify the user is now active
        user = userRepository.findById(userId).orElseThrow();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getStatus()).isEqualTo(com.isaac.job_matching.user.UserStatus.ACTIVE);
    }

    private void login() throws Exception {
        String requestJson = String.format("""
            {
                "email": "%s",
                "password": "%s"
            }
            """, TEST_EMAIL, TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL.toLowerCase()))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        accessToken = response.get("accessToken").asText();
        refreshToken = response.get("refreshToken").asText();

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
    }

    private void getInitialProfile() throws Exception {
        // Profile should be auto-created for job seeker
        MvcResult result = mockMvc.perform(get("/api/profiles/me")
                        .with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.profileComplete").value(false))
                .andExpect(jsonPath("$.searchable").value(true))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        profileId = UUID.fromString(response.get("id").asText());
    }

    private void updateProfileBasicInfo() throws Exception {
        String requestJson = """
            {
                "firstName": "John",
                "lastName": "Doe",
                "bio": "Passionate software developer with 5+ years of experience in Java and Python. Looking for exciting opportunities in tech.",
                "location": {
                    "city": "San Francisco",
                    "state": "California",
                    "country": "United States",
                    "latitude": 37.7749,
                    "longitude": -122.4194
                },
                "availability": "TWO_WEEKS",
                "remotePreference": "HYBRID"
            }
            """;

        mockMvc.perform(put("/api/profiles/me")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.bio").exists())
                .andExpect(jsonPath("$.location.city").value("San Francisco"))
                .andExpect(jsonPath("$.availability").value("TWO_WEEKS"))
                .andExpect(jsonPath("$.remotePreference").value("HYBRID"));
    }

    private void addWorkExperiences() throws Exception {
        // Add first work experience (previous job)
        String experience1 = """
            {
                "company": "TechCorp Inc",
                "role": "Software Engineer",
                "startDate": "2019-03-15",
                "endDate": "2022-06-30",
                "description": "Developed and maintained backend services using Java and Spring Boot. Led a team of 3 developers.",
                "location": {
                    "city": "San Jose",
                    "state": "California",
                    "country": "United States"
                }
            }
            """;

        mockMvc.perform(post("/api/profiles/me/work-experiences")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(experience1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workExperiences[0].company").value("TechCorp Inc"))
                .andExpect(jsonPath("$.workExperiences[0].current").value(false));

        // Add second work experience (current job)
        String experience2 = """
            {
                "company": "InnovateTech",
                "role": "Senior Software Engineer",
                "startDate": "2022-07-01",
                "description": "Leading development of microservices architecture. Mentoring junior developers."
            }
            """;

        mockMvc.perform(post("/api/profiles/me/work-experiences")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(experience2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workExperiences.length()").value(2))
                .andExpect(jsonPath("$.workExperiences[1].company").value("InnovateTech"))
                .andExpect(jsonPath("$.workExperiences[1].current").value(true));
    }

    private void addEducationRecords() throws Exception {
        // Add Bachelor's degree
        String education1 = """
            {
                "institution": "Stanford University",
                "degree": "Bachelor of Science",
                "field": "Computer Science",
                "graduationYear": 2019
            }
            """;

        mockMvc.perform(post("/api/profiles/me/educations")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(education1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.educations[0].institution").value("Stanford University"))
                .andExpect(jsonPath("$.educations[0].degree").value("Bachelor of Science"));

        // Add certification
        String education2 = """
            {
                "institution": "AWS",
                "degree": "Certification",
                "field": "AWS Solutions Architect"
            }
            """;

        mockMvc.perform(post("/api/profiles/me/educations")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(education2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.educations.length()").value(2));
    }

    private void addSkills() throws Exception {
        // Add Java skill with Expert proficiency
        String skill1 = String.format("""
            {
                "skillId": "%s",
                "proficiencyLevel": "EXPERT"
            }
            """, javaSkill.getId());

        mockMvc.perform(post("/api/profiles/me/skills")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skill1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skills[0].skillName").value("Java"))
                .andExpect(jsonPath("$.skills[0].proficiencyLevel").value("EXPERT"));

        // Add Python skill with Intermediate proficiency
        String skill2 = String.format("""
            {
                "skillId": "%s",
                "proficiencyLevel": "INTERMEDIATE"
            }
            """, pythonSkill.getId());

        mockMvc.perform(post("/api/profiles/me/skills")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skill2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skills.length()").value(2));
    }

    private void updateProfileDetails() throws Exception {
        // Update salary expectations
        String requestJson = """
            {
                "salaryExpectationMin": {
                    "amount": 120000,
                    "currency": "USD"
                },
                "salaryExpectationMax": {
                    "amount": 180000,
                    "currency": "USD"
                }
            }
            """;

        mockMvc.perform(put("/api/profiles/me")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salaryExpectationMin.amount").value(120000))
                .andExpect(jsonPath("$.salaryExpectationMax.amount").value(180000));
    }

    private void verifyCompleteProfile() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/profiles/me")
                        .with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(profileId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.bio").exists())
                .andExpect(jsonPath("$.location.city").value("San Francisco"))
                .andExpect(jsonPath("$.workExperiences.length()").value(2))
                .andExpect(jsonPath("$.educations.length()").value(2))
                .andExpect(jsonPath("$.skills.length()").value(2))
                .andExpect(jsonPath("$.salaryExpectationMin").exists())
                .andExpect(jsonPath("$.salaryExpectationMax").exists())
                .andExpect(jsonPath("$.availability").value("TWO_WEEKS"))
                .andExpect(jsonPath("$.remotePreference").value("HYBRID"))
                .andExpect(jsonPath("$.searchable").value(true))
                .andExpect(jsonPath("$.headline").exists()) // Should have headline from work experience
                .andExpect(jsonPath("$.totalExperienceYears").exists())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        
        // Additional assertions
        assertThat(response.get("totalExperienceYears").asInt()).isGreaterThan(0);
        assertThat(response.get("createdAt")).isNotNull();
        assertThat(response.get("updatedAt")).isNotNull();
    }

    private void verifyProfileCompleteness() throws Exception {
        // Profile should now be complete (has firstName, lastName, and at least one skill)
        mockMvc.perform(get("/api/profiles/me")
                        .with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileComplete").value(true));
    }

    private void verifyAuthorizationEnforcement() throws Exception {
        // Try to access another user's profile as if we had their ID
        UUID anotherProfileId = UUID.randomUUID();
        
        // Getting a profile by ID should be allowed but return 404 for non-existent profile
        mockMvc.perform(get("/api/profiles/" + anotherProfileId)
                        .with(userJwt()))
                .andExpect(status().isNotFound());

        // Test token refresh works
        String refreshJson = String.format("""
            {
                "refreshToken": "%s"
            }
            """, refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // Test employer cannot access job seeker profile endpoints
        // Create a temporary employer user
        com.isaac.job_matching.user.User employer = new com.isaac.job_matching.user.User(
                "employer@example.com", "hash", com.isaac.job_matching.user.UserRole.fromString("EMPLOYER"));
        employer.verifyEmail();
        final com.isaac.job_matching.user.User savedEmployer = userRepository.save(employer);

        mockMvc.perform(get("/api/profiles/me")
                        .with(jwt()
                                .jwt(builder -> builder
                                        .subject(savedEmployer.getId().toString())
                                        .claim("role", "EMPLOYER"))
                                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYER"))))
                .andExpect(status().isForbidden());
    }

    // ==================== Additional Test Cases ====================

    @Test
    @Order(2)
    @DisplayName("Should prevent duplicate email registration")
    void shouldPreventDuplicateEmailRegistration() throws Exception {
        // First registration
        String requestJson = """
            {
                "email": "duplicate@example.com",
                "password": "SecurePassword123!",
                "role": "JOB_SEEKER"
            }
            """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Second registration with same email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @DisplayName("Should handle work experience CRUD operations")
    void shouldHandleWorkExperienceCRUD() throws Exception {
        // Setup user
        setupTestUser();

        // CREATE: Add work experience
        String createJson = """
            {
                "company": "Test Company",
                "role": "Developer",
                "startDate": "2020-01-01"
            }
            """;

        mockMvc.perform(post("/api/profiles/me/work-experiences")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated());

        // READ: Get profile to get experience ID
        MvcResult result = mockMvc.perform(get("/api/profiles/me")
                        .with(userJwt()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String experienceId = response.get("workExperiences").get(0).get("id").asText();

        // UPDATE: Update the experience
        String updateJson = """
            {
                "company": "Updated Company",
                "role": "Senior Developer",
                "startDate": "2020-01-01",
                "endDate": "2023-12-31"
            }
            """;

        mockMvc.perform(put("/api/profiles/me/work-experiences/" + experienceId)
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workExperiences[0].company").value("Updated Company"));

        // DELETE: Remove the experience
        mockMvc.perform(delete("/api/profiles/me/work-experiences/" + experienceId)
                        .with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workExperiences").isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("Should handle profile searchability toggle")
    void shouldHandleProfileSearchabilityToggle() throws Exception {
        setupTestUser();

        // Initially searchable (default)
        mockMvc.perform(get("/api/profiles/me")
                        .with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchable").value(true));

        // Set to not searchable
        String hideJson = """
            {
                "searchable": false
            }
            """;

        mockMvc.perform(put("/api/profiles/me/searchable")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hideJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchable").value(false));

        // Set back to searchable
        String showJson = """
            {
                "searchable": true
            }
            """;

        mockMvc.perform(put("/api/profiles/me/searchable")
                        .with(userJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(showJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchable").value(true));
    }

    // ==================== Helper Methods ====================

    private void setupTestUser() throws Exception {
        // Register
        String registerJson = String.format("""
            {
                "email": "%s",
                "password": "%s",
                "role": "JOB_SEEKER"
            }
            """, TEST_EMAIL, TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        userId = UUID.fromString(response.get("id").asText());

        // Verify email directly
        com.isaac.job_matching.user.User user = userRepository.findById(userId).orElseThrow();
        user.verifyEmail();
        userRepository.save(user);

        // Get profile ID
        var profile = profileRepository.findByUserId(userId).orElseThrow();
        profileId = profile.getId();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
        return jwt()
                .jwt(builder -> builder
                        .subject(userId.toString())
                        .claim("role", "JOB_SEEKER")
                        .claim("email", TEST_EMAIL))
                .authorities(new SimpleGrantedAuthority("ROLE_JOB_SEEKER"));
    }
}
