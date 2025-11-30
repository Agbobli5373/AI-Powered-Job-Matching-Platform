package com.isaac.job_matching.shared.exception;

/**
 * Exception thrown when a business rule is violated.
 * 
 * <p>
 * Results in a 422 Unprocessable Entity HTTP response.
 * Use this for domain-level business logic violations that are not
 * simple validation errors.
 * 
 * <p>
 * Examples:
 * <ul>
 * <li>Cannot apply to a closed job</li>
 * <li>Cannot withdraw an already rejected application</li>
 * <li>Employer cannot view profile without access request approval</li>
 * </ul>
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
    }

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
