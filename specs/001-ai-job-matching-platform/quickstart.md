# Job Matching Platform - Quickstart Guide

## Prerequisites

- **JDK 25** (with preview features enabled)
- **Docker & Docker Compose** (for local services)
- **Maven 3.9+** or use included wrapper (`./mvnw`)
- **IDE**: IntelliJ IDEA 2024.3+ or VS Code with Java extensions
- **Mistral AI API Key** (for AI features)

## Initial Setup

### 1. Clone and Navigate

```bash
cd job-matching
```

### 2. Start Local Services

```bash
# Start PostgreSQL, Redis, and Elasticsearch
docker-compose up -d

# Verify services are running
docker-compose ps
```

**docker-compose.yml** (create if not exists):
```yaml
version: '3.8'
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: jobmatching
      POSTGRES_USER: jobmatching
      POSTGRES_PASSWORD: localdev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.12.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

volumes:
  postgres_data:
  es_data:
  minio_data:
```

### 3. Configure Environment

Create `src/main/resources/application-local.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/jobmatching
    username: jobmatching
    password: localdev
  
  data:
    redis:
      host: localhost
      port: 6379
    elasticsearch:
      uris: http://localhost:9200

  ai:
    mistralai:
      api-key: ${MISTRAL_API_KEY}

storage:
  type: minio
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: job-matching-files
```

### 4. Set Environment Variables

```bash
# Linux/Mac
export MISTRAL_API_KEY=your-mistral-api-key-here

# Windows PowerShell
$env:MISTRAL_API_KEY="your-mistral-api-key-here"

# Windows CMD
set MISTRAL_API_KEY=your-mistral-api-key-here
```

### 5. Build and Run

```bash
# Build with preview features
./mvnw clean compile -Djava.enable.preview=true

# Run with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="--enable-preview"
```

Application starts at: **http://localhost:8080**

## Verify Installation

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "elasticsearch": { "status": "UP" }
  }
}
```

### API Documentation

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Spec: http://localhost:8080/v3/api-docs

## Project Structure

```
src/main/java/com/isaac/job_matching/
├── JobMatchingApplication.java          # Main entry point
├── shared/                              # Cross-cutting concerns
│   ├── config/                          # Configuration classes
│   ├── security/                        # Security configuration
│   └── exception/                       # Global exception handling
├── user/                                # User module
│   ├── api/                             # REST controllers
│   ├── domain/                          # Entities, repositories
│   └── service/                         # Business logic
├── profile/                             # Candidate profile module
├── job/                                 # Job posting module
├── company/                             # Company module
├── application/                         # Job application module
├── matching/                            # AI matching module
├── resume/                              # Resume processing module
├── notification/                        # Notification module
├── messaging/                           # In-app messaging module
├── search/                              # Search module
└── analytics/                           # Analytics module
```

## Module Conventions

Each module follows Spring Modulith conventions:

```java
// Module API - public interface
@ApplicationModuleListener
public class JobEventListener {
    @EventListener
    public void on(JobPostedEvent event) { ... }
}

// Internal implementation - package-private
class JobRepositoryImpl implements JobRepository { ... }
```

### Module Dependencies

```
shared → (no dependencies)
user → shared
profile → user, shared
company → user, shared
job → company, shared
application → job, profile, shared
matching → job, profile, shared
resume → profile, shared
notification → user, shared
messaging → user, shared
search → job, profile, company, shared
analytics → shared
```

## Common Development Tasks

### Create a New Module

```bash
mkdir -p src/main/java/com/isaac/job_matching/newmodule/{api,domain,service}
```

### Run Tests

```bash
# All tests
./mvnw test

# Specific module
./mvnw test -Dtest="**/profile/**"

# With coverage
./mvnw test jacoco:report
```

### Database Migrations

Using Flyway (migrations in `src/main/resources/db/migration/`):

```bash
# Create new migration
touch src/main/resources/db/migration/V002__add_skills_table.sql

# Run migrations
./mvnw flyway:migrate
```

### Generate API Client

```bash
# Generate TypeScript client from OpenAPI spec
npx openapi-typescript-codegen \
  --input http://localhost:8080/v3/api-docs \
  --output ./frontend/src/api
```

## Debugging

### Enable Debug Logging

```yaml
# application-local.yaml
logging:
  level:
    com.isaac.job_matching: DEBUG
    org.springframework.ai: DEBUG
    org.hibernate.SQL: DEBUG
```

### View Module Structure

```java
// Add to test class
@SpringBootTest
class ModuleStructureTest {
    @Autowired
    ApplicationModules modules;
    
    @Test
    void verifyModuleStructure() {
        modules.verify();
    }
}
```

## Troubleshooting

### PostgreSQL pgvector Extension

If vector operations fail:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Elasticsearch Connection

Verify ES is running:
```bash
curl http://localhost:9200/_cluster/health
```

### Redis Connection

Test Redis:
```bash
docker exec -it job-matching-redis-1 redis-cli ping
```

### JDK Preview Features

Ensure preview features are enabled in IDE:
- IntelliJ: Settings → Build → Compiler → Java Compiler → Additional command line parameters: `--enable-preview`
- VS Code: In `settings.json`: `"java.compile.vmargs": "--enable-preview"`

## Next Steps

1. **Phase 1**: Implement User, Profile, and Company modules
2. **Phase 2**: Implement Job, Application, and Search modules  
3. **Phase 3**: Implement AI Matching and Resume parsing
4. **Phase 4**: Implement Notifications and Messaging
5. **Phase 5**: Polish and optimize

See `plan.md` for detailed phase breakdown and tasks.
