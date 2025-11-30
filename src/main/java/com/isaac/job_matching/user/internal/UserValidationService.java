package com.isaac.job_matching.user.internal;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.isaac.job_matching.shared.exception.ValidationException;

/**
 * Internal service for validating user input.
 * 
 * <p>
 * This service is internal to the User module and should not be accessed
 * directly by other modules. It provides validation logic for:
 * <ul>
 * <li>Email format validation</li>
 * <li>Password strength validation</li>
 * </ul>
 */
@Service
public class UserValidationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    /**
     * Validates an email address format.
     * 
     * @param email the email to validate
     * @throws ValidationException if the email is invalid
     */
    public void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("email", "Email is required");
        }

        String trimmedEmail = email.trim();

        if (trimmedEmail.length() > 255) {
            throw new ValidationException("email", "Email must not exceed 255 characters");
        }

        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new ValidationException("email", "Email format is invalid");
        }
    }

    /**
     * Validates password strength.
     * 
     * <p>
     * Password requirements:
     * <ul>
     * <li>Minimum 8 characters</li>
     * <li>Maximum 128 characters</li>
     * <li>At least one uppercase letter</li>
     * <li>At least one lowercase letter</li>
     * <li>At least one digit</li>
     * </ul>
     * 
     * @param password the password to validate
     * @throws ValidationException if the password is invalid
     */
    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("password", "Password is required");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("password",
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("password",
                    "Password must not exceed " + MAX_PASSWORD_LENGTH + " characters");
        }

        if (!containsUppercase(password)) {
            throw new ValidationException("password", "Password must contain at least one uppercase letter");
        }

        if (!containsLowercase(password)) {
            throw new ValidationException("password", "Password must contain at least one lowercase letter");
        }

        if (!containsDigit(password)) {
            throw new ValidationException("password", "Password must contain at least one digit");
        }
    }

    private boolean containsUppercase(String str) {
        return str.chars().anyMatch(Character::isUpperCase);
    }

    private boolean containsLowercase(String str) {
        return str.chars().anyMatch(Character::isLowerCase);
    }

    private boolean containsDigit(String str) {
        return str.chars().anyMatch(Character::isDigit);
    }
}
