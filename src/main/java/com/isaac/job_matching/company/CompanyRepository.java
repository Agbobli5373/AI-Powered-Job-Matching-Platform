package com.isaac.job_matching.company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Company entity persistence operations.
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    /**
     * Finds a company by owner user ID.
     * 
     * @param userId the owner user ID
     * @return Optional containing the company if found
     */
    Optional<Company> findByUserId(UUID userId);

    /**
     * Checks if a company exists for the given user.
     * 
     * @param userId the user ID
     * @return true if a company exists for the user
     */
    boolean existsByUserId(UUID userId);

    /**
     * Checks if a company with the given name already exists.
     * 
     * @param name the company name
     * @return true if a company with this name exists
     */
    boolean existsByName(String name);

    /**
     * Finds companies by industry.
     * 
     * @param industry the industry to filter by
     * @return list of companies in the specified industry
     */
    List<Company> findByIndustry(String industry);

    /**
     * Finds verified companies.
     * 
     * @return list of verified companies
     */
    List<Company> findByVerifiedTrue();

    /**
     * Finds companies by size.
     * 
     * @param size the company size
     * @return list of companies of the specified size
     */
    List<Company> findBySize(CompanySize size);

    /**
     * Searches companies by name (case-insensitive partial match).
     * 
     * @param name the search term
     * @return list of matching companies
     */
    @Query("SELECT c FROM Company c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Company> searchByName(@Param("name") String name);

    /**
     * Counts verified companies.
     * 
     * @return count of verified companies
     */
    @Query("SELECT COUNT(c) FROM Company c WHERE c.verified = true")
    long countVerified();

    /**
     * Counts companies by industry.
     * 
     * @param industry the industry
     * @return count of companies in the industry
     */
    long countByIndustry(String industry);

    /**
     * Gets distinct industries with at least one company.
     * 
     * @return list of distinct industry names
     */
    @Query("SELECT DISTINCT c.industry FROM Company c WHERE c.industry IS NOT NULL ORDER BY c.industry")
    List<String> findDistinctIndustries();
}
