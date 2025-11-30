-- V4: Create Spring Modulith event publication table
-- Required for reliable event-driven communication between modules

-- This table stores domain events that need to be published to other modules
-- Spring Modulith uses this for the transactional outbox pattern
CREATE TABLE event_publication (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    listener_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE
);

-- Index for finding incomplete publications (outbox pattern)
CREATE INDEX idx_event_publication_incomplete 
ON event_publication(publication_date) 
WHERE completion_date IS NULL;

-- Index for cleanup of old completed events
CREATE INDEX idx_event_publication_completion 
ON event_publication(completion_date) 
WHERE completion_date IS NOT NULL;

-- Index for finding events by type
CREATE INDEX idx_event_publication_type ON event_publication(event_type);

-- Add comments for documentation
COMMENT ON TABLE event_publication IS 'Spring Modulith event publication outbox for reliable messaging';
COMMENT ON COLUMN event_publication.listener_id IS 'Identifier of the listener that should process this event';
COMMENT ON COLUMN event_publication.event_type IS 'Fully qualified class name of the event';
COMMENT ON COLUMN event_publication.serialized_event IS 'JSON serialized event payload';
COMMENT ON COLUMN event_publication.completion_date IS 'When the event was successfully processed (NULL = pending)';
