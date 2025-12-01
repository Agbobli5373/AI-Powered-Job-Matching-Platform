-- V7: Create jobs and related tables
-- Job postings with skills and requirements

-- Remote option enum
CREATE TYPE remote_option AS ENUM ('REMOTE', 'HYBRID', 'ONSITE');

-- Experience level enum
CREATE TYPE experience_level AS ENUM ('ENTRY', 'MID', 'SENIOR', 'LEAD', 'EXECUTIVE');

-- Employment type enum
CREATE TYPE employment_type AS ENUM ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'FREELANCE');

-- Job status enum
CREATE TYPE job_status AS ENUM ('DRAFT', 'ACTIVE', 'PAUSED', 'CLOSED');

-- Close reason enum
CREATE TYPE close_reason AS ENUM ('FILLED', 'CANCELLED', 'EXPIRED');

-- Skill importance enum
CREATE TYPE skill_importance AS ENUM ('MUST_HAVE', 'NICE_TO_HAVE');

-- Jobs table
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    -- Location (embedded)
    location_city VARCHAR(100),
    location_state VARCHAR(100),
    location_country VARCHAR(100),
    location_latitude DOUBLE PRECISION,
    location_longitude DOUBLE PRECISION,
    -- Job type settings
    remote_option remote_option NOT NULL,
    experience_level experience_level NOT NULL,
    employment_type employment_type NOT NULL,
    -- Salary (embedded as Money)
    salary_min DECIMAL(12, 2),
    salary_max DECIMAL(12, 2),
    salary_currency VARCHAR(3) DEFAULT 'USD',
    salary_visible BOOLEAN NOT NULL DEFAULT TRUE,
    -- Status
    status job_status NOT NULL DEFAULT 'DRAFT',
    status_data JSONB,  -- Additional status metadata (pause reason, close reason, etc.)
    -- Dates
    posted_at TIMESTAMP WITH TIME ZONE,
    deadline DATE,
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for company lookup
CREATE INDEX idx_jobs_company_id ON jobs(company_id);

-- Index for status filtering
CREATE INDEX idx_jobs_status ON jobs(status);

-- Index for active jobs ordered by posted date (most common query)
CREATE INDEX idx_jobs_active_posted ON jobs(posted_at DESC) WHERE status = 'ACTIVE';

-- Index for location search
CREATE INDEX idx_jobs_location ON jobs(location_country, location_state, location_city) WHERE status = 'ACTIVE';

-- Index for remote jobs
CREATE INDEX idx_jobs_remote ON jobs(remote_option) WHERE status = 'ACTIVE';

-- Index for experience level filtering
CREATE INDEX idx_jobs_experience ON jobs(experience_level) WHERE status = 'ACTIVE';

-- Index for employment type filtering
CREATE INDEX idx_jobs_employment_type ON jobs(employment_type) WHERE status = 'ACTIVE';

-- Full-text search index on title and description
CREATE INDEX idx_jobs_search ON jobs USING gin(to_tsvector('english', title || ' ' || description));

-- Job skills join table
CREATE TABLE job_skills (
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    importance skill_importance NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (job_id, skill_id)
);

-- Index for skill lookup (find jobs requiring a specific skill)
CREATE INDEX idx_job_skills_skill_id ON job_skills(skill_id);

-- Index for must-have skills (for matching)
CREATE INDEX idx_job_skills_must_have ON job_skills(job_id, skill_id) WHERE importance = 'MUST_HAVE';

-- Comments for documentation
COMMENT ON TABLE jobs IS 'Job postings from employers';
COMMENT ON COLUMN jobs.status IS 'DRAFT: not published, ACTIVE: live posting, PAUSED: temporarily hidden, CLOSED: no longer accepting applications';
COMMENT ON COLUMN jobs.status_data IS 'Additional status metadata like pause reason or close reason';
COMMENT ON COLUMN jobs.posted_at IS 'Timestamp when job was first published (status changed to ACTIVE)';
COMMENT ON COLUMN jobs.deadline IS 'Optional application deadline';
COMMENT ON COLUMN jobs.salary_visible IS 'Whether salary range is shown to candidates';
COMMENT ON TABLE job_skills IS 'Skills required or preferred for job postings';
COMMENT ON COLUMN job_skills.importance IS 'MUST_HAVE: required skill, NICE_TO_HAVE: preferred but not required';
