package com.isaac.job_matching.shared;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Skill entity operations.
 * 
 * <p>
 * Provides standard CRUD operations plus semantic search capabilities
 * using pgvector for finding similar skills.
 */
@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {

    /**
     * Finds a skill by its exact name (case-insensitive).
     * 
     * @param name the skill name
     * @return optional skill if found
     */
    Optional<Skill> findByNameIgnoreCase(String name);

    /**
     * Finds all skills in a specific category.
     * 
     * @param category the category name
     * @return list of skills in the category
     */
    List<Skill> findByCategory(String category);

    /**
     * Finds all skills in a category (case-insensitive).
     * 
     * @param category the category name
     * @return list of skills in the category
     */
    List<Skill> findByCategoryIgnoreCase(String category);

    /**
     * Checks if a skill with the given name exists.
     * 
     * @param name the skill name
     * @return true if skill exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Finds skills whose names contain the given string (case-insensitive).
     * Useful for autocomplete functionality.
     * 
     * @param namePattern the partial name to match
     * @return list of matching skills
     */
    List<Skill> findByNameContainingIgnoreCase(String namePattern);

    /**
     * Returns all distinct skill categories.
     * 
     * @return list of category names
     */
    @Query("SELECT DISTINCT s.category FROM Skill s WHERE s.category IS NOT NULL ORDER BY s.category")
    List<String> findAllCategories();

    /**
     * Finds semantically similar skills using pgvector cosine similarity.
     * 
     * <p>
     * Uses the pgvector {@code <=>} operator for cosine distance,
     * returning skills ordered by similarity (most similar first).
     * 
     * @param embedding the query embedding vector
     * @param limit     maximum number of results
     * @return list of similar skills with similarity scores
     */
    @Query(value = """
            SELECT s.*, 1 - (s.embedding <=> cast(:embedding as vector)) as similarity
            FROM skills s
            WHERE s.embedding IS NOT NULL
            ORDER BY s.embedding <=> cast(:embedding as vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Skill> findSimilarByEmbedding(@Param("embedding") String embedding, @Param("limit") int limit);

    /**
     * Finds skills that don't have embeddings yet.
     * Useful for batch embedding generation.
     * 
     * @return list of skills without embeddings
     */
    @Query("SELECT s FROM Skill s WHERE s.embedding IS NULL")
    List<Skill> findSkillsWithoutEmbeddings();

    /**
     * Counts skills without embeddings.
     * 
     * @return count of skills needing embeddings
     */
    @Query("SELECT COUNT(s) FROM Skill s WHERE s.embedding IS NULL")
    long countSkillsWithoutEmbeddings();
}
