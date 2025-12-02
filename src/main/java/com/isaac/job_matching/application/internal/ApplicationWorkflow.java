package com.isaac.job_matching.application.internal;

import com.isaac.job_matching.application.ApplicationStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Workflow manager for application status transitions.
 * Ensures valid state transitions and business rules.
 */
@Component
public class ApplicationWorkflow {

    /**
     * Valid status transitions.
     * Key: current status, Value: allowed next statuses
     */
    private static final java.util.Map<String, Set<String>> VALID_TRANSITIONS = java.util.Map.of(
            "PENDING", Set.of("REVIEWED", "REJECTED", "WITHDRAWN"),
            "REVIEWED", Set.of("ACCEPTED", "REJECTED", "WITHDRAWN"),
            "ACCEPTED", Set.of(), // Terminal state
            "REJECTED", Set.of(), // Terminal state
            "WITHDRAWN", Set.of() // Terminal state
    );

    /**
     * Check if a status transition is valid.
     */
    public boolean canTransition(String currentStatus, String newStatus) {
        Set<String> allowedStatuses = VALID_TRANSITIONS.get(currentStatus);
        return allowedStatuses != null && allowedStatuses.contains(newStatus);
    }

    /**
     * Get allowed next statuses for current status.
     */
    public Set<String> getAllowedTransitions(String currentStatus) {
        return VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
    }

    /**
     * Check if status is terminal (no further transitions allowed).
     */
    public boolean isTerminalStatus(String status) {
        Set<String> allowedStatuses = VALID_TRANSITIONS.get(status);
        return allowedStatuses == null || allowedStatuses.isEmpty();
    }
}