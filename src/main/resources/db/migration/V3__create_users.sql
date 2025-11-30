-- V3: Create users table
-- Platform users with authentication and role information

-- User status enum
CREATE TYPE user_status AS ENUM ('PENDING', 'ACTIVE', 'SUSPENDED');

-- User role enum
CREATE TYPE user_role AS ENUM ('JOB_SEEKER', 'EMPLOYER', 'ADMIN');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),  -- NULL for OAuth-only users
    role user_role NOT NULL,
    status user_status NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMP WITH TIME ZONE
);

-- Index for email lookups (used in authentication)
CREATE INDEX idx_users_email ON users(LOWER(email));

-- Index for filtering by role
CREATE INDEX idx_users_role ON users(role);

-- Index for filtering by status
CREATE INDEX idx_users_status ON users(status);

-- Composite index for active users by role (common query)
CREATE INDEX idx_users_active_role ON users(role, status) WHERE status = 'ACTIVE';

-- Add comments for documentation
COMMENT ON TABLE users IS 'Platform users with authentication credentials';
COMMENT ON COLUMN users.password_hash IS 'Bcrypt hashed password (NULL for OAuth users)';
COMMENT ON COLUMN users.email_verified IS 'Whether email has been verified via confirmation link';
COMMENT ON COLUMN users.last_login_at IS 'Timestamp of last successful login';
