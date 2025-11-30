package com.isaac.job_matching.profile;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entity representing an education entry in a profile.
 */
@Entity
@Table(name = "educations")
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "institution", nullable = false, length = 255)
    @NotBlank(message = "Institution is required")
    @Size(max = 255, message = "Institution name must not exceed 255 characters")
    private String institution;

    @Column(name = "degree", length = 100)
    @Size(max = 100, message = "Degree must not exceed 100 characters")
    private String degree;

    @Column(name = "field", length = 100)
    @Size(max = 100, message = "Field of study must not exceed 100 characters")
    private String field;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    protected Education() {
        // JPA constructor
    }

    public Education(String institution, String degree, String field, Integer graduationYear) {
        this.institution = institution;
        this.degree = degree;
        this.field = field;
        this.graduationYear = graduationYear;
    }

    public Education(String institution) {
        this(institution, null, null, null);
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public Profile getProfile() {
        return profile;
    }

    public String getInstitution() {
        return institution;
    }

    public String getDegree() {
        return degree;
    }

    public String getField() {
        return field;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    /**
     * Gets a formatted string representing this education entry.
     * 
     * @return formatted education string
     */
    public String getFormatted() {
        StringBuilder sb = new StringBuilder();
        if (degree != null) {
            sb.append(degree);
        }
        if (field != null) {
            if (!sb.isEmpty())
                sb.append(" in ");
            sb.append(field);
        }
        if (!sb.isEmpty())
            sb.append(" - ");
        sb.append(institution);
        if (graduationYear != null) {
            sb.append(" (").append(graduationYear).append(")");
        }
        return sb.toString();
    }

    // Setters

    void setProfile(Profile profile) {
        this.profile = profile;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Education that = (Education) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Education{id=" + id + ", institution='" + institution + "', degree='" + degree + "'}";
    }
}
