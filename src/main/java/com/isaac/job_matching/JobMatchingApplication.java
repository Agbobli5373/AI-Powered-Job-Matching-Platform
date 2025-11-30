package com.isaac.job_matching;

import org.springframework.ai.model.mistralai.autoconfigure.MistralAiChatAutoConfiguration;
import org.springframework.ai.model.mistralai.autoconfigure.MistralAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.mistralai.autoconfigure.MistralAiModerationAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI-Powered Job Matching Platform
 * 
 * A modular monolith application using Spring Modulith that provides:
 * - Job seeker registration and profile management
 * - Employer job posting and candidate search
 * - AI-powered job matching using semantic similarity
 * - Resume parsing and skill extraction
 * - Application tracking and messaging
 */
@SpringBootApplication(exclude = {
		MistralAiChatAutoConfiguration.class,
		MistralAiEmbeddingAutoConfiguration.class,
		MistralAiModerationAutoConfiguration.class
})
@Modulithic(systemName = "Job Matching Platform", sharedModules = "shared")
@EnableAsync
public class JobMatchingApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobMatchingApplication.class, args);
	}

}
