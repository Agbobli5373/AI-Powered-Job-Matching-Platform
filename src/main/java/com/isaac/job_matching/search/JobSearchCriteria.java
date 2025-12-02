package com.isaac.job_matching.search;

import java.math.BigDecimal;
import java.util.List;

/**
 * Criteria for searching jobs.
 * Used by the search endpoint to filter and sort job results.
 */
public record JobSearchCriteria(
        String keyword,
        String location,
        List<String> skills,
        String remoteOption,
        String experienceLevel,
        String employmentType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String postedWithin,
        int page,
        int size,
        String sort) {
    public JobSearchCriteria {
        page = page < 0 ? 0 : page;
        size = size <= 0 || size > 100 ? 20 : size;
        sort = sort == null ? "relevance" : sort;
    }
}