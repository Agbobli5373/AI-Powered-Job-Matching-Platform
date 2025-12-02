package com.isaac.job_matching.application;

/**
 * Sealed interface for job application statuses.
 * Defines the possible states an application can be in during its lifecycle.
 */
public sealed interface ApplicationStatus permits
        ApplicationStatus.Pending,
        ApplicationStatus.Reviewed,
        ApplicationStatus.Accepted,
        ApplicationStatus.Rejected,
        ApplicationStatus.Withdrawn {

    /**
     * Application has been submitted but not yet reviewed.
     */
    record Pending() implements ApplicationStatus {
    }

    /**
     * Application has been reviewed by the employer.
     */
    record Reviewed() implements ApplicationStatus {
    }

    /**
     * Application has been accepted by the employer.
     */
    record Accepted() implements ApplicationStatus {
    }

    /**
     * Application has been rejected by the employer.
     */
    record Rejected() implements ApplicationStatus {
    }

    /**
     * Application has been withdrawn by the job seeker.
     */
    record Withdrawn() implements ApplicationStatus {
    }
}