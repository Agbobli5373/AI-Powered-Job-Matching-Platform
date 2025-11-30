# Research: AI-Powered Job Matching Platform

**Feature**: 001-ai-job-matching-platform  
**Date**: 2025-11-30  
**Status**: Complete

## Research Tasks

### 1. Spring Modulith 2.0 Best Practices

**Decision**: Use Spring Modulith 2.0 with event-driven architecture

**Rationale**:
- Native Spring Boot integration with minimal configuration
- Built-in module boundary verification at compile time
- Transactional outbox pattern for reliable event delivery
- `@ApplicationModuleListener` for async event handling with virtual threads
- Automatic documentation generation (PlantUML diagrams)

**Key Patterns**:
```java
// Module API: Public types at package root
// Internal: implementation in internal/ subpackage
// Events: Records for domain events
// Listeners: @ApplicationModuleListener for async processing
```

**Alternatives Considered**:
- Microservices: Rejected - premature complexity for MVP
- Manual modularization: Rejected - no enforcement

---

### 2. Spring AI for Resume Parsing & Embeddings

**Decision**: Spring AI with Mistral AI backend

**Rationale**:
- Native Spring integration via `spring-ai-mistralai-spring-boot-starter`
- Unified API for chat completions and embeddings
- Built-in retry and error handling
- Cost-effective with strong multilingual support
- European data sovereignty (EU-hosted infrastructure)

**Implementation Approach**:
```java
// Resume parsing: Mistral Large with structured output
@Service
class ResumeParser {
    private final ChatClient chatClient;
    
    ParsedResumeData parse(String resumeText) {
        return chatClient.prompt()
            .user(STR."Extract structured data from resume:\n\{resumeText}")
            .call()
            .entity(ParsedResumeData.class);
    }
}

// Embeddings: mistral-embed (1024 dimensions)
@Service
class EmbeddingService {
    private final EmbeddingModel embeddingModel;
    
    float[] embed(String text) {
        return embeddingModel.embed(text);
    }
}
```

**Alternatives Considered**:
- OpenAI: Rejected - higher cost, US data residency concerns
- Direct Mistral SDK: Rejected - less Spring integration
- LangChain4j: Considered - Spring AI more native

---

### 3. PostgreSQL pgvector for Vector Search

**Decision**: PostgreSQL 16 + pgvector extension

**Rationale**:
- Single database for relational + vector data
- Simpler operations than dedicated vector DB
- HNSW index for fast approximate nearest neighbor search
- Native Spring Data JPA support via custom queries

**Implementation Approach**:
```sql
-- Enable extension
CREATE EXTENSION vector;

-- Skills table with embeddings (1024 dims for mistral-embed)
CREATE TABLE skills (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    category VARCHAR(100),
    embedding VECTOR(1024)
);

-- Create HNSW index for fast similarity search
CREATE INDEX skills_embedding_idx ON skills 
USING hnsw (embedding vector_cosine_ops);

-- Query similar skills
SELECT name, 1 - (embedding <=> $1) as similarity
FROM skills
ORDER BY embedding <=> $1
LIMIT 10;
```

**Alternatives Considered**:
- Pinecone: Rejected - additional infrastructure
- Elasticsearch vectors: Rejected - PostgreSQL simpler for MVP

---

### 4. Elasticsearch 8 for Full-Text Search

**Decision**: Elasticsearch 8 with Spring Data Elasticsearch

**Rationale**:
- Proven full-text search with relevance scoring
- Rich filtering and aggregations
- Autocomplete and fuzzy matching support
- Spring Data Elasticsearch for repository pattern

**Implementation Approach**:
```java
@Document(indexName = "jobs")
public record JobSearchDocument(
    String id,
    String title,
    String description,
    String companyName,
    String location,
    List<String> skills,
    String remoteOption,
    BigDecimal salaryMin,
    BigDecimal salaryMax
) {}

interface JobSearchRepository extends ElasticsearchRepository<JobSearchDocument, String> {
    Page<JobSearchDocument> findByTitleContainingOrDescriptionContaining(
        String title, String description, Pageable pageable);
}
```

**Alternatives Considered**:
- PostgreSQL full-text: Rejected - less feature-rich
- Algolia: Rejected - cost at scale

---

### 5. JWT + OAuth2 Authentication

**Decision**: Spring Security 6 with JWT (access + refresh tokens) + OAuth2

**Rationale**:
- Stateless authentication for scalability
- Short-lived access tokens (15 min) + long-lived refresh tokens (7 days)
- OAuth2 for social login (Google, LinkedIn, GitHub)
- Role-based access control built-in

**Implementation Approach**:
```java
// JWT configuration
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) {
    return http
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/employer/**").hasRole("EMPLOYER")
            .anyRequest().authenticated())
        .build();
}

// Token pair record
record TokenPair(String accessToken, String refreshToken, Instant expiresAt) {}
```

**Alternatives Considered**:
- Session-based: Rejected - less scalable
- Auth0/Keycloak: Considered for production - MVP uses built-in

---

### 6. JDK 25 Features Usage

**Decision**: Leverage preview features for cleaner code

**Features to Use**:

| Feature | Usage |
|---------|-------|
| Records | Domain events, DTOs, value objects, API responses |
| Sealed Classes | `ApplicationStatus`, `JobStatus`, `UserRole`, `NotificationType` |
| Pattern Matching | Event handlers, status transitions, validation |
| Virtual Threads | `@ApplicationModuleListener`, AI API calls, async processing |
| Structured Concurrency | Parallel AI processing (skills + experience matching) |
| String Templates | Email templates, log messages, search queries |

**Example**:
```java
// Sealed interface with records
public sealed interface ApplicationStatus 
    permits Applied, UnderReview, Interviewed, Offered, Rejected, Withdrawn {
    
    record Applied(Instant appliedAt) implements ApplicationStatus {}
    record UnderReview(Instant reviewStarted) implements ApplicationStatus {}
    record Interviewed(Instant date, String feedback) implements ApplicationStatus {}
    record Offered(Money salary, Instant offerDate) implements ApplicationStatus {}
    record Rejected(String reason, Instant rejectedAt) implements ApplicationStatus {}
    record Withdrawn(Instant withdrawnAt) implements ApplicationStatus {}
}

// Pattern matching in switch
String formatStatus(ApplicationStatus status) {
    return switch (status) {
        case Applied(var at) -> STR."Applied on \{at.toString()}";
        case Offered(var salary, _) -> STR."Offered \{salary.format()}";
        case Rejected(var reason, _) -> STR."Rejected: \{reason}";
        default -> status.getClass().getSimpleName();
    };
}
```

---

### 7. AI Matching Algorithm Design

**Decision**: Multi-factor weighted matching with semantic skill similarity

**Algorithm Components**:

1. **Skill Matching (40% weight)**
   - Exact match: 100% score
   - Semantic match via embeddings: cosine similarity score
   - Must-have vs Nice-to-have weighting

2. **Experience Matching (25% weight)**
   - Years of experience alignment
   - Industry relevance scoring
   - Role progression analysis

3. **Location Matching (15% weight)**
   - Remote preference alignment
   - Geographic distance (if applicable)
   - Relocation willingness

4. **Salary Matching (15% weight)**
   - Range overlap calculation
   - Expectation vs offer alignment

5. **Education Matching (5% weight)**
   - Degree requirement fulfillment
   - Field relevance

**Implementation**:
```java
record MatchResult(
    UUID candidateId,
    UUID jobId,
    int overallScore,          // 0-100
    SkillMatchScore skills,
    ExperienceMatchScore experience,
    LocationMatchScore location,
    SalaryMatchScore salary,
    String explanation
) {}

record SkillMatchScore(
    int score,
    List<String> matchedSkills,
    List<String> missingMustHave,
    List<String> missingNiceToHave
) {}
```

---

### 8. Graceful Degradation Strategy

**Decision**: Fallback to keyword-based search when AI unavailable

**Rationale**: Platform must remain functional during AI service outages

**Implementation**:
```java
@Service
class MatchingService {
    
    MatchResult match(Profile profile, Job job) {
        try {
            return aiMatch(profile, job);
        } catch (AIServiceUnavailableException e) {
            log.warn("AI matching unavailable, falling back to keyword match");
            return keywordMatch(profile, job);
        }
    }
    
    private MatchResult keywordMatch(Profile profile, Job job) {
        // Simple skill name matching without embeddings
        var matchedSkills = profile.skills().stream()
            .filter(ps -> job.skills().stream()
                .anyMatch(js -> js.name().equalsIgnoreCase(ps.skill().name())))
            .toList();
        
        int score = (matchedSkills.size() * 100) / job.skills().size();
        return new MatchResult(/* ... with reduced confidence flag */);
    }
}
```

---

### 9. File Storage for Resumes

**Decision**: AWS S3 / MinIO with presigned URLs

**Rationale**:
- Scalable object storage
- Presigned URLs for secure direct upload/download
- MinIO for local development (S3-compatible)

**Implementation**:
```java
@Service
class ResumeStorageService {
    private final S3Client s3Client;
    
    String upload(MultipartFile file, UUID profileId) {
        String key = STR."resumes/\{profileId}/\{UUID.randomUUID()}.pdf";
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build(),
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        return key;
    }
    
    URL getPresignedUrl(String key, Duration duration) {
        return s3Presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build())
                .build()
        ).url();
    }
}
```

---

### 10. Event Publication & Reliability

**Decision**: Spring Modulith Event Publication Registry with JDBC

**Rationale**:
- Transactional outbox pattern ensures events are published
- Failed events automatically retried
- No additional message broker needed for MVP

**Configuration**:
```yaml
spring:
  modulith:
    events:
      jdbc:
        schema-initialization:
          enabled: true
      republish-outstanding-events-on-restart: true
      completion-mode: DELETE
```

**Event Flow**:
```
1. Business transaction + event insert (same TX)
2. TX commits
3. Event published to listeners
4. Listener completes → event marked complete
5. Listener fails → event retried with backoff
```

---

## Dependencies Summary

```xml
<!-- Spring Modulith -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jpa</artifactId>
</dependency>

<!-- Spring AI with Mistral -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mistralai-spring-boot-starter</artifactId>
</dependency>

<!-- PostgreSQL + pgvector -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
    <version>0.1.6</version>
</dependency>

<!-- Elasticsearch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>

<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- S3 -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```
