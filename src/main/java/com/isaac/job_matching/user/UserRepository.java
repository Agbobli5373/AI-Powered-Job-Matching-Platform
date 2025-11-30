package com.isaac.job_matching.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for User entity operations.
 * 
 * <p>
 * Provides data access for user management including:
 * <ul>
 * <li>Email-based lookups (case-insensitive)</li>
 * <li>Existence checks for registration validation</li>
 * <li>Role-based queries</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email address (case-insensitive).
     * 
     * @param email the email address to search
     * @return Optional containing the user if found
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Checks if a user exists with the given email (case-insensitive).
     * 
     * @param email the email address to check
     * @return true if a user with this email exists
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Finds all users with a specific role.
     * 
     * @param role the role to filter by
     * @return list of users with the specified role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role")
    java.util.List<User> findByRole(@Param("role") String role);

    /**
     * Finds all users with a specific status.
     * 
     * @param status the status to filter by
     * @return list of users with the specified status
     */
    java.util.List<User> findByStatus(UserStatus status);

    /**
     * Counts users by role.
     * 
     * @param role the role to count
     * @return count of users with the specified role
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") String role);

    /**
     * Counts users by status.
     * 
     * @param status the status to count
     * @return count of users with the specified status
     */
    long countByStatus(UserStatus status);

    /**
     * Finds all active users with a specific role.
     * 
     * @param role the role to filter by
     * @return list of active users with the specified role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = 'ACTIVE'")
    java.util.List<User> findActiveByRole(@Param("role") String role);
}
