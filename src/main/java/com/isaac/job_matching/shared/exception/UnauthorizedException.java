package com.isaac.job_matching.shared.exception;

/**
 * Exception thrown when a user is not authorized to perform an action.
 * 
 * <p>
 * Results in a 401 Unauthorized HTTP response.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException() {
        super("Authentication required");
    }
}
