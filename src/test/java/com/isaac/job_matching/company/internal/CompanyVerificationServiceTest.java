package com.isaac.job_matching.company.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.isaac.job_matching.company.Company;
import com.isaac.job_matching.shared.Location;

/**
 * Unit tests for CompanyVerificationService.
 */
class CompanyVerificationServiceTest {

    private CompanyVerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new CompanyVerificationService();
    }

    @Nested
    @DisplayName("canBeVerified Tests")
    class CanBeVerifiedTests {

        @Test
        @DisplayName("Should return true for complete company profile with website")
        void shouldReturnTrueForCompleteProfile() {
            // Given
            Company company = createCompleteCompany();

            // When
            boolean result = verificationService.canBeVerified(company);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for null company")
        void shouldReturnFalseForNullCompany() {
            // When
            boolean result = verificationService.canBeVerified(null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for company without name")
        void shouldReturnFalseForCompanyWithoutName() {
            // Given
            Company company = new Company(UUID.randomUUID(), "");
            company.setDescription("Description");
            company.addLocation(Location.of("City", "State", "Country"));
            company.setWebsiteUrl("https://example.com");

            // When
            boolean result = verificationService.canBeVerified(company);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for company without description")
        void shouldReturnFalseForCompanyWithoutDescription() {
            // Given
            Company company = new Company(UUID.randomUUID(), "Test Corp");
            company.addLocation(Location.of("City", "State", "Country"));
            company.setWebsiteUrl("https://example.com");
            // No description set

            // When
            boolean result = verificationService.canBeVerified(company);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for company without location")
        void shouldReturnFalseForCompanyWithoutLocation() {
            // Given
            Company company = new Company(UUID.randomUUID(), "Test Corp");
            company.setDescription("A great company");
            company.setWebsiteUrl("https://example.com");
            // No location added

            // When
            boolean result = verificationService.canBeVerified(company);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for company without website")
        void shouldReturnFalseForCompanyWithoutWebsite() {
            // Given
            Company company = new Company(UUID.randomUUID(), "Test Corp");
            company.setDescription("A great company");
            company.addLocation(Location.of("City", "State", "Country"));
            // No website set

            // When
            boolean result = verificationService.canBeVerified(company);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidWebsiteUrl Tests")
    class WebsiteUrlValidationTests {

        @Test
        @DisplayName("Should return true for https URL")
        void shouldReturnTrueForHttpsUrl() {
            // When
            boolean result = verificationService.isValidWebsiteUrl("https://example.com");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for http URL")
        void shouldReturnTrueForHttpUrl() {
            // When
            boolean result = verificationService.isValidWebsiteUrl("http://example.com");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for uppercase protocol")
        void shouldReturnTrueForUppercaseProtocol() {
            // When
            boolean result = verificationService.isValidWebsiteUrl("HTTPS://EXAMPLE.COM");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for null URL")
        void shouldReturnFalseForNullUrl() {
            // When
            boolean result = verificationService.isValidWebsiteUrl(null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for blank URL")
        void shouldReturnFalseForBlankUrl() {
            // When
            boolean result = verificationService.isValidWebsiteUrl("   ");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for URL without protocol")
        void shouldReturnFalseForUrlWithoutProtocol() {
            // When
            boolean result = verificationService.isValidWebsiteUrl("example.com");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for invalid protocol")
        void shouldReturnFalseForInvalidProtocol() {
            // When
            boolean result = verificationService.isValidWebsiteUrl("ftp://example.com");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getVerificationChecklist Tests")
    class VerificationChecklistTests {

        @Test
        @DisplayName("Should return all true for complete company")
        void shouldReturnAllTrueForCompleteCompany() {
            // Given
            Company company = createCompleteCompany();
            company.setIndustry("Technology");
            company.setLogoUrl("https://example.com/logo.png");

            // When
            var checklist = verificationService.getVerificationChecklist(company);

            // Then
            assertThat(checklist.hasName()).isTrue();
            assertThat(checklist.hasDescription()).isTrue();
            assertThat(checklist.hasLocation()).isTrue();
            assertThat(checklist.hasIndustry()).isTrue();
            assertThat(checklist.hasValidWebsite()).isTrue();
            assertThat(checklist.hasLogo()).isTrue();
            assertThat(checklist.completedCount()).isEqualTo(6);
            assertThat(checklist.completionPercentage()).isEqualTo(100);
            assertThat(checklist.meetsMinimumRequirements()).isTrue();
        }

        @Test
        @DisplayName("Should return partial completion for incomplete company")
        void shouldReturnPartialCompletionForIncompleteCompany() {
            // Given
            Company company = new Company(UUID.randomUUID(), "Test Corp");
            company.setDescription("Description");
            // Missing: location, industry, website, logo

            // When
            var checklist = verificationService.getVerificationChecklist(company);

            // Then
            assertThat(checklist.hasName()).isTrue();
            assertThat(checklist.hasDescription()).isTrue();
            assertThat(checklist.hasLocation()).isFalse();
            assertThat(checklist.hasIndustry()).isFalse();
            assertThat(checklist.hasValidWebsite()).isFalse();
            assertThat(checklist.hasLogo()).isFalse();
            assertThat(checklist.completedCount()).isEqualTo(2);
            assertThat(checklist.completionPercentage()).isEqualTo(33); // 2/6 = 33%
            assertThat(checklist.meetsMinimumRequirements()).isFalse();
        }

        @Test
        @DisplayName("Should meet minimum requirements with name, description, location, and website")
        void shouldMeetMinimumRequirements() {
            // Given
            Company company = createCompleteCompany();
            // Industry and logo are optional for minimum requirements

            // When
            var checklist = verificationService.getVerificationChecklist(company);

            // Then
            assertThat(checklist.meetsMinimumRequirements()).isTrue();
        }

        @Test
        @DisplayName("Should calculate total count correctly")
        void shouldCalculateTotalCountCorrectly() {
            // Given
            Company company = new Company(UUID.randomUUID(), "Test Corp");

            // When
            var checklist = verificationService.getVerificationChecklist(company);

            // Then
            assertThat(checklist.totalCount()).isEqualTo(6);
        }

        @Test
        @DisplayName("Should return zero completion for empty company")
        void shouldReturnZeroCompletionForEmptyCompany() {
            // Given
            Company company = new Company(UUID.randomUUID(), "");

            // When
            var checklist = verificationService.getVerificationChecklist(company);

            // Then
            assertThat(checklist.completedCount()).isEqualTo(0);
            assertThat(checklist.completionPercentage()).isEqualTo(0);
        }
    }

    // ==================== Test Utilities ====================

    private Company createCompleteCompany() {
        Company company = new Company(UUID.randomUUID(), "Complete Corp");
        company.setDescription("A complete company description");
        company.addLocation(Location.of("San Francisco", "CA", "USA"));
        company.setWebsiteUrl("https://completecorp.com");
        return company;
    }
}
