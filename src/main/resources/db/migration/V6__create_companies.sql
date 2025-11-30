-- V6: Create companies table
-- Company entity for employer organizations

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
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index for finding company by owner
CREATE INDEX idx_companies_user_id ON companies(user_id);

-- Index for filtering by industry
CREATE INDEX idx_companies_industry ON companies(industry);

-- Index for filtering verified companies
CREATE INDEX idx_companies_verified ON companies(verified) WHERE verified = TRUE;

-- Index for searching company names
CREATE INDEX idx_companies_name ON companies USING gin(to_tsvector('english', name));

COMMENT ON TABLE companies IS 'Employer company profiles';
COMMENT ON COLUMN companies.user_id IS 'Owner user (must have EMPLOYER role)';
COMMENT ON COLUMN companies.size IS 'Company size enum: STARTUP_1_10, SMALL_11_50, MEDIUM_51_200, LARGE_201_1000, ENTERPRISE_1001_PLUS';
COMMENT ON COLUMN companies.locations IS 'JSONB array of location objects with city, state, country, latitude, longitude';
COMMENT ON COLUMN companies.verified IS 'Whether the company has been verified by admin';
