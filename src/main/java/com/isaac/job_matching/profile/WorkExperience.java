package com.isaac.job_matching.profile;

import java.time.LocalDate;
import java.util.UUID;

import com.isaac.job_matching.shared.DateRange;
import com.isaac.job_matching.shared.Location;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entity representing a work experience entry in a profile.
 */
@Entity
@Table(name = "work_experiences")
public class WorkExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "company", nullable = false, length = 255)
    @NotBlank(message = "Company is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String company;

    @Column(name = "role", nullable = false, length = 255)
    @NotBlank(message = "Role is required")
    @Size(max = 255, message = "Role must not exceed 255 characters")
    private String role;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "city", column = @Column(name = "location_city")),
            @AttributeOverride(name = "state", column = @Column(name = "location_state")),
            @AttributeOverride(name = "country", column = @Column(name = "location_country")),
            @AttributeOverride(name = "latitude", column = @Column(insertable = false, updatable = false)),
            @AttributeOverride(name = "longitude", column = @Column(insertable = false, updatable = false))
    })
    private Location location;

    protected WorkExperience() {
        // JPA constructor
    }

    public WorkExperience(String company, String role, LocalDate startDate, LocalDate endDate) {
        this.company = company;
        this.role = role;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public WorkExperience(String company, String role, LocalDate startDate) {
        this(company, role, startDate, null);
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public Profile getProfile() {
        return profile;
    }

    public String getCompany() {
        return company;
    }

    public String getRole() {
        return role;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    public Location getLocation() {
        return location;
    }

    /**
     * Gets the date range for this work experience.
     * 
     * @return DateRange representing the employment period
     */
    public DateRange getDateRange() {
        return new DateRange(startDate, endDate);
    }

    /**
     * Checks if this is a current position.
     * 
     * @return true if endDate is null
     */
    public boolean isCurrent() {
        return endDate == null;
    }

    // Setters

    void setProfile(Profile profile) {
        this.profile = profile;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WorkExperience that = (WorkExperience) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "WorkExperience{id=" + id + ", company='" + company + "', role='" + role + "'}";
    }
}
