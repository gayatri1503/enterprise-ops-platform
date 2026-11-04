package com.platform.notification.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        String reqId = correlationId(request);
        log.warn("[{}] API exception: {} ({})", reqId, ex.getMessage(), ex.getCode());
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(reqId, ex.getStatus().value(), ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String reqId = correlationId(request);
        log.warn("[{}] Access denied: {}", reqId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(reqId, HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Insufficient permissions"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String reqId = correlationId(request);
        List<ErrorResponse.FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toIssue)
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(reqId, HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED", "Invalid request body", issues));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        String reqId = correlationId(request);
        log.error("[{}] Unhandled error: {}", reqId, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(reqId, HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private ErrorResponse.FieldIssue toIssue(FieldError fe) {
        String msg = fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value";
        return new ErrorResponse.FieldIssue(fe.getField(), msg);
    }

    private String correlationId(HttpServletRequest request) {
        String id = request.getHeader("X-Correlation-Id");
        return (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
    }
}
