package com.footballstats.backend.controller;

import com.footballstats.backend.dto.auth.AuthResponse;
import com.footballstats.backend.dto.auth.ChangePasswordRequest;
import com.footballstats.backend.dto.auth.UpdateAccountRequest;
import com.footballstats.backend.dto.auth.UserAccessResponse;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.AccessControlService;
import com.footballstats.backend.service.AuthCookieService;
import com.footballstats.backend.service.AuthService;
import com.footballstats.backend.service.RefreshTokenService;
import com.footballstats.backend.service.NotificationPreferenceService;
import com.footballstats.backend.service.FavoriteService;
import com.footballstats.backend.domain.FavoriteType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@PreAuthorize("isAuthenticated()")
public class AccountController {

    private final AuthService authService;
    private final AccessControlService accessControlService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final FavoriteService favoriteService;

    public AccountController(
        AuthService authService,
        AccessControlService accessControlService,
        RefreshTokenService refreshTokenService,
        AuthCookieService authCookieService,
        NotificationPreferenceService notificationPreferenceService,
        FavoriteService favoriteService
    ) {
        this.authService = authService;
        this.accessControlService = accessControlService;
        this.refreshTokenService = refreshTokenService;
        this.authCookieService = authCookieService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/favorites")
    public java.util.List<FavoriteService.FavoriteData> getFavorites(Authentication authentication) {
        return favoriteService.list(requirePrincipal(authentication).getUserId());
    }

    @PostMapping("/favorites/{type}/{targetId}")
    public FavoriteService.FavoriteData addFavorite(
        @PathVariable FavoriteType type, @PathVariable Long targetId, Authentication authentication
    ) {
        return favoriteService.add(requirePrincipal(authentication).getUserId(), type, targetId);
    }

    @DeleteMapping("/favorites/{type}/{targetId}")
    public ResponseEntity<Void> removeFavorite(
        @PathVariable FavoriteType type, @PathVariable Long targetId, Authentication authentication
    ) {
        favoriteService.remove(requirePrincipal(authentication).getUserId(), type, targetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notification-settings")
    public java.util.List<NotificationPreferenceService.Setting> getNotificationSettings(Authentication authentication) {
        return notificationPreferenceService.getSettings(requirePrincipal(authentication).getUserId());
    }

    @PutMapping("/notification-settings")
    public java.util.List<NotificationPreferenceService.Setting> updateNotificationSettings(
        @RequestBody java.util.List<NotificationPreferenceService.SettingUpdate> updates,
        Authentication authentication
    ) {
        return notificationPreferenceService.updateSettings(requirePrincipal(authentication).getUserId(), updates);
    }

    @GetMapping
    public UserAccessResponse getAccount(Authentication authentication) {
        return accessControlService.getUserAccess(requirePrincipal(authentication).getUserId());
    }

    @PatchMapping
    public UserAccessResponse updateAccount(
        @Valid @RequestBody UpdateAccountRequest request,
        Authentication authentication
    ) {
        return authService.updateProfile(requirePrincipal(authentication), request);
    }

    @PostMapping("/password")
    public ResponseEntity<AuthResponse> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        AppUserPrincipal principal = requirePrincipal(authentication);
        AuthResponse response = authService.changePassword(principal, request);
        refreshTokenService.revokeAllForUserId(principal.getUserId());
        String refreshToken = refreshTokenService.issueTokenForUserId(
            principal.getUserId(),
            httpRequest.getHeader("User-Agent"),
            httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, authCookieService.buildRefreshTokenCookie(refreshToken))
            .body(response);
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        AppUserPrincipal principal = requirePrincipal(authentication);
        authService.invalidateAllSessions(principal);
        refreshTokenService.revokeAllForUserId(principal.getUserId());
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, authCookieService.buildClearRefreshTokenCookie())
            .build();
    }

    private AppUserPrincipal requirePrincipal(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof AppUserPrincipal appUserPrincipal)
            || appUserPrincipal.getUserId() == null
            || appUserPrincipal.getUserId() <= 0) {
            throw new IllegalArgumentException("Личный кабинет доступен только зарегистрированному пользователю.");
        }
        return appUserPrincipal;
    }
}
