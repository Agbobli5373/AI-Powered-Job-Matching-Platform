package com.isaac.job_matching.profile.internal;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.isaac.job_matching.profile.Profile;

/**
 * Repository for Profile entity operations.
 * 
 * <p>
 * Provides data access for profile management including:
 * <ul>
 * <li>User-based lookups</li>
 * <li>Searchable profile queries</li>
 * <li>Aggregation queries</li>
 * </ul>
 */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

        /**
         * Finds a profile by user ID.
         * 
         * @param userId the user ID
         * @return Optional containing the profile if found
         */
        Optional<Profile> findByUserId(UUID userId);

        /**
         * Checks if a profile exists for the given user.
         * 
         * @param userId the user ID
         * @return true if a profile exists
         */
        boolean existsByUserId(UUID userId);

        /**
         * Finds a profile by user ID with work experiences eagerly loaded.
         * 
         * @param userId the user ID
         * @return Optional containing the profile if found
         */
        @Query("SELECT p FROM Profile p LEFT JOIN FETCH p.workExperiences WHERE p.userId = :userId")
        Optional<Profile> findByUserIdWithWorkExperiences(@Param("userId") UUID userId);

        /**
         * Finds a profile by user ID with all associations eagerly loaded.
         * 
         * @param userId the user ID
         * @return Optional containing the profile if found
         */
        @Query("SELECT DISTINCT p FROM Profile p " +
                        "LEFT JOIN FETCH p.workExperiences " +
                        "LEFT JOIN FETCH p.educations " +
                        "LEFT JOIN FETCH p.skills s " +
                        "LEFT JOIN FETCH s.skill " +
                        "WHERE p.userId = :userId")
        Optional<Profile> findByUserIdWithAllAssociations(@Param("userId") UUID userId);

        /**
         * Finds a profile by ID with all associations eagerly loaded.
         * 
         * @param id the profile ID
         * @return Optional containing the profile if found
         */
        @Query("SELECT DISTINCT p FROM Profile p " +
                        "LEFT JOIN FETCH p.workExperiences " +
                        "LEFT JOIN FETCH p.educations " +
                        "LEFT JOIN FETCH p.skills s " +
                        "LEFT JOIN FETCH s.skill " +
                        "WHERE p.id = :id")
        Optional<Profile> findByIdWithAllAssociations(@Param("id") UUID id);

        /**
         * Counts searchable profiles.
         * 
         * @return count of searchable profiles
         */
        @Query("SELECT COUNT(p) FROM Profile p WHERE p.searchable = true")
        long countSearchable();

        /**
         * Counts complete profiles.
         * 
         * @return count of complete profiles
         */
        @Query("SELECT COUNT(p) FROM Profile p WHERE p.profileComplete = true")
        long countComplete();
}
