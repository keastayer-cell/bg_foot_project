package com.footballstats.backend.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void generatesAndParsesUserTokenClaims() {
        JwtService jwtService = new JwtService(SECRET, 30);

        String token = jwtService.generateToken(42L, "captain@example.com", "Captain", 7);
        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("captain@example.com");
        assertThat(claims.get("uid", Integer.class)).isEqualTo(42);
        assertThat(claims.get("name", String.class)).isEqualTo("Captain");
        assertThat(claims.get("ver", Integer.class)).isEqualTo(7);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void generatesGuestTokenWithGuestRole() {
        JwtService jwtService = new JwtService(SECRET, 30);

        Claims claims = jwtService.parseToken(jwtService.generateGuestToken("Viewer"));

        assertThat(claims.getSubject()).isEqualTo("guest@football.local");
        assertThat(claims.get("uid", Integer.class)).isZero();
        assertThat(claims.get("name", String.class)).isEqualTo("Viewer");
        assertThat(claims.get("roles", List.class)).containsExactly("GUEST");
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 30))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET");
    }
}
