package com.isaac.job_matching.user.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.isaac.job_matching.shared.exception.ValidationException;

/**
 * Unit tests for UserValidationService.
 * 
 * <p>
 * Test Categories:
 * <ul>
 * <li>Email Validation - format, length, special cases</li>
 * <li>Password Validation - length, complexity requirements</li>
 * </ul>
 */
class UserValidationServiceTest {

    private UserValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new UserValidationService();
    }

    // ==================== Email Validation Tests ====================

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept valid email formats")
        @ValueSource(strings = {
                "test@example.com",
                "user.name@example.com",
                "user+tag@example.com",
                "user123@example.com",
                "test@subdomain.example.com",
                "user_name@example.com",
                "USER@EXAMPLE.COM",
                "test@example.co.uk"
        })
        void shouldAcceptValidEmails(String email) {
            assertThatCode(() -> validationService.validateEmail(email))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @DisplayName("Should reject invalid email formats")
        @ValueSource(strings = {
                "invalid-email",
                "test@",
                "@example.com",
                "test@example",
                "test..test@example.com",
                "test@.example.com",
                "test@ example.com",
                "test @example.com"
        })
        void shouldRejectInvalidEmails(String email) {
            assertThatThrownBy(() -> validationService.validateEmail(email))
                    .isInstanceOf(ValidationException.class);
        }

        @ParameterizedTest
        @DisplayName("Should reject null or empty email")
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        void shouldRejectNullOrEmptyEmail(String email) {
            assertThatThrownBy(() -> validationService.validateEmail(email))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Email is required");
        }

        @Test
        @DisplayName("Should reject email exceeding max length")
        void shouldRejectEmailExceedingMaxLength() {
            String longEmail = "a".repeat(250) + "@example.com";
            
            assertThatThrownBy(() -> validationService.validateEmail(longEmail))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("255 characters");
        }

        @Test
        @DisplayName("Should trim email whitespace")
        void shouldTrimEmailWhitespace() {
            assertThatCode(() -> validationService.validateEmail("  test@example.com  "))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== Password Validation Tests ====================

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @ParameterizedTest
        @DisplayName("Should accept valid passwords")
        @ValueSource(strings = {
                "Password1",
                "Password123",
                "Password123!",
                "ComplexP@ssw0rd!",
                "A1bcdefg",
                "abcdefG1"
        })
        void shouldAcceptValidPasswords(String password) {
            assertThatCode(() -> validationService.validatePassword(password))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @DisplayName("Should reject null or empty password")
        @NullAndEmptySource
        void shouldRejectNullOrEmptyPassword(String password) {
            assertThatThrownBy(() -> validationService.validatePassword(password))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Password is required");
        }

        @Test
        @DisplayName("Should reject password shorter than 8 characters")
        void shouldRejectShortPassword() {
            assertThatThrownBy(() -> validationService.validatePassword("Pass1!"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("at least 8 characters");
        }

        @Test
        @DisplayName("Should reject password longer than 128 characters")
        void shouldRejectLongPassword() {
            String longPassword = "A" + "a".repeat(127) + "1";
            
            assertThatThrownBy(() -> validationService.validatePassword(longPassword))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("128 characters");
        }

        @Test
        @DisplayName("Should reject password without uppercase letter")
        void shouldRejectPasswordWithoutUppercase() {
            assertThatThrownBy(() -> validationService.validatePassword("password123"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("uppercase letter");
        }

        @Test
        @DisplayName("Should reject password without lowercase letter")
        void shouldRejectPasswordWithoutLowercase() {
            assertThatThrownBy(() -> validationService.validatePassword("PASSWORD123"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("lowercase letter");
        }

        @Test
        @DisplayName("Should reject password without digit")
        void shouldRejectPasswordWithoutDigit() {
            assertThatThrownBy(() -> validationService.validatePassword("Passwordtest"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("digit");
        }

        @Test
        @DisplayName("Should accept password at minimum length boundary")
        void shouldAcceptPasswordAtMinLength() {
            // Exactly 8 characters with all requirements
            assertThatCode(() -> validationService.validatePassword("Abcdefg1"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept password at maximum length boundary")
        void shouldAcceptPasswordAtMaxLength() {
            // Exactly 128 characters with all requirements
            String maxLengthPassword = "A" + "a".repeat(125) + "1a";
            assertThatCode(() -> validationService.validatePassword(maxLengthPassword))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept password with special characters")
        void shouldAcceptPasswordWithSpecialCharacters() {
            assertThatCode(() -> validationService.validatePassword("Password1!@#$%^&*()"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept password with unicode characters")
        void shouldAcceptPasswordWithUnicodeCharacters() {
            assertThatCode(() -> validationService.validatePassword("Password1éàü"))
                    .doesNotThrowAnyException();
        }
    }
}
