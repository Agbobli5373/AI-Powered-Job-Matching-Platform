package com.isaac.job_matching.profile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.isaac.job_matching.shared.BaseEntity;
import com.isaac.job_matching.shared.Location;
import com.isaac.job_matching.shared.Money;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

/**
 * Profile entity representing a job seeker's professional profile.
 * 
 * <p>
 * This is the aggregate root for the Profile module. A profile contains:
 * <ul>
 * <li>Personal information (name, bio)</li>
 * <li>Work experience history</li>
 * <li>Education history</li>
 * <li>Skills with proficiency levels</li>
 * <li>Job preferences (location, salary, remote work)</li>
 * </ul>
 */
@Entity
@Table(name = "profiles")
public class Profile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "first_name", length = 100)
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Column(name = "last_name", length = 100)
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "city", column = @Column(name = "location_city")),
            @AttributeOverride(name = "state", column = @Column(name = "location_state")),
            @AttributeOverride(name = "country", column = @Column(name = "location_country")),
            @AttributeOverride(name = "latitude", column = @Column(name = "location_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "location_longitude"))
    })
    private Location location;

    @Column(name = "salary_expectation_min", precision = 12, scale = 2)
    private BigDecimal salaryExpectationMin;

    @Column(name = "salary_expectation_max", precision = 12, scale = 2)
    private BigDecimal salaryExpectationMax;

    @Column(name = "salary_currency", length = 3)
    private String salaryCurrency = "USD";

    @Column(name = "availability", length = 20)
    @Enumerated(EnumType.STRING)
    private Availability availability;

    @Column(name = "remote_preference", length = 20)
    @Enumerated(EnumType.STRING)
    private RemotePreference remotePreference;

    @Column(name = "profile_complete", nullable = false)
    private boolean profileComplete = false;

    @Column(name = "searchable", nullable = false)
    private boolean searchable = true;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkExperience> workExperiences = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Education> educations = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProfileSkill> skills = new ArrayList<>();

    protected Profile() {
        // JPA constructor
    }

    /**
     * Creates a new profile for a user.
     * 
     * @param userId the user ID this profile belongs to
     */
    public Profile(UUID userId) {
        this.userId = userId;
    }

    // Getters

    public UUID getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (firstName != null) {
            sb.append(firstName);
        }
        if (lastName != null) {
            if (!sb.isEmpty())
                sb.append(" ");
            sb.append(lastName);
        }
        return sb.toString();
    }

    public String getBio() {
        return bio;
    }

    public Location getLocation() {
        return location;
    }

    public Money getSalaryExpectationMin() {
        if (salaryExpectationMin == null) {
            return null;
        }
        return new Money(salaryExpectationMin, salaryCurrency);
    }

    public Money getSalaryExpectationMax() {
        if (salaryExpectationMax == null) {
            return null;
        }
        return new Money(salaryExpectationMax, salaryCurrency);
    }

    public Availability getAvailability() {
        return availability;
    }

    public RemotePreference getRemotePreference() {
        return remotePreference;
    }

    public boolean isProfileComplete() {
        return profileComplete;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public List<WorkExperience> getWorkExperiences() {
        return new ArrayList<>(workExperiences);
    }

    public List<Education> getEducations() {
        return new ArrayList<>(educations);
    }

    public List<ProfileSkill> getSkills() {
        return new ArrayList<>(skills);
    }

    // Setters and business methods

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateProfileCompleteness();
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateProfileCompleteness();
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setSalaryExpectation(Money min, Money max) {
        if (min != null) {
            this.salaryExpectationMin = min.amount();
            this.salaryCurrency = min.currency();
        } else {
            this.salaryExpectationMin = null;
        }

        if (max != null) {
            this.salaryExpectationMax = max.amount();
            if (min == null) {
                this.salaryCurrency = max.currency();
            }
        } else {
            this.salaryExpectationMax = null;
        }
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    public void setRemotePreference(RemotePreference remotePreference) {
        this.remotePreference = remotePreference;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    // Work Experience management

    public WorkExperience addWorkExperience(WorkExperience experience) {
        experience.setProfile(this);
        workExperiences.add(experience);
        updateProfileCompleteness();
        return experience;
    }

    public void removeWorkExperience(WorkExperience experience) {
        workExperiences.remove(experience);
        experience.setProfile(null);
        updateProfileCompleteness();
    }

    // Education management

    public Education addEducation(Education education) {
        education.setProfile(this);
        educations.add(education);
        return education;
    }

    public void removeEducation(Education education) {
        educations.remove(education);
        education.setProfile(null);
    }

    // Skills management

    public ProfileSkill addSkill(ProfileSkill skill) {
        skill.setProfile(this);
        skills.add(skill);
        updateProfileCompleteness();
        return skill;
    }

    public void removeSkill(ProfileSkill skill) {
        skills.remove(skill);
        skill.setProfile(null);
        updateProfileCompleteness();
    }

    public void clearSkills() {
        skills.forEach(s -> s.setProfile(null));
        skills.clear();
        updateProfileCompleteness();
    }

    /**
     * Calculates total years of work experience.
     * 
     * @return total years of experience
     */
    public int getTotalExperienceYears() {
        return workExperiences.stream()
                .mapToInt(we -> we.getDateRange().durationInYears())
                .sum();
    }

    /**
     * Gets a headline for the profile based on most recent experience.
     * 
     * @return headline string or null if no work experience
     */
    public String getHeadline() {
        return workExperiences.stream()
                .filter(we -> we.getDateRange().isCurrent())
                .findFirst()
                .map(we -> we.getRole() + " at " + we.getCompany())
                .orElseGet(() -> workExperiences.stream()
                        .findFirst()
                        .map(we -> we.getRole())
                        .orElse(null));
    }

    /**
     * Updates the profile completeness flag based on current data.
     * A profile is considered complete when it has:
     * - First name
     * - Last name
     * - At least one skill
     */
    private void updateProfileCompleteness() {
        this.profileComplete = firstName != null && !firstName.isBlank()
                && lastName != null && !lastName.isBlank()
                && !skills.isEmpty();
    }

    @Override
    public String toString() {
        return "Profile{id=" + getId() + ", userId=" + userId + ", name='" + getFullName() + "'}";
    }
}
