# Data Model: AI-Powered Job Matching Platform

**Feature**: 001-ai-job-matching-platform  
**Date**: 2025-11-30  
**Status**: Complete

## Entity Relationship Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USER MODULE                                     │
│  ┌─────────┐                                                                │
│  │  User   │──┬──────────────────────────────────────────────────────┐      │
│  │(Aggregate)│  │                                                     │      │
│  └─────────┘  │                                                       │      │
└───────────────┼───────────────────────────────────────────────────────┼──────┘
                │                                                       │
                ▼                                                       ▼
┌───────────────────────────────┐               ┌───────────────────────────────┐
│        PROFILE MODULE         │               │        COMPANY MODULE          │
│  ┌─────────┐                  │               │  ┌─────────┐                   │
│  │ Profile │◄─────────────────┼───────────────┼──│ Company │                   │
│  │(Aggregate)│                │               │  │(Aggregate)│                  │
│  └────┬────┘                  │               │  └────┬────┘                   │
│       │                       │               │       │                        │
│       ├──► WorkExperience     │               └───────┼────────────────────────┘
│       ├──► Education          │                       │
│       ├──► ProfileSkill ──────┼───────┐               │
│       └──► ProfileAccess      │       │               │
└───────────────────────────────┘       │               ▼
                                        │   ┌───────────────────────────────┐
                                        │   │         JOB MODULE            │
┌───────────────────────────────┐       │   │  ┌─────────┐                  │
│        SHARED MODULE          │       │   │  │   Job   │◄─────────────────┤
│  ┌─────────┐                  │       │   │  │(Aggregate)│                │
│  │  Skill  │◄─────────────────┼───────┤   │  └────┬────┘                  │
│  │(Entity) │                  │       │   │       │                       │
│  └─────────┘                  │       │   │       └──► JobSkill ──────────┤
│  ┌─────────┐                  │       │   └───────────────────────────────┘
│  │ Money   │ (Value Object)   │       │               │
│  └─────────┘                  │       │               │
│  ┌─────────┐                  │       │               ▼
│  │Location │ (Value Object)   │       │   ┌───────────────────────────────┐
│  └─────────┘                  │       │   │     APPLICATION MODULE        │
└───────────────────────────────┘       │   │  ┌─────────────┐              │
                                        │   │  │JobApplication│◄────────────┤
┌───────────────────────────────┐       │   │  │ (Aggregate)  │             │
│        RESUME MODULE          │       │   │  └─────────────┘              │
│  ┌─────────┐                  │       │   └───────────────────────────────┘
│  │ Resume  │◄─────────────────┼───────┘
│  │(Aggregate)│                │
│  └─────────┘                  │
└───────────────────────────────┘

┌───────────────────────────────┐   ┌───────────────────────────────┐
│      MESSAGING MODULE         │   │     NOTIFICATION MODULE       │
│  ┌─────────┐                  │   │  ┌──────────────┐             │
│  │ Message │                  │   │  │ Notification │             │
│  │(Aggregate)│                │   │  │  (Aggregate) │             │
│  └─────────┘                  │   │  └──────────────┘             │
│  ┌─────────────┐              │   │  ┌──────────────────────┐     │
│  │Conversation │              │   │  │NotificationPreference│     │
│  │ (Aggregate) │              │   │  └──────────────────────┘     │
│  └─────────────┘              │   └───────────────────────────────┘
└───────────────────────────────┘
```

---

## Entities by Module

### SHARED Module

#### Skill
Platform-wide skill definition with semantic relationships.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| name | String | UNIQUE, NOT NULL, max 100 | Skill name (e.g., "Java") |
| category | String | max 100 | Category (e.g., "Programming Language") |
| embedding | float[1536] | nullable | Vector embedding for semantic search |
| createdAt | Instant | NOT NULL | Creation timestamp |

**Relationships**: Referenced by ProfileSkill, JobSkill

---

#### Money (Value Object)
```java
public record Money(BigDecimal amount, String currency) {
    public Money {
        Objects.requireNonNull(amount);
        currency = currency == null ? "USD" : currency;
    }
    
    public String format() {
        return STR."\{currency} \{amount.setScale(2, RoundingMode.HALF_UP)}";
    }
}
```

#### Location (Value Object)
```java
public record Location(
    String city,
    String state,
    String country,
    Double latitude,
    Double longitude
) {}
```

#### DateRange (Value Object)
```java
public record DateRange(LocalDate startDate, LocalDate endDate) {
    public boolean isCurrent() {
        return endDate == null;
    }
    
    public Period duration() {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return Period.between(startDate, end);
    }
}
```

---

### USER Module

#### User (Aggregate Root)
Platform user with authentication credentials.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| email | String | UNIQUE, NOT NULL, max 255 | Email address |
| passwordHash | String | nullable | Hashed password (null for OAuth) |
| role | UserRole | NOT NULL | JOB_SEEKER, EMPLOYER, ADMIN |
| status | UserStatus | NOT NULL | PENDING, ACTIVE, SUSPENDED |
| emailVerified | boolean | NOT NULL, default false | Email verification status |
| createdAt | Instant | NOT NULL | Registration timestamp |
| lastLoginAt | Instant | nullable | Last login timestamp |

**Relationships**: 
- One-to-One with Profile (for JOB_SEEKER)
- One-to-One with Company (for EMPLOYER)

#### UserRole (Sealed Interface)
```java
public sealed interface UserRole permits JobSeeker, Employer, Admin {
    record JobSeeker() implements UserRole {}
    record Employer() implements UserRole {}
    record Admin() implements UserRole {}
}
```

#### UserStatus (Enum)
```java
public enum UserStatus { PENDING, ACTIVE, SUSPENDED }
```

---

### PROFILE Module

#### Profile (Aggregate Root)
Job seeker's professional profile.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| userId | UUID | FK → User, UNIQUE | Owner user |
| firstName | String | max 100 | First name |
| lastName | String | max 100 | Last name |
| bio | String | TEXT | Professional summary |
| location | Location | embedded | Current location |
| salaryExpectation | Money | embedded | Min salary expectation |
| salaryExpectationMax | Money | embedded | Max salary expectation |
| availability | Availability | NOT NULL | Availability status |
| remotePreference | RemotePreference | | Remote work preference |
| profileComplete | boolean | default false | Profile completion status |
| searchable | boolean | default true | Visible to employers |
| createdAt | Instant | NOT NULL | Creation timestamp |
| updatedAt | Instant | NOT NULL | Last update timestamp |

**Relationships**:
- One-to-Many with WorkExperience
- One-to-Many with Education
- Many-to-Many with Skill (via ProfileSkill)
- One-to-Many with Resume
- One-to-Many with ProfileAccessRequest

#### WorkExperience
Employment history entry.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| profileId | UUID | FK → Profile, NOT NULL | Parent profile |
| company | String | NOT NULL, max 255 | Company name |
| role | String | NOT NULL, max 255 | Job title |
| dateRange | DateRange | embedded, NOT NULL | Employment period |
| description | String | TEXT | Role description |
| location | Location | embedded | Office location |

#### Education
Academic credential.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| profileId | UUID | FK → Profile, NOT NULL | Parent profile |
| institution | String | NOT NULL, max 255 | School/University |
| degree | String | max 100 | Degree type |
| field | String | max 100 | Field of study |
| graduationYear | Integer | | Year of graduation |

#### ProfileSkill
Association between Profile and Skill.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| profileId | UUID | PK, FK → Profile | Profile reference |
| skillId | UUID | PK, FK → Skill | Skill reference |
| proficiencyLevel | ProficiencyLevel | NOT NULL | BEGINNER, INTERMEDIATE, EXPERT |

#### ProficiencyLevel (Enum)
```java
public enum ProficiencyLevel { BEGINNER, INTERMEDIATE, EXPERT }
```

#### Availability (Enum)
```java
public enum Availability { IMMEDIATE, TWO_WEEKS, ONE_MONTH, THREE_MONTHS, NOT_LOOKING }
```

#### RemotePreference (Enum)
```java
public enum RemotePreference { REMOTE_ONLY, HYBRID, ONSITE_ONLY, FLEXIBLE }
```

#### ProfileAccessRequest
Employer request for candidate sensitive data.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| profileId | UUID | FK → Profile | Target profile |
| employerId | UUID | FK → User | Requesting employer |
| status | AccessRequestStatus | NOT NULL | PENDING, APPROVED, DENIED |
| requestedAt | Instant | NOT NULL | Request timestamp |
| respondedAt | Instant | nullable | Response timestamp |

---

### COMPANY Module

#### Company (Aggregate Root)
Employer's organization.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| userId | UUID | FK → User, UNIQUE | Owner user |
| name | String | NOT NULL, max 255 | Company name |
| description | String | TEXT | Company description |
| industry | String | max 100 | Industry sector |
| size | CompanySize | | Company size range |
| logoUrl | String | max 500 | Logo URL |
| websiteUrl | String | max 500 | Website URL |
| locations | List<Location> | embedded | Office locations |
| verified | boolean | default false | Verification status |
| createdAt | Instant | NOT NULL | Creation timestamp |

#### CompanySize (Enum)
```java
public enum CompanySize { 
    STARTUP_1_10, SMALL_11_50, MEDIUM_51_200, 
    LARGE_201_1000, ENTERPRISE_1001_PLUS 
}
```

---

### JOB Module

#### Job (Aggregate Root)
Job posting.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| companyId | UUID | FK → Company, NOT NULL | Posting company |
| title | String | NOT NULL, max 255 | Job title |
| description | String | TEXT, NOT NULL | Job description |
| location | Location | embedded | Job location |
| remoteOption | RemoteOption | NOT NULL | REMOTE, HYBRID, ONSITE |
| salaryMin | Money | embedded | Minimum salary |
| salaryMax | Money | embedded | Maximum salary |
| salaryVisible | boolean | default true | Show salary to candidates |
| experienceLevel | ExperienceLevel | NOT NULL | Required experience |
| employmentType | EmploymentType | NOT NULL | Employment type |
| status | JobStatus | NOT NULL | Current status |
| postedAt | Instant | | Publication timestamp |
| deadline | LocalDate | nullable | Application deadline |
| createdAt | Instant | NOT NULL | Creation timestamp |
| updatedAt | Instant | NOT NULL | Last update timestamp |

**Relationships**:
- Many-to-Many with Skill (via JobSkill)
- One-to-Many with JobApplication

#### JobSkill
Association between Job and required Skill.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| jobId | UUID | PK, FK → Job | Job reference |
| skillId | UUID | PK, FK → Skill | Skill reference |
| importance | SkillImportance | NOT NULL | MUST_HAVE, NICE_TO_HAVE |

#### JobStatus (Sealed Interface)
```java
public sealed interface JobStatus 
    permits Draft, Active, Paused, Closed {
    
    record Draft(Instant createdAt) implements JobStatus {}
    record Active(Instant publishedAt) implements JobStatus {}
    record Paused(Instant pausedAt, String reason) implements JobStatus {}
    record Closed(Instant closedAt, CloseReason reason) implements JobStatus {}
}

public enum CloseReason { FILLED, CANCELLED, EXPIRED }
```

#### RemoteOption (Enum)
```java
public enum RemoteOption { REMOTE, HYBRID, ONSITE }
```

#### ExperienceLevel (Enum)
```java
public enum ExperienceLevel { ENTRY, MID, SENIOR, LEAD, EXECUTIVE }
```

#### EmploymentType (Enum)
```java
public enum EmploymentType { FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, FREELANCE }
```

#### SkillImportance (Enum)
```java
public enum SkillImportance { MUST_HAVE, NICE_TO_HAVE }
```

---

### APPLICATION Module

#### JobApplication (Aggregate Root)
Job seeker's application to a job.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| jobId | UUID | FK → Job, NOT NULL | Target job |
| profileId | UUID | FK → Profile, NOT NULL | Applicant profile |
| resumeId | UUID | FK → Resume | Selected resume |
| status | ApplicationStatus | NOT NULL | Current status |
| matchScore | Integer | 0-100 | AI match score |
| matchBreakdown | MatchScoreBreakdown | JSONB | Detailed scoring |
| appliedAt | Instant | NOT NULL | Application timestamp |
| updatedAt | Instant | NOT NULL | Last update timestamp |

**Constraints**: UNIQUE(jobId, profileId)

#### ApplicationStatus (Sealed Interface)
```java
public sealed interface ApplicationStatus 
    permits Applied, UnderReview, Interviewed, Offered, Rejected, Withdrawn {
    
    record Applied(Instant appliedAt) implements ApplicationStatus {}
    record UnderReview(Instant reviewStarted) implements ApplicationStatus {}
    record Interviewed(Instant interviewDate, String feedback) implements ApplicationStatus {}
    record Offered(Money salary, Instant offerDate, LocalDate responseDeadline) implements ApplicationStatus {}
    record Rejected(String reason, Instant rejectedAt) implements ApplicationStatus {}
    record Withdrawn(Instant withdrawnAt) implements ApplicationStatus {}
    
    Instant timestamp();
}
```

#### MatchScoreBreakdown (Value Object)
```java
public record MatchScoreBreakdown(
    int skillsScore,
    int experienceScore,
    int locationScore,
    int salaryScore,
    int educationScore,
    List<String> matchedSkills,
    List<String> missingMustHave,
    List<String> missingNiceToHave,
    String explanation
) {}
```

---

### RESUME Module

#### Resume (Aggregate Root)
Uploaded resume with parsed content.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| profileId | UUID | FK → Profile, NOT NULL | Owner profile |
| fileName | String | NOT NULL, max 255 | Original filename |
| fileUrl | String | NOT NULL, max 500 | S3/MinIO storage URL |
| fileSize | Long | NOT NULL | File size in bytes |
| contentType | String | NOT NULL | MIME type |
| parsedContent | ParsedResumeData | JSONB | AI-extracted data |
| parseStatus | ParseStatus | NOT NULL | PENDING, COMPLETED, FAILED |
| isDefault | boolean | default false | Default resume for applications |
| createdAt | Instant | NOT NULL | Upload timestamp |

#### ParsedResumeData (Value Object)
```java
public record ParsedResumeData(
    String fullName,
    String email,
    String phone,
    String summary,
    List<ParsedWorkExperience> workExperiences,
    List<ParsedEducation> education,
    List<String> skills,
    List<String> certifications,
    String rawText
) {}
```

#### ParseStatus (Enum)
```java
public enum ParseStatus { PENDING, PROCESSING, COMPLETED, FAILED }
```

---

### MESSAGING Module

#### Message
Direct message between users.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| conversationId | UUID | FK → Conversation | Parent conversation |
| senderId | UUID | FK → User, NOT NULL | Message sender |
| content | String | TEXT, NOT NULL | Message content |
| attachmentUrl | String | max 500 | Attachment URL |
| readAt | Instant | nullable | Read timestamp |
| createdAt | Instant | NOT NULL | Send timestamp |

#### Conversation
Message thread between two users.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| participant1Id | UUID | FK → User, NOT NULL | First participant |
| participant2Id | UUID | FK → User, NOT NULL | Second participant |
| jobId | UUID | FK → Job, nullable | Related job (if any) |
| lastMessageAt | Instant | | Last message timestamp |
| createdAt | Instant | NOT NULL | Creation timestamp |

**Constraints**: UNIQUE(participant1Id, participant2Id)

---

### NOTIFICATION Module

#### Notification
User notification.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| userId | UUID | FK → User, NOT NULL | Target user |
| type | NotificationType | NOT NULL | Notification type |
| title | String | NOT NULL, max 255 | Notification title |
| content | String | TEXT | Notification content |
| data | JSONB | | Additional context data |
| readAt | Instant | nullable | Read timestamp |
| createdAt | Instant | NOT NULL | Creation timestamp |

#### NotificationType (Sealed Interface)
```java
public sealed interface NotificationType permits
    NewJobMatch, ApplicationStatusChange, NewApplication,
    NewMessage, AccessRequestReceived, AccessRequestResponse {
    
    record NewJobMatch(UUID jobId, int matchScore) implements NotificationType {}
    record ApplicationStatusChange(UUID applicationId, String newStatus) implements NotificationType {}
    record NewApplication(UUID applicationId, UUID jobId) implements NotificationType {}
    record NewMessage(UUID conversationId, UUID senderId) implements NotificationType {}
    record AccessRequestReceived(UUID requestId, UUID employerId) implements NotificationType {}
    record AccessRequestResponse(UUID requestId, boolean approved) implements NotificationType {}
}
```

#### NotificationPreferences
User notification settings.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| userId | UUID | FK → User, UNIQUE | Target user |
| emailEnabled | boolean | default true | Email notifications |
| pushEnabled | boolean | default true | Push notifications |
| inAppEnabled | boolean | default true | In-app notifications |
| jobMatchDigest | DigestFrequency | default DAILY | Job match digest |
| applicationUpdates | boolean | default true | Application updates |
| messageNotifications | boolean | default true | Message alerts |

#### DigestFrequency (Enum)
```java
public enum DigestFrequency { IMMEDIATE, DAILY, WEEKLY, NONE }
```

---

## Database Schema (PostgreSQL)

```sql
-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- USER MODULE
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- SHARED MODULE
CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    category VARCHAR(100),
    embedding VECTOR(1536),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_skills_name ON skills(name);
CREATE INDEX idx_skills_embedding ON skills USING hnsw (embedding vector_cosine_ops);

-- PROFILE MODULE
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    bio TEXT,
    location_city VARCHAR(100),
    location_state VARCHAR(100),
    location_country VARCHAR(100),
    location_latitude DOUBLE PRECISION,
    location_longitude DOUBLE PRECISION,
    salary_expectation_min DECIMAL(12,2),
    salary_expectation_max DECIMAL(12,2),
    salary_currency VARCHAR(3) DEFAULT 'USD',
    availability VARCHAR(50),
    remote_preference VARCHAR(50),
    profile_complete BOOLEAN DEFAULT FALSE,
    searchable BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profiles_user_id ON profiles(user_id);
CREATE INDEX idx_profiles_searchable ON profiles(searchable) WHERE searchable = TRUE;

CREATE TABLE work_experiences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    company VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    description TEXT,
    location_city VARCHAR(100),
    location_country VARCHAR(100)
);

CREATE INDEX idx_work_experiences_profile ON work_experiences(profile_id);

CREATE TABLE educations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    institution VARCHAR(255) NOT NULL,
    degree VARCHAR(100),
    field VARCHAR(100),
    graduation_year INTEGER
);

CREATE INDEX idx_educations_profile ON educations(profile_id);

CREATE TABLE profile_skills (
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    proficiency_level VARCHAR(20) NOT NULL,
    PRIMARY KEY (profile_id, skill_id)
);

CREATE TABLE profile_access_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    employer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP,
    UNIQUE (profile_id, employer_id)
);

-- COMPANY MODULE
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    industry VARCHAR(100),
    size VARCHAR(50),
    logo_url VARCHAR(500),
    website_url VARCHAR(500),
    locations JSONB,
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_companies_user_id ON companies(user_id);
CREATE INDEX idx_companies_industry ON companies(industry);

-- JOB MODULE
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    location_city VARCHAR(100),
    location_state VARCHAR(100),
    location_country VARCHAR(100),
    remote_option VARCHAR(50) NOT NULL,
    salary_min DECIMAL(12,2),
    salary_max DECIMAL(12,2),
    salary_currency VARCHAR(3) DEFAULT 'USD',
    salary_visible BOOLEAN DEFAULT TRUE,
    experience_level VARCHAR(50) NOT NULL,
    employment_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    status_data JSONB,
    posted_at TIMESTAMP,
    deadline DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_company ON jobs(company_id);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_posted_at ON jobs(posted_at DESC) WHERE status = 'ACTIVE';

CREATE TABLE job_skills (
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    importance VARCHAR(20) NOT NULL,
    PRIMARY KEY (job_id, skill_id)
);

-- APPLICATION MODULE
CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    resume_id UUID,
    status VARCHAR(50) NOT NULL,
    status_data JSONB,
    match_score INTEGER CHECK (match_score >= 0 AND match_score <= 100),
    match_breakdown JSONB,
    applied_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (job_id, profile_id)
);

CREATE INDEX idx_applications_job ON applications(job_id);
CREATE INDEX idx_applications_profile ON applications(profile_id);
CREATE INDEX idx_applications_status ON applications(status);

-- RESUME MODULE
CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    parsed_content JSONB,
    parse_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resumes_profile ON resumes(profile_id);

-- Add FK to applications
ALTER TABLE applications ADD CONSTRAINT fk_applications_resume 
    FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE SET NULL;

-- MESSAGING MODULE
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    participant1_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    participant2_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id UUID REFERENCES jobs(id) ON DELETE SET NULL,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (participant1_id, participant2_id)
);

CREATE INDEX idx_conversations_participants ON conversations(participant1_id, participant2_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    attachment_url VARCHAR(500),
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at DESC);

-- NOTIFICATION MODULE
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    data JSONB,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id) WHERE read_at IS NULL;

CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email_enabled BOOLEAN DEFAULT TRUE,
    push_enabled BOOLEAN DEFAULT TRUE,
    in_app_enabled BOOLEAN DEFAULT TRUE,
    job_match_digest VARCHAR(50) DEFAULT 'DAILY',
    application_updates BOOLEAN DEFAULT TRUE,
    message_notifications BOOLEAN DEFAULT TRUE
);

-- SPRING MODULITH EVENT PUBLICATION
CREATE TABLE event_publication (
    id UUID PRIMARY KEY,
    listener_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP NOT NULL,
    completion_date TIMESTAMP
);

CREATE INDEX idx_event_publication_incomplete ON event_publication(publication_date) 
    WHERE completion_date IS NULL;
```

---

## Validation Rules

### User
- Email must be valid format
- Password must be min 8 characters with uppercase, lowercase, number

### Profile
- First name and last name required for profile completion
- At least one skill required for searchable status
- Salary expectation max >= min

### Job
- Title max 255 characters
- At least one must-have skill required
- Salary max >= min
- Deadline must be future date

### Application
- Cannot apply to same job twice
- Cannot apply to closed jobs
- Cannot apply to own company's jobs

### Resume
- Max file size 10MB
- Allowed formats: PDF, DOCX
