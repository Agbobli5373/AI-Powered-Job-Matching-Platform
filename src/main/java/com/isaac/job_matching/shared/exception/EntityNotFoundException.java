package com.isaac.job_matching.shared.exception;

/**
 * Exception thrown when a requested entity cannot be found.
 * 
 * <p>
 * Results in a 404 Not Found HTTP response.
 */
public class EntityNotFoundException extends RuntimeException {

    private final String entityType;
    private final Object identifier;

    public EntityNotFoundException(String entityType, Object identifier) {
        super(entityType + " not found with identifier: " + identifier);
        this.entityType = entityType;
        this.identifier = identifier;
    }

    public EntityNotFoundException(String entityType, String field, String value) {
        super(entityType + " not found with " + field + ": " + value);
        this.entityType = entityType;
        this.identifier = field + "=" + value;
    }

    public EntityNotFoundException(String message) {
        super(message);
        this.entityType = null;
        this.identifier = null;
    }

    public String getEntityType() {
        return entityType;
    }

    public Object getIdentifier() {
        return identifier;
    }
}
