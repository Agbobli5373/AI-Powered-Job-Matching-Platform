package com.isaac.job_matching.shared;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Platform-wide skill definition with semantic embeddings.
 * 
 * <p>
 * Skills are shared across the platform and referenced by:
 * <ul>
 * <li>ProfileSkill - skills associated with job seeker profiles</li>
 * <li>JobSkill - skills required for job postings</li>
 * </ul>
 * 
 * <p>
 * The embedding vector enables semantic skill matching using pgvector,
 * allowing the system to find similar skills (e.g., "Java" similar to
 * "Kotlin").
 */
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    /**
     * Vector embedding for semantic search (1024 dimensions for mistral-embed).
     * Stored as float array in Java, VECTOR(1024) in PostgreSQL with pgvector.
     * 
     * <p>
     * Note: This field is mapped using a custom Hibernate type or native query
     * for pgvector compatibility.
     */
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private float[] embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Skill() {
    }

    public Skill(String name, String category) {
        this.name = name;
        this.category = category;
        this.createdAt = Instant.now();
    }

    public Skill(String name) {
        this(name, null);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public boolean hasEmbedding() {
        return embedding != null && embedding.length > 0;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Skill skill = (Skill) o;
        return id != null && id.equals(skill.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Skill{id=" + id + ", name='" + name + "', category='" + category + "'}";
    }
}
