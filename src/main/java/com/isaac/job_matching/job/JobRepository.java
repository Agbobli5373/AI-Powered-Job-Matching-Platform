package com.isaac.job_matching.job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Job entity CRUD operations.
 */
@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Finds all jobs for a company.
     * 
     * @param companyId the company ID
     * @return list of jobs
     */
    List<Job> findByCompanyId(UUID companyId);

    /**
     * Finds all jobs for a company with pagination.
     * 
     * @param companyId the company ID
     * @param pageable pagination parameters
     * @return page of jobs
     */
    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);

    /**
     * Finds all jobs for a company with a specific status.
     * 
     * @param companyId the company ID
     * @param status the job status
     * @param pageable pagination parameters
     * @return page of jobs
     */
    Page<Job> findByCompanyIdAndStatusName(UUID companyId, Job.StatusName status, Pageable pageable);

    /**
     * Finds a job by ID with skills eagerly loaded.
     * 
     * @param id the job ID
     * @return optional containing the job if found
     */
    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.skills WHERE j.id = :id")
    Optional<Job> findByIdWithSkills(@Param("id") UUID id);

    /**
     * Finds all active jobs.
     * 
     * @param pageable pagination parameters
     * @return page of active jobs
     */
    @Query("SELECT j FROM Job j WHERE j.statusName = 'ACTIVE' ORDER BY j.postedAt DESC")
    Page<Job> findActiveJobs(Pageable pageable);

    /**
     * Finds active jobs by remote option.
     * 
     * @param remoteOption the remote option
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Query("SELECT j FROM Job j WHERE j.statusName = 'ACTIVE' AND j.remoteOption = :remoteOption ORDER BY j.postedAt DESC")
    Page<Job> findActiveByRemoteOption(@Param("remoteOption") RemoteOption remoteOption, Pageable pageable);

    /**
     * Finds active jobs by experience level.
     * 
     * @param experienceLevel the experience level
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Query("SELECT j FROM Job j WHERE j.statusName = 'ACTIVE' AND j.experienceLevel = :experienceLevel ORDER BY j.postedAt DESC")
    Page<Job> findActiveByExperienceLevel(@Param("experienceLevel") ExperienceLevel experienceLevel, Pageable pageable);

    /**
     * Finds active jobs by employment type.
     * 
     * @param employmentType the employment type
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Query("SELECT j FROM Job j WHERE j.statusName = 'ACTIVE' AND j.employmentType = :employmentType ORDER BY j.postedAt DESC")
    Page<Job> findActiveByEmploymentType(@Param("employmentType") EmploymentType employmentType, Pageable pageable);

    /**
     * Finds active jobs by location country.
     * 
     * @param country the country
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Query("SELECT j FROM Job j WHERE j.statusName = 'ACTIVE' AND j.locationCountry = :country ORDER BY j.postedAt DESC")
    Page<Job> findActiveByCountry(@Param("country") String country, Pageable pageable);

    /**
     * Finds active jobs by location city and country.
     * 
     * @param city the city
     * @param country the country
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Query("SELECT j FROM Job j WHERE j.statusName = 'ACTIVE' AND j.locationCity = :city AND j.locationCountry = :country ORDER BY j.postedAt DESC")
    Page<Job> findActiveByLocation(@Param("city") String city, @Param("country") String country, Pageable pageable);

    /**
     * Searches active jobs by title keyword.
     * 
     * @param keyword the search keyword
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Query("SELECT j FROM Job j WHERE j.statusName = 'ACTIVE' AND LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY j.postedAt DESC")
    Page<Job> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Searches active jobs by title or description keyword.
     * 
     * @param keyword the search keyword
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Query("SELECT j FROM Job j WHERE j.statusName = 'ACTIVE' AND (LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY j.postedAt DESC")
    Page<Job> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Finds active jobs requiring a specific skill.
     * 
     * @param skillId the skill ID
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Query("SELECT DISTINCT j FROM Job j JOIN j.skills s WHERE j.statusName = 'ACTIVE' AND s.skillId = :skillId ORDER BY j.postedAt DESC")
    Page<Job> findActiveBySkill(@Param("skillId") UUID skillId, Pageable pageable);

    /**
     * Finds active jobs within salary range.
     * 
     * @param minSalary minimum salary
     * @param maxSalary maximum salary
     * @param pageable pagination parameters
     * @return page of matching jobs
     */
    @Query("SELECT j FROM Job j WHERE j.statusName = 'ACTIVE' AND j.salaryVisible = true AND j.salaryMin >= :minSalary AND j.salaryMax <= :maxSalary ORDER BY j.postedAt DESC")
    Page<Job> findActiveBySalaryRange(@Param("minSalary") java.math.BigDecimal minSalary, 
                                       @Param("maxSalary") java.math.BigDecimal maxSalary, 
                                       Pageable pageable);

    /**
     * Counts jobs by status.
     * 
     * @param status the job status
     * @return count of jobs with the status
     */
    long countByStatusName(Job.StatusName status);

    /**
     * Counts jobs for a company.
     * 
     * @param companyId the company ID
     * @return count of jobs
     */
    long countByCompanyId(UUID companyId);

    /**
     * Counts jobs for a company by status.
     * 
     * @param companyId the company ID
     * @param status the job status
     * @return count of matching jobs
     */
    long countByCompanyIdAndStatusName(UUID companyId, Job.StatusName status);

    /**
     * Checks if a company has any active jobs.
     * 
     * @param companyId the company ID
     * @return true if company has active jobs
     */
    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END FROM Job j WHERE j.companyId = :companyId AND j.statusName = 'ACTIVE'")
    boolean hasActiveJobs(@Param("companyId") UUID companyId);

    /**
     * Finds jobs with deadlines that have passed and are still active.
     * 
     * @return list of jobs that should be auto-closed
     */
    @Query("SELECT j FROM Job j WHERE j.statusName = 'ACTIVE' AND j.deadline < CURRENT_DATE")
    List<Job> findExpiredActiveJobs();
}
