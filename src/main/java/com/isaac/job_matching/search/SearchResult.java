package com.isaac.job_matching.search;

import com.isaac.job_matching.shared.Location;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Result item from job search.
 * Contains essential job information for search results display.
 */
public record SearchResult(
        String id,
        String title,
        CompanySummary company,
        Location location,
        String remoteOption,
        String salaryRange,
        String experienceLevel,
        String employmentType,
        String status,
        Instant postedAt,
        LocalDate deadline) {
    public record CompanySummary(
            String id,
            String name,
            String logoUrl) {
    }
}