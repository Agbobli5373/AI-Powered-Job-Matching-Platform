package com.isaac.job_matching.company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isaac.job_matching.shared.exception.DuplicateEntityException;
import com.isaac.job_matching.shared.exception.EntityNotFoundException;
import com.isaac.job_matching.user.UserRegisteredEvent;

/**
 * Public service API for company management operations.
 * 
 * <p>
 * This is the main entry point for the Company module, providing:
 * <ul>
 * <li>Company CRUD operations</li>
 * <li>Company verification (admin)</li>
 * <li>Company search and lookup</li>
 * <li>Statistics and analytics</li>
 * </ul>
 * 
 * <p>
 * Listens for UserRegisteredEvent to automatically create companies
 * for new employer users.
 */
@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    // ================ Event Listeners ================

    /**
     * Creates a company shell when an employer registers.
     * The company will need to be completed by the user.
     * 
     * @param event the user registered event
     */
    @ApplicationModuleListener
    public void onUserRegistered(UserRegisteredEvent event) {
        if ("EMPLOYER".equals(event.role())) {
            // Create a placeholder company - employer will complete the profile
            createCompanyForUser(event.userId(), "Company - " + event.email().split("@")[0]);
        }
    }

    // ================ Company CRUD ================

    /**
     * Creates a new company for the specified user.
     * 
     * @param userId the owner user ID
     * @param name   the company name
     * @return the created company
     * @throws DuplicateEntityException if user already has a company
     */
    public Company createCompanyForUser(UUID userId, String name) {
        if (companyRepository.existsByUserId(userId)) {
            // Return existing company instead of failing
            return companyRepository.findByUserId(userId).orElseThrow();
        }

        Company company = new Company(userId, name);
        return companyRepository.save(company);
    }

    /**
     * Creates a company with full details.
     * 
     * @param userId  the owner user ID
     * @param company the company details
     * @return the created company
     * @throws DuplicateEntityException if user already has a company
     */
    public Company createCompany(UUID userId, Company company) {
        if (companyRepository.existsByUserId(userId)) {
            throw new DuplicateEntityException("Company", "userId", userId.toString());
        }

        Company newCompany = new Company(userId, company.getName());
        newCompany.setDescription(company.getDescription());
        newCompany.setIndustry(company.getIndustry());
        newCompany.setSize(company.getSize());
        newCompany.setLogoUrl(company.getLogoUrl());
        newCompany.setWebsiteUrl(company.getWebsiteUrl());
        newCompany.setLocations(company.getLocations());

        return companyRepository.save(newCompany);
    }

    /**
     * Gets a company by ID.
     * 
     * @param id the company ID
     * @return the company
     * @throws EntityNotFoundException if company not found
     */
    @Transactional(readOnly = true)
    public Company getCompanyById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company", id));
    }

    /**
     * Gets a company by owner user ID.
     * 
     * @param userId the owner user ID
     * @return the company
     * @throws EntityNotFoundException if company not found
     */
    @Transactional(readOnly = true)
    public Company getCompanyByUserId(UUID userId) {
        return companyRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Company", "userId", userId.toString()));
    }

    /**
     * Gets a company by owner user ID if it exists.
     * 
     * @param userId the owner user ID
     * @return Optional containing the company if found
     */
    @Transactional(readOnly = true)
    public Optional<Company> findCompanyByUserId(UUID userId) {
        return companyRepository.findByUserId(userId);
    }

    /**
     * Updates a company's information.
     * 
     * @param companyId the company ID
     * @param updates   the updated company data
     * @return the updated company
     * @throws EntityNotFoundException if company not found
     */
    public Company updateCompany(UUID companyId, Company updates) {
        Company company = getCompanyById(companyId);

        if (updates.getName() != null) {
            company.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            company.setDescription(updates.getDescription());
        }
        if (updates.getIndustry() != null) {
            company.setIndustry(updates.getIndustry());
        }
        if (updates.getSize() != null) {
            company.setSize(updates.getSize());
        }
        if (updates.getLogoUrl() != null) {
            company.setLogoUrl(updates.getLogoUrl());
        }
        if (updates.getWebsiteUrl() != null) {
            company.setWebsiteUrl(updates.getWebsiteUrl());
        }
        if (updates.getLocations() != null && !updates.getLocations().isEmpty()) {
            company.setLocations(updates.getLocations());
        }

        return companyRepository.save(company);
    }

    /**
     * Saves a company (after direct modifications).
     * 
     * @param company the company to save
     * @return the saved company
     */
    public Company saveCompany(Company company) {
        return companyRepository.save(company);
    }

    /**
     * Deletes a company.
     * 
     * @param companyId the company ID to delete
     * @throws EntityNotFoundException if company not found
     */
    public void deleteCompany(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new EntityNotFoundException("Company", companyId);
        }
        companyRepository.deleteById(companyId);
    }

    // ================ Verification ================

    /**
     * Verifies a company (admin operation).
     * 
     * @param companyId the company ID to verify
     * @return the verified company
     * @throws EntityNotFoundException if company not found
     */
    public Company verifyCompany(UUID companyId) {
        Company company = getCompanyById(companyId);
        company.verify();
        return companyRepository.save(company);
    }

    /**
     * Revokes company verification (admin operation).
     * 
     * @param companyId the company ID
     * @return the updated company
     * @throws EntityNotFoundException if company not found
     */
    public Company revokeVerification(UUID companyId) {
        Company company = getCompanyById(companyId);
        company.revokeVerification();
        return companyRepository.save(company);
    }

    // ================ Search and Lookup ================

    /**
     * Searches companies by name.
     * 
     * @param name the search term
     * @return list of matching companies
     */
    @Transactional(readOnly = true)
    public List<Company> searchByName(String name) {
        return companyRepository.searchByName(name);
    }

    /**
     * Finds companies by industry.
     * 
     * @param industry the industry
     * @return list of companies in the industry
     */
    @Transactional(readOnly = true)
    public List<Company> findByIndustry(String industry) {
        return companyRepository.findByIndustry(industry);
    }

    /**
     * Finds verified companies.
     * 
     * @return list of verified companies
     */
    @Transactional(readOnly = true)
    public List<Company> findVerified() {
        return companyRepository.findByVerifiedTrue();
    }

    /**
     * Finds companies by size.
     * 
     * @param size the company size
     * @return list of companies
     */
    @Transactional(readOnly = true)
    public List<Company> findBySize(CompanySize size) {
        return companyRepository.findBySize(size);
    }

    /**
     * Gets all distinct industries.
     * 
     * @return list of industry names
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctIndustries() {
        return companyRepository.findDistinctIndustries();
    }

    // ================ Statistics ================

    /**
     * Gets count of all companies.
     * 
     * @return total company count
     */
    @Transactional(readOnly = true)
    public long countAll() {
        return companyRepository.count();
    }

    /**
     * Gets count of verified companies.
     * 
     * @return verified company count
     */
    @Transactional(readOnly = true)
    public long countVerified() {
        return companyRepository.countVerified();
    }

    /**
     * Gets count of companies in an industry.
     * 
     * @param industry the industry
     * @return count of companies
     */
    @Transactional(readOnly = true)
    public long countByIndustry(String industry) {
        return companyRepository.countByIndustry(industry);
    }

    /**
     * Checks if a user has a company.
     * 
     * @param userId the user ID
     * @return true if the user has a company
     */
    @Transactional(readOnly = true)
    public boolean hasCompany(UUID userId) {
        return companyRepository.existsByUserId(userId);
    }
}
