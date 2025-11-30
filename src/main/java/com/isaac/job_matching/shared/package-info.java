/**
 * Shared module containing common types used across all application modules.
 * 
 * <p>
 * This module provides:
 * <ul>
 * <li>Value objects: {@link com.isaac.job_matching.shared.Money},
 * {@link com.isaac.job_matching.shared.Location}, {@link DateRange}</li>
 * <li>Base entities: {@link com.isaac.job_matching.shared.BaseEntity}</li>
 * <li>Shared entities: {@link com.isaac.job_matching.shared.Skill} with
 * semantic embeddings</li>
 * <li>Configuration classes for security, caching, and observability</li>
 * <li>Exception handling infrastructure</li>
 * </ul>
 * 
 * <p>
 * The module is marked as {@code OPEN} to allow all other modules to access its
 * types.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN, allowedDependencies = {})
package com.isaac.job_matching.shared;
