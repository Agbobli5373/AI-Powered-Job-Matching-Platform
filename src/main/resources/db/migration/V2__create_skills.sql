-- V2: Create skills table
-- Platform-wide skills with semantic embeddings for AI matching

CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(100),
    -- Vector embedding for semantic similarity search (1024 dims for mistral-embed)
    embedding VECTOR(1024),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for name lookups
CREATE INDEX idx_skills_name ON skills(LOWER(name));

-- Index for category filtering
CREATE INDEX idx_skills_category ON skills(category);

-- HNSW index for fast approximate nearest neighbor search on embeddings
-- Uses cosine similarity (vector_cosine_ops)
CREATE INDEX idx_skills_embedding ON skills 
USING hnsw (embedding vector_cosine_ops)
WHERE embedding IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE skills IS 'Platform-wide skill definitions with semantic embeddings';
COMMENT ON COLUMN skills.embedding IS 'Vector embedding for semantic skill matching (1024 dimensions)';
COMMENT ON COLUMN skills.category IS 'Skill category (e.g., Programming Language, Framework, Soft Skill)';
