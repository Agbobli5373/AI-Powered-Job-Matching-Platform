package com.isaac.job_matching.profile;

/**
 * Enumeration of remote work preferences.
 */
public enum RemotePreference {

    /**
     * Only interested in fully remote positions.
     */
    REMOTE_ONLY("Remote only"),

    /**
     * Open to hybrid arrangements.
     */
    HYBRID("Hybrid (remote + office)"),

    /**
     * Prefers on-site work.
     */
    ONSITE_ONLY("On-site only"),

    /**
     * Flexible on remote/on-site arrangements.
     */
    FLEXIBLE("Flexible");

    private final String description;

    RemotePreference(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
