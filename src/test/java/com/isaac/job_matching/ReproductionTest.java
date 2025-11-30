package com.isaac.job_matching;

import org.junit.jupiter.api.Test;

public class ReproductionTest {

    @Test
    void testRestClientAutoConfigurationExists() {
        try {
            Class.forName("org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration");
            System.out.println("RestClientAutoConfiguration found!");
        } catch (ClassNotFoundException e) {
            System.out.println("RestClientAutoConfiguration NOT found!");
        }
    }
}
