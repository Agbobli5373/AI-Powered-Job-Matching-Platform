package com.isaac.job_matching.auth;

import java.time.Instant;

/**
 * Record representing an access/refresh token pair.
 * 
 * <p>
 * This is returned after successful authentication and contains:
 * <ul>
 * <li>Access token - short-lived JWT for API authorization</li>
 * <li>Refresh token - longer-lived token for obtaining new access tokens</li>
 * <li>Expiration time of the access token</li>
 * </ul>
 * 
 * @param accessToken  the JWT access token
 * @param refreshToken the refresh token
 * @param expiresAt    when the access token expires
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        Instant expiresAt) {

    /**
     * Creates a TokenPair with calculated expiration.
     * 
     * @param accessToken      the JWT access token
     * @param refreshToken     the refresh token
     * @param expirationMillis expiration time in milliseconds from now
     * @return TokenPair with calculated expiration
     */
    public static TokenPair of(String accessToken, String refreshToken, long expirationMillis) {
        return new TokenPair(
                accessToken,
                refreshToken,
                Instant.now().plusMillis(expirationMillis));
    }

    /**
     * Checks if the access token has expired.
     * 
     * @return true if the access token has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Gets the remaining lifetime of the access token in seconds.
     * 
     * @return remaining seconds, or 0 if expired
     */
    public long remainingSeconds() {
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
}
