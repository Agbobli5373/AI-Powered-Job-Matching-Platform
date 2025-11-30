package com.isaac.job_matching.profile;

/**
 * Enumeration of availability statuses for job seekers.
 */
public enum Availability {

    /**
     * Available to start immediately.
     */
    IMMEDIATE("Available immediately"),

    /**
     * Available in two weeks.
     */
    TWO_WEEKS("Available in 2 weeks"),

    /**
     * Available in one month.
     */
    ONE_MONTH("Available in 1 month"),

    /**
     * Available in three months.
     */
    THREE_MONTHS("Available in 3 months"),

    /**
     * Not currently looking for new opportunities.
     */
    NOT_LOOKING("Not currently looking");

    private final String description;

    Availability(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
