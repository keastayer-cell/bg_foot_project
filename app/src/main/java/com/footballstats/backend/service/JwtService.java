package com.footballstats.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiresMinutes;

    public JwtService(
        @Value("${JWT_SECRET:football_stats_app_super_secret_key_change_me_1234567890}") String jwtSecret,
        @Value("${JWT_EXPIRES_MINUTES:480}") long expiresMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expiresMinutes = expiresMinutes;
    }

    public String generateToken(Long userId, String email, String name, Integer tokenVersion) {
        return generateToken(userId, email, name, tokenVersion, null);
    }

    public String generateGuestToken(String name) {
        return generateToken(0L, "guest@football.local", name, 0, List.of("GUEST"));
    }

    private String generateToken(Long userId, String email, String name, Integer tokenVersion, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(expiresMinutes, ChronoUnit.MINUTES);

        var builder = Jwts.builder()
            .subject(email)
            .claim("uid", userId)
            .claim("name", name)
            .claim("ver", tokenVersion == null ? 0 : tokenVersion)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(secretKey);

        if (roles != null && !roles.isEmpty()) {
            builder.claim("roles", roles);
        }

        return builder.compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
