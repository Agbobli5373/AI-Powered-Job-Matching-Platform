package com.isaac.job_matching.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.isaac.job_matching.shared.exception.DuplicateEntityException;
import com.isaac.job_matching.shared.exception.EntityNotFoundException;
import com.isaac.job_matching.shared.exception.ValidationException;
import com.isaac.job_matching.user.internal.PasswordEncodingService;
import com.isaac.job_matching.user.internal.UserValidationService;

/**
 * Unit tests for UserService.
 * 
 * <p>
 * Test Categories:
 * <ul>
 * <li>User Registration - email/password validation, duplicate checks, event publishing</li>
 * <li>OAuth Registration - user creation and retrieval</li>
 * <li>User Lookup - by ID and email</li>
 * <li>Email Verification - status transition</li>
 * <li>Password Management - update with validation</li>
 * <li>Account Status - suspend/reactivate</li>
 * <li>Credential Validation - password matching</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserValidationService validationService;

    @Mock
    private PasswordEncodingService passwordEncodingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                validationService,
                passwordEncodingService,
                eventPublisher);
    }

    // ==================== Registration Tests ====================

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register new user successfully with valid data")
        void shouldRegisterUserSuccessfully() {
            // Given
            String email = "test@example.com";
            String password = "Password123!";
            String hashedPassword = "hashed_password";
            UserRole role = UserRole.fromString("JOB_SEEKER");

            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncodingService.encode(password)).thenReturn(hashedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                return user;
            });

            // When
            User result = userService.registerUser(email, password, role);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(result.isEmailVerified()).isFalse();

            verify(validationService).validateEmail(email);
            verify(validationService).validatePassword(password);
            verify(passwordEncodingService).encode(password);
            verify(userRepository).save(any(User.class));
            verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException for existing email")
        void shouldThrowExceptionForDuplicateEmail() {
            // Given
            String email = "existing@example.com";
            String password = "Password123!";
            UserRole role = UserRole.fromString("JOB_SEEKER");

            when(userRepository.existsByEmail(email)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.registerUser(email, password, role))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessageContaining("email");

            verify(userRepository, never()).save(any(User.class));
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw ValidationException for invalid email")
        void shouldThrowExceptionForInvalidEmail() {
            // Given
            String email = "invalid-email";
            String password = "Password123!";
            UserRole role = UserRole.fromString("JOB_SEEKER");

            ValidationException validationException = new ValidationException("email", "Email format is invalid");
            org.mockito.Mockito.doThrow(validationException).when(validationService).validateEmail(email);

            // When/Then
            assertThatThrownBy(() -> userService.registerUser(email, password, role))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("email");

            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw ValidationException for weak password")
        void shouldThrowExceptionForWeakPassword() {
            // Given
            String email = "test@example.com";
            String password = "weak";
            UserRole role = UserRole.fromString("JOB_SEEKER");

            ValidationException validationException = new ValidationException("password", "Password must be at least 8 characters");
            org.mockito.Mockito.doThrow(validationException).when(validationService).validatePassword(password);

            // When/Then
            assertThatThrownBy(() -> userService.registerUser(email, password, role))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("password");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should publish UserRegisteredEvent on successful registration")
        void shouldPublishEventOnRegistration() {
            // Given
            String email = "event@example.com";
            String password = "Password123!";
            UserRole role = UserRole.fromString("JOB_SEEKER");

            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncodingService.encode(password)).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.registerUser(email, password, role);

            // Then
            ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            UserRegisteredEvent event = eventCaptor.getValue();
            assertThat(event.email()).isEqualTo(email);
            assertThat(event.role()).isEqualTo("JOB_SEEKER");
        }
    }

    // ==================== OAuth Registration Tests ====================

    @Nested
    @DisplayName("OAuth Registration Tests")
    class OAuthRegistrationTests {

        @Test
        @DisplayName("Should create new user for OAuth registration")
        void shouldCreateNewOAuthUser() {
            // Given
            String email = "oauth@example.com";
            UserRole role = UserRole.fromString("JOB_SEEKER");

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.registerOrGetOAuthUser(email, role);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.isEmailVerified()).isTrue();
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.isOAuthUser()).isTrue();

            verify(userRepository).save(any(User.class));
            verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
        }

        @Test
        @DisplayName("Should return existing user for OAuth registration")
        void shouldReturnExistingOAuthUser() {
            // Given
            String email = "existing.oauth@example.com";
            UserRole role = UserRole.fromString("JOB_SEEKER");
            User existingUser = new User(email, null, role);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

            // When
            User result = userService.registerOrGetOAuthUser(email, role);

            // Then
            assertThat(result).isSameAs(existingUser);
            verify(userRepository, never()).save(any(User.class));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    // ==================== User Lookup Tests ====================

    @Nested
    @DisplayName("User Lookup Tests")
    class UserLookupTests {

        @Test
        @DisplayName("Should find user by ID successfully")
        void shouldFindUserById() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = new User("test@example.com", "hash", UserRole.fromString("JOB_SEEKER"));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When
            User result = userService.getUserById(userId);

            // Then
            assertThat(result).isSameAs(user);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when user not found by ID")
        void shouldThrowExceptionWhenUserNotFoundById() {
            // Given
            UUID userId = UUID.randomUUID();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.getUserById(userId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User");
        }

        @Test
        @DisplayName("Should find user by email successfully")
        void shouldFindUserByEmail() {
            // Given
            String email = "test@example.com";
            User user = new User(email, "hash", UserRole.fromString("JOB_SEEKER"));

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // When
            Optional<User> result = userService.findByEmail(email);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(user);
        }

        @Test
        @DisplayName("Should return empty optional when user not found by email")
        void shouldReturnEmptyWhenUserNotFoundByEmail() {
            // Given
            String email = "nonexistent@example.com";

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.findByEmail(email);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should check if email is registered")
        void shouldCheckIfEmailIsRegistered() {
            // Given
            String email = "existing@example.com";

            when(userRepository.existsByEmail(email)).thenReturn(true);

            // When
            boolean result = userService.isEmailRegistered(email);

            // Then
            assertThat(result).isTrue();
        }
    }

    // ==================== Email Verification Tests ====================

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email and activate account")
        void shouldVerifyEmailAndActivateAccount() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = new User("test@example.com", "hash", UserRole.fromString("JOB_SEEKER"));
            assertThat(user.isEmailVerified()).isFalse();
            assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.verifyEmail(userId);

            // Then
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should handle already verified email")
        void shouldHandleAlreadyVerifiedEmail() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = User.forOAuth("test@example.com", UserRole.fromString("JOB_SEEKER"));
            assertThat(user.isEmailVerified()).isTrue();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.verifyEmail(userId);

            // Then
            assertThat(user.isEmailVerified()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException for non-existent user")
        void shouldThrowExceptionForNonExistentUserVerification() {
            // Given
            UUID userId = UUID.randomUUID();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.verifyEmail(userId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ==================== Password Management Tests ====================

    @Nested
    @DisplayName("Password Management Tests")
    class PasswordManagementTests {

        @Test
        @DisplayName("Should update password successfully")
        void shouldUpdatePasswordSuccessfully() {
            // Given
            UUID userId = UUID.randomUUID();
            String newPassword = "NewPassword123!";
            String hashedPassword = "new_hashed_password";
            User user = new User("test@example.com", "old_hash", UserRole.fromString("JOB_SEEKER"));

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncodingService.encode(newPassword)).thenReturn(hashedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.updatePassword(userId, newPassword);

            // Then
            assertThat(user.getPasswordHash()).isEqualTo(hashedPassword);
            verify(validationService).validatePassword(newPassword);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw ValidationException for weak new password")
        void shouldThrowExceptionForWeakNewPassword() {
            // Given
            UUID userId = UUID.randomUUID();
            String weakPassword = "weak";

            ValidationException validationException = new ValidationException("password", "Password must be at least 8 characters");
            org.mockito.Mockito.doThrow(validationException).when(validationService).validatePassword(weakPassword);

            // When/Then
            assertThatThrownBy(() -> userService.updatePassword(userId, weakPassword))
                    .isInstanceOf(ValidationException.class);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    // ==================== Account Status Tests ====================

    @Nested
    @DisplayName("Account Status Tests")
    class AccountStatusTests {

        @Test
        @DisplayName("Should suspend active user")
        void shouldSuspendActiveUser() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = User.forOAuth("test@example.com", UserRole.fromString("JOB_SEEKER"));
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.suspendUser(userId);

            // Then
            assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should reactivate suspended user")
        void shouldReactivateSuspendedUser() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = User.forOAuth("test@example.com", UserRole.fromString("JOB_SEEKER"));
            user.suspend();
            assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.reactivateUser(userId);

            // Then
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should throw exception when suspending non-active user")
        void shouldThrowExceptionWhenSuspendingNonActiveUser() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = new User("test@example.com", "hash", UserRole.fromString("JOB_SEEKER"));
            assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // When/Then
            assertThatThrownBy(() -> userService.suspendUser(userId))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ==================== Credential Validation Tests ====================

    @Nested
    @DisplayName("Credential Validation Tests")
    class CredentialValidationTests {

        @Test
        @DisplayName("Should validate correct credentials")
        void shouldValidateCorrectCredentials() {
            // Given
            String email = "test@example.com";
            String password = "Password123!";
            String hashedPassword = "hashed_password";
            User user = new User(email, hashedPassword, UserRole.fromString("JOB_SEEKER"));

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncodingService.matches(password, hashedPassword)).thenReturn(true);

            // When
            Optional<User> result = userService.validateCredentials(email, password);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(user);
        }

        @Test
        @DisplayName("Should reject invalid password")
        void shouldRejectInvalidPassword() {
            // Given
            String email = "test@example.com";
            String password = "WrongPassword";
            String hashedPassword = "hashed_password";
            User user = new User(email, hashedPassword, UserRole.fromString("JOB_SEEKER"));

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncodingService.matches(password, hashedPassword)).thenReturn(false);

            // When
            Optional<User> result = userService.validateCredentials(email, password);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should reject non-existent user")
        void shouldRejectNonExistentUser() {
            // Given
            String email = "nonexistent@example.com";
            String password = "Password123!";

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.validateCredentials(email, password);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should reject OAuth user attempting password login")
        void shouldRejectOAuthUserPasswordLogin() {
            // Given
            String email = "oauth@example.com";
            String password = "Password123!";
            User oauthUser = User.forOAuth(email, UserRole.fromString("JOB_SEEKER"));

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(oauthUser));

            // When
            Optional<User> result = userService.validateCredentials(email, password);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== Statistics Tests ====================

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should count users by role")
        void shouldCountUsersByRole() {
            // Given
            UserRole role = UserRole.fromString("JOB_SEEKER");

            when(userRepository.countByRole("JOB_SEEKER")).thenReturn(42L);

            // When
            long count = userService.countByRole(role);

            // Then
            assertThat(count).isEqualTo(42L);
        }

        @Test
        @DisplayName("Should count users by status")
        void shouldCountUsersByStatus() {
            // Given
            UserStatus status = UserStatus.ACTIVE;

            when(userRepository.countByStatus(status)).thenReturn(100L);

            // When
            long count = userService.countByStatus(status);

            // Then
            assertThat(count).isEqualTo(100L);
        }
    }
}
