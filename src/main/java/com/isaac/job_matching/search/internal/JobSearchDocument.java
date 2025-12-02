package com.isaac.job_matching.search.internal;

import org.springframework.data.elasticsearch.annotations.Document;

import java.math.BigDecimal;
import java.util.List;

/**
 * Elasticsearch document for job search indexing.
 * Contains searchable fields from Job entity.
 */
@Document(indexName = "jobs")
public record JobSearchDocument(
        String id,
        String title,
        String description,
        String companyId,
        String companyName,
        String location,
        List<String> skills,
        String remoteOption,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String experienceLevel,
        String employmentType,
        String status) {
}