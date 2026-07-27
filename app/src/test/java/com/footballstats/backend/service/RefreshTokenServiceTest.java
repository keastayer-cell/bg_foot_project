package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.RefreshTokenSession;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.RefreshTokenSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenSessionRepository refreshTokenSessionRepository;
    @Mock
    private AppUserRepository appUserRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenSessionRepository, appUserRepository, 30);
    }

    @Test
    void issueAndRotateRefreshTokenRevokesPreviousSession() {
        AppUser user = user(5L, 2);
        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));

        String rawToken = refreshTokenService.issueTokenForUserId(5L, " browser ", " 127.0.0.1 ");

        ArgumentCaptor<RefreshTokenSession> issuedCaptor = ArgumentCaptor.forClass(RefreshTokenSession.class);
        verify(refreshTokenSessionRepository).save(issuedCaptor.capture());
        RefreshTokenSession current = issuedCaptor.getValue();
        when(refreshTokenSessionRepository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));

        RefreshTokenService.RefreshTokenRotation rotation =
            refreshTokenService.rotateToken(rawToken, "new browser", "10.0.0.2");

        assertThat(rotation.userId()).isEqualTo(5L);
        assertThat(rotation.refreshToken()).isNotBlank().isNotEqualTo(rawToken);
        assertThat(current.getRevokedAt()).isNotNull();
        assertThat(current.getReplacedByTokenHash()).hasSize(64);
        verify(refreshTokenSessionRepository, atLeastOnce()).save(any(RefreshTokenSession.class));
    }

    @Test
    void logoutRevokesKnownRefreshToken() {
        RefreshTokenSession session = activeSession(user(8L, 0), 0);
        when(refreshTokenSessionRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(session));

        refreshTokenService.revoke("refresh-token");

        assertThat(session.getRevokedAt()).isNotNull();
        verify(refreshTokenSessionRepository).save(session);
    }

    @Test
    void rotationRejectsSessionFromPreviousTokenVersion() {
        RefreshTokenSession session = activeSession(user(12L, 4), 3);
        when(refreshTokenSessionRepository.findByTokenHash(any(String.class))).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> refreshTokenService.rotateToken("old-refresh", null, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Сессия устарела");

        assertThat(session.getRevokedAt()).isNotNull();
    }

    private RefreshTokenSession activeSession(AppUser user, int tokenVersion) {
        RefreshTokenSession session = new RefreshTokenSession();
        session.setUser(user);
        session.setTokenVersion(tokenVersion);
        session.setExpiresAt(OffsetDateTime.now().plusDays(1));
        return session;
    }

    private AppUser user(Long id, int tokenVersion) {
        AppUser user = new AppUser();
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmail("user@example.com");
        user.setName("Test User");
        user.setTokenVersion(tokenVersion);
        return user;
    }
}
