package com.isaac.job_matching.search.internal;

import com.isaac.job_matching.search.JobSearchCriteria;
import com.isaac.job_matching.search.SearchResult;
import com.isaac.job_matching.search.SearchService;
import com.isaac.job_matching.company.CompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of SearchService using Elasticsearch.
 * Handles job search queries and result mapping.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchJobRepository elasticsearchRepository;
    private final CompanyRepository companyRepository;

    public SearchServiceImpl(ElasticsearchJobRepository elasticsearchRepository,
            CompanyRepository companyRepository) {
        this.elasticsearchRepository = elasticsearchRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public Page<SearchResult> searchJobs(JobSearchCriteria criteria) {
        // Build pageable
        Pageable pageable = PageRequest.of(criteria.page(), criteria.size());

        // For simplicity, use basic search - in real impl, build complex query
        Page<JobSearchDocument> documents;
        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            documents = elasticsearchRepository.findByTitleContainingOrDescriptionContaining(
                    criteria.keyword(), criteria.keyword(), pageable);
        } else {
            // If no keyword, get all active jobs
            documents = elasticsearchRepository.findByStatus("Active", pageable);
        }

        // Map to SearchResult
        List<SearchResult> results = documents.getContent().stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());

        return new PageImpl<>(results, pageable, documents.getTotalElements());
    }

    private SearchResult mapToSearchResult(JobSearchDocument doc) {
        // For company, fetch by id
        UUID companyId = UUID.fromString(doc.companyId());
        var company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalStateException("Company not found: " + companyId));

        SearchResult.CompanySummary companySummary = new SearchResult.CompanySummary(
                company.getId().toString(),
                company.getName(),
                null // logoUrl not implemented yet
        );

        // Location from string - assume format "City, State"
        // For simplicity, create Location
        // This is placeholder
        com.isaac.job_matching.shared.Location location = new com.isaac.job_matching.shared.Location("City", "State",
                "Country", null, null);

        return new SearchResult(
                doc.id(),
                doc.title(),
                companySummary,
                location,
                doc.remoteOption(),
                formatSalaryRange(doc.salaryMin(), doc.salaryMax()),
                doc.experienceLevel(),
                doc.employmentType(),
                doc.status(),
                null, // postedAt not in doc
                null // deadline not in doc
        );
    }

    private String formatSalaryRange(BigDecimal min, BigDecimal max) {
        if (min == null && max == null)
            return null;
        if (min != null && max != null)
            return "$" + min + " - $" + max;
        if (min != null)
            return "$" + min + "+";
        return "Up to $" + max;
    }
}