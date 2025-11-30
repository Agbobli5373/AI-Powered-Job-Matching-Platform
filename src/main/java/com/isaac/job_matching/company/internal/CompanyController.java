package com.isaac.job_matching.company.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isaac.job_matching.company.Company;
import com.isaac.job_matching.company.CompanyService;
import com.isaac.job_matching.company.CompanySize;
import com.isaac.job_matching.shared.Location;
import com.isaac.job_matching.shared.exception.ForbiddenException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * REST controller for company management endpoints.
 * 
 * <p>
 * Endpoints:
 * <ul>
 * <li>GET /api/companies/me - Get current user's company</li>
 * <li>PUT /api/companies/me - Update current user's company</li>
 * <li>GET /api/companies/{id} - Get company by ID</li>
 * <li>GET /api/companies - Search companies</li>
 * <li>GET /api/companies/industries - Get distinct industries</li>
 * <li>POST /api/admin/companies/{id}/verify - Verify company (admin)</li>
 * <li>DELETE /api/admin/companies/{id}/verify - Revoke verification
 * (admin)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Companies", description = "Company profile management")
@SecurityRequirement(name = "bearerAuth")
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyVerificationService verificationService;

    public CompanyController(
            CompanyService companyService,
            CompanyVerificationService verificationService) {
        this.companyService = companyService;
        this.verificationService = verificationService;
    }

    // ================ Employer Endpoints ================

    @GetMapping("/companies/me")
    @Operation(summary = "Get current user's company", description = "Returns the company profile of the authenticated employer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<CompanyResponse> getMyCompany(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Company company = companyService.getCompanyByUserId(userId);
        return ResponseEntity.ok(toResponse(company));
    }

    @PutMapping("/companies/me")
    @Operation(summary = "Update current user's company", description = "Updates the company profile of the authenticated employer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<CompanyResponse> updateMyCompany(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCompanyRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Company company = companyService.getCompanyByUserId(userId);

        // Update fields from request
        updateCompanyFromRequest(company, request);
        Company savedCompany = companyService.saveCompany(company);

        return ResponseEntity.ok(toResponse(savedCompany));
    }

    @PostMapping("/companies")
    @Operation(summary = "Create company", description = "Creates a new company for the authenticated employer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Company created"),
            @ApiResponse(responseCode = "400", description = "Invalid input or company already exists"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<CompanyResponse> createCompany(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCompanyRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Company company = new Company(userId, request.name());
        company.setDescription(request.description());
        company.setIndustry(request.industry());
        if (request.size() != null) {
            company.setSize(CompanySize.valueOf(request.size()));
        }
        company.setLogoUrl(request.logoUrl());
        company.setWebsiteUrl(request.websiteUrl());

        if (request.locations() != null) {
            for (LocationRequest loc : request.locations()) {
                company.addLocation(toLocation(loc));
            }
        }

        Company savedCompany = companyService.createCompany(userId, company);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(savedCompany));
    }

    // ================ Public Endpoints ================

    @GetMapping("/companies/{id}")
    @Operation(summary = "Get company by ID", description = "Returns a company's public profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company found"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<CompanyResponse> getCompanyById(@PathVariable UUID id) {
        Company company = companyService.getCompanyById(id);
        return ResponseEntity.ok(toResponse(company));
    }

    @GetMapping("/companies")
    @Operation(summary = "Search companies", description = "Search companies by name, industry, or other criteria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Companies retrieved")
    })
    public ResponseEntity<List<CompanyResponse>> searchCompanies(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Boolean verified) {

        List<Company> companies;

        if (name != null && !name.isBlank()) {
            companies = companyService.searchByName(name);
        } else if (industry != null && !industry.isBlank()) {
            companies = companyService.findByIndustry(industry);
        } else if (size != null && !size.isBlank()) {
            companies = companyService.findBySize(CompanySize.valueOf(size));
        } else if (Boolean.TRUE.equals(verified)) {
            companies = companyService.findVerified();
        } else {
            // Return empty list if no criteria specified (prevent full table scan)
            companies = List.of();
        }

        List<CompanyResponse> responses = companies.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/companies/industries")
    @Operation(summary = "Get industries", description = "Returns list of distinct industries with companies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Industries retrieved")
    })
    public ResponseEntity<List<String>> getIndustries() {
        return ResponseEntity.ok(companyService.getDistinctIndustries());
    }

    // ================ Verification Status ================

    @GetMapping("/companies/me/verification-status")
    @Operation(summary = "Get verification checklist", description = "Returns verification requirements and status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checklist retrieved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<VerificationStatusResponse> getVerificationStatus(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Company company = companyService.getCompanyByUserId(userId);

        var checklist = verificationService.getVerificationChecklist(company);

        return ResponseEntity.ok(new VerificationStatusResponse(
                company.isVerified(),
                verificationService.canBeVerified(company),
                checklist.completionPercentage(),
                checklist.hasName(),
                checklist.hasDescription(),
                checklist.hasLocation(),
                checklist.hasIndustry(),
                checklist.hasValidWebsite(),
                checklist.hasLogo()));
    }

    // ================ Admin Endpoints ================

    @PostMapping("/admin/companies/{id}/verify")
    @Operation(summary = "Verify company", description = "Marks a company as verified (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company verified"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<CompanyResponse> verifyCompany(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        // Check admin role
        String role = jwt.getClaimAsString("role");
        if (!"ADMIN".equals(role)) {
            throw new ForbiddenException("Only admins can verify companies");
        }

        Company company = companyService.verifyCompany(id);
        return ResponseEntity.ok(toResponse(company));
    }

    @DeleteMapping("/admin/companies/{id}/verify")
    @Operation(summary = "Revoke verification", description = "Revokes company verification (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification revoked"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<CompanyResponse> revokeVerification(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        // Check admin role
        String role = jwt.getClaimAsString("role");
        if (!"ADMIN".equals(role)) {
            throw new ForbiddenException("Only admins can revoke company verification");
        }

        Company company = companyService.revokeVerification(id);
        return ResponseEntity.ok(toResponse(company));
    }

    // ================ Helper Methods ================

    private CompanyResponse toResponse(Company company) {
        List<LocationResponse> locationResponses = company.getLocations().stream()
                .map(loc -> new LocationResponse(
                        loc.city(),
                        loc.state(),
                        loc.country(),
                        loc.latitude(),
                        loc.longitude()))
                .toList();

        return new CompanyResponse(
                company.getId(),
                company.getUserId(),
                company.getName(),
                company.getDescription(),
                company.getIndustry(),
                company.getSize() != null ? company.getSize().name() : null,
                company.getSize() != null ? company.getSize().getDescription() : null,
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                locationResponses,
                company.isVerified(),
                company.isProfileComplete(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }

    private void updateCompanyFromRequest(Company company, UpdateCompanyRequest request) {
        if (request.name() != null) {
            company.setName(request.name());
        }
        if (request.description() != null) {
            company.setDescription(request.description());
        }
        if (request.industry() != null) {
            company.setIndustry(request.industry());
        }
        if (request.size() != null) {
            company.setSize(CompanySize.valueOf(request.size()));
        }
        if (request.logoUrl() != null) {
            company.setLogoUrl(request.logoUrl());
        }
        if (request.websiteUrl() != null) {
            company.setWebsiteUrl(request.websiteUrl());
        }
        if (request.locations() != null) {
            company.clearLocations();
            for (LocationRequest loc : request.locations()) {
                company.addLocation(toLocation(loc));
            }
        }
    }

    private Location toLocation(LocationRequest request) {
        if (request.latitude() != null && request.longitude() != null) {
            return Location.withCoordinates(
                    request.city(),
                    request.state(),
                    request.country(),
                    request.latitude(),
                    request.longitude());
        }
        return Location.of(request.city(), request.state(), request.country());
    }

    // ================ Request/Response DTOs ================

    public record CreateCompanyRequest(
            @NotBlank(message = "Company name is required") @Size(max = 255) String name,
            String description,
            @Size(max = 100) String industry,
            String size,
            @Size(max = 500) String logoUrl,
            @Size(max = 500) String websiteUrl,
            List<LocationRequest> locations) {
    }

    public record UpdateCompanyRequest(
            @Size(max = 255) String name,
            String description,
            @Size(max = 100) String industry,
            String size,
            @Size(max = 500) String logoUrl,
            @Size(max = 500) String websiteUrl,
            List<LocationRequest> locations) {
    }

    public record LocationRequest(
            String city,
            String state,
            String country,
            Double latitude,
            Double longitude) {
    }

    public record CompanyResponse(
            UUID id,
            UUID userId,
            String name,
            String description,
            String industry,
            String size,
            String sizeDescription,
            String logoUrl,
            String websiteUrl,
            List<LocationResponse> locations,
            boolean verified,
            boolean profileComplete,
            java.time.Instant createdAt,
            java.time.Instant updatedAt) {
    }

    public record LocationResponse(
            String city,
            String state,
            String country,
            Double latitude,
            Double longitude) {
    }

    public record VerificationStatusResponse(
            boolean verified,
            boolean canBeVerified,
            int completionPercentage,
            boolean hasName,
            boolean hasDescription,
            boolean hasLocation,
            boolean hasIndustry,
            boolean hasValidWebsite,
            boolean hasLogo) {
    }
}
