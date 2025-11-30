# Implementation Plan: AI-Powered Job Matching Platform

**Branch**: `001-ai-job-matching-platform` | **Date**: 2025-11-30 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-ai-job-matching-platform/spec.md`

## Summary

Build a modern job marketplace using Spring Modulith 2.0 modular monolith architecture that uses AI (via Spring AI + Mistral AI) to intelligently match candidates with job postings. The platform supports three user roles (Job Seeker, Employer, Admin) with features including profile management, resume parsing, job posting, AI-powered matching with semantic skill recognition, application tracking, and messaging. The system uses event-driven communication between 12 application modules, PostgreSQL with pgvector for embeddings, Elasticsearch for search, and Redis for caching.

## Technical Context

**Language/Version**: Java 25 (JDK 25) with preview features enabled  
**Framework**: Spring Boot 4.0.0, Spring Modulith 2.0  
**Primary Dependencies**: Spring Security 6 (JWT/OAuth2), Spring AI, Spring Data JPA, Spring Data Elasticsearch  
**Storage**: PostgreSQL 16 + pgvector extension, Redis 7, Elasticsearch 8, AWS S3/MinIO  
**Testing**: JUnit 5, Spring Modulith Test, Testcontainers, AssertJ  
**Target Platform**: Linux server (Docker/Kubernetes), REST API  
**Project Type**: Modular monolith (Spring Modulith)  
**Performance Goals**: 10,000 concurrent users, <2s search response, <3s AI matching  
**Constraints**: 99.5% uptime, <3s user feedback, request-based data access for privacy  
**Scale/Scope**: 12 application modules, 60+ functional requirements, 13 entities

## Constitution Check

*GATE: Constitution template not customized - using default principles.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Modular Architecture | ✅ PASS | Spring Modulith enforces module boundaries |
| Event-Driven Communication | ✅ PASS | Domain events for inter-module communication |
| Test-First Approach | ✅ PASS | Module verification tests, integration tests planned |
| Observability | ✅ PASS | Micrometer + Spring Modulith Observability |

## Project Structure

### Documentation (this feature)

```text
specs/001-ai-job-matching-platform/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI specs)
│   ├── auth-api.yaml
│   ├── jobs-api.yaml
│   ├── profiles-api.yaml
│   ├── applications-api.yaml
│   └── matching-api.yaml
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
src/main/java/com/isaac/job_matching/
├── JobMatchingApplication.java          # @Modulithic @SpringBootApplication
│
├── user/                                 # 👤 USER MODULE
│   ├── User.java                        # Aggregate root
│   ├── UserService.java                 # Public API
│   ├── UserRepository.java
│   ├── UserRegisteredEvent.java         # Domain event (record)
│   ├── UserRole.java                    # Sealed interface
│   └── internal/
│       ├── UserValidationService.java
│       ├── PasswordEncodingService.java
│       └── UserController.java
│
├── profile/                              # 📋 PROFILE MODULE
│   ├── Profile.java
│   ├── ProfileService.java
│   ├── WorkExperience.java
│   ├── Education.java
│   ├── ProfileSkill.java
│   ├── ProfileUpdatedEvent.java
│   ├── ProfileAccessRequest.java
│   └── internal/
│       ├── ProfileMapper.java
│       ├── ProfileRepository.java
│       └── ProfileController.java
│
├── job/                                  # 💼 JOB MODULE
│   ├── Job.java
│   ├── JobService.java
│   ├── JobRepository.java
│   ├── JobSkill.java
│   ├── JobStatus.java                   # Sealed interface
│   ├── JobPostedEvent.java
│   ├── JobClosedEvent.java
│   └── internal/
│       ├── JobValidationService.java
│       └── JobController.java
│
├── company/                              # 🏢 COMPANY MODULE
│   ├── Company.java
│   ├── CompanyService.java
│   ├── CompanyRepository.java
│   └── internal/
│       ├── CompanyVerificationService.java
│       └── CompanyController.java
│
├── application/                          # 📝 APPLICATION MODULE
│   ├── JobApplication.java
│   ├── ApplicationService.java
│   ├── ApplicationStatus.java           # Sealed interface
│   ├── ApplicationSubmittedEvent.java
│   ├── ApplicationStatusChangedEvent.java
│   └── internal/
│       ├── ApplicationWorkflow.java
│       ├── ApplicationRepository.java
│       └── ApplicationController.java
│
├── matching/                             # 🤖 AI MATCHING MODULE
│   ├── MatchingService.java             # Public API
│   ├── MatchResult.java                 # Record
│   ├── JobRecommendation.java
│   ├── CandidateRecommendation.java
│   ├── MatchScoreBreakdown.java
│   └── internal/
│       ├── EmbeddingService.java        # Spring AI embeddings
│       ├── SkillMatcher.java
│       ├── ExperienceMatcher.java
│       ├── LocationMatcher.java
│       ├── SalaryMatcher.java
│       ├── VectorSimilarityEngine.java
│       └── MatchingController.java
│
├── resume/                               # 📄 RESUME MODULE
│   ├── Resume.java
│   ├── ResumeService.java
│   ├── ParsedResumeData.java            # Record
│   ├── ResumeUploadedEvent.java
│   ├── ResumeParsedEvent.java
│   └── internal/
│       ├── ResumeParser.java            # Spring AI parsing
│       ├── ResumeStorageService.java    # S3/MinIO
│       ├── SkillExtractor.java
│       ├── ResumeRepository.java
│       └── ResumeController.java
│
├── notification/                         # 🔔 NOTIFICATION MODULE
│   ├── NotificationService.java
│   ├── NotificationPreferences.java
│   ├── NotificationType.java            # Sealed interface
│   └── internal/
│       ├── EmailNotificationService.java
│       ├── PushNotificationService.java
│       ├── NotificationTemplates.java
│       ├── NotificationRepository.java
│       └── EventListeners.java          # @ApplicationModuleListener
│
├── messaging/                            # 💬 MESSAGING MODULE
│   ├── Message.java
│   ├── MessagingService.java
│   ├── MessageSentEvent.java
│   └── internal/
│       ├── MessageRepository.java
│       └── MessagingController.java
│
├── search/                               # 🔍 SEARCH MODULE
│   ├── SearchService.java
│   ├── JobSearchCriteria.java           # Record
│   ├── CandidateSearchCriteria.java     # Record
│   ├── SearchResult.java                # Record
│   └── internal/
│       ├── ElasticsearchJobRepository.java
│       ├── ElasticsearchProfileRepository.java
│       ├── SearchIndexer.java           # Event listener
│       └── SearchController.java
│
├── analytics/                            # 📊 ANALYTICS MODULE
│   ├── AnalyticsService.java
│   ├── JobAnalytics.java                # Record
│   ├── ProfileAnalytics.java            # Record
│   └── internal/
│       ├── MetricsAggregator.java
│       ├── AnalyticsRepository.java
│       └── AnalyticsController.java
│
├── auth/                                 # 🔐 AUTH MODULE
│   ├── AuthService.java
│   ├── TokenPair.java                   # Record
│   ├── AuthenticatedUser.java           # Record
│   └── internal/
│       ├── JwtTokenService.java
│       ├── OAuth2Service.java
│       ├── RefreshTokenRepository.java
│       └── AuthController.java
│
└── shared/                               # 🔧 SHARED MODULE
    ├── package-info.java                # @ApplicationModule(type = OPEN)
    ├── Money.java                       # Value object (record)
    ├── DateRange.java                   # Value object (record)
    ├── Location.java                    # Value object (record)
    ├── Skill.java                       # Shared entity
    ├── SkillRepository.java
    └── BaseEntity.java                  # UUID, timestamps

src/main/resources/
├── application.yaml
├── application-dev.yaml
├── application-prod.yaml
├── db/migration/                        # Flyway migrations
│   ├── V1__create_users.sql
│   ├── V2__create_profiles.sql
│   ├── V3__create_companies_jobs.sql
│   ├── V4__create_applications.sql
│   ├── V5__create_resumes.sql
│   ├── V6__create_messaging.sql
│   ├── V7__create_notifications.sql
│   └── V8__create_event_publication.sql
└── templates/                           # Email templates

src/test/java/com/isaac/job_matching/
├── ModuleStructureTests.java            # Verify module boundaries
├── user/
│   └── UserModuleTests.java
├── profile/
│   └── ProfileModuleTests.java
├── job/
│   └── JobModuleTests.java
├── matching/
│   └── MatchingModuleTests.java
└── integration/
    ├── ApplicationFlowTests.java
    └── MatchingFlowTests.java
```

**Structure Decision**: Spring Modulith modular monolith with 13 application modules (12 domain + 1 shared). Each module has public API types at package root and internal implementation in `internal/` subpackage. Event-driven communication via `@ApplicationModuleListener`.

## Module Dependencies

```text
┌─────────────────────────────────────────────────────────────────────┐
│                           SHARED MODULE                              │
│  (Money, Location, DateRange, Skill, BaseEntity)                    │
└─────────────────────────────────────────────────────────────────────┘
                                    ▲
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
┌───────┴───────┐           ┌───────┴───────┐           ┌───────┴───────┐
│     USER      │           │    COMPANY    │           │     AUTH      │
│   Module      │◄──────────│    Module     │           │    Module     │
└───────┬───────┘           └───────┬───────┘           └───────────────┘
        │                           │
        ▼                           ▼
┌───────────────┐           ┌───────────────┐
│    PROFILE    │           │      JOB      │
│    Module     │           │    Module     │
└───────┬───────┘           └───────┬───────┘
        │                           │
        └─────────┬─────────────────┘
                  │
        ┌─────────▼─────────┐
        │    APPLICATION    │
        │      Module       │
        └─────────┬─────────┘
                  │
    ┌─────────────┼─────────────┐
    │             │             │
    ▼             ▼             ▼
┌───────┐   ┌─────────┐   ┌─────────┐
│RESUME │   │MATCHING │   │ SEARCH  │
│Module │   │ Module  │   │ Module  │
└───────┘   └─────────┘   └─────────┘

         ┌─────────────────────────────┐
         │  EVENT-DRIVEN CONSUMERS     │
         │  (Listen via domain events) │
         ├─────────────────────────────┤
         │  NOTIFICATION Module        │
         │  MESSAGING Module           │
         │  ANALYTICS Module           │
         └─────────────────────────────┘
```

## Complexity Tracking

> No constitution violations identified - default structure follows Spring Modulith best practices.

| Decision | Rationale | Alternative Considered |
|----------|-----------|------------------------|
| 13 modules | Domain-driven boundaries per business capability | Fewer modules would couple unrelated concerns |
| pgvector for embeddings | Native PostgreSQL vector search, simpler ops | Dedicated vector DB (Pinecone) - additional infrastructure |
| Elasticsearch for search | Full-text + filtering, proven at scale | PostgreSQL full-text - less feature-rich |
| Spring AI + Mistral | Cost-effective, EU data residency, native Spring integration | OpenAI - higher cost, US data residency |

