-- V1: Enable PostgreSQL extensions
-- Required extensions for the Job Matching Platform

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable vector operations for semantic search (pgvector)
CREATE EXTENSION IF NOT EXISTS vector;
