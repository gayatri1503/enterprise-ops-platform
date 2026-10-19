package com.platform.auth.controller;

import com.platform.auth.dto.LoginRequest;
import com.platform.auth.dto.LoginResponse;
import com.platform.auth.service.AuthService;
import com.platform.auth.service.AuthService.AuthResult;
import com.platform.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final boolean cookieSecure;
    private final long refreshTtlDays;

    public AuthController(AuthService authService,
                          @Value("${auth.cookie-secure:false}") boolean cookieSecure,
                          @Value("${jwt.refresh-ttl-days:7}") long refreshTtlDays) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
        this.refreshTtlDays = refreshTtlDays;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest http) {
        return respond(authService.login(request, clientIp(http), http.getHeader("User-Agent")));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return respond(authService.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = COOKIE_NAME, required = false) String refreshToken) {
        return respond(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    /** Behind the gateway the real client IP arrives in X-Forwarded-For. */
    private String clientIp(HttpServletRequest http) {
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }

    private ResponseEntity<LoginResponse> respond(AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(result.refreshToken(), Duration.ofDays(refreshTtlDays)).toString())
                .body(result.body());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)          // JavaScript can't read it -> XSS can't steal it
                .secure(cookieSecure)    // true once you serve over HTTPS
                .sameSite("Strict")
                .path("/api/auth")       // browser only sends it to auth endpoints
                .maxAge(maxAge)
                .build();
    }
}