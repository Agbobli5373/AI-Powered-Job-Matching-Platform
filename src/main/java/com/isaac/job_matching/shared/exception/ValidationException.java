package com.isaac.job_matching.shared.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when input validation fails.
 * 
 * <p>
 * Results in a 400 Bad Request HTTP response.
 * Can include field-level error details.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors != null ? new HashMap<>(errors) : new HashMap<>();
    }

    public ValidationException(String field, String error) {
        super("Validation failed for field '" + field + "': " + error);
        this.errors = new HashMap<>();
        this.errors.put(field, error);
    }

    /**
     * Adds a field-level error to this exception.
     * 
     * @param field the field name
     * @param error the error message
     * @return this exception for fluent chaining
     */
    public ValidationException addError(String field, String error) {
        this.errors.put(field, error);
        return this;
    }

    public Map<String, String> getErrors() {
        return new HashMap<>(errors);
    }

    public boolean hasFieldErrors() {
        return !errors.isEmpty();
    }
}
