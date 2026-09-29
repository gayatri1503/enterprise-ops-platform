package com.platform.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(
                rid(req.getHeader("X-Correlation-Id")), ex.getStatus().value(), ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of(
                rid(req.getHeader("X-Correlation-Id")), 403, "FORBIDDEN", "Insufficient permissions"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.of(
                rid(req.getHeader("X-Correlation-Id")), 500, "INTERNAL_ERROR", "Something went wrong"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorResponse.FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldIssue(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                rid(request.getHeader("X-Correlation-Id")), 400, "VALIDATION_ERROR", "Request validation failed", issues));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String message = (body instanceof ProblemDetail pd && pd.getDetail() != null && status.is4xxClientError())
                ? pd.getDetail() : status.getReasonPhrase();
        return ResponseEntity.status(statusCode).headers(headers).body(ErrorResponse.of(
                rid(request.getHeader("X-Correlation-Id")), status.value(), status.name(), message));
    }

    private static String rid(String header) {
        return header != null ? header : "n/a";
    }
}