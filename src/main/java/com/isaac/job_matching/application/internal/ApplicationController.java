package com.isaac.job_matching.application.internal;

import com.isaac.job_matching.application.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for application management endpoints.
 */
@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Job application management endpoints")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/apply/{jobId}")
    @Operation(summary = "Apply to job", description = "Submit an application for a job posting")
    public ResponseEntity<ApplicationResponse> applyToJob(
            @PathVariable UUID jobId,
            @Valid @RequestBody ApplyRequest request) {
        ApplicationResponse response = applicationService.applyToJob(jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get my applications", description = "Get applications submitted by current user")
    public ResponseEntity<Page<ApplicationSummary>> getMyApplications(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ApplicationSummary> applications = applicationService.getMyApplications(status, pageable);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application details", description = "Get detailed view of a specific application")
    public ResponseEntity<ApplicationDetails> getApplication(@PathVariable UUID id) {
        ApplicationDetails application = applicationService.getApplication(id);
        return ResponseEntity.ok(application);
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw application", description = "Withdraw a pending or reviewed application")
    public ResponseEntity<Void> withdrawApplication(@PathVariable UUID id) {
        applicationService.withdrawApplication(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update application status", description = "Update application status (employer only)")
    public ResponseEntity<Void> updateApplicationStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest request) {
        applicationService.updateApplicationStatus(id, request.newStatus());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/job/{jobId}")
    @Operation(summary = "Get applications for job", description = "Get all applications for a specific job (employer only)")
    public ResponseEntity<Page<ApplicationSummary>> getJobApplications(
            @PathVariable UUID jobId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ApplicationSummary> applications = applicationService.getJobApplications(jobId, pageable);
        return ResponseEntity.ok(applications);
    }
}