/**
 * Job module for job posting management.
 * 
 * <p>
 * This module provides functionality for:
 * <ul>
 *   <li>Creating and managing job postings</li>
 *   <li>Job lifecycle management (draft, publish, pause, close)</li>
 *   <li>Job skills and requirements</li>
 *   <li>Job search and filtering</li>
 * </ul>
 * 
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.isaac.job_matching.job.Job} - Job aggregate root</li>
 *   <li>{@link com.isaac.job_matching.job.JobSkill} - Job skill requirement</li>
 *   <li>{@link com.isaac.job_matching.job.JobService} - Public service API</li>
 *   <li>{@link com.isaac.job_matching.job.JobRepository} - Data access</li>
 *   <li>{@link com.isaac.job_matching.job.JobStatus} - Status sealed interface</li>
 *   <li>{@link com.isaac.job_matching.job.JobPostedEvent} - Published when job is posted</li>
 *   <li>{@link com.isaac.job_matching.job.JobClosedEvent} - Published when job is closed</li>
 * </ul>
 * 
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>shared - For Skill, Location, Money value objects</li>
 *   <li>company - For company information lookup</li>
 * </ul>
 * 
 * @see com.isaac.job_matching.job.JobService
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "company"}
)
package com.isaac.job_matching.job;
