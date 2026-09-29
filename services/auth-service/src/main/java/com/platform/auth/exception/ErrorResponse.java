package com.platform.auth.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Matches the error shape in docs/api-contract.md. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String timestamp, String requestId, int status, String code,
                            String message, List<FieldIssue> validationErrors) {

    public record FieldIssue(String field, String message) {}

    public static ErrorResponse of(String requestId, int status, String code, String message) {
        return new ErrorResponse(Instant.now().toString(), requestId, status, code, message, null);
    }

    public static ErrorResponse of(String requestId, int status, String code, String message,
                                   List<FieldIssue> issues) {
        return new ErrorResponse(Instant.now().toString(), requestId, status, code, message, issues);
    }
}