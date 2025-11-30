package com.isaac.job_matching.auth.internal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isaac.job_matching.auth.AuthService;
import com.isaac.job_matching.user.User;
import com.isaac.job_matching.user.UserRepository;
import com.isaac.job_matching.user.UserRole;

/**
 * Integration tests for AuthController.
 * 
 * <p>
 * Test Categories:
 * <ul>
 * <li>User Registration - success and failure scenarios</li>
 * <li>User Login - valid credentials, invalid credentials, inactive accounts</li>
 * <li>Token Refresh - valid and invalid tokens</li>
 * <li>Email Verification - valid and invalid/expired tokens</li>
 * <li>Password Reset - initiation and completion</li>
 * <li>Logout - token revocation</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    private static final String API_AUTH_REGISTER = "/api/auth/register";
    private static final String API_AUTH_LOGIN = "/api/auth/login";
    private static final String API_AUTH_REFRESH = "/api/auth/refresh";
    private static final String API_AUTH_VERIFY_EMAIL = "/api/auth/verify-email";
    private static final String API_AUTH_FORGOT_PASSWORD = "/api/auth/forgot-password";
    private static final String API_AUTH_RESET_PASSWORD = "/api/auth/reset-password";
    private static final String API_AUTH_LOGOUT = "/api/auth/logout";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
    }

    // ==================== Registration Tests ====================

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register a new job seeker successfully")
        void shouldRegisterJobSeekerSuccessfully() throws Exception {
            String requestJson = """
                {
                    "email": "jobseeker@example.com",
                    "password": "Password123!",
                    "role": "JOB_SEEKER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.email", is("jobseeker@example.com")))
                    .andExpect(jsonPath("$.message", containsString("Registration successful")));
        }

        @Test
        @DisplayName("Should register a new employer successfully")
        void shouldRegisterEmployerSuccessfully() throws Exception {
            String requestJson = """
                {
                    "email": "employer@example.com",
                    "password": "Password123!",
                    "role": "EMPLOYER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.email", is("employer@example.com")));
        }

        @Test
        @DisplayName("Should reject registration with duplicate email")
        void shouldRejectDuplicateEmail() throws Exception {
            // First registration
            String requestJson = """
                {
                    "email": "duplicate@example.com",
                    "password": "Password123!",
                    "role": "JOB_SEEKER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated());

            // Second registration with same email
            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should reject registration with invalid email format")
        void shouldRejectInvalidEmailFormat() throws Exception {
            String requestJson = """
                {
                    "email": "invalid-email",
                    "password": "Password123!",
                    "role": "JOB_SEEKER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with weak password - too short")
        void shouldRejectWeakPasswordTooShort() throws Exception {
            String requestJson = """
                {
                    "email": "user@example.com",
                    "password": "Pass1!",
                    "role": "JOB_SEEKER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with weak password - no uppercase")
        void shouldRejectWeakPasswordNoUppercase() throws Exception {
            String requestJson = """
                {
                    "email": "user@example.com",
                    "password": "password123!",
                    "role": "JOB_SEEKER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with weak password - no lowercase")
        void shouldRejectWeakPasswordNoLowercase() throws Exception {
            String requestJson = """
                {
                    "email": "user@example.com",
                    "password": "PASSWORD123!",
                    "role": "JOB_SEEKER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with weak password - no digit")
        void shouldRejectWeakPasswordNoDigit() throws Exception {
            String requestJson = """
                {
                    "email": "user@example.com",
                    "password": "PasswordTest!",
                    "role": "JOB_SEEKER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with missing email")
        void shouldRejectMissingEmail() throws Exception {
            String requestJson = """
                {
                    "password": "Password123!",
                    "role": "JOB_SEEKER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with missing password")
        void shouldRejectMissingPassword() throws Exception {
            String requestJson = """
                {
                    "email": "user@example.com",
                    "role": "JOB_SEEKER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with missing role")
        void shouldRejectMissingRole() throws Exception {
            String requestJson = """
                {
                    "email": "user@example.com",
                    "password": "Password123!"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject self-registration as admin")
        void shouldRejectAdminSelfRegistration() throws Exception {
            String requestJson = """
                {
                    "email": "admin@example.com",
                    "password": "Password123!",
                    "role": "ADMIN"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() throws Exception {
            String requestJson = """
                {
                    "email": "USER@EXAMPLE.COM",
                    "password": "Password123!",
                    "role": "JOB_SEEKER"
                }
                """;

            mockMvc.perform(post(API_AUTH_REGISTER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email", is("user@example.com")));
        }
    }

    // ==================== Login Tests ====================

    @Nested
    @DisplayName("User Login Tests")
    class LoginTests {

        @BeforeEach
        void setUpUser() {
            // Register and verify a user for login tests
            User user = authService.register("login@example.com", "Password123!", UserRole.fromString("JOB_SEEKER"));
            authService.verifyEmail(authService.generateVerificationToken(user.getId()));
        }

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            String requestJson = """
                {
                    "email": "login@example.com",
                    "password": "Password123!"
                }
                """;

            mockMvc.perform(post(API_AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.refreshToken", notNullValue()))
                    .andExpect(jsonPath("$.expiresAt", notNullValue()))
                    .andExpect(jsonPath("$.user.email", is("login@example.com")));
        }

        @Test
        @DisplayName("Should reject login with invalid password")
        void shouldRejectInvalidPassword() throws Exception {
            String requestJson = """
                {
                    "email": "login@example.com",
                    "password": "WrongPassword123!"
                }
                """;

            mockMvc.perform(post(API_AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject login with non-existent email")
        void shouldRejectNonExistentEmail() throws Exception {
            String requestJson = """
                {
                    "email": "nonexistent@example.com",
                    "password": "Password123!"
                }
                """;

            mockMvc.perform(post(API_AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject login for pending account")
        void shouldRejectLoginForPendingAccount() throws Exception {
            // Register a new user without verification
            authService.register("pending@example.com", "Password123!", UserRole.fromString("JOB_SEEKER"));

            String requestJson = """
                {
                    "email": "pending@example.com",
                    "password": "Password123!"
                }
                """;

            mockMvc.perform(post(API_AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject login with missing email")
        void shouldRejectMissingEmail() throws Exception {
            String requestJson = """
                {
                    "password": "Password123!"
                }
                """;

            mockMvc.perform(post(API_AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject login with missing password")
        void shouldRejectMissingPassword() throws Exception {
            String requestJson = """
                {
                    "email": "login@example.com"
                }
                """;

            mockMvc.perform(post(API_AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Token Refresh Tests ====================

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        private String refreshToken;

        @BeforeEach
        void setUpTokens() throws Exception {
            // Register, verify, and login to get tokens
            User user = authService.register("refresh@example.com", "Password123!", UserRole.fromString("JOB_SEEKER"));
            authService.verifyEmail(authService.generateVerificationToken(user.getId()));

            String loginJson = """
                {
                    "email": "refresh@example.com",
                    "password": "Password123!"
                }
                """;

            MvcResult result = mockMvc.perform(post(API_AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
            refreshToken = jsonNode.get("refreshToken").asText();
        }

        @Test
        @DisplayName("Should refresh tokens successfully with valid refresh token")
        void shouldRefreshTokensSuccessfully() throws Exception {
            String requestJson = String.format("""
                {
                    "refreshToken": "%s"
                }
                """, refreshToken);

            mockMvc.perform(post(API_AUTH_REFRESH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.refreshToken", notNullValue()))
                    .andExpect(jsonPath("$.expiresAt", notNullValue()));
        }

        @Test
        @DisplayName("Should reject refresh with invalid token")
        void shouldRejectInvalidRefreshToken() throws Exception {
            String requestJson = """
                {
                    "refreshToken": "invalid-token"
                }
                """;

            mockMvc.perform(post(API_AUTH_REFRESH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject refresh with missing token")
        void shouldRejectMissingRefreshToken() throws Exception {
            String requestJson = "{}";

            mockMvc.perform(post(API_AUTH_REFRESH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Email Verification Tests ====================

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        private User registeredUser;
        private String verificationToken;

        @BeforeEach
        void setUpUser() {
            registeredUser = authService.register("verify@example.com", "Password123!", UserRole.fromString("JOB_SEEKER"));
            verificationToken = authService.generateVerificationToken(registeredUser.getId());
        }

        @Test
        @DisplayName("Should verify email successfully with valid token")
        void shouldVerifyEmailSuccessfully() throws Exception {
            String requestJson = String.format("""
                {
                    "token": "%s"
                }
                """, verificationToken);

            mockMvc.perform(post(API_AUTH_VERIFY_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("verified")));
        }

        @Test
        @DisplayName("Should reject verification with invalid token")
        void shouldRejectInvalidVerificationToken() throws Exception {
            String requestJson = """
                {
                    "token": "invalid-verification-token"
                }
                """;

            mockMvc.perform(post(API_AUTH_VERIFY_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject verification with missing token")
        void shouldRejectMissingVerificationToken() throws Exception {
            String requestJson = "{}";

            mockMvc.perform(post(API_AUTH_VERIFY_EMAIL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Password Reset Tests ====================

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @BeforeEach
        void setUpUser() {
            User user = authService.register("reset@example.com", "Password123!", UserRole.fromString("JOB_SEEKER"));
            authService.verifyEmail(authService.generateVerificationToken(user.getId()));
        }

        @Test
        @DisplayName("Should initiate password reset successfully")
        void shouldInitiatePasswordResetSuccessfully() throws Exception {
            String requestJson = """
                {
                    "email": "reset@example.com"
                }
                """;

            mockMvc.perform(post(API_AUTH_FORGOT_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("reset link")));
        }

        @Test
        @DisplayName("Should return success even for non-existent email (security)")
        void shouldReturnSuccessForNonExistentEmail() throws Exception {
            String requestJson = """
                {
                    "email": "nonexistent@example.com"
                }
                """;

            // Should return success to prevent email enumeration
            mockMvc.perform(post(API_AUTH_FORGOT_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should reject password reset with invalid token")
        void shouldRejectInvalidResetToken() throws Exception {
            String requestJson = """
                {
                    "token": "invalid-reset-token",
                    "newPassword": "NewPassword123!"
                }
                """;

            mockMvc.perform(post(API_AUTH_RESET_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject password reset with weak new password")
        void shouldRejectWeakNewPassword() throws Exception {
            // Get a valid reset token first
            String resetToken = authService.initiatePasswordReset("reset@example.com");

            String requestJson = String.format("""
                {
                    "token": "%s",
                    "newPassword": "weak"
                }
                """, resetToken);

            mockMvc.perform(post(API_AUTH_RESET_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Logout Tests ====================

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        private String refreshToken;

        @BeforeEach
        void setUpTokens() throws Exception {
            User user = authService.register("logout@example.com", "Password123!", UserRole.fromString("JOB_SEEKER"));
            authService.verifyEmail(authService.generateVerificationToken(user.getId()));

            String loginJson = """
                {
                    "email": "logout@example.com",
                    "password": "Password123!"
                }
                """;

            MvcResult result = mockMvc.perform(post(API_AUTH_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
            refreshToken = jsonNode.get("refreshToken").asText();
        }

        @Test
        @DisplayName("Should logout successfully and revoke refresh token")
        void shouldLogoutSuccessfully() throws Exception {
            String requestJson = String.format("""
                {
                    "refreshToken": "%s"
                }
                """, refreshToken);

            mockMvc.perform(post(API_AUTH_LOGOUT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("Logged out")));
        }

        @Test
        @DisplayName("Should handle logout with invalid token gracefully")
        void shouldHandleInvalidTokenLogout() throws Exception {
            String requestJson = """
                {
                    "refreshToken": "invalid-token"
                }
                """;

            // Should still return success (idempotent)
            mockMvc.perform(post(API_AUTH_LOGOUT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());
        }
    }
}
