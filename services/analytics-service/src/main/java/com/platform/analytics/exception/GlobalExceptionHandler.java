package com.platform.analytics.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.of(rid(req), 403, "FORBIDDEN", "Insufficient permissions"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.of(rid(req), 401, "UNAUTHORIZED", "Authentication required"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError().body(
                ErrorResponse.of(rid(req), 500, "INTERNAL_ERROR", "Something went wrong"));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus s = HttpStatus.valueOf(statusCode.value());
        String message = (body instanceof ProblemDetail pd && pd.getDetail() != null && s.is4xxClientError())
                ? pd.getDetail() : s.getReasonPhrase();
        return ResponseEntity.status(statusCode).headers(headers).body(
                ErrorResponse.of(rid(request), s.value(), s.name(), message));
    }

    private static String rid(HttpServletRequest req) {
        String h = req.getHeader("X-Correlation-Id");
        return h != null ? h : "n/a";
    }

    private static String rid(WebRequest req) {
        String h = req.getHeader("X-Correlation-Id");
        return h != null ? h : "n/a";
    }
}
