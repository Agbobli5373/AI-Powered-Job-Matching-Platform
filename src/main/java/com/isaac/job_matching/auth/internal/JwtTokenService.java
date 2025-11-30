package com.isaac.job_matching.auth.internal;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.isaac.job_matching.auth.AuthenticatedUser;
import com.isaac.job_matching.shared.config.JwtConfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Internal service for JWT token operations.
 * 
 * <p>
 * Handles generation and validation of:
 * <ul>
 * <li>Access tokens (short-lived, for API authorization)</li>
 * <li>Refresh tokens (longer-lived, for obtaining new access tokens)</li>
 * <li>Verification tokens (for email verification)</li>
 * <li>Password reset tokens</li>
 * </ul>
 */
@Service
public class JwtTokenService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TYPE = "type";

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String TOKEN_TYPE_VERIFICATION = "verification";
    private static final String TOKEN_TYPE_PASSWORD_RESET = "password_reset";

    private static final long VERIFICATION_TOKEN_EXPIRATION = 24 * 60 * 60 * 1000; // 24 hours
    private static final long PASSWORD_RESET_TOKEN_EXPIRATION = 60 * 60 * 1000; // 1 hour

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public JwtTokenService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.secretKey = Keys.hmacShaKeyFor(
                jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates an access token for the authenticated user.
     * 
     * @param user the authenticated user information
     * @return the JWT access token
     */
    public String generateAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtConfig.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(user.id().toString())
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_ROLE, user.role())
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .issuer(jwtConfig.getIssuerUri())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generates a refresh token for a user.
     * 
     * @param userId the user ID
     * @return the refresh token
     */
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtConfig.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .issuer(jwtConfig.getIssuerUri())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generates a verification token for email verification.
     * 
     * @param userId the user ID
     * @return the verification token
     */
    public String generateVerificationToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(VERIFICATION_TOKEN_EXPIRATION);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_VERIFICATION)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .issuer(jwtConfig.getIssuerUri())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generates a password reset token.
     * 
     * @param userId the user ID
     * @return the password reset token
     */
    public String generatePasswordResetToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(PASSWORD_RESET_TOKEN_EXPIRATION);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_PASSWORD_RESET)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .issuer(jwtConfig.getIssuerUri())
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates an access token and returns the authenticated user.
     * 
     * @param token the JWT access token
     * @return Optional containing the authenticated user if valid
     */
    public Optional<AuthenticatedUser> validateAccessToken(String token) {
        return parseAndValidate(token, TOKEN_TYPE_ACCESS)
                .map(claims -> new AuthenticatedUser(
                        UUID.fromString(claims.getSubject()),
                        claims.get(CLAIM_EMAIL, String.class),
                        claims.get(CLAIM_ROLE, String.class)));
    }

    /**
     * Validates a refresh token and returns the user ID.
     * 
     * @param token the refresh token
     * @return Optional containing the user ID if valid
     */
    public Optional<UUID> validateRefreshToken(String token) {
        return parseAndValidate(token, TOKEN_TYPE_REFRESH)
                .map(claims -> UUID.fromString(claims.getSubject()));
    }

    /**
     * Validates a verification token and returns the user ID.
     * 
     * @param token the verification token
     * @return Optional containing the user ID if valid
     */
    public Optional<UUID> validateVerificationToken(String token) {
        return parseAndValidate(token, TOKEN_TYPE_VERIFICATION)
                .map(claims -> UUID.fromString(claims.getSubject()));
    }

    /**
     * Validates a password reset token and returns the user ID.
     * 
     * @param token the password reset token
     * @return Optional containing the user ID if valid
     */
    public Optional<UUID> validatePasswordResetToken(String token) {
        return parseAndValidate(token, TOKEN_TYPE_PASSWORD_RESET)
                .map(claims -> UUID.fromString(claims.getSubject()));
    }

    /**
     * Gets the access token expiration time in milliseconds.
     * 
     * @return expiration time in milliseconds
     */
    public long getAccessTokenExpirationMillis() {
        return jwtConfig.getAccessTokenExpiration();
    }

    private Optional<Claims> parseAndValidate(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get(CLAIM_TYPE, String.class);
            if (!expectedType.equals(tokenType)) {
                return Optional.empty();
            }

            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
