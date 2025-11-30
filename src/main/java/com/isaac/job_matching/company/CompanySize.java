package com.isaac.job_matching.company;

/**
 * Company size categories based on employee count.
 */
public enum CompanySize {
    STARTUP_1_10("1-10 employees"),
    SMALL_11_50("11-50 employees"),
    MEDIUM_51_200("51-200 employees"),
    LARGE_201_1000("201-1000 employees"),
    ENTERPRISE_1001_PLUS("1001+ employees");

    private final String description;

    CompanySize(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Estimates average company size for matching purposes.
     * 
     * @return estimated average employee count
     */
    public int estimatedSize() {
        return switch (this) {
            case STARTUP_1_10 -> 5;
            case SMALL_11_50 -> 30;
            case MEDIUM_51_200 -> 125;
            case LARGE_201_1000 -> 500;
            case ENTERPRISE_1001_PLUS -> 5000;
        };
    }
}
