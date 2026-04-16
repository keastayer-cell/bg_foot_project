package com.footballstats.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "football_refresh_token";

    private final long refreshExpiresDays;
    private final boolean secureCookie;
    private final String sameSite;

    public AuthCookieService(
        @Value("${JWT_REFRESH_EXPIRES_DAYS:30}") long refreshExpiresDays,
        @Value("${AUTH_REFRESH_COOKIE_SECURE:true}") boolean secureCookie,
        @Value("${AUTH_REFRESH_COOKIE_SAME_SITE:Strict}") String sameSite
    ) {
        this.refreshExpiresDays = refreshExpiresDays;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
    }

    public String buildRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path("/api/auth")
            .maxAge(Duration.ofDays(refreshExpiresDays))
            .build()
            .toString();
    }

    public String buildClearRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path("/api/auth")
            .maxAge(Duration.ZERO)
            .build()
            .toString();
    }
}