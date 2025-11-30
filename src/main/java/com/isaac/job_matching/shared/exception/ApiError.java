package com.isaac.job_matching.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API error response format.
 * 
 * <p>Provides consistent error responses across all API endpoints with:
 * <ul>
 *   <li>HTTP status code and reason</li>
 *   <li>Human-readable error message</li>
 *   <li>Request path for debugging</li>
 *   <li>Timestamp for correlation</li>
 *   <li>Optional field-level errors for validation failures</li>
 * </ul>
 * 
 * @param status HTTP status code (e.g., 400, 404, 500)
 * @param error HTTP status reason phrase (e.g., "Bad Request", "Not Found")
 * @param message Human-readable error description
 * @param path The request path that caused the error
 * @param timestamp When the error occurred
 * @param errors Optional map of field-level validation errors
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    int status,
    String error,
    String message,
    String path,
    Instant timestamp,
    Map<String, String> errors
) {
    
    /**
     * Creates an ApiError without field-level errors.
     * 
     * @param status HTTP status code
     * @param error HTTP status reason phrase
     * @param message error message
     * @param path request path
     * @return ApiError with current timestamp
     */
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, Instant.now(), null);
    }
    
    /**
     * Creates an ApiError with field-level validation errors.
     * 
     * @param status HTTP status code
     * @param error HTTP status reason phrase
     * @param message error message
     * @param path request path
     * @param errors map of field names to error messages
     * @return ApiError with current timestamp and field errors
     */
    public static ApiError withErrors(int status, String error, String message, 
                                      String path, Map<String, String> errors) {
        return new ApiError(status, error, message, path, Instant.now(), errors);
    }
}
