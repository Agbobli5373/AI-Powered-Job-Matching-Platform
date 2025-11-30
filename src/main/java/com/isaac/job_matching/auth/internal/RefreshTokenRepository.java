package com.isaac.job_matching.auth.internal;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.isaac.job_matching.shared.config.JwtConfig;

/**
 * Repository for managing refresh tokens in Redis.
 * 
 * <p>
 * Refresh tokens are stored with:
 * <ul>
 * <li>Key: "refresh_token:{token}" → Value: userId</li>
 * <li>Key: "user_tokens:{userId}" → Set of tokens</li>
 * </ul>
 * 
 * <p>
 * This allows:
 * <ul>
 * <li>Quick token validation</li>
 * <li>Revoking individual tokens</li>
 * <li>Revoking all tokens for a user (logout from all devices)</li>
 * </ul>
 */
@Repository
public class RefreshTokenRepository {

    private static final String TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";

    private final StringRedisTemplate redisTemplate;
    private final JwtConfig jwtConfig;
    private final JwtTokenService jwtTokenService;

    public RefreshTokenRepository(
            StringRedisTemplate redisTemplate,
            JwtConfig jwtConfig,
            JwtTokenService jwtTokenService) {
        this.redisTemplate = redisTemplate;
        this.jwtConfig = jwtConfig;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * Saves a refresh token associated with a user.
     * 
     * @param token  the refresh token
     * @param userId the user ID
     */
    public void save(String token, UUID userId) {
        String tokenKey = TOKEN_PREFIX + token;
        String userTokensKey = USER_TOKENS_PREFIX + userId.toString();
        Duration ttl = Duration.ofMillis(jwtConfig.getRefreshTokenExpiration());

        // Store token → userId mapping
        redisTemplate.opsForValue().set(tokenKey, userId.toString(), ttl);

        // Add token to user's set of tokens
        redisTemplate.opsForSet().add(userTokensKey, token);
        redisTemplate.expire(userTokensKey, ttl);
    }

    /**
     * Validates a refresh token and returns the associated user ID.
     * First validates the JWT signature, then checks if it's not revoked.
     * 
     * @param token the refresh token
     * @return Optional containing the user ID if valid and not revoked
     */
    public Optional<UUID> validateAndGetUserId(String token) {
        // First validate the JWT
        Optional<UUID> jwtUserId = jwtTokenService.validateRefreshToken(token);
        if (jwtUserId.isEmpty()) {
            return Optional.empty();
        }

        // Then check if it's in Redis (not revoked)
        String tokenKey = TOKEN_PREFIX + token;
        String userId = redisTemplate.opsForValue().get(tokenKey);

        if (userId == null) {
            return Optional.empty();
        }

        // Verify the user ID matches
        UUID storedUserId = UUID.fromString(userId);
        if (!storedUserId.equals(jwtUserId.get())) {
            return Optional.empty();
        }

        return Optional.of(storedUserId);
    }

    /**
     * Revokes a specific refresh token.
     * 
     * @param token the refresh token to revoke
     */
    public void revoke(String token) {
        String tokenKey = TOKEN_PREFIX + token;

        // Get the user ID before deleting
        String userId = redisTemplate.opsForValue().get(tokenKey);

        // Delete the token
        redisTemplate.delete(tokenKey);

        // Remove from user's token set
        if (userId != null) {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().remove(userTokensKey, token);
        }
    }

    /**
     * Revokes all refresh tokens for a user.
     * 
     * @param userId the user ID
     */
    public void revokeAllForUser(UUID userId) {
        String userTokensKey = USER_TOKENS_PREFIX + userId.toString();

        // Get all tokens for the user
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

        if (tokens != null && !tokens.isEmpty()) {
            // Delete each token
            for (String token : tokens) {
                redisTemplate.delete(TOKEN_PREFIX + token);
            }
        }

        // Delete the user's token set
        redisTemplate.delete(userTokensKey);
    }

    /**
     * Checks if a refresh token exists (not revoked).
     * 
     * @param token the refresh token
     * @return true if the token exists
     */
    public boolean exists(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + token));
    }
}
