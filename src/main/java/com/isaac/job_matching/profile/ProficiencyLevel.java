package com.isaac.job_matching.profile;

/**
 * Enumeration of skill proficiency levels.
 */
public enum ProficiencyLevel {

    /**
     * Basic understanding, limited practical experience.
     */
    BEGINNER("Beginner"),

    /**
     * Solid practical experience, can work independently.
     */
    INTERMEDIATE("Intermediate"),

    /**
     * Deep expertise, can mentor others.
     */
    EXPERT("Expert");

    private final String description;

    ProficiencyLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
