package com.isaac.job_matching.auth;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isaac.job_matching.auth.internal.JwtTokenService;
import com.isaac.job_matching.auth.internal.RefreshTokenRepository;
import com.isaac.job_matching.shared.exception.UnauthorizedException;
import com.isaac.job_matching.shared.exception.ValidationException;
import com.isaac.job_matching.user.User;
import com.isaac.job_matching.user.UserRole;
import com.isaac.job_matching.user.UserService;

/**
 * Public service API for authentication operations.
 * 
 * <p>
 * This is the main entry point for the Auth module, providing:
 * <ul>
 * <li>User registration</li>
 * <li>User login (email/password)</li>
 * <li>Token refresh</li>
 * <li>Email verification</li>
 * <li>Password reset</li>
 * </ul>
 */
@Service
@Transactional
public class AuthService {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(
            UserService userService,
            JwtTokenService jwtTokenService,
            RefreshTokenRepository refreshTokenRepository) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Registers a new user and returns authentication tokens.
     * 
     * @param email    the user's email address
     * @param password the user's password
     * @param role     the user's role (JOB_SEEKER or EMPLOYER)
     * @return the registered user
     */
    public User register(String email, String password, UserRole role) {
        // Only JOB_SEEKER and EMPLOYER can self-register
        if (role.isAdmin()) {
            throw new ValidationException("role", "Cannot self-register as admin");
        }
        return userService.registerUser(email, password, role);
    }

    /**
     * Authenticates a user with email and password.
     * 
     * @param email    the user's email address
     * @param password the user's password
     * @return token pair containing access and refresh tokens
     * @throws UnauthorizedException if credentials are invalid or account is not
     *                               active
     */
    public TokenPair login(String email, String password) {
        User user = userService.validateCredentials(email, password)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.canLogin()) {
            throw new UnauthorizedException("Account is not active. Status: " + user.getStatus().getDescription());
        }

        userService.recordLogin(user.getId());

        return generateTokens(user);
    }

    /**
     * Refreshes an access token using a refresh token.
     * 
     * @param refreshToken the refresh token
     * @return new token pair
     * @throws UnauthorizedException if refresh token is invalid or expired
     */
    public TokenPair refreshToken(String refreshToken) {
        UUID userId = refreshTokenRepository.validateAndGetUserId(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        User user = userService.getUserById(userId);

        if (!user.canLogin()) {
            refreshTokenRepository.revokeAllForUser(userId);
            throw new UnauthorizedException("Account is not active");
        }

        // Revoke the used refresh token and generate new tokens
        refreshTokenRepository.revoke(refreshToken);

        return generateTokens(user);
    }

    /**
     * Verifies a user's email using a verification token.
     * 
     * @param token the verification token
     * @throws UnauthorizedException if token is invalid
     */
    public void verifyEmail(String token) {
        UUID userId = jwtTokenService.validateVerificationToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired verification token"));

        userService.verifyEmail(userId);
    }

    /**
     * Initiates password reset by generating a reset token.
     * 
     * @param email the user's email address
     * @return reset token (to be sent via email)
     */
    public String initiatePasswordReset(String email) {
        return userService.findByEmail(email)
                .map(user -> jwtTokenService.generatePasswordResetToken(user.getId()))
                .orElse(null); // Return null to prevent email enumeration
    }

    /**
     * Resets a user's password using a reset token.
     * 
     * @param token       the password reset token
     * @param newPassword the new password
     * @throws UnauthorizedException if token is invalid
     */
    public void resetPassword(String token, String newPassword) {
        UUID userId = jwtTokenService.validatePasswordResetToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));

        userService.updatePassword(userId, newPassword);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllForUser(userId);
    }

    /**
     * Generates a verification token for a user.
     * 
     * @param userId the user ID
     * @return the verification token
     */
    public String generateVerificationToken(UUID userId) {
        return jwtTokenService.generateVerificationToken(userId);
    }

    /**
     * Logs out a user by revoking their refresh token.
     * 
     * @param refreshToken the refresh token to revoke
     */
    public void logout(String refreshToken) {
        refreshTokenRepository.revoke(refreshToken);
    }

    /**
     * Logs out a user from all devices by revoking all refresh tokens.
     * 
     * @param userId the user ID
     */
    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    /**
     * Gets the authenticated user from a JWT token.
     * 
     * @param token the JWT access token
     * @return the authenticated user information
     * @throws UnauthorizedException if token is invalid
     */
    @Transactional(readOnly = true)
    public AuthenticatedUser getAuthenticatedUser(String token) {
        return jwtTokenService.validateAccessToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired access token"));
    }

    private TokenPair generateTokens(User user) {
        AuthenticatedUser authUser = new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getRoleAsString());

        String accessToken = jwtTokenService.generateAccessToken(authUser);
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId());

        // Store refresh token
        refreshTokenRepository.save(refreshToken, user.getId());

        return TokenPair.of(
                accessToken,
                refreshToken,
                jwtTokenService.getAccessTokenExpirationMillis());
    }
}
