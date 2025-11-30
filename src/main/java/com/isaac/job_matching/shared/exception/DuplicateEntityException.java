package com.isaac.job_matching.shared.exception;

/**
 * Exception thrown when attempting to create a duplicate entity.
 * 
 * <p>
 * Results in a 409 Conflict HTTP response.
 */
public class DuplicateEntityException extends RuntimeException {

    private final String entityType;
    private final String conflictingField;
    private final Object conflictingValue;

    public DuplicateEntityException(String entityType, String conflictingField, Object conflictingValue) {
        super(entityType + " already exists with " + conflictingField + ": " + conflictingValue);
        this.entityType = entityType;
        this.conflictingField = conflictingField;
        this.conflictingValue = conflictingValue;
    }

    public DuplicateEntityException(String message) {
        super(message);
        this.entityType = null;
        this.conflictingField = null;
        this.conflictingValue = null;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getConflictingField() {
        return conflictingField;
    }

    public Object getConflictingValue() {
        return conflictingValue;
    }
}
