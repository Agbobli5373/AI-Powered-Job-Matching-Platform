package com.isaac.job_matching.company.internal;

import org.springframework.stereotype.Service;

import com.isaac.job_matching.company.Company;

/**
 * Internal service for company verification logic.
 * 
 * <p>
 * This service handles the business rules for verifying companies,
 * including validation criteria and automated verification checks.
 * 
 * <p>
 * Verification may include:
 * <ul>
 * <li>Business registration validation</li>
 * <li>Website domain verification</li>
 * <li>Email domain verification</li>
 * <li>Manual admin review</li>
 * </ul>
 */
@Service
public class CompanyVerificationService {

    /**
     * Checks if a company meets the criteria for verification.
     * 
     * <p>
     * Currently checks:
     * <ul>
     * <li>Company has a name</li>
     * <li>Company has a description</li>
     * <li>Company has at least one location</li>
     * <li>Company has a website URL</li>
     * </ul>
     * 
     * @param company the company to check
     * @return true if the company can be verified
     */
    public boolean canBeVerified(Company company) {
        if (company == null) {
            return false;
        }

        // Must have complete profile
        if (!company.isProfileComplete()) {
            return false;
        }

        // Must have a website URL
        if (company.getWebsiteUrl() == null || company.getWebsiteUrl().isBlank()) {
            return false;
        }

        return true;
    }

    /**
     * Validates the company's website URL format.
     * 
     * @param websiteUrl the website URL to validate
     * @return true if the URL is valid
     */
    public boolean isValidWebsiteUrl(String websiteUrl) {
        if (websiteUrl == null || websiteUrl.isBlank()) {
            return false;
        }

        // Basic URL validation
        String lowerUrl = websiteUrl.toLowerCase();
        return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://");
    }

    /**
     * Generates a verification checklist for a company.
     * 
     * @param company the company to check
     * @return a checklist of verification requirements
     */
    public VerificationChecklist getVerificationChecklist(Company company) {
        return new VerificationChecklist(
                company.getName() != null && !company.getName().isBlank(),
                company.getDescription() != null && !company.getDescription().isBlank(),
                company.getLocations() != null && !company.getLocations().isEmpty(),
                company.getIndustry() != null && !company.getIndustry().isBlank(),
                isValidWebsiteUrl(company.getWebsiteUrl()),
                company.getLogoUrl() != null && !company.getLogoUrl().isBlank());
    }

    /**
     * Record containing verification checklist items.
     */
    public record VerificationChecklist(
            boolean hasName,
            boolean hasDescription,
            boolean hasLocation,
            boolean hasIndustry,
            boolean hasValidWebsite,
            boolean hasLogo) {

        /**
         * Returns the number of completed checklist items.
         */
        public int completedCount() {
            int count = 0;
            if (hasName)
                count++;
            if (hasDescription)
                count++;
            if (hasLocation)
                count++;
            if (hasIndustry)
                count++;
            if (hasValidWebsite)
                count++;
            if (hasLogo)
                count++;
            return count;
        }

        /**
         * Returns the total number of checklist items.
         */
        public int totalCount() {
            return 6;
        }

        /**
         * Returns the completion percentage.
         */
        public int completionPercentage() {
            return (completedCount() * 100) / totalCount();
        }

        /**
         * Returns whether all required items are complete.
         * Required: name, description, location, website
         */
        public boolean meetsMinimumRequirements() {
            return hasName && hasDescription && hasLocation && hasValidWebsite;
        }
    }
}
