package com.isaac.job_matching.company;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.isaac.job_matching.shared.BaseEntity;
import com.isaac.job_matching.shared.Location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Company entity representing an employer organization.
 * 
 * <p>
 * This is the aggregate root for the Company module. Companies are owned by
 * users with EMPLOYER role and can post job listings.
 * 
 * <p>
 * Company lifecycle:
 * <ol>
 * <li>Employer registers → company created (unverified)</li>
 * <li>Admin verifies → verified status (optional, enhances trust)</li>
 * </ol>
 * 
 * <p>
 * A verified company badge indicates the organization has been validated
 * by platform administrators.
 */
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "industry", length = 100)
    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @Column(name = "size", length = 50)
    @Enumerated(EnumType.STRING)
    private CompanySize size;

    @Column(name = "logo_url", length = 500)
    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;

    @Column(name = "website_url", length = 500)
    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    private String websiteUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "locations", columnDefinition = "jsonb")
    private List<Location> locations = new ArrayList<>();

    @Column(name = "verified", nullable = false)
    private boolean verified;

    protected Company() {
        // JPA constructor
    }

    /**
     * Creates a new company for the specified employer user.
     * 
     * @param userId the owner user ID (must be EMPLOYER role)
     * @param name   the company name
     */
    public Company(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
        this.verified = false;
        this.locations = new ArrayList<>();
    }

    // Getters

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIndustry() {
        return industry;
    }

    public CompanySize getSize() {
        return size;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public List<Location> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    public boolean isVerified() {
        return verified;
    }

    // Setters for mutable fields

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public void setSize(CompanySize size) {
        this.size = size;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    // Location management

    /**
     * Adds a location to the company.
     * 
     * @param location the location to add
     */
    public void addLocation(Location location) {
        if (locations == null) {
            locations = new ArrayList<>();
        }
        locations.add(location);
    }

    /**
     * Removes a location from the company.
     * 
     * @param location the location to remove
     */
    public void removeLocation(Location location) {
        if (locations != null) {
            locations.remove(location);
        }
    }

    /**
     * Sets all locations for the company, replacing existing ones.
     * 
     * @param locations the new list of locations
     */
    public void setLocations(List<Location> locations) {
        this.locations = locations != null ? new ArrayList<>(locations) : new ArrayList<>();
    }

    /**
     * Clears all locations from the company.
     */
    public void clearLocations() {
        if (locations != null) {
            locations.clear();
        }
    }

    /**
     * Gets the primary (first) location of the company.
     * 
     * @return the primary location, or null if no locations
     */
    public Location getPrimaryLocation() {
        return locations != null && !locations.isEmpty() ? locations.get(0) : null;
    }

    // Business methods

    /**
     * Verifies the company (admin operation).
     */
    public void verify() {
        this.verified = true;
    }

    /**
     * Revokes company verification (admin operation).
     */
    public void revokeVerification() {
        this.verified = false;
    }

    /**
     * Checks if the company profile is complete.
     * A profile is considered complete if it has name, description, and at least
     * one location.
     * 
     * @return true if the profile is complete
     */
    public boolean isProfileComplete() {
        return name != null && !name.isBlank() &&
                description != null && !description.isBlank() &&
                locations != null && !locations.isEmpty();
    }

    /**
     * Checks if the specified user owns this company.
     * 
     * @param userId the user ID to check
     * @return true if the user owns this company
     */
    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
    }

    @Override
    public String toString() {
        return "Company{id=" + getId() + ", name='" + name + "', industry='" + industry +
                "', verified=" + verified + "}";
    }
}
