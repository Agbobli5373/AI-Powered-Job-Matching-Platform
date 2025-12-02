package com.isaac.job_matching.search.internal;

import com.isaac.job_matching.search.JobSearchCriteria;
import com.isaac.job_matching.search.SearchResult;
import com.isaac.job_matching.search.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for job search operations.
 * Provides endpoints for searching and filtering jobs.
 */
@RestController
@RequestMapping("/api/jobs")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<Page<SearchResult>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) String remoteOption,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) String postedWithin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sort) {

        JobSearchCriteria criteria = new JobSearchCriteria(
                keyword, location, skills, remoteOption, experienceLevel,
                employmentType, salaryMin, salaryMax, postedWithin, page, size, sort);

        Page<SearchResult> results = searchService.searchJobs(criteria);
        return ResponseEntity.ok(results);
    }
}