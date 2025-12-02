-- V8__create_applications.sql
-- Create job applications table for job seekers to apply to jobs

CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    cover_letter TEXT,
    resume_url VARCHAR(500),
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Ensure one application per user per job
    UNIQUE(job_id, user_id),

    -- Indexes for performance
    INDEX idx_applications_job_id (job_id),
    INDEX idx_applications_user_id (user_id),
    INDEX idx_applications_status (status),
    INDEX idx_applications_applied_at (applied_at)
);

-- Add comments for documentation
COMMENT ON TABLE applications IS 'Job applications submitted by job seekers';
COMMENT ON COLUMN applications.id IS 'Unique identifier for the application';
COMMENT ON COLUMN applications.job_id IS 'Reference to the job being applied for';
COMMENT ON COLUMN applications.user_id IS 'Reference to the job seeker submitting the application';
COMMENT ON COLUMN applications.status IS 'Current status of the application (PENDING, REVIEWED, ACCEPTED, REJECTED, WITHDRAWN)';
COMMENT ON COLUMN applications.cover_letter IS 'Optional cover letter from the job seeker';
COMMENT ON COLUMN applications.resume_url IS 'URL to the job seeker''s resume';
COMMENT ON COLUMN applications.applied_at IS 'Timestamp when the application was submitted';
COMMENT ON COLUMN applications.updated_at IS 'Timestamp when the application was last updated';