package com.platform.auth.dto;

public record LoginResponse(String accessToken, long expiresInSeconds, UserSummary user) {}