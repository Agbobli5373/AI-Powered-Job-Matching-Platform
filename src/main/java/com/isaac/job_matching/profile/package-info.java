/**
 * Profile module - manages job seeker professional profiles.
 * 
 * <p>
 * This module is responsible for:
 * <ul>
 * <li>Profile creation and updates</li>
 * <li>Work experience management</li>
 * <li>Education management</li>
 * <li>Skills management</li>
 * <li>Profile access control (for employer viewing)</li>
 * </ul>
 * 
 * <p>
 * Public API:
 * <ul>
 * <li>{@link com.isaac.job_matching.profile.Profile} - Aggregate root</li>
 * <li>{@link com.isaac.job_matching.profile.ProfileService} - Public service
 * API</li>
 * <li>{@link com.isaac.job_matching.profile.WorkExperience} - Work history
 * entity</li>
 * <li>{@link com.isaac.job_matching.profile.Education} - Education entity</li>
 * <li>{@link com.isaac.job_matching.profile.ProfileSkill} - Skill
 * association</li>
 * <li>{@link com.isaac.job_matching.profile.ProfileUpdatedEvent} - Domain
 * event</li>
 * </ul>
 * 
 * <p>
 * Events published:
 * <ul>
 * <li>{@link com.isaac.job_matching.profile.ProfileUpdatedEvent} - When a
 * profile is updated</li>
 * </ul>
 * 
 * <p>
 * Events consumed:
 * <ul>
 * <li>UserRegisteredEvent - Creates initial profile for job seekers</li>
 * </ul>
 * 
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(displayName = "Profile Management", allowedDependencies = { "user",
        "shared", "shared::config", "shared::exception" })
package com.isaac.job_matching.profile;
