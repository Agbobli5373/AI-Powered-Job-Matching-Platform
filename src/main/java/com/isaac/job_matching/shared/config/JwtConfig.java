package com.isaac.job_matching.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * JWT configuration properties.
 * 
 * <p>
 * Binds to {@code app.jwt.*} properties in application.yaml.
 * 
 * <p>
 * Token expiration settings:
 * <ul>
 * <li>Access token: 15 minutes (short-lived for security)</li>
 * <li>Refresh token: 7 days (long-lived for user convenience)</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {

    /**
     * Secret key for signing JWTs.
     * Should be a strong, random string of at least 256 bits.
     */
    private String secret;

    /**
     * Access token expiration time in milliseconds.
     * Default: 15 minutes (900000 ms)
     */
    private long accessTokenExpiration = 900_000;

    /**
     * Refresh token expiration time in milliseconds.
     * Default: 7 days (604800000 ms)
     */
    private long refreshTokenExpiration = 604_800_000;

    /**
     * Issuer URI for JWT tokens.
     * Used in the 'iss' claim.
     */
    private String issuerUri;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(long accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(long refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    /**
     * Gets access token expiration as Duration.
     * 
     * @return Duration representing access token lifetime
     */
    public Duration getAccessTokenExpirationDuration() {
        return Duration.ofMillis(accessTokenExpiration);
    }

    /**
     * Gets refresh token expiration as Duration.
     * 
     * @return Duration representing refresh token lifetime
     */
    public Duration getRefreshTokenExpirationDuration() {
        return Duration.ofMillis(refreshTokenExpiration);
    }
}
