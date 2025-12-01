package com.isaac.job_matching.job.internal;

import static org.hamcrest.Matchers.greaterThan;
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

import java.lang.reflect.Field;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.job_matching.company.Company;
import com.isaac.job_matching.company.CompanyRepository;
import com.isaac.job_matching.job.JobRepository;
import com.isaac.job_matching.shared.Skill;
import com.isaac.job_matching.shared.SkillRepository;
import com.isaac.job_matching.user.User;
import com.isaac.job_matching.user.UserRepository;
import com.isaac.job_matching.user.UserRole;

/**
 * Integration tests for JobController REST endpoints.
 * 
 * <p>
 * Tests the complete request/response cycle including:
 * <ul>
 * <li>Job CRUD operations</li>
 * <li>Status transitions (publish, pause, close)</li>
 * <li>Skills management</li>
 * <li>Search and filtering</li>
 * <li>Authorization enforcement</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("Job Controller Integration Tests")
class JobControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private SkillRepository skillRepository;

    private User employerUser;
    private Company company;
    private Skill javaSkill;
    private Skill pythonSkill;
    private Skill springSkill;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Clean up
        jobRepository.deleteAll();
        companyRepository.deleteAll();
        skillRepository.deleteAll();
        userRepository.deleteAll();

        // Create test employer
        employerUser = new User("employer@test.com", "hashedPassword", UserRole.fromString("EMPLOYER"));
        employerUser.verifyEmail();
        employerUser = userRepository.save(employerUser);

        // Create company for employer
        company = new Company(employerUser.getId(), "Test Company Inc");
        company.setDescription("A great technology company");
        company.setIndustry("Technology");
        company = companyRepository.save(company);

        // Create skills
        javaSkill = skillRepository.save(new Skill("Java", "Programming"));
        pythonSkill = skillRepository.save(new Skill("Python", "Programming"));
        springSkill = skillRepository.save(new Skill("Spring Boot", "Framework"));
    }

    // ==================== Test Utilities ====================

    private org.springframework.test.web.servlet.request.RequestPostProcessor employerJwt() {
        return jwt()
                .jwt(builder -> builder
                        .subject(employerUser.getId().toString())
                        .claim("role", "EMPLOYER")
                        .claim("email", "employer@test.com"))
                .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYER"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jobSeekerJwt() {
        return jwt()
                .jwt(builder -> builder
                        .subject(UUID.randomUUID().toString())
                        .claim("role", "JOB_SEEKER")
                        .claim("email", "seeker@test.com"))
                .authorities(new SimpleGrantedAuthority("ROLE_JOB_SEEKER"));
    }

    private String createJobRequest(String title, String description) {
        return String.format("""
            {
                "title": "%s",
                "description": "%s",
                "remoteOption": "REMOTE",
                "experienceLevel": "MID",
                "employmentType": "FULL_TIME"
            }
            """, title, description);
    }

    private UUID createDraftJob(String title) throws Exception {
        String requestJson = createJobRequest(title, 
                "A detailed job description that is at least 50 characters long for validation purposes.");

        MvcResult result = mockMvc.perform(post("/api/jobs")
                        .with(employerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("id").asText());
    }

    // ==================== Job Creation Tests ====================

    @Nested
    @DisplayName("Job Creation Tests")
    class JobCreationTests {

        @Test
        @DisplayName("Should create job successfully")
        void shouldCreateJob() throws Exception {
            String requestJson = createJobRequest("Senior Java Developer",
                    "We are looking for a senior Java developer to join our growing team. " +
                    "The ideal candidate will have 5+ years of experience with Java and Spring Boot.");

            mockMvc.perform(post("/api/jobs")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.title").value("Senior Java Developer"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.remoteOption").value("REMOTE"))
                    .andExpect(jsonPath("$.experienceLevel").value("MID"))
                    .andExpect(jsonPath("$.employmentType").value("FULL_TIME"))
                    .andExpect(jsonPath("$.companyId").value(company.getId().toString()));
        }

        @Test
        @DisplayName("Should create job with all fields")
        void shouldCreateJobWithAllFields() throws Exception {
            String requestJson = String.format("""
                {
                    "title": "Full Stack Developer",
                    "description": "We are looking for a full stack developer with experience in both frontend and backend technologies. Must be comfortable with agile methodologies.",
                    "remoteOption": "HYBRID",
                    "experienceLevel": "SENIOR",
                    "employmentType": "FULL_TIME",
                    "location": {
                        "city": "San Francisco",
                        "state": "California",
                        "country": "United States"
                    },
                    "salaryMin": {
                        "amount": 120000,
                        "currency": "USD"
                    },
                    "salaryMax": {
                        "amount": 180000,
                        "currency": "USD"
                    },
                    "salaryVisible": true,
                    "deadline": "2025-12-31"
                }
                """);

            mockMvc.perform(post("/api/jobs")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Full Stack Developer"))
                    .andExpect(jsonPath("$.remoteOption").value("HYBRID"))
                    .andExpect(jsonPath("$.experienceLevel").value("SENIOR"))
                    .andExpect(jsonPath("$.location.city").value("San Francisco"))
                    .andExpect(jsonPath("$.salaryMin.amount").value(120000))
                    .andExpect(jsonPath("$.salaryMax.amount").value(180000))
                    .andExpect(jsonPath("$.salaryVisible").value(true));
        }

        @Test
        @DisplayName("Should reject job creation with missing title")
        void shouldRejectJobWithMissingTitle() throws Exception {
            String requestJson = """
                {
                    "description": "A detailed job description with more than fifty characters for testing purposes.",
                    "remoteOption": "REMOTE",
                    "experienceLevel": "MID",
                    "employmentType": "FULL_TIME"
                }
                """;

            mockMvc.perform(post("/api/jobs")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject job creation for job seeker")
        void shouldRejectJobCreationForJobSeeker() throws Exception {
            String requestJson = createJobRequest("Test Job", 
                    "A detailed description with more than fifty characters for testing purposes.");

            mockMvc.perform(post("/api/jobs")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject job creation without authentication")
        void shouldRejectJobCreationWithoutAuth() throws Exception {
            String requestJson = createJobRequest("Test Job",
                    "A detailed description with more than fifty characters for testing purposes.");

            mockMvc.perform(post("/api/jobs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== Job Retrieval Tests ====================

    @Nested
    @DisplayName("Job Retrieval Tests")
    class JobRetrievalTests {

        @Test
        @DisplayName("Should get job by ID")
        void shouldGetJobById() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            mockMvc.perform(get("/api/jobs/" + jobId)
                            .with(employerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(jobId.toString()))
                    .andExpect(jsonPath("$.title").value("Test Job"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent job")
        void shouldReturn404ForNonExistentJob() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/api/jobs/" + nonExistentId)
                            .with(employerJwt()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should get jobs for company")
        void shouldGetJobsForCompany() throws Exception {
            createDraftJob("Job 1");
            createDraftJob("Job 2");

            mockMvc.perform(get("/api/jobs/company/" + company.getId())
                            .with(employerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        @DisplayName("Should get employer's own jobs")
        void shouldGetEmployerOwnJobs() throws Exception {
            createDraftJob("My Job 1");
            createDraftJob("My Job 2");

            mockMvc.perform(get("/api/jobs/my-jobs")
                            .with(employerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }
    }

    // ==================== Job Update Tests ====================

    @Nested
    @DisplayName("Job Update Tests")
    class JobUpdateTests {

        @Test
        @DisplayName("Should update job title and description")
        void shouldUpdateJobTitleAndDescription() throws Exception {
            UUID jobId = createDraftJob("Original Title");

            String updateJson = """
                {
                    "title": "Updated Title",
                    "description": "Updated description that is at least 50 characters long for validation."
                }
                """;

            mockMvc.perform(put("/api/jobs/" + jobId)
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }

        @Test
        @DisplayName("Should update job location")
        void shouldUpdateJobLocation() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            String updateJson = """
                {
                    "location": {
                        "city": "New York",
                        "state": "NY",
                        "country": "United States"
                    }
                }
                """;

            mockMvc.perform(put("/api/jobs/" + jobId)
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.location.city").value("New York"));
        }

        @Test
        @DisplayName("Should update job salary")
        void shouldUpdateJobSalary() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            String updateJson = """
                {
                    "salaryMin": { "amount": 80000, "currency": "USD" },
                    "salaryMax": { "amount": 120000, "currency": "USD" },
                    "salaryVisible": true
                }
                """;

            mockMvc.perform(put("/api/jobs/" + jobId)
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.salaryMin.amount").value(80000))
                    .andExpect(jsonPath("$.salaryMax.amount").value(120000));
        }

        @Test
        @DisplayName("Should delete draft job")
        void shouldDeleteDraftJob() throws Exception {
            UUID jobId = createDraftJob("Job to Delete");

            mockMvc.perform(delete("/api/jobs/" + jobId)
                            .with(employerJwt()))
                    .andExpect(status().isNoContent());

            // Verify job is deleted
            mockMvc.perform(get("/api/jobs/" + jobId)
                            .with(employerJwt()))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Skills Management Tests ====================

    @Nested
    @DisplayName("Skills Management Tests")
    class SkillsManagementTests {

        @Test
        @DisplayName("Should add skill to job")
        void shouldAddSkillToJob() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            String skillJson = String.format("""
                {
                    "skillId": "%s",
                    "importance": "MUST_HAVE"
                }
                """, javaSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skillJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skills", hasSize(1)))
                    .andExpect(jsonPath("$.skills[0].skillName").value("Java"))
                    .andExpect(jsonPath("$.skills[0].importance").value("MUST_HAVE"));
        }

        @Test
        @DisplayName("Should add multiple skills to job")
        void shouldAddMultipleSkillsToJob() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            // Add first skill
            String skill1Json = String.format("""
                { "skillId": "%s", "importance": "MUST_HAVE" }
                """, javaSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skill1Json))
                    .andExpect(status().isOk());

            // Add second skill
            String skill2Json = String.format("""
                { "skillId": "%s", "importance": "NICE_TO_HAVE" }
                """, pythonSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skill2Json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skills", hasSize(2)));
        }

        @Test
        @DisplayName("Should remove skill from job")
        void shouldRemoveSkillFromJob() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            // First add a skill
            String skillJson = String.format("""
                { "skillId": "%s", "importance": "MUST_HAVE" }
                """, javaSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skillJson))
                    .andExpect(status().isOk());

            // Then remove it
            mockMvc.perform(delete("/api/jobs/" + jobId + "/skills/" + javaSkill.getId())
                            .with(employerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skills", hasSize(0)));
        }
    }

    // ==================== Status Transition Tests ====================

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should publish job")
        void shouldPublishJob() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            // Add required skill first
            String skillJson = String.format("""
                { "skillId": "%s", "importance": "MUST_HAVE" }
                """, javaSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skillJson))
                    .andExpect(status().isOk());

            // Publish
            mockMvc.perform(post("/api/jobs/" + jobId + "/publish")
                            .with(employerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.postedAt").exists());
        }

        @Test
        @DisplayName("Should not publish job without required skills")
        void shouldNotPublishJobWithoutRequiredSkills() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            mockMvc.perform(post("/api/jobs/" + jobId + "/publish")
                            .with(employerJwt()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should pause active job")
        void shouldPauseActiveJob() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            // Add required skill
            String skillJson = String.format("""
                { "skillId": "%s", "importance": "MUST_HAVE" }
                """, javaSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skillJson))
                    .andExpect(status().isOk());

            // Publish first
            mockMvc.perform(post("/api/jobs/" + jobId + "/publish")
                            .with(employerJwt()))
                    .andExpect(status().isOk());

            // Pause
            String pauseJson = """
                { "reason": "Position filled temporarily" }
                """;

            mockMvc.perform(post("/api/jobs/" + jobId + "/pause")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(pauseJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PAUSED"));
        }

        @Test
        @DisplayName("Should resume paused job")
        void shouldResumePausedJob() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            // Add required skill, publish, pause
            String skillJson = String.format("""
                { "skillId": "%s", "importance": "MUST_HAVE" }
                """, javaSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skillJson))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/jobs/" + jobId + "/publish")
                            .with(employerJwt()))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/jobs/" + jobId + "/pause")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());

            // Resume
            mockMvc.perform(post("/api/jobs/" + jobId + "/resume")
                            .with(employerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should close job with reason")
        void shouldCloseJobWithReason() throws Exception {
            UUID jobId = createDraftJob("Test Job");

            // Add required skill and publish
            String skillJson = String.format("""
                { "skillId": "%s", "importance": "MUST_HAVE" }
                """, javaSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skillJson))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/jobs/" + jobId + "/publish")
                            .with(employerJwt()))
                    .andExpect(status().isOk());

            // Close
            String closeJson = """
                { "reason": "FILLED" }
                """;

            mockMvc.perform(post("/api/jobs/" + jobId + "/close")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(closeJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSED"));
        }
    }

    // ==================== Search Tests ====================

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        private void publishJobWithSkill(String title) throws Exception {
            UUID jobId = createDraftJob(title);

            String skillJson = String.format("""
                { "skillId": "%s", "importance": "MUST_HAVE" }
                """, javaSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skillJson))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/jobs/" + jobId + "/publish")
                            .with(employerJwt()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should get active jobs")
        void shouldGetActiveJobs() throws Exception {
            publishJobWithSkill("Active Job 1");
            publishJobWithSkill("Active Job 2");
            createDraftJob("Draft Job"); // Not published

            mockMvc.perform(get("/api/jobs/active")
                            .with(employerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        @DisplayName("Should search jobs by keyword")
        void shouldSearchJobsByKeyword() throws Exception {
            publishJobWithSkill("Java Developer Position");
            publishJobWithSkill("Python Engineer");

            mockMvc.perform(get("/api/jobs/search")
                            .param("keyword", "Java")
                            .with(employerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].title").value("Java Developer Position"));
        }

        @Test
        @DisplayName("Should filter by remote option")
        void shouldFilterByRemoteOption() throws Exception {
            publishJobWithSkill("Remote Position");

            mockMvc.perform(get("/api/jobs/filter/remote")
                            .param("option", "REMOTE")
                            .with(employerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
        }
    }

    // ==================== Statistics Tests ====================

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get job statistics")
        void shouldGetJobStatistics() throws Exception {
            createDraftJob("Draft Job");

            // Add skill and publish one job
            UUID jobId = createDraftJob("Active Job");
            String skillJson = String.format("""
                { "skillId": "%s", "importance": "MUST_HAVE" }
                """, javaSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skillJson))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/jobs/" + jobId + "/publish")
                            .with(employerJwt()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/jobs/stats")
                            .with(employerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalJobs").value(greaterThan(0)))
                    .andExpect(jsonPath("$.activeJobs").value(1));
        }
    }

    // ==================== Authorization Tests ====================

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should not allow job seeker to create jobs")
        void shouldNotAllowJobSeekerToCreateJobs() throws Exception {
            String requestJson = createJobRequest("Test Job",
                    "A detailed description with more than fifty characters for testing purposes.");

            mockMvc.perform(post("/api/jobs")
                            .with(jobSeekerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should not allow updating other employer's job")
        void shouldNotAllowUpdatingOtherEmployersJob() throws Exception {
            UUID jobId = createDraftJob("My Job");

            // Create another employer
            User anotherEmployer = new User("other@test.com", "hash", UserRole.fromString("EMPLOYER"));
            anotherEmployer.verifyEmail();
            anotherEmployer = userRepository.save(anotherEmployer);
            final UUID otherEmployerId = anotherEmployer.getId();

            String updateJson = """
                { "title": "Hacked Title" }
                """;

            mockMvc.perform(put("/api/jobs/" + jobId)
                            .with(jwt()
                                    .jwt(builder -> builder
                                            .subject(otherEmployerId.toString())
                                            .claim("role", "EMPLOYER"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_EMPLOYER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Job seeker can view active jobs")
        void jobSeekerCanViewActiveJobs() throws Exception {
            // Create and publish a job
            UUID jobId = createDraftJob("Public Job");
            String skillJson = String.format("""
                { "skillId": "%s", "importance": "MUST_HAVE" }
                """, javaSkill.getId());

            mockMvc.perform(post("/api/jobs/" + jobId + "/skills")
                            .with(employerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(skillJson))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/jobs/" + jobId + "/publish")
                            .with(employerJwt()))
                    .andExpect(status().isOk());

            // Job seeker views active jobs
            mockMvc.perform(get("/api/jobs/active")
                            .with(jobSeekerJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
        }
    }
}
