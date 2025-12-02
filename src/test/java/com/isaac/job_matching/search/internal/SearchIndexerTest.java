package com.isaac.job_matching.search.internal;

import com.isaac.job_matching.job.Job;
import com.isaac.job_matching.job.JobPostedEvent;
import com.isaac.job_matching.job.JobRepository;
import com.isaac.job_matching.company.Company;
import com.isaac.job_matching.company.CompanyRepository;
import com.isaac.job_matching.shared.Location;
import com.isaac.job_matching.shared.Money;
import com.isaac.job_matching.job.JobSkill;
import com.isaac.job_matching.shared.Skill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchIndexerTest {

    @Mock
    private ElasticsearchJobRepository elasticsearchRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private SearchIndexer searchIndexer;

    @Captor
    private ArgumentCaptor<JobSearchDocument> documentCaptor;

    @Test
    void shouldIndexJobWhenPosted() {
        // Given
        UUID jobId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        JobPostedEvent event = new JobPostedEvent(jobId, companyId, "Software Engineer", "Description", "City, State",
                "REMOTE", "ACTIVE", null);

        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        when(job.getCompanyId()).thenReturn(companyId);
        when(job.getTitle()).thenReturn("Software Engineer");
        when(job.getDescription()).thenReturn("Description");
        when(job.getLocation()).thenReturn(new Location("City", "State", "Country", null, null));
        when(job.getRemoteOption()).thenReturn(com.isaac.job_matching.job.RemoteOption.REMOTE);
        when(job.getSalaryMin()).thenReturn(new Money(BigDecimal.valueOf(50000), "USD"));
        when(job.getSalaryMax()).thenReturn(new Money(BigDecimal.valueOf(70000), "USD"));
        when(job.getExperienceLevel()).thenReturn(com.isaac.job_matching.job.ExperienceLevel.MID);
        when(job.getEmploymentType()).thenReturn(com.isaac.job_matching.job.EmploymentType.FULL_TIME);
        when(job.getStatus()).thenReturn(new com.isaac.job_matching.job.JobStatus.Active(null));
        JobSkill javaSkill = mock(JobSkill.class);
        Skill javaSkillEntity = new Skill("Java", "Programming");
        when(javaSkill.getSkill()).thenReturn(javaSkillEntity);

        JobSkill springSkill = mock(JobSkill.class);
        Skill springSkillEntity = new Skill("Spring", "Framework");
        when(springSkill.getSkill()).thenReturn(springSkillEntity);

        when(job.getSkills()).thenReturn(List.of(javaSkill, springSkill));

        Company company = mock(Company.class);
        when(company.getId()).thenReturn(companyId);
        when(company.getName()).thenReturn("Tech Corp");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        // When
        searchIndexer.onJobPosted(event);

        // Then
        verify(elasticsearchRepository).save(documentCaptor.capture());
        JobSearchDocument document = documentCaptor.getValue();

        assertThat(document.id()).isEqualTo(jobId.toString());
        assertThat(document.title()).isEqualTo("Software Engineer");
        assertThat(document.companyId()).isEqualTo(companyId.toString());
        assertThat(document.companyName()).isEqualTo("Tech Corp");
        assertThat(document.skills()).contains("Java", "Spring");
    }
}