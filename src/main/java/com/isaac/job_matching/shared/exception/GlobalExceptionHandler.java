package com.isaac.job_matching.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Job Matching Platform.
 * 
 * <p>
 * Provides centralized exception handling across all controllers,
 * converting exceptions to standardized {@link ApiError} responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        /**
         * Handles entity not found exceptions.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 404 Not Found response
         */
        @ExceptionHandler(EntityNotFoundException.class)
        public ResponseEntity<ApiError> handleEntityNotFound(
                        EntityNotFoundException ex, WebRequest request) {

                log.warn("Entity not found: {}", ex.getMessage());

                ApiError error = new ApiError(
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                ex.getMessage(),
                                getPath(request),
                                Instant.now(),
                                null);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        /**
         * Handles validation exceptions.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 400 Bad Request response
         */
        @ExceptionHandler(ValidationException.class)
        public ResponseEntity<ApiError> handleValidation(
                        ValidationException ex, WebRequest request) {

                log.warn("Validation error: {}", ex.getMessage());

                ApiError error = new ApiError(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                ex.getMessage(),
                                getPath(request),
                                Instant.now(),
                                ex.getErrors());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Handles Spring validation errors from @Valid annotations.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 400 Bad Request response with field errors
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException ex, WebRequest request) {

                Map<String, String> fieldErrors = new HashMap<>();
                for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
                        fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
                }

                log.warn("Validation errors: {}", fieldErrors);

                ApiError error = new ApiError(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                "Validation failed",
                                getPath(request),
                                Instant.now(),
                                fieldErrors);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Handles duplicate entity exceptions.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 409 Conflict response
         */
        @ExceptionHandler(DuplicateEntityException.class)
        public ResponseEntity<ApiError> handleDuplicateEntity(
                        DuplicateEntityException ex, WebRequest request) {

                log.warn("Duplicate entity: {}", ex.getMessage());

                ApiError error = new ApiError(
                                HttpStatus.CONFLICT.value(),
                                HttpStatus.CONFLICT.getReasonPhrase(),
                                ex.getMessage(),
                                getPath(request),
                                Instant.now(),
                                null);

                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        /**
         * Handles unauthorized access exceptions.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 401 Unauthorized response
         */
        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ApiError> handleUnauthorized(
                        UnauthorizedException ex, WebRequest request) {

                log.warn("Unauthorized access: {}", ex.getMessage());

                ApiError error = new ApiError(
                                HttpStatus.UNAUTHORIZED.value(),
                                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                                ex.getMessage(),
                                getPath(request),
                                Instant.now(),
                                null);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        /**
         * Handles Spring Security authentication exceptions.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 401 Unauthorized response
         */
        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ApiError> handleAuthenticationException(
                        AuthenticationException ex, WebRequest request) {

                log.warn("Authentication failed: {}", ex.getMessage());

                ApiError error = new ApiError(
                                HttpStatus.UNAUTHORIZED.value(),
                                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                                "Authentication required",
                                getPath(request),
                                Instant.now(),
                                null);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        /**
         * Handles forbidden exceptions.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 403 Forbidden response
         */
        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ApiError> handleForbidden(
                        ForbiddenException ex, WebRequest request) {

                log.warn("Forbidden: {}", ex.getMessage());

                ApiError error = new ApiError(
                                HttpStatus.FORBIDDEN.value(),
                                HttpStatus.FORBIDDEN.getReasonPhrase(),
                                ex.getMessage(),
                                getPath(request),
                                Instant.now(),
                                null);

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        /**
         * Handles access denied exceptions.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 403 Forbidden response
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiError> handleAccessDenied(
                        AccessDeniedException ex, WebRequest request) {

                log.warn("Access denied: {}", ex.getMessage());

                ApiError error = new ApiError(
                                HttpStatus.FORBIDDEN.value(),
                                HttpStatus.FORBIDDEN.getReasonPhrase(),
                                "Access denied",
                                getPath(request),
                                Instant.now(),
                                null);

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        /**
         * Handles business logic exceptions.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 422 Unprocessable Entity response
         */
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiError> handleBusinessException(
                        BusinessException ex, WebRequest request) {

                log.warn("Business error: {}", ex.getMessage());

                // Using value 422 directly as HttpStatus.UNPROCESSABLE_ENTITY is deprecated
                ApiError error = new ApiError(
                                422,
                                "Unprocessable Entity",
                                ex.getMessage(),
                                getPath(request),
                                Instant.now(),
                                null);

                return ResponseEntity.status(422).body(error);
        }

        /**
         * Handles illegal argument exceptions.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 400 Bad Request response
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> handleIllegalArgument(
                        IllegalArgumentException ex, WebRequest request) {

                log.warn("Illegal argument: {}", ex.getMessage());

                ApiError error = new ApiError(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                ex.getMessage(),
                                getPath(request),
                                Instant.now(),
                                null);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Handles all other uncaught exceptions.
         * 
         * @param ex      the exception
         * @param request the web request
         * @return 500 Internal Server Error response
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleAllUncaughtException(
                        Exception ex, WebRequest request) {

                log.error("Unexpected error occurred", ex);

                ApiError error = new ApiError(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                "An unexpected error occurred. Please try again later.",
                                getPath(request),
                                Instant.now(),
                                null);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        private String getPath(WebRequest request) {
                return request.getDescription(false).replace("uri=", "");
        }
}
