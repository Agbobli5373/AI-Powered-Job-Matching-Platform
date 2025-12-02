package com.isaac.job_matching.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public API for job search functionality.
 * Provides methods to search and filter jobs using various criteria.
 */
public interface SearchService {

    /**
     * Search jobs based on the provided criteria.
     *
     * @param criteria the search criteria
     * @return page of search results
     */
    Page<SearchResult> searchJobs(JobSearchCriteria criteria);
}