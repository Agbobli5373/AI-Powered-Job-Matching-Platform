/**
 * Authentication module - handles user authentication and token management.
 * 
 * <p>
 * This module is responsible for:
 * <ul>
 * <li>User authentication (login/logout)</li>
 * <li>JWT token generation and validation</li>
 * <li>Refresh token management</li>
 * <li>Email verification tokens</li>
 * <li>Password reset tokens</li>
 * <li>OAuth2 integration (Google, LinkedIn, GitHub)</li>
 * </ul>
 * 
 * <p>
 * Public API:
 * <ul>
 * <li>{@link com.isaac.job_matching.auth.AuthService} - Public service API</li>
 * <li>{@link com.isaac.job_matching.auth.TokenPair} - Access/refresh token
 * pair</li>
 * <li>{@link com.isaac.job_matching.auth.AuthenticatedUser} - Authenticated
 * user info</li>
 * </ul>
 * 
 * <p>
 * Dependencies:
 * <ul>
 * <li>User module - for user management and validation</li>
 * <li>Shared module - for configuration and exceptions</li>
 * </ul>
 * 
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(displayName = "Authentication", allowedDependencies = { "user",
        "shared", "shared::config", "shared::exception" })
package com.isaac.job_matching.auth;
