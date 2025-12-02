package com.isaac.job_matching.search.internal;

import com.isaac.job_matching.job.JobPostedEvent;
import com.isaac.job_matching.job.Job;
import com.isaac.job_matching.job.JobRepository;
import com.isaac.job_matching.company.CompanyRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Event listener that indexes jobs in Elasticsearch when they are posted.
 * Listens to JobPostedEvent from the job module.
 */
@Component
public class SearchIndexer {

    private final ElasticsearchJobRepository elasticsearchRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    public SearchIndexer(ElasticsearchJobRepository elasticsearchRepository,
            JobRepository jobRepository,
            CompanyRepository companyRepository) {
        this.elasticsearchRepository = elasticsearchRepository;
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
    }

    @ApplicationModuleListener
    public void onJobPosted(JobPostedEvent event) {
        Job job = jobRepository.findById(event.jobId())
                .orElseThrow(() -> new IllegalStateException("Job not found: " + event.jobId()));

        var company = companyRepository.findById(job.getCompanyId())
                .orElseThrow(() -> new IllegalStateException("Company not found: " + job.getCompanyId()));

        List<String> skillNames = job.getSkills().stream()
                .map(jobSkill -> jobSkill.getSkill().getName())
                .toList();

        JobSearchDocument document = new JobSearchDocument(
                job.getId().toString(),
                job.getTitle(),
                job.getDescription(),
                job.getCompanyId().toString(),
                company.getName(),
                job.getLocation().toString(), // Assuming Location has toString
                skillNames,
                job.getRemoteOption().name(),
                job.getSalaryMin() != null ? job.getSalaryMin().amount() : null,
                job.getSalaryMax() != null ? job.getSalaryMax().amount() : null,
                job.getExperienceLevel().name(),
                job.getEmploymentType().name(),
                job.getStatus().toString() // Assuming JobStatus has toString
        );

        elasticsearchRepository.save(document);
    }
}