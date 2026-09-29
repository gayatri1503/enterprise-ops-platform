package com.platform.auth.exception;

import com.platform.auth.service.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.platform.auth.service.EmailAlreadyUsedException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.of(rid(req.getHeader("X-Correlation-Id")), 401, "INVALID_CREDENTIALS", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleEmailTaken(EmailAlreadyUsedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.of(rid(req.getHeader("X-Correlation-Id")), 409, "EMAIL_TAKEN", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex); // details stay in the server log, never in the response
        return ResponseEntity.internalServerError().body(
                ErrorResponse.of(rid(req.getHeader("X-Correlation-Id")), 500, "INTERNAL_ERROR", "Something went wrong"));
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