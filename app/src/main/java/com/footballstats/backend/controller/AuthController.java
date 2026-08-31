package com.footballstats.backend.controller;

import com.footballstats.backend.dto.auth.AuthResponse;
import com.footballstats.backend.dto.auth.ChangePasswordRequest;
import com.footballstats.backend.dto.auth.GuestLoginRequest;
import com.footballstats.backend.dto.auth.LoginRequest;
import com.footballstats.backend.dto.auth.PasswordResetRequest;
import com.footballstats.backend.dto.auth.RegisterRequest;
import com.footballstats.backend.dto.auth.UserResponse;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.AuthCookieService;
import com.footballstats.backend.service.AuthService;
import com.footballstats.backend.service.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;
    private final boolean trustForwardHeaders;

    public AuthController(
        AuthService authService,
        RefreshTokenService refreshTokenService,
        AuthCookieService authCookieService,
        @Value("${APP_TRUST_FORWARD_HEADERS:false}") boolean trustForwardHeaders
    ) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.authCookieService = authCookieService;
        this.trustForwardHeaders = trustForwardHeaders;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request);
        return buildSessionResponse(response, HttpStatus.CREATED, httpRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request);
        return buildSessionResponse(response, HttpStatus.OK, httpRequest);
    }

    @PostMapping("/guest")
    public ResponseEntity<AuthResponse> guestLogin(@Valid @RequestBody(required = false) GuestLoginRequest request) {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, authCookieService.buildClearRefreshTokenCookie())
            .body(authService.guestLogin(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AppUserPrincipal appUserPrincipal)) {
            throw new IllegalArgumentException("Не удалось определить пользователя из токена.");
        }
        AuthResponse response = authService.changePassword(appUserPrincipal, request);
        return buildSessionResponse(response, HttpStatus.OK, httpRequest);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        @CookieValue(value = AuthCookieService.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
        HttpServletRequest httpRequest
    ) {
        RefreshTokenService.RefreshTokenRotation rotation = refreshTokenService.rotateToken(
            refreshToken,
            extractUserAgent(httpRequest),
            extractClientIp(httpRequest)
        );

        AuthResponse response = authService.buildAuthResponseForUserId(rotation.userId());
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, authCookieService.buildRefreshTokenCookie(rotation.refreshToken()))
            .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(value = AuthCookieService.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken
    ) {
        refreshTokenService.revoke(refreshToken);
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, authCookieService.buildClearRefreshTokenCookie())
            .build();
    }

    @PostMapping("/password-reset/complete")
    public ResponseEntity<Void> completePasswordReset(@Valid @RequestBody CompletePasswordResetRequest request) {
        authService.completePasswordReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(authService.getCurrentUser(authHeader));
    }

    private ResponseEntity<AuthResponse> buildSessionResponse(AuthResponse response, HttpStatus status, HttpServletRequest httpRequest) {
        if (response.getUserId() == null || response.getUserId() <= 0) {
            return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, authCookieService.buildClearRefreshTokenCookie())
                .body(response);
        }

        String refreshToken = refreshTokenService.issueTokenForUserId(
            response.getUserId(),
            extractUserAgent(httpRequest),
            extractClientIp(httpRequest)
        );

        return ResponseEntity.status(status)
            .header(HttpHeaders.SET_COOKIE, authCookieService.buildRefreshTokenCookie(refreshToken))
            .body(response);
    }

    private String extractUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String extractClientIp(HttpServletRequest request) {
        if (!trustForwardHeaders) {
            return request.getRemoteAddr();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String candidate = forwardedFor.split(",")[0].trim();
            if (!candidate.isEmpty() && candidate.length() <= 64 && candidate.chars().noneMatch(Character::isWhitespace)) {
                return candidate;
            }
        }
        return request.getRemoteAddr();
    }

    public record CompletePasswordResetRequest(
        @NotBlank(message = "Токен обязателен.") String token,
        @NotBlank(message = "Новый пароль обязателен.") @Size(min = 8, max = 120, message = "Новый пароль должен содержать от 8 до 120 символов.") String newPassword
    ) {
    }
}
