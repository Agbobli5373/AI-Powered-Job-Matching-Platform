package com.isaac.job_matching.search;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JobSearchCriteriaTest {

    @Test
    void shouldCreateWithDefaults() {
        JobSearchCriteria criteria = new JobSearchCriteria(null, null, null, null, null, null, null, null, null, -1, 0,
                null);

        assertThat(criteria.page()).isZero();
        assertThat(criteria.size()).isEqualTo(20);
        assertThat(criteria.sort()).isEqualTo("relevance");
    }

    @Test
    void shouldClampSize() {
        JobSearchCriteria criteria = new JobSearchCriteria(null, null, null, null, null, null, null, null, null, 0, 150,
                "postedAt");

        assertThat(criteria.size()).isEqualTo(20);
    }
}