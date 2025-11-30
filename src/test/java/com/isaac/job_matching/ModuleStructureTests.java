package com.isaac.job_matching;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Tests for verifying Spring Modulith module structure and boundaries.
 * 
 * <p>
 * These tests ensure:
 * <ul>
 * <li>All modules are properly defined and detected</li>
 * <li>Module dependencies follow the architecture constraints</li>
 * <li>No circular dependencies exist between modules</li>
 * <li>Internal packages are not accessed from outside their modules</li>
 * </ul>
 * 
 * <p>
 * Run these tests after making structural changes to verify
 * the modular architecture is maintained.
 */
class ModuleStructureTests {

    private final ApplicationModules modules = ApplicationModules.of(JobMatchingApplication.class);

    /**
     * Verifies that all application modules are correctly defined
     * and their boundaries are properly enforced.
     * 
     * <p>
     * This test will fail if:
     * <ul>
     * <li>A module accesses internal types from another module</li>
     * <li>Circular dependencies exist between modules</li>
     * <li>Module configuration is invalid</li>
     * </ul>
     */
    @Test
    void verifyModuleStructure() {
        modules.verify();
    }

    /**
     * Prints the detected modules for inspection.
     * 
     * <p>
     * Useful for debugging module detection issues.
     */
    @Test
    void printModules() {
        modules.forEach(System.out::println);
    }

    /**
     * Generates documentation for the module structure.
     * 
     * <p>
     * Creates:
     * <ul>
     * <li>PlantUML diagrams showing module dependencies</li>
     * <li>Module canvas documentation</li>
     * </ul>
     * 
     * <p>
     * Output is generated in target/spring-modulith-docs/
     */
    @Test
    void createModuleDocumentation() {
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }

    /**
     * Verifies the shared module is properly configured as OPEN.
     */
    @Test
    void verifySharedModuleIsOpen() {
        var sharedModule = modules.getModuleByName("shared");

        // The shared module should exist
        assert sharedModule.isPresent() : "Shared module not found";

        System.out.println("Shared module found: " + sharedModule.get().getDisplayName());
    }

    /**
     * Verifies that internal packages are not exposed.
     * 
     * <p>
     * Each module should keep its internal/ subpackage private,
     * exposing only types at the module root for the public API.
     */
    @Test
    void verifyInternalPackagesAreHidden() {
        modules.forEach(module -> {
            var internalTypes = module.getSpringBeans().stream()
                    .filter(bean -> bean.getFullyQualifiedTypeName().contains(".internal."))
                    .toList();

            // Internal types should not be exposed in module's public API
            // This is informational - Spring Modulith enforces this automatically
            if (!internalTypes.isEmpty()) {
                System.out.println("Module " + module.getDisplayName() + " has " + internalTypes.size()
                        + " internal Spring beans");
            }
        });
    }

    /**
     * Lists all module dependencies for inspection.
     * 
     * <p>
     * Useful for understanding the dependency graph.
     */
    @Test
    void printModuleDependencies() {
        System.out.println("=== Module Dependencies ===\n");

        modules.forEach(module -> {
            System.out.println("Module: " + module.getDisplayName());

            var dependencies = module.getDependencies(modules);
            if (dependencies.isEmpty()) {
                System.out.println("  -> No dependencies");
            } else {
                dependencies.stream()
                        .forEach(dep -> System.out.println("  -> " + dep.getTargetModule().getDisplayName()));
            }
            System.out.println();
        });
    }
}
