package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.dto.auth.AuthResponse;
import com.footballstats.backend.dto.auth.LoginRequest;
import com.footballstats.backend.dto.auth.PasswordResetResponse;
import com.footballstats.backend.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private NotificationEventService notificationEventService;

    private BCryptPasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(JWT_SECRET, 30);
        authService = new AuthService(
            appUserRepository,
            jwtService,
            accessControlService,
            notificationEventService,
            passwordEncoder,
            "https://football.example"
        );
    }

    @Test
    void loginNormalizesEmailAndReturnsUserRoles() {
        AppUser user = user(7L, 3);
        user.setPasswordHash(passwordEncoder.encode("secret12"));
        when(appUserRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(accessControlService.getRoleCodes(7L)).thenReturn(List.of("TEAM_REP"));

        LoginRequest request = new LoginRequest();
        request.setEmail(" User@Example.com ");
        request.setPassword("secret12");

        AuthResponse response = authService.login(request);

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getRoles()).containsExactly("TEAM_REP");
        assertThat(jwtService.parseToken(response.getToken()).get("ver", Integer.class)).isEqualTo(3);
    }

    @Test
    void adminResetCreatesOneTimeLinkAndInvalidatesExistingSessions() {
        AppUser user = user(9L, 4);
        when(appUserRepository.findById(9L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetResponse response = authService.resetUserPasswordByAdmin(1L, 9L);

        assertThat(response.getResetPath()).startsWith("/reset-password?token=");
        assertThat(user.getTokenVersion()).isEqualTo(5);
        assertThat(user.isMustChangePassword()).isTrue();
        assertThat(user.getPasswordResetTokenHash()).hasSize(64);
        assertThat(user.getPasswordResetExpiresAt()).isAfter(OffsetDateTime.now());
        verify(notificationEventService).enqueuePasswordResetRequested(
            any(AppUser.class),
            org.mockito.ArgumentMatchers.startsWith("https://football.example/reset-password?token="),
            any(OffsetDateTime.class)
        );
    }

    @Test
    void completesPasswordResetAndConsumesOneTimeToken() {
        AppUser user = user(10L, 1);
        when(appUserRepository.findById(10L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PasswordResetResponse reset = authService.resetUserPasswordByAdmin(1L, 10L);
        String rawToken = reset.getResetPath().substring(reset.getResetPath().indexOf("token=") + 6);
        when(appUserRepository.findByPasswordResetTokenHash(user.getPasswordResetTokenHash()))
            .thenReturn(Optional.of(user));

        authService.completePasswordReset(rawToken, "new-secret-12");

        assertThat(passwordEncoder.matches("new-secret-12", user.getPasswordHash())).isTrue();
        assertThat(user.getTokenVersion()).isEqualTo(3);
        assertThat(user.isMustChangePassword()).isFalse();
        assertThat(user.getPasswordResetTokenHash()).isNull();
        assertThat(user.getPasswordResetExpiresAt()).isNull();
    }

    @Test
    void tokenVersionRejectsAnAccessTokenIssuedBeforePasswordChange() {
        AppUser user = user(11L, 1);
        String oldToken = jwtService.generateToken(11L, user.getEmail(), user.getName(), 0);
        when(appUserRepository.findById(11L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.getCurrentUser("Bearer " + oldToken))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Сессия устарела");
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
