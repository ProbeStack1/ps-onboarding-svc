package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.common.ApiError;
import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.dto.common.PageMeta;
import io.probestack.onboarding.exception.DuplicateResourceException;
import io.probestack.onboarding.exception.ForbiddenOperationException;
import io.probestack.onboarding.exception.InvalidStatusTransitionException;
import io.probestack.onboarding.exception.OrganizationMismatchException;
import io.probestack.onboarding.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> ApiError.builder()
                        .field(error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName())
                        .message(error.getDefaultMessage())
                        .build())
                .toList();
        return failure(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", errors, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return failure(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), null, request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return failure(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", ex.getMessage(), null, request);
    }

    @ExceptionHandler({ForbiddenOperationException.class, OrganizationMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(RuntimeException ex, HttpServletRequest request) {
        return failure(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), null, request);
    }

    @ExceptionHandler({IllegalArgumentException.class, InvalidStatusTransitionException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return failure(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null, request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        log.error("Unhandled onboarding exception", ex);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", null, request);
    }

    private ResponseEntity<ApiResponse<Void>> failure(HttpStatus status, String code, String message, List<ApiError> errors, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiResponse.failure(code, message, errors, PageMeta.simple(traceId(request))));
    }

    private String traceId(HttpServletRequest request) {
        return request == null ? null : request.getHeader("X-Trace-Id");
    }
}
