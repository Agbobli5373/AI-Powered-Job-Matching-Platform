# Tasks: AI-Powered Job Matching Platform

**Input**: Design documents from `/specs/001-ai-job-matching-platform/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: Not explicitly requested - implementation tasks only.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Based on plan.md structure (Spring Modulith):
- Source: `src/main/java/com/isaac/job_matching/`
- Resources: `src/main/resources/`
- Tests: `src/test/java/com/isaac/job_matching/`
- Migrations: `src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, dependencies, and base configuration

- [X] T001 Update pom.xml with all dependencies (Spring Modulith 2.0, Spring AI Mistral, pgvector, Elasticsearch, Redis, S3) in `pom.xml`
- [X] T002 [P] Create docker-compose.yml with PostgreSQL 16 + pgvector, Redis 7, Elasticsearch 8, MinIO in `docker-compose.yml`
- [X] T003 [P] Configure application.yaml with datasource, redis, elasticsearch, and AI settings in `src/main/resources/application.yaml`
- [X] T004 [P] Create application-local.yaml for local development in `src/main/resources/application-local.yaml`
- [X] T005 [P] Create application-test.yaml for testing in `src/main/resources/application-test.yaml`
- [X] T006 Add @Modulithic annotation to main application class in `src/main/java/com/isaac/job_matching/JobMatchingApplication.java`

**Checkpoint**: Project builds and runs with Docker services ✅

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Shared Module

- [X] T007 Create shared module package-info.java with @ApplicationModule(type = OPEN) in `src/main/java/com/isaac/job_matching/shared/package-info.java`
- [X] T008 [P] Create BaseEntity with UUID and timestamps in `src/main/java/com/isaac/job_matching/shared/BaseEntity.java`
- [X] T009 [P] Create Money value object (record) in `src/main/java/com/isaac/job_matching/shared/Money.java`
- [X] T010 [P] Create Location value object (record) in `src/main/java/com/isaac/job_matching/shared/Location.java`
- [X] T011 [P] Create DateRange value object (record) in `src/main/java/com/isaac/job_matching/shared/DateRange.java`
- [X] T012 Create Skill entity with embedding vector in `src/main/java/com/isaac/job_matching/shared/Skill.java`
- [X] T013 Create SkillRepository in `src/main/java/com/isaac/job_matching/shared/SkillRepository.java`

### Database Migrations (Foundation)

- [X] T014 Create V1__enable_extensions.sql (pgvector, uuid-ossp) in `src/main/resources/db/migration/V1__enable_extensions.sql`
- [X] T015 Create V2__create_skills.sql with vector column in `src/main/resources/db/migration/V2__create_skills.sql`
- [X] T016 Create V3__create_users.sql in `src/main/resources/db/migration/V3__create_users.sql`
- [X] T017 Create V4__create_event_publication.sql for Spring Modulith in `src/main/resources/db/migration/V4__create_event_publication.sql`

### Security Configuration

- [X] T018 Create SecurityConfig with JWT resource server in `src/main/java/com/isaac/job_matching/shared/config/SecurityConfig.java`
- [X] T019 [P] Create JwtConfig for token settings in `src/main/java/com/isaac/job_matching/shared/config/JwtConfig.java`
- [X] T020 [P] Create CorsConfig for CORS settings in `src/main/java/com/isaac/job_matching/shared/config/CorsConfig.java`

### Exception Handling

- [X] T021 Create GlobalExceptionHandler with @ControllerAdvice in `src/main/java/com/isaac/job_matching/shared/exception/GlobalExceptionHandler.java`
- [X] T022 [P] Create ApiError record for error responses in `src/main/java/com/isaac/job_matching/shared/exception/ApiError.java`
- [X] T023 [P] Create domain exception classes (EntityNotFoundException, ValidationException) in `src/main/java/com/isaac/job_matching/shared/exception/`

### Module Structure Verification

- [X] T024 Create ModuleStructureTests to verify module boundaries in `src/test/java/com/isaac/job_matching/ModuleStructureTests.java`

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Job Seeker Registration & Profile Creation (Priority: P1) 🎯 MVP

**Goal**: Job seekers can register, verify email, and create professional profiles with work experience, education, and skills.

**Independent Test**: Register → verify email → complete profile → view profile

### User Module (US1)

- [X] T025 [P] [US1] Create UserRole sealed interface with JOB_SEEKER, EMPLOYER, ADMIN in `src/main/java/com/isaac/job_matching/user/UserRole.java`
- [X] T026 [P] [US1] Create UserStatus enum (PENDING, ACTIVE, SUSPENDED) in `src/main/java/com/isaac/job_matching/user/UserStatus.java`
- [X] T027 [US1] Create User entity (aggregate root) in `src/main/java/com/isaac/job_matching/user/User.java`
- [X] T028 [US1] Create UserRepository in `src/main/java/com/isaac/job_matching/user/UserRepository.java`
- [X] T029 [US1] Create UserRegisteredEvent (record) in `src/main/java/com/isaac/job_matching/user/UserRegisteredEvent.java`
- [X] T030 [US1] Create UserService public API in `src/main/java/com/isaac/job_matching/user/UserService.java`
- [X] T031 [P] [US1] Create UserValidationService in `src/main/java/com/isaac/job_matching/user/internal/UserValidationService.java`
- [X] T032 [P] [US1] Create PasswordEncodingService in `src/main/java/com/isaac/job_matching/user/internal/PasswordEncodingService.java`

### Auth Module (US1)

- [X] T033 [P] [US1] Create TokenPair record in `src/main/java/com/isaac/job_matching/auth/TokenPair.java`
- [X] T034 [P] [US1] Create AuthenticatedUser record in `src/main/java/com/isaac/job_matching/auth/AuthenticatedUser.java`
- [X] T035 [US1] Create AuthService public API in `src/main/java/com/isaac/job_matching/auth/AuthService.java`
- [X] T036 [US1] Create JwtTokenService in `src/main/java/com/isaac/job_matching/auth/internal/JwtTokenService.java`
- [X] T037 [US1] Create RefreshTokenRepository in `src/main/java/com/isaac/job_matching/auth/internal/RefreshTokenRepository.java`
- [X] T038 [US1] Create AuthController with register/login/refresh/verify endpoints in `src/main/java/com/isaac/job_matching/auth/internal/AuthController.java`

### Profile Module (US1)

- [X] T039 [P] [US1] Create V5__create_profiles.sql migration in `src/main/resources/db/migration/V5__create_profiles.sql`
- [X] T040 [US1] Create Profile entity (aggregate root) in `src/main/java/com/isaac/job_matching/profile/Profile.java`
- [X] T041 [P] [US1] Create WorkExperience entity in `src/main/java/com/isaac/job_matching/profile/WorkExperience.java`
- [X] T042 [P] [US1] Create Education entity in `src/main/java/com/isaac/job_matching/profile/Education.java`
- [X] T043 [P] [US1] Create ProfileSkill entity in `src/main/java/com/isaac/job_matching/profile/ProfileSkill.java`
- [X] T044 [US1] Create ProfileUpdatedEvent (record) in `src/main/java/com/isaac/job_matching/profile/ProfileUpdatedEvent.java`
- [X] T045 [US1] Create ProfileRepository in `src/main/java/com/isaac/job_matching/profile/internal/ProfileRepository.java`
- [X] T046 [US1] Create ProfileMapper in `src/main/java/com/isaac/job_matching/profile/internal/ProfileMapper.java`
- [X] T047 [US1] Create ProfileService public API in `src/main/java/com/isaac/job_matching/profile/ProfileService.java`
- [X] T048 [US1] Create ProfileController with CRUD endpoints in `src/main/java/com/isaac/job_matching/profile/internal/ProfileController.java`

**Checkpoint**: User Story 1 complete - job seekers can register and create profiles

---

## Phase 4: User Story 2 - Employer Job Posting (Priority: P1) 🎯 MVP

**Goal**: Employers can register, create company profiles, and post job listings with skills and requirements.

**Independent Test**: Register as employer → create company → post job → view job listing

### Company Module (US2)

- [X] T049 [P] [US2] Create V6__create_companies.sql migration in `src/main/resources/db/migration/V6__create_companies.sql`
- [X] T050 [US2] Create Company entity (aggregate root) in `src/main/java/com/isaac/job_matching/company/Company.java`
- [X] T051 [US2] Create CompanyRepository in `src/main/java/com/isaac/job_matching/company/CompanyRepository.java`
- [X] T052 [US2] Create CompanyService public API in `src/main/java/com/isaac/job_matching/company/CompanyService.java`
- [X] T053 [P] [US2] Create CompanyVerificationService in `src/main/java/com/isaac/job_matching/company/internal/CompanyVerificationService.java`
- [X] T054 [US2] Create CompanyController in `src/main/java/com/isaac/job_matching/company/internal/CompanyController.java`

### Job Module (US2)

- [X] T055 [P] [US2] Create V7__create_jobs.sql migration in `src/main/resources/db/migration/V7__create_jobs.sql`
- [X] T056 [P] [US2] Create JobStatus sealed interface (DRAFT, ACTIVE, PAUSED, CLOSED) in `src/main/java/com/isaac/job_matching/job/JobStatus.java`
- [X] T057 [US2] Create Job entity (aggregate root) in `src/main/java/com/isaac/job_matching/job/Job.java`
- [X] T058 [P] [US2] Create JobSkill entity with importance level in `src/main/java/com/isaac/job_matching/job/JobSkill.java`
- [X] T059 [US2] Create JobRepository in `src/main/java/com/isaac/job_matching/job/JobRepository.java`
- [X] T060 [P] [US2] Create JobPostedEvent (record) in `src/main/java/com/isaac/job_matching/job/JobPostedEvent.java`
- [X] T061 [P] [US2] Create JobClosedEvent (record) in `src/main/java/com/isaac/job_matching/job/JobClosedEvent.java`
- [X] T062 [US2] Create JobValidationService in `src/main/java/com/isaac/job_matching/job/internal/JobValidationService.java`
- [X] T063 [US2] Create JobService public API in `src/main/java/com/isaac/job_matching/job/JobService.java`
- [X] T064 [US2] Create JobController with CRUD endpoints in `src/main/java/com/isaac/job_matching/job/internal/JobController.java`

**Checkpoint**: User Story 2 complete - employers can post jobs

---

## Phase 5: User Story 3 - Job Search & Application (Priority: P1) 🎯 MVP

**Goal**: Job seekers can search for jobs with filters and submit applications.

**Independent Test**: Search jobs → filter by criteria → view job details → apply → see "Applied" status

### Search Module (US3)

- [ ] T065 [P] [US3] Create JobSearchCriteria record in `src/main/java/com/isaac/job_matching/search/JobSearchCriteria.java`
- [ ] T066 [P] [US3] Create SearchResult record in `src/main/java/com/isaac/job_matching/search/SearchResult.java`
- [ ] T067 [US3] Create ElasticsearchJobRepository in `src/main/java/com/isaac/job_matching/search/internal/ElasticsearchJobRepository.java`
- [ ] T068 [US3] Create JobSearchDocument for ES indexing in `src/main/java/com/isaac/job_matching/search/internal/JobSearchDocument.java`
- [ ] T069 [US3] Create SearchIndexer @ApplicationModuleListener for JobPostedEvent in `src/main/java/com/isaac/job_matching/search/internal/SearchIndexer.java`
- [ ] T070 [US3] Create SearchService public API in `src/main/java/com/isaac/job_matching/search/SearchService.java`
- [ ] T071 [US3] Create SearchController with search endpoint in `src/main/java/com/isaac/job_matching/search/internal/SearchController.java`

### Application Module (US3)

- [ ] T072 [P] [US3] Create V8__create_applications.sql migration in `src/main/resources/db/migration/V8__create_applications.sql`
- [ ] T073 [P] [US3] Create ApplicationStatus sealed interface in `src/main/java/com/isaac/job_matching/application/ApplicationStatus.java`
- [ ] T074 [US3] Create JobApplication entity (aggregate root) in `src/main/java/com/isaac/job_matching/application/JobApplication.java`
- [ ] T075 [US3] Create ApplicationRepository in `src/main/java/com/isaac/job_matching/application/internal/ApplicationRepository.java`
- [ ] T076 [P] [US3] Create ApplicationSubmittedEvent (record) in `src/main/java/com/isaac/job_matching/application/ApplicationSubmittedEvent.java`
- [ ] T077 [P] [US3] Create ApplicationStatusChangedEvent (record) in `src/main/java/com/isaac/job_matching/application/ApplicationStatusChangedEvent.java`
- [ ] T078 [US3] Create ApplicationWorkflow for status transitions in `src/main/java/com/isaac/job_matching/application/internal/ApplicationWorkflow.java`
- [ ] T079 [US3] Create ApplicationService public API in `src/main/java/com/isaac/job_matching/application/ApplicationService.java`
- [ ] T080 [US3] Create ApplicationController with apply/view endpoints in `src/main/java/com/isaac/job_matching/application/internal/ApplicationController.java`

**Checkpoint**: MVP Complete - job seekers can register, search jobs, and apply

---

## Phase 6: User Story 4 - Resume Upload & AI Parsing (Priority: P2)

**Goal**: Job seekers upload resumes and AI extracts skills, experience, education for profile completion.

**Independent Test**: Upload PDF/DOCX → see extracted data → confirm to update profile

### Resume Module (US4)

- [ ] T081 [P] [US4] Create V9__create_resumes.sql migration in `src/main/resources/db/migration/V9__create_resumes.sql`
- [ ] T082 [P] [US4] Create ParsedResumeData record in `src/main/java/com/isaac/job_matching/resume/ParsedResumeData.java`
- [ ] T083 [US4] Create Resume entity (aggregate root) in `src/main/java/com/isaac/job_matching/resume/Resume.java`
- [ ] T084 [US4] Create ResumeRepository in `src/main/java/com/isaac/job_matching/resume/internal/ResumeRepository.java`
- [ ] T085 [P] [US4] Create ResumeUploadedEvent (record) in `src/main/java/com/isaac/job_matching/resume/ResumeUploadedEvent.java`
- [ ] T086 [P] [US4] Create ResumeParsedEvent (record) in `src/main/java/com/isaac/job_matching/resume/ResumeParsedEvent.java`
- [ ] T087 [US4] Create ResumeStorageService for S3/MinIO in `src/main/java/com/isaac/job_matching/resume/internal/ResumeStorageService.java`
- [ ] T088 [US4] Create ResumeParser using Spring AI Mistral in `src/main/java/com/isaac/job_matching/resume/internal/ResumeParser.java`
- [ ] T089 [US4] Create SkillExtractor for normalizing extracted skills in `src/main/java/com/isaac/job_matching/resume/internal/SkillExtractor.java`
- [ ] T090 [US4] Create ResumeService public API in `src/main/java/com/isaac/job_matching/resume/ResumeService.java`
- [ ] T091 [US4] Create ResumeController with upload/parse/confirm endpoints in `src/main/java/com/isaac/job_matching/resume/internal/ResumeController.java`

**Checkpoint**: User Story 4 complete - resumes can be uploaded and parsed

---

## Phase 7: User Story 5 - AI Job Recommendations for Job Seekers (Priority: P2)

**Goal**: Job seekers receive personalized job recommendations with match percentages and explanations.

**Independent Test**: Complete profile → view recommendations → see match breakdown

### Matching Module (US5)

- [ ] T092 [P] [US5] Create MatchResult record in `src/main/java/com/isaac/job_matching/matching/MatchResult.java`
- [ ] T093 [P] [US5] Create MatchScoreBreakdown record in `src/main/java/com/isaac/job_matching/matching/MatchScoreBreakdown.java`
- [ ] T094 [P] [US5] Create JobRecommendation record in `src/main/java/com/isaac/job_matching/matching/JobRecommendation.java`
- [ ] T095 [US5] Create EmbeddingService using Spring AI Mistral in `src/main/java/com/isaac/job_matching/matching/internal/EmbeddingService.java`
- [ ] T096 [US5] Create VectorSimilarityEngine for pgvector queries in `src/main/java/com/isaac/job_matching/matching/internal/VectorSimilarityEngine.java`
- [ ] T097 [P] [US5] Create SkillMatcher (40% weight) in `src/main/java/com/isaac/job_matching/matching/internal/SkillMatcher.java`
- [ ] T098 [P] [US5] Create ExperienceMatcher (25% weight) in `src/main/java/com/isaac/job_matching/matching/internal/ExperienceMatcher.java`
- [ ] T099 [P] [US5] Create LocationMatcher (15% weight) in `src/main/java/com/isaac/job_matching/matching/internal/LocationMatcher.java`
- [ ] T100 [P] [US5] Create SalaryMatcher (15% weight) in `src/main/java/com/isaac/job_matching/matching/internal/SalaryMatcher.java`
- [ ] T101 [US5] Create MatchingService public API in `src/main/java/com/isaac/job_matching/matching/MatchingService.java`
- [ ] T102 [US5] Create MatchingController for recommendations endpoint in `src/main/java/com/isaac/job_matching/matching/internal/MatchingController.java`

**Checkpoint**: User Story 5 complete - job seekers get AI recommendations

---

## Phase 8: User Story 6 - AI Candidate Matching for Employers (Priority: P2)

**Goal**: Employers receive AI-ranked candidate suggestions with match scores and skill gap analysis.

**Independent Test**: Post job → view candidate matches → see skill gaps

### Matching Module Extension (US6)

- [ ] T103 [P] [US6] Create CandidateRecommendation record in `src/main/java/com/isaac/job_matching/matching/CandidateRecommendation.java`
- [ ] T104 [US6] Create CandidateSearchCriteria record in `src/main/java/com/isaac/job_matching/search/CandidateSearchCriteria.java`
- [ ] T105 [US6] Create ElasticsearchProfileRepository in `src/main/java/com/isaac/job_matching/search/internal/ElasticsearchProfileRepository.java`
- [ ] T106 [US6] Create ProfileSearchDocument for ES indexing in `src/main/java/com/isaac/job_matching/search/internal/ProfileSearchDocument.java`
- [ ] T107 [US6] Update SearchIndexer to handle ProfileUpdatedEvent in `src/main/java/com/isaac/job_matching/search/internal/SearchIndexer.java`
- [ ] T108 [US6] Add getCandidateMatches method to MatchingService in `src/main/java/com/isaac/job_matching/matching/MatchingService.java`
- [ ] T109 [US6] Add candidate matching endpoint to MatchingController in `src/main/java/com/isaac/job_matching/matching/internal/MatchingController.java`

### Profile Access Control (US6)

- [ ] T110 [P] [US6] Create ProfileAccessRequest entity in `src/main/java/com/isaac/job_matching/profile/ProfileAccessRequest.java`
- [ ] T111 [US6] Create ProfileAccessRepository in `src/main/java/com/isaac/job_matching/profile/internal/ProfileAccessRepository.java`
- [ ] T112 [US6] Add access request methods to ProfileService in `src/main/java/com/isaac/job_matching/profile/ProfileService.java`
- [ ] T113 [US6] Add access request endpoints to ProfileController in `src/main/java/com/isaac/job_matching/profile/internal/ProfileController.java`

**Checkpoint**: User Story 6 complete - employers can find matching candidates

---

## Phase 9: User Story 7 - Application Management for Employers (Priority: P2)

**Goal**: Employers can review applications, update statuses, shortlist/reject candidates, and schedule interviews.

**Independent Test**: View applications → change status → bulk reject → schedule interview

- [ ] T114 [US7] Create employer application view in ApplicationController in `src/main/java/com/isaac/job_matching/application/internal/ApplicationController.java`
- [ ] T115 [US7] Add status update methods to ApplicationService in `src/main/java/com/isaac/job_matching/application/ApplicationService.java`
- [ ] T116 [US7] Add bulk status update endpoint in `src/main/java/com/isaac/job_matching/application/internal/ApplicationController.java`
- [ ] T117 [US7] Create interview scheduling integration in ApplicationWorkflow in `src/main/java/com/isaac/job_matching/application/internal/ApplicationWorkflow.java`

**Checkpoint**: User Story 7 complete - employers can manage applications

---

## Phase 10: User Story 8 - Application Tracking for Job Seekers (Priority: P2)

**Goal**: Job seekers can track application status and receive updates with full timeline.

**Independent Test**: View my applications → see status → click for timeline

- [ ] T118 [US8] Add getMyApplications method to ApplicationService in `src/main/java/com/isaac/job_matching/application/ApplicationService.java`
- [ ] T119 [US8] Create ApplicationTimelineEntry record in `src/main/java/com/isaac/job_matching/application/ApplicationTimelineEntry.java`
- [ ] T120 [US8] Add timeline tracking to JobApplication entity in `src/main/java/com/isaac/job_matching/application/JobApplication.java`
- [ ] T121 [US8] Add my applications endpoint to ApplicationController in `src/main/java/com/isaac/job_matching/application/internal/ApplicationController.java`

**Checkpoint**: User Story 8 complete - job seekers can track applications

---

## Phase 11: User Story 9 - Messaging (Priority: P3)

**Goal**: Employers and candidates can communicate through in-platform messaging.

**Independent Test**: Send message → receive notification → reply → view thread

### Messaging Module (US9)

- [ ] T122 [P] [US9] Create V10__create_messaging.sql migration in `src/main/resources/db/migration/V10__create_messaging.sql`
- [ ] T123 [US9] Create Message entity in `src/main/java/com/isaac/job_matching/messaging/Message.java`
- [ ] T124 [US9] Create Conversation entity (aggregate root) in `src/main/java/com/isaac/job_matching/messaging/Conversation.java`
- [ ] T125 [US9] Create MessageSentEvent (record) in `src/main/java/com/isaac/job_matching/messaging/MessageSentEvent.java`
- [ ] T126 [US9] Create MessageRepository in `src/main/java/com/isaac/job_matching/messaging/internal/MessageRepository.java`
- [ ] T127 [US9] Create MessagingService public API in `src/main/java/com/isaac/job_matching/messaging/MessagingService.java`
- [ ] T128 [US9] Create MessagingController in `src/main/java/com/isaac/job_matching/messaging/internal/MessagingController.java`

**Checkpoint**: User Story 9 complete - users can message each other

---

## Phase 12: User Story 10 - Notifications & Alerts (Priority: P3)

**Goal**: Users receive timely notifications for matches, status changes, and messages.

**Independent Test**: Configure preferences → trigger event → receive notification

### Notification Module (US10)

- [ ] T129 [P] [US10] Create V11__create_notifications.sql migration in `src/main/resources/db/migration/V11__create_notifications.sql`
- [ ] T130 [P] [US10] Create NotificationType sealed interface in `src/main/java/com/isaac/job_matching/notification/NotificationType.java`
- [ ] T131 [US10] Create Notification entity in `src/main/java/com/isaac/job_matching/notification/Notification.java`
- [ ] T132 [US10] Create NotificationPreferences entity in `src/main/java/com/isaac/job_matching/notification/NotificationPreferences.java`
- [ ] T133 [US10] Create NotificationRepository in `src/main/java/com/isaac/job_matching/notification/internal/NotificationRepository.java`
- [ ] T134 [P] [US10] Create EmailNotificationService in `src/main/java/com/isaac/job_matching/notification/internal/EmailNotificationService.java`
- [ ] T135 [P] [US10] Create PushNotificationService in `src/main/java/com/isaac/job_matching/notification/internal/PushNotificationService.java`
- [ ] T136 [US10] Create EventListeners @ApplicationModuleListener for domain events in `src/main/java/com/isaac/job_matching/notification/internal/EventListeners.java`
- [ ] T137 [US10] Create NotificationService public API in `src/main/java/com/isaac/job_matching/notification/NotificationService.java`
- [ ] T138 [US10] Create NotificationController in `src/main/java/com/isaac/job_matching/notification/internal/NotificationController.java`

**Checkpoint**: User Story 10 complete - notifications are delivered

---

## Phase 13: User Story 11 - Analytics Dashboard (Priority: P3)

**Goal**: Users see profile/job performance metrics and trends.

**Independent Test**: View dashboard → see metrics → filter by date range

### Analytics Module (US11)

- [ ] T139 [P] [US11] Create JobAnalytics record in `src/main/java/com/isaac/job_matching/analytics/JobAnalytics.java`
- [ ] T140 [P] [US11] Create ProfileAnalytics record in `src/main/java/com/isaac/job_matching/analytics/ProfileAnalytics.java`
- [ ] T141 [US11] Create MetricsAggregator in `src/main/java/com/isaac/job_matching/analytics/internal/MetricsAggregator.java`
- [ ] T142 [US11] Create AnalyticsRepository in `src/main/java/com/isaac/job_matching/analytics/internal/AnalyticsRepository.java`
- [ ] T143 [US11] Create AnalyticsService public API in `src/main/java/com/isaac/job_matching/analytics/AnalyticsService.java`
- [ ] T144 [US11] Create AnalyticsController in `src/main/java/com/isaac/job_matching/analytics/internal/AnalyticsController.java`

**Checkpoint**: User Story 11 complete - analytics available

---

## Phase 14: User Story 12 - Admin Platform Management (Priority: P3)

**Goal**: Admins can manage users, moderate content, and view platform metrics.

**Independent Test**: Login as admin → search users → suspend user → view platform metrics

- [ ] T145 [US12] Create AdminController with user management endpoints in `src/main/java/com/isaac/job_matching/user/internal/AdminController.java`
- [ ] T146 [US12] Add admin methods to UserService in `src/main/java/com/isaac/job_matching/user/UserService.java`
- [ ] T147 [US12] Add content moderation endpoints to JobController in `src/main/java/com/isaac/job_matching/job/internal/JobController.java`
- [ ] T148 [US12] Add platform-wide analytics to AnalyticsController in `src/main/java/com/isaac/job_matching/analytics/internal/AnalyticsController.java`

**Checkpoint**: User Story 12 complete - admin functionality available

---

## Phase 15: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

### OAuth2 Integration

- [ ] T149 [P] Create OAuth2Service for Google/LinkedIn/GitHub in `src/main/java/com/isaac/job_matching/auth/internal/OAuth2Service.java`
- [ ] T150 Add OAuth2 endpoints to AuthController in `src/main/java/com/isaac/job_matching/auth/internal/AuthController.java`

### Caching

- [ ] T151 [P] Configure Redis caching in `src/main/java/com/isaac/job_matching/shared/config/CacheConfig.java`
- [ ] T152 Add @Cacheable annotations to frequently accessed services

### Performance & Observability

- [ ] T153 [P] Configure Micrometer metrics in `src/main/java/com/isaac/job_matching/shared/config/ObservabilityConfig.java`
- [ ] T154 [P] Add Spring Modulith Observability configuration
- [ ] T155 Add performance logging and slow query monitoring

### Documentation

- [X] T156 [P] Configure OpenAPI/Swagger documentation in `src/main/java/com/isaac/job_matching/shared/config/OpenApiConfig.java`
- [ ] T157 Validate implementation against quickstart.md scenarios

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies - can start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 - **BLOCKS all user stories**
- **Phases 3-5 (P1 Stories)**: Depend on Phase 2 - Form MVP core
- **Phases 6-10 (P2 Stories)**: Depend on Phase 2, can proceed in parallel after
- **Phases 11-14 (P3 Stories)**: Depend on Phase 2, can proceed in parallel after
- **Phase 15 (Polish)**: Depends on all desired user stories being complete

### User Story Dependencies

| Story | Priority | Dependencies | Notes |
|-------|----------|--------------|-------|
| US1 - Registration/Profile | P1 | Foundational only | No story dependencies |
| US2 - Job Posting | P1 | Foundational only | No story dependencies |
| US3 - Search/Application | P1 | US1, US2 (for data) | Needs profiles and jobs to search |
| US4 - Resume Parsing | P2 | US1 (Profile module) | Enhances profile |
| US5 - Job Recommendations | P2 | US1, US2 | Needs profiles and jobs |
| US6 - Candidate Matching | P2 | US1, US2 | Needs profiles and jobs |
| US7 - Application Mgmt | P2 | US3 | Needs applications |
| US8 - Application Tracking | P2 | US3 | Needs applications |
| US9 - Messaging | P3 | US1 | Needs users |
| US10 - Notifications | P3 | US1 | Needs users, consumes events |
| US11 - Analytics | P3 | US1, US2 | Needs activity to measure |
| US12 - Admin | P3 | US1, US2 | Needs content to moderate |

### Parallel Opportunities

**Within Setup (Phase 1)**:
```
T002, T003, T004, T005 can run in parallel (different files)
```

**Within Foundational (Phase 2)**:
```
T008, T009, T010, T011 can run in parallel (shared value objects)
T019, T020 can run in parallel (config classes)
T022, T023 can run in parallel (exception classes)
```

**After Foundational - Story Independence**:
```
US1 + US2 can proceed in parallel (different modules)
Then US3 when both have data

US4, US5, US6 can proceed in parallel after foundational
US7, US8 can proceed in parallel after US3

US9, US10, US11, US12 can proceed in parallel after foundational
```

---

## Implementation Strategy

### MVP First (User Stories 1-3)

1. Complete Phase 1: Setup ✓
2. Complete Phase 2: Foundational ✓
3. Complete Phase 3: US1 - Registration/Profile
4. Complete Phase 4: US2 - Job Posting
5. Complete Phase 5: US3 - Search/Application
6. **STOP and VALIDATE**: Full job marketplace working
7. Deploy MVP

### Incremental AI Features (Stories 4-6)

1. Add US4: Resume Parsing → Auto-populate profiles
2. Add US5: Job Recommendations → AI for job seekers
3. Add US6: Candidate Matching → AI for employers
4. Each story independently testable and deployable

### Workflow & Communication (Stories 7-10)

1. Add US7: Application Management → Employer workflow
2. Add US8: Application Tracking → Job seeker visibility
3. Add US9: Messaging → Direct communication
4. Add US10: Notifications → Proactive alerts

### Platform Maturity (Stories 11-12)

1. Add US11: Analytics → Insights
2. Add US12: Admin → Governance
3. Polish phase for cross-cutting concerns

---

## Notes

- **[P]** tasks = different files, no dependencies on incomplete tasks
- **[USn]** label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Spring Modulith @ApplicationModuleListener for async event handling
- Use records for all DTOs, events, and value objects (JDK 25)
- Use sealed interfaces for status enums (JDK 25)
