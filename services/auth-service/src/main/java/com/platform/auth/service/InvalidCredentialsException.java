package com.platform.auth.service;

/** Thrown when login credentials don't match. Mapped to HTTP 401 by GlobalExceptionHandler. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}