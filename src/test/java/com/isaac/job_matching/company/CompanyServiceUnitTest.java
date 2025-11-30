package com.isaac.job_matching.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.isaac.job_matching.shared.Location;
import com.isaac.job_matching.shared.exception.DuplicateEntityException;
import com.isaac.job_matching.shared.exception.EntityNotFoundException;
import com.isaac.job_matching.user.UserRegisteredEvent;

/**
 * Unit tests for CompanyService.
 * 
 * <p>
 * Test Categories:
 * <ul>
 * <li>Company Creation - event-driven and manual creation</li>
 * <li>Company Lookup - by ID and user ID</li>
 * <li>Company Update - field updates</li>
 * <li>Company Verification - admin operations</li>
 * <li>Company Search - by name, industry, size</li>
 * <li>Statistics - counts and aggregations</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceUnitTest {

    @Mock
    private CompanyRepository companyRepository;

    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(companyRepository);
    }

    // ==================== Test Utilities ====================

    private void setEntityId(Object entity, UUID id) {
        try {
            Class<?> clazz = entity.getClass();
            Field idField = null;

            while (clazz != null && idField == null) {
                try {
                    idField = clazz.getDeclaredField("id");
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }

            if (idField != null) {
                idField.setAccessible(true);
                idField.set(entity, id);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set entity ID", e);
        }
    }

    private Company createCompany(UUID userId, String name) {
        Company company = new Company(userId, name);
        setEntityId(company, UUID.randomUUID());
        return company;
    }

    // ==================== Company Creation Tests ====================

    @Nested
    @DisplayName("Company Creation Tests")
    class CompanyCreationTests {

        @Test
        @DisplayName("Should create company on UserRegisteredEvent for employer")
        void shouldCreateCompanyOnUserRegisteredEvent() {
            // Given
            UUID userId = UUID.randomUUID();
            UserRegisteredEvent event = new UserRegisteredEvent(
                    userId, "employer@example.com", "EMPLOYER", java.time.Instant.now());

            when(companyRepository.existsByUserId(userId)).thenReturn(false);
            when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            companyService.onUserRegistered(event);

            // Then
            ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(companyCaptor.capture());

            Company savedCompany = companyCaptor.getValue();
            assertThat(savedCompany.getUserId()).isEqualTo(userId);
            assertThat(savedCompany.getName()).contains("employer");
        }

        @Test
        @DisplayName("Should not create company for job seeker on UserRegisteredEvent")
        void shouldNotCreateCompanyForJobSeeker() {
            // Given
            UUID userId = UUID.randomUUID();
            UserRegisteredEvent event = new UserRegisteredEvent(
                    userId, "jobseeker@example.com", "JOB_SEEKER", java.time.Instant.now());

            // When
            companyService.onUserRegistered(event);

            // Then
            verify(companyRepository, never()).save(any(Company.class));
        }

        @Test
        @DisplayName("Should create company for user manually")
        void shouldCreateCompanyForUserManually() {
            // Given
            UUID userId = UUID.randomUUID();
            String companyName = "Tech Corp";

            when(companyRepository.existsByUserId(userId)).thenReturn(false);
            when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Company result = companyService.createCompanyForUser(userId, companyName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getName()).isEqualTo(companyName);
            assertThat(result.isVerified()).isFalse();
            verify(companyRepository).save(any(Company.class));
        }

        @Test
        @DisplayName("Should return existing company if user already has one")
        void shouldReturnExistingCompany() {
            // Given
            UUID userId = UUID.randomUUID();
            Company existingCompany = createCompany(userId, "Existing Corp");

            when(companyRepository.existsByUserId(userId)).thenReturn(true);
            when(companyRepository.findByUserId(userId)).thenReturn(Optional.of(existingCompany));

            // When
            Company result = companyService.createCompanyForUser(userId, "New Name");

            // Then
            assertThat(result).isSameAs(existingCompany);
            verify(companyRepository, never()).save(any(Company.class));
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when creating company with existing user")
        void shouldThrowDuplicateExceptionWhenCreatingFullCompany() {
            // Given
            UUID userId = UUID.randomUUID();
            Company newCompany = new Company(userId, "New Corp");

            when(companyRepository.existsByUserId(userId)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> companyService.createCompany(userId, newCompany))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("Should create company with full details")
        void shouldCreateCompanyWithFullDetails() {
            // Given
            UUID userId = UUID.randomUUID();
            Company companyDetails = new Company(userId, "Full Corp");
            companyDetails.setDescription("A great company");
            companyDetails.setIndustry("Technology");
            companyDetails.setSize(CompanySize.MEDIUM_51_200);
            companyDetails.setWebsiteUrl("https://example.com");
            companyDetails.addLocation(Location.of("San Francisco", "CA", "USA"));

            when(companyRepository.existsByUserId(userId)).thenReturn(false);
            when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Company result = companyService.createCompany(userId, companyDetails);

            // Then
            assertThat(result.getName()).isEqualTo("Full Corp");
            assertThat(result.getDescription()).isEqualTo("A great company");
            assertThat(result.getIndustry()).isEqualTo("Technology");
            assertThat(result.getSize()).isEqualTo(CompanySize.MEDIUM_51_200);
            assertThat(result.getWebsiteUrl()).isEqualTo("https://example.com");
            assertThat(result.getLocations()).hasSize(1);
        }
    }

    // ==================== Company Lookup Tests ====================

    @Nested
    @DisplayName("Company Lookup Tests")
    class CompanyLookupTests {

        @Test
        @DisplayName("Should get company by ID successfully")
        void shouldGetCompanyById() {
            // Given
            UUID companyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Company company = createCompany(userId, "Test Corp");
            setEntityId(company, companyId);

            when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

            // When
            Company result = companyService.getCompanyById(companyId);

            // Then
            assertThat(result).isSameAs(company);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when company not found by ID")
        void shouldThrowExceptionWhenCompanyNotFoundById() {
            // Given
            UUID companyId = UUID.randomUUID();

            when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> companyService.getCompanyById(companyId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Company");
        }

        @Test
        @DisplayName("Should get company by user ID successfully")
        void shouldGetCompanyByUserId() {
            // Given
            UUID userId = UUID.randomUUID();
            Company company = createCompany(userId, "Test Corp");

            when(companyRepository.findByUserId(userId)).thenReturn(Optional.of(company));

            // When
            Company result = companyService.getCompanyByUserId(userId);

            // Then
            assertThat(result).isSameAs(company);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when company not found by user ID")
        void shouldThrowExceptionWhenCompanyNotFoundByUserId() {
            // Given
            UUID userId = UUID.randomUUID();

            when(companyRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> companyService.getCompanyByUserId(userId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Company");
        }

        @Test
        @DisplayName("Should find company by user ID optionally")
        void shouldFindCompanyByUserIdOptionally() {
            // Given
            UUID userId = UUID.randomUUID();
            Company company = createCompany(userId, "Test Corp");

            when(companyRepository.findByUserId(userId)).thenReturn(Optional.of(company));

            // When
            Optional<Company> result = companyService.findCompanyByUserId(userId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(company);
        }

        @Test
        @DisplayName("Should return empty optional when company not found")
        void shouldReturnEmptyWhenCompanyNotFound() {
            // Given
            UUID userId = UUID.randomUUID();

            when(companyRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // When
            Optional<Company> result = companyService.findCompanyByUserId(userId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should check if user has company")
        void shouldCheckIfUserHasCompany() {
            // Given
            UUID userId = UUID.randomUUID();

            when(companyRepository.existsByUserId(userId)).thenReturn(true);

            // When
            boolean result = companyService.hasCompany(userId);

            // Then
            assertThat(result).isTrue();
        }
    }

    // ==================== Company Update Tests ====================

    @Nested
    @DisplayName("Company Update Tests")
    class CompanyUpdateTests {

        @Test
        @DisplayName("Should update company fields")
        void shouldUpdateCompanyFields() {
            // Given
            UUID companyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Company existingCompany = createCompany(userId, "Old Name");
            setEntityId(existingCompany, companyId);

            Company updates = new Company(userId, "New Name");
            updates.setDescription("New Description");
            updates.setIndustry("Finance");

            when(companyRepository.findById(companyId)).thenReturn(Optional.of(existingCompany));
            when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Company result = companyService.updateCompany(companyId, updates);

            // Then
            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getDescription()).isEqualTo("New Description");
            assertThat(result.getIndustry()).isEqualTo("Finance");
            verify(companyRepository).save(existingCompany);
        }

        @Test
        @DisplayName("Should preserve null fields on update")
        void shouldPreserveNullFieldsOnUpdate() {
            // Given
            UUID companyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Company existingCompany = createCompany(userId, "Original Name");
            existingCompany.setDescription("Original Description");
            setEntityId(existingCompany, companyId);

            Company updates = new Company(userId, null); // null name should not update

            when(companyRepository.findById(companyId)).thenReturn(Optional.of(existingCompany));
            when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Company result = companyService.updateCompany(companyId, updates);

            // Then
            assertThat(result.getName()).isEqualTo("Original Name"); // preserved
            assertThat(result.getDescription()).isEqualTo("Original Description"); // preserved
        }

        @Test
        @DisplayName("Should save company directly")
        void shouldSaveCompanyDirectly() {
            // Given
            UUID userId = UUID.randomUUID();
            Company company = createCompany(userId, "Test Corp");

            when(companyRepository.save(company)).thenReturn(company);

            // When
            Company result = companyService.saveCompany(company);

            // Then
            assertThat(result).isSameAs(company);
            verify(companyRepository).save(company);
        }

        @Test
        @DisplayName("Should delete company")
        void shouldDeleteCompany() {
            // Given
            UUID companyId = UUID.randomUUID();

            when(companyRepository.existsById(companyId)).thenReturn(true);

            // When
            companyService.deleteCompany(companyId);

            // Then
            verify(companyRepository).deleteById(companyId);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent company")
        void shouldThrowExceptionWhenDeletingNonExistentCompany() {
            // Given
            UUID companyId = UUID.randomUUID();

            when(companyRepository.existsById(companyId)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> companyService.deleteCompany(companyId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ==================== Verification Tests ====================

    @Nested
    @DisplayName("Verification Tests")
    class VerificationTests {

        @Test
        @DisplayName("Should verify company")
        void shouldVerifyCompany() {
            // Given
            UUID companyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Company company = createCompany(userId, "Unverified Corp");
            setEntityId(company, companyId);
            assertThat(company.isVerified()).isFalse();

            when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
            when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Company result = companyService.verifyCompany(companyId);

            // Then
            assertThat(result.isVerified()).isTrue();
            verify(companyRepository).save(company);
        }

        @Test
        @DisplayName("Should revoke verification")
        void shouldRevokeVerification() {
            // Given
            UUID companyId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Company company = createCompany(userId, "Verified Corp");
            company.verify();
            setEntityId(company, companyId);
            assertThat(company.isVerified()).isTrue();

            when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
            when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Company result = companyService.revokeVerification(companyId);

            // Then
            assertThat(result.isVerified()).isFalse();
            verify(companyRepository).save(company);
        }
    }

    // ==================== Search Tests ====================

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        @Test
        @DisplayName("Should search companies by name")
        void shouldSearchByName() {
            // Given
            String searchTerm = "Tech";
            List<Company> expectedCompanies = List.of(
                    createCompany(UUID.randomUUID(), "Tech Corp"),
                    createCompany(UUID.randomUUID(), "Tech Solutions"));

            when(companyRepository.searchByName(searchTerm)).thenReturn(expectedCompanies);

            // When
            List<Company> result = companyService.searchByName(searchTerm);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should find companies by industry")
        void shouldFindByIndustry() {
            // Given
            String industry = "Technology";
            List<Company> expectedCompanies = List.of(
                    createCompany(UUID.randomUUID(), "Tech Corp"));

            when(companyRepository.findByIndustry(industry)).thenReturn(expectedCompanies);

            // When
            List<Company> result = companyService.findByIndustry(industry);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should find verified companies")
        void shouldFindVerified() {
            // Given
            Company verifiedCompany = createCompany(UUID.randomUUID(), "Verified Corp");
            verifiedCompany.verify();
            List<Company> expectedCompanies = List.of(verifiedCompany);

            when(companyRepository.findByVerifiedTrue()).thenReturn(expectedCompanies);

            // When
            List<Company> result = companyService.findVerified();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isVerified()).isTrue();
        }

        @Test
        @DisplayName("Should find companies by size")
        void shouldFindBySize() {
            // Given
            CompanySize size = CompanySize.STARTUP_1_10;
            Company startup = createCompany(UUID.randomUUID(), "Small Startup");
            startup.setSize(size);
            List<Company> expectedCompanies = List.of(startup);

            when(companyRepository.findBySize(size)).thenReturn(expectedCompanies);

            // When
            List<Company> result = companyService.findBySize(size);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get distinct industries")
        void shouldGetDistinctIndustries() {
            // Given
            List<String> expectedIndustries = List.of("Finance", "Healthcare", "Technology");

            when(companyRepository.findDistinctIndustries()).thenReturn(expectedIndustries);

            // When
            List<String> result = companyService.getDistinctIndustries();

            // Then
            assertThat(result).containsExactly("Finance", "Healthcare", "Technology");
        }
    }

    // ==================== Statistics Tests ====================

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should count all companies")
        void shouldCountAllCompanies() {
            // Given
            when(companyRepository.count()).thenReturn(100L);

            // When
            long count = companyService.countAll();

            // Then
            assertThat(count).isEqualTo(100L);
        }

        @Test
        @DisplayName("Should count verified companies")
        void shouldCountVerifiedCompanies() {
            // Given
            when(companyRepository.countVerified()).thenReturn(50L);

            // When
            long count = companyService.countVerified();

            // Then
            assertThat(count).isEqualTo(50L);
        }

        @Test
        @DisplayName("Should count companies by industry")
        void shouldCountByIndustry() {
            // Given
            String industry = "Technology";
            when(companyRepository.countByIndustry(industry)).thenReturn(25L);

            // When
            long count = companyService.countByIndustry(industry);

            // Then
            assertThat(count).isEqualTo(25L);
        }
    }
}
