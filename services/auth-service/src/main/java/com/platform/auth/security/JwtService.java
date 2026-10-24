package com.platform.auth.security;

import com.platform.auth.domain.Permission;
import com.platform.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtlSeconds;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-ttl-minutes:15}") long accessTtlMinutes) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "jwt.secret must be at least 32 bytes for HS256 — current value is too short");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.accessTtlSeconds = accessTtlMinutes * 60;
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .claim("perms", user.getRole().getPermissions().stream().map(Permission::name).sorted().toList())
                .claim("deptId", user.getDepartmentId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parses and validates a JWT, returning its claims.
     * Throws {@link JwtException} for any tampered, expired, or malformed token.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
