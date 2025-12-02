package com.isaac.job_matching.search.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Elasticsearch repository for job search operations.
 * Provides full-text search and filtering capabilities.
 */
public interface ElasticsearchJobRepository extends ElasticsearchRepository<JobSearchDocument, String> {

    // Full-text search in title and description
    Page<JobSearchDocument> findByTitleContainingOrDescriptionContaining(
            String titleKeyword, String descriptionKeyword, Pageable pageable);

    // Search by skills
    Page<JobSearchDocument> findBySkillsIn(List<String> skills, Pageable pageable);

    // Search by status
    Page<JobSearchDocument> findByStatus(String status, Pageable pageable);
}