-- V5: Create profiles and related tables
-- Job seeker professional profiles with work experience, education, and skills

-- Availability enum
CREATE TYPE availability AS ENUM ('IMMEDIATE', 'TWO_WEEKS', 'ONE_MONTH', 'THREE_MONTHS', 'NOT_LOOKING');

-- Remote preference enum
CREATE TYPE remote_preference AS ENUM ('REMOTE_ONLY', 'HYBRID', 'ONSITE_ONLY', 'FLEXIBLE');

-- Proficiency level enum for skills
CREATE TYPE proficiency_level AS ENUM ('BEGINNER', 'INTERMEDIATE', 'EXPERT');

-- Access request status enum
CREATE TYPE access_request_status AS ENUM ('PENDING', 'APPROVED', 'DENIED');

-- Profiles table (job seeker profiles)
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    bio TEXT,
    -- Location (embedded)
    location_city VARCHAR(100),
    location_state VARCHAR(100),
    location_country VARCHAR(100),
    location_latitude DOUBLE PRECISION,
    location_longitude DOUBLE PRECISION,
    -- Salary expectations (embedded as Money)
    salary_expectation_min DECIMAL(12, 2),
    salary_expectation_max DECIMAL(12, 2),
    salary_currency VARCHAR(3) DEFAULT 'USD',
    -- Status fields
    availability availability,
    remote_preference remote_preference,
    profile_complete BOOLEAN NOT NULL DEFAULT FALSE,
    searchable BOOLEAN NOT NULL DEFAULT TRUE,
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for user lookup
CREATE INDEX idx_profiles_user_id ON profiles(user_id);

-- Index for searchable profiles
CREATE INDEX idx_profiles_searchable ON profiles(searchable) WHERE searchable = TRUE;

-- Index for filtering by availability
CREATE INDEX idx_profiles_availability ON profiles(availability) WHERE searchable = TRUE;

-- Index for filtering by remote preference
CREATE INDEX idx_profiles_remote_preference ON profiles(remote_preference) WHERE searchable = TRUE;

-- Work experience table
CREATE TABLE work_experiences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    company VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,  -- NULL means current position
    description TEXT,
    -- Location (embedded)
    location_city VARCHAR(100),
    location_state VARCHAR(100),
    location_country VARCHAR(100),
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for profile lookup
CREATE INDEX idx_work_experiences_profile_id ON work_experiences(profile_id);

-- Education table
CREATE TABLE educations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    institution VARCHAR(255) NOT NULL,
    degree VARCHAR(100),
    field VARCHAR(100),
    graduation_year INTEGER,
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for profile lookup
CREATE INDEX idx_educations_profile_id ON educations(profile_id);

-- Profile skills join table
CREATE TABLE profile_skills (
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    proficiency_level proficiency_level NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (profile_id, skill_id)
);

-- Index for skill lookup
CREATE INDEX idx_profile_skills_skill_id ON profile_skills(skill_id);

-- Profile access requests (for employers to request access to candidate data)
CREATE TABLE profile_access_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    employer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status access_request_status NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (profile_id, employer_id)
);

-- Index for profile lookup
CREATE INDEX idx_profile_access_requests_profile_id ON profile_access_requests(profile_id);

-- Index for employer lookup
CREATE INDEX idx_profile_access_requests_employer_id ON profile_access_requests(employer_id);

-- Index for pending requests
CREATE INDEX idx_profile_access_requests_pending ON profile_access_requests(profile_id, status) 
    WHERE status = 'PENDING';

-- Comments for documentation
COMMENT ON TABLE profiles IS 'Job seeker professional profiles';
COMMENT ON COLUMN profiles.profile_complete IS 'True when minimum required fields are filled';
COMMENT ON COLUMN profiles.searchable IS 'Whether profile appears in employer searches';
COMMENT ON TABLE work_experiences IS 'Employment history entries for profiles';
COMMENT ON COLUMN work_experiences.end_date IS 'NULL indicates current position';
COMMENT ON TABLE educations IS 'Education history entries for profiles';
COMMENT ON TABLE profile_skills IS 'Skills associated with profiles with proficiency levels';
COMMENT ON TABLE profile_access_requests IS 'Employer requests for access to candidate sensitive data';
