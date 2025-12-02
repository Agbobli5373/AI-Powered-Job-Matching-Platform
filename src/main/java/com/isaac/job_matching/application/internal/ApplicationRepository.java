package com.isaac.job_matching.application.internal;

import com.isaac.job_matching.application.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for JobApplication entities.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<JobApplication, UUID> {

    /**
     * Find all applications for a specific job.
     */
    List<JobApplication> findByJobId(UUID jobId);

    /**
     * Find all applications for a specific user.
     */
    List<JobApplication> findByUserId(UUID userId);

    /**
     * Find all applications for a specific user with a specific status.
     */
    List<JobApplication> findByUserIdAndStatus(UUID userId, String status);

    /**
     * Check if a user has already applied to a specific job.
     */
    boolean existsByJobIdAndUserId(UUID jobId, UUID userId);

    /**
     * Find application by job and user.
     */
    Optional<JobApplication> findByJobIdAndUserId(UUID jobId, UUID userId);

    /**
     * Count applications by status for a specific job.
     */
    @Query("SELECT COUNT(a) FROM JobApplication a WHERE a.jobId = :jobId AND a.status = :status")
    long countByJobIdAndStatus(@Param("jobId") UUID jobId, @Param("status") String status);
}