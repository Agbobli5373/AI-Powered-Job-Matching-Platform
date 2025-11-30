package com.isaac.job_matching.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts to access a resource they don't have
 * permission for.
 * 
 * <p>
 * This maps to HTTP 403 Forbidden status.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String resource, String reason) {
        super("Access to " + resource + " is forbidden: " + reason);
    }
}
