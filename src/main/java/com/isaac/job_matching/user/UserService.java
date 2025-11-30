package com.isaac.job_matching.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isaac.job_matching.shared.exception.DuplicateEntityException;
import com.isaac.job_matching.shared.exception.EntityNotFoundException;
import com.isaac.job_matching.user.internal.PasswordEncodingService;
import com.isaac.job_matching.user.internal.UserValidationService;

/**
 * Public service API for user management operations.
 * 
 * <p>
 * This is the main entry point for the User module, providing:
 * <ul>
 * <li>User registration (email/password and OAuth)</li>
 * <li>User lookup and management</li>
 * <li>Email verification</li>
 * <li>Password management</li>
 * <li>Account status management (admin only)</li>
 * </ul>
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserValidationService validationService;
    private final PasswordEncodingService passwordEncodingService;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(
            UserRepository userRepository,
            UserValidationService validationService,
            PasswordEncodingService passwordEncodingService,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.passwordEncodingService = passwordEncodingService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registers a new user with email and password.
     * 
     * @param email    the user's email address
     * @param password the plaintext password
     * @param role     the user's role (JOB_SEEKER or EMPLOYER)
     * @return the created user
     * @throws DuplicateEntityException if email is already registered
     */
    public User registerUser(String email, String password, UserRole role) {
        validationService.validateEmail(email);
        validationService.validatePassword(password);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEntityException("User", "email", email);
        }

        String passwordHash = passwordEncodingService.encode(password);
        User user = new User(email, passwordHash, role);
        user = userRepository.save(user);

        eventPublisher.publishEvent(UserRegisteredEvent.from(user));

        return user;
    }

    /**
     * Registers or retrieves a user via OAuth authentication.
     * If the user already exists, returns the existing user.
     * 
     * @param email the user's email from OAuth provider
     * @param role  the user's role (JOB_SEEKER or EMPLOYER)
     * @return the user (existing or newly created)
     */
    public User registerOrGetOAuthUser(String email, UserRole role) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = User.forOAuth(email, role);
                    user = userRepository.save(user);
                    eventPublisher.publishEvent(UserRegisteredEvent.from(user));
                    return user;
                });
    }

    /**
     * Finds a user by ID.
     * 
     * @param id the user ID
     * @return the user
     * @throws EntityNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    /**
     * Finds a user by email address.
     * 
     * @param email the email address
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Verifies a user's email address.
     * 
     * @param userId the user ID
     * @throws EntityNotFoundException if user not found
     */
    public void verifyEmail(UUID userId) {
        User user = getUserById(userId);
        user.verifyEmail();
        userRepository.save(user);
    }

    /**
     * Records a successful login for the user.
     * 
     * @param userId the user ID
     */
    public void recordLogin(UUID userId) {
        User user = getUserById(userId);
        user.recordLogin();
        userRepository.save(user);
    }

    /**
     * Updates a user's password.
     * 
     * @param userId      the user ID
     * @param newPassword the new plaintext password
     * @throws EntityNotFoundException if user not found
     */
    public void updatePassword(UUID userId, String newPassword) {
        validationService.validatePassword(newPassword);

        User user = getUserById(userId);
        String passwordHash = passwordEncodingService.encode(newPassword);
        user.updatePassword(passwordHash);
        userRepository.save(user);
    }

    /**
     * Suspends a user account (admin operation).
     * 
     * @param userId the user ID to suspend
     * @throws EntityNotFoundException if user not found
     * @throws IllegalStateException   if user cannot be suspended
     */
    public void suspendUser(UUID userId) {
        User user = getUserById(userId);
        user.suspend();
        userRepository.save(user);
    }

    /**
     * Reactivates a suspended user account (admin operation).
     * 
     * @param userId the user ID to reactivate
     * @throws EntityNotFoundException if user not found
     * @throws IllegalStateException   if user cannot be reactivated
     */
    public void reactivateUser(UUID userId) {
        User user = getUserById(userId);
        user.reactivate();
        userRepository.save(user);
    }

    /**
     * Checks if an email is already registered.
     * 
     * @param email the email to check
     * @return true if the email is already in use
     */
    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Validates credentials and returns the user if valid.
     * 
     * @param email    the email address
     * @param password the plaintext password
     * @return Optional containing the user if credentials are valid
     */
    @Transactional(readOnly = true)
    public Optional<User> validateCredentials(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> !user.isOAuthUser())
                .filter(user -> passwordEncodingService.matches(password, user.getPasswordHash()));
    }

    /**
     * Gets count of users by role.
     * 
     * @param role the role to count
     * @return number of users with the specified role
     */
    @Transactional(readOnly = true)
    public long countByRole(UserRole role) {
        return userRepository.countByRole(role.name());
    }

    /**
     * Gets count of users by status.
     * 
     * @param status the status to count
     * @return number of users with the specified status
     */
    @Transactional(readOnly = true)
    public long countByStatus(UserStatus status) {
        return userRepository.countByStatus(status);
    }
}
