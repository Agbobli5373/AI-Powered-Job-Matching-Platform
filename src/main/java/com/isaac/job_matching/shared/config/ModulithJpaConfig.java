package com.isaac.job_matching.shared.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Spring Modulith JPA event publication entity with Hibernate
 * so that event persistence works correctly.
 * 
 * The DefaultJpaEventPublication entity is in the 'updating' subpackage.
 * The ArchivedJpaEventPublication entity is in the 'archiving' subpackage.
 */
@Configuration
@EntityScan(basePackages = {
        "com.isaac.job_matching",
        "org.springframework.modulith.events.jpa.updating",
        "org.springframework.modulith.events.jpa.archiving"
})
public class ModulithJpaConfig {
}
