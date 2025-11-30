/**
 * User module - manages platform user accounts and authentication data.
 * 
 * <p>
 * This module is responsible for:
 * <ul>
 * <li>User registration and account management</li>
 * <li>Email verification</li>
 * <li>Password management</li>
 * <li>User role assignment</li>
 * </ul>
 * 
 * <p>
 * Public API:
 * <ul>
 * <li>{@link com.isaac.job_matching.user.User} - Aggregate root</li>
 * <li>{@link com.isaac.job_matching.user.UserService} - Public service API</li>
 * <li>{@link com.isaac.job_matching.user.UserRole} - User role definitions</li>
 * <li>{@link com.isaac.job_matching.user.UserStatus} - User status
 * enumeration</li>
 * <li>{@link com.isaac.job_matching.user.UserRegisteredEvent} - Domain
 * event</li>
 * <li>{@link com.isaac.job_matching.user.UserRepository} - Data access</li>
 * </ul>
 * 
 * <p>
 * Events published:
 * <ul>
 * <li>{@link com.isaac.job_matching.user.UserRegisteredEvent} - When a new user
 * registers</li>
 * </ul>
 * 
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(displayName = "User Management", allowedDependencies = { "shared",
        "shared::config", "shared::exception" })
package com.isaac.job_matching.user;
