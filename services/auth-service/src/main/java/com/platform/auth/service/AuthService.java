package com.platform.auth.service;

import com.platform.auth.domain.RefreshToken;
import com.platform.auth.domain.User;
import com.platform.auth.dto.LoginRequest;
import com.platform.auth.dto.LoginResponse;
import com.platform.auth.dto.UserSummary;
import com.platform.auth.repository.RefreshTokenRepository;
import com.platform.auth.repository.UserRepository;
import com.platform.auth.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import com.platform.auth.domain.Role;
import com.platform.auth.dto.RegisterRequest;

@Service
public class AuthService {

    /** What the controller needs: JSON body for the response + raw refresh token for the cookie. */
    public record AuthResult(LoginResponse body, String refreshToken) {}

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshTtlDays;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       @Value("${jwt.refresh-ttl-days:7}") long refreshTtlDays) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTtlDays = refreshTtlDays;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new EmailAlreadyUsedException();
        }
        // Role and department are fixed here on the server.
        User user = users.save(new User(
                email, passwordEncoder.encode(request.password()), request.fullName().trim(), Role.USER, 1L));
        return issueTokens(user); // registering also signs the user in
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthResult login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.recordFailedLogin();
            throw new InvalidCredentialsException("Invalid email or password");
        }

        user.resetFailedLogins();
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthResult refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidCredentialsException("Missing refresh token");
        }

        RefreshToken stored = refreshTokens.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (stored.isRevoked()) {
            // A rotated (already used) token came back: treat as theft, kill all sessions of this user.
            refreshTokens.revokeAllForUser(stored.getUserId());
            throw new InvalidCredentialsException("Refresh token reuse detected");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException("Refresh token expired");
        }

        User user = users.findById(stored.getUserId())
                .filter(User::isEnabled)
                .orElseThrow(() -> new InvalidCredentialsException("Account disabled"));

        stored.revoke(); // rotation: the old token can never be used again
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.findByTokenHash(sha256(rawToken)).ifPresent(RefreshToken::revoke);
    }

    private AuthResult issueTokens(User user) {
        String raw = newOpaqueToken();
        refreshTokens.save(new RefreshToken(
                user.getId(), sha256(raw), Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS)));

        LoginResponse body = new LoginResponse(
                jwtService.generateAccessToken(user),
                jwtService.getAccessTtlSeconds(),
                UserSummary.from(user));
        return new AuthResult(body, raw);
    }

    private String newOpaqueToken() {
        byte[] bytes = new byte[32]; // 256 bits of randomness
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}