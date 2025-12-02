package com.isaac.job_matching.application.internal;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApplicationWorkflow.
 */
class ApplicationWorkflowTest {

    private final ApplicationWorkflow workflow = new ApplicationWorkflow();

    @Test
    void shouldAllowPendingToReviewedTransition() {
        assertThat(workflow.canTransition("PENDING", "REVIEWED")).isTrue();
    }

    @Test
    void shouldAllowPendingToRejectedTransition() {
        assertThat(workflow.canTransition("PENDING", "REJECTED")).isTrue();
    }

    @Test
    void shouldAllowPendingToWithdrawnTransition() {
        assertThat(workflow.canTransition("PENDING", "WITHDRAWN")).isTrue();
    }

    @Test
    void shouldAllowReviewedToAcceptedTransition() {
        assertThat(workflow.canTransition("REVIEWED", "ACCEPTED")).isTrue();
    }

    @Test
    void shouldAllowReviewedToRejectedTransition() {
        assertThat(workflow.canTransition("REVIEWED", "REJECTED")).isTrue();
    }

    @Test
    void shouldAllowReviewedToWithdrawnTransition() {
        assertThat(workflow.canTransition("REVIEWED", "WITHDRAWN")).isTrue();
    }

    @Test
    void shouldNotAllowAcceptedToAnyTransition() {
        assertThat(workflow.canTransition("ACCEPTED", "REVIEWED")).isFalse();
        assertThat(workflow.canTransition("ACCEPTED", "REJECTED")).isFalse();
        assertThat(workflow.canTransition("ACCEPTED", "WITHDRAWN")).isFalse();
    }

    @Test
    void shouldNotAllowRejectedToAnyTransition() {
        assertThat(workflow.canTransition("REJECTED", "ACCEPTED")).isFalse();
        assertThat(workflow.canTransition("REJECTED", "REVIEWED")).isFalse();
        assertThat(workflow.canTransition("REJECTED", "WITHDRAWN")).isFalse();
    }

    @Test
    void shouldNotAllowWithdrawnToAnyTransition() {
        assertThat(workflow.canTransition("WITHDRAWN", "ACCEPTED")).isFalse();
        assertThat(workflow.canTransition("WITHDRAWN", "REVIEWED")).isFalse();
        assertThat(workflow.canTransition("WITHDRAWN", "REJECTED")).isFalse();
    }

    @Test
    void shouldNotAllowInvalidTransitions() {
        assertThat(workflow.canTransition("PENDING", "ACCEPTED")).isFalse();
        assertThat(workflow.canTransition("REVIEWED", "PENDING")).isFalse();
        assertThat(workflow.canTransition("INVALID", "PENDING")).isFalse();
        assertThat(workflow.canTransition("PENDING", "INVALID")).isFalse();
    }

    @Test
    void shouldReturnCorrectAllowedTransitionsForPending() {
        assertThat(workflow.getAllowedTransitions("PENDING"))
                .containsExactlyInAnyOrder("REVIEWED", "REJECTED", "WITHDRAWN");
    }

    @Test
    void shouldReturnCorrectAllowedTransitionsForReviewed() {
        assertThat(workflow.getAllowedTransitions("REVIEWED"))
                .containsExactlyInAnyOrder("ACCEPTED", "REJECTED", "WITHDRAWN");
    }

    @Test
    void shouldReturnEmptyAllowedTransitionsForTerminalStates() {
        assertThat(workflow.getAllowedTransitions("ACCEPTED")).isEmpty();
        assertThat(workflow.getAllowedTransitions("REJECTED")).isEmpty();
        assertThat(workflow.getAllowedTransitions("WITHDRAWN")).isEmpty();
    }

    @Test
    void shouldReturnEmptyAllowedTransitionsForInvalidStatus() {
        assertThat(workflow.getAllowedTransitions("INVALID")).isEmpty();
    }

    @Test
    void shouldIdentifyTerminalStates() {
        assertThat(workflow.isTerminalStatus("ACCEPTED")).isTrue();
        assertThat(workflow.isTerminalStatus("REJECTED")).isTrue();
        assertThat(workflow.isTerminalStatus("WITHDRAWN")).isTrue();
    }

    @Test
    void shouldIdentifyNonTerminalStates() {
        assertThat(workflow.isTerminalStatus("PENDING")).isFalse();
        assertThat(workflow.isTerminalStatus("REVIEWED")).isFalse();
    }

    @Test
    void shouldIdentifyInvalidStatusAsTerminal() {
        assertThat(workflow.isTerminalStatus("INVALID")).isTrue();
    }
}