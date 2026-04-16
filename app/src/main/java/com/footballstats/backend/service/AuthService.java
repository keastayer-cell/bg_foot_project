package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.dto.auth.AuthResponse;
import com.footballstats.backend.dto.auth.ChangePasswordRequest;
import com.footballstats.backend.dto.auth.GuestLoginRequest;
import com.footballstats.backend.dto.auth.LoginRequest;
import com.footballstats.backend.dto.auth.PasswordResetResponse;
import com.footballstats.backend.dto.auth.RegisterRequest;
import com.footballstats.backend.dto.auth.UserResponse;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.repository.AppUserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final AccessControlService accessControlService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final int PASSWORD_RESET_TTL_MINUTES = 30;

    public AuthService(
        AppUserRepository appUserRepository,
        JwtService jwtService,
        AccessControlService accessControlService,
        BCryptPasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.accessControlService = accessControlService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует.");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(request.getName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setMustChangePassword(false);
        user.setTokenVersion(0);
        user.setPasswordChangedAt(OffsetDateTime.now());

        AppUser saved = appUserRepository.save(user);
        accessControlService.assignDefaultUserRole(saved.getId());
        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Неверный email или пароль.");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse guestLogin(GuestLoginRequest request) {
        String guestName = "Гость";
        if (request != null && StringUtils.hasText(request.getName())) {
            guestName = request.getName().trim();
        }
        if (guestName.length() > 80) {
            guestName = guestName.substring(0, 80);
        }

        String token = jwtService.generateGuestToken(guestName);
        return new AuthResponse(token, 0L, "guest@football.local", guestName, List.of(RoleCode.GUEST.name()), false);
    }

    public AuthResponse changePassword(AppUserPrincipal principal, ChangePasswordRequest request) {
        if (principal == null || principal.getUserId() == null || principal.getUserId() <= 0) {
            throw new IllegalArgumentException("Смена пароля для гостя недоступна.");
        }

        AppUser user = appUserRepository.findById(principal.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь больше не найден. Войдите снова."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Текущий пароль указан неверно.");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("Новый пароль должен отличаться от текущего.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setTokenVersion(nextTokenVersion(user));
        user.setPasswordChangedAt(OffsetDateTime.now());
        AppUser saved = appUserRepository.save(user);

        return buildAuthResponse(saved);
    }

    public PasswordResetResponse resetUserPasswordByAdmin(Long actorUserId, Long targetUserId) {
        AppUser targetUser = appUserRepository.findById(targetUserId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));

        String resetToken = generateOpaqueToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(PASSWORD_RESET_TTL_MINUTES);

        targetUser.setPasswordHash(passwordEncoder.encode(generateOpaqueToken()));
        targetUser.setMustChangePassword(true);
        targetUser.setTokenVersion(nextTokenVersion(targetUser));
        targetUser.setPasswordChangedAt(OffsetDateTime.now());
        targetUser.setPasswordResetTokenHash(hashOpaqueToken(resetToken));
        targetUser.setPasswordResetExpiresAt(expiresAt);
        AppUser saved = appUserRepository.save(targetUser);

        return new PasswordResetResponse(saved.getId(), saved.getEmail(), "/reset-password?token=" + resetToken, expiresAt);
    }

    public void completePasswordReset(String rawToken, String newPassword) {
        if (!StringUtils.hasText(rawToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Токен сброса пароля отсутствует.");
        }

        if (!StringUtils.hasText(newPassword) || newPassword.trim().length() < 8 || newPassword.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Новый пароль должен содержать от 8 до 120 символов.");
        }

        AppUser user = appUserRepository.findByPasswordResetTokenHash(hashOpaqueToken(rawToken.trim()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ссылка сброса пароля недействительна или устарела."));

        if (user.getPasswordResetExpiresAt() == null || OffsetDateTime.now().isAfter(user.getPasswordResetExpiresAt())) {
            clearPasswordResetState(user);
            appUserRepository.save(user);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ссылка сброса пароля истекла. Запросите новую ссылку у администратора.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setTokenVersion(nextTokenVersion(user));
        user.setPasswordChangedAt(OffsetDateTime.now());
        clearPasswordResetState(user);
        appUserRepository.save(user);
    }

    public UserResponse getCurrentUser(String authHeader) {
        String token = extractBearerToken(authHeader);
        Claims claims = jwtService.parseToken(token);

        Long userId = readUserId(claims.get("uid"));
        boolean isGuest = userId == null || userId <= 0;
        Integer tokenVersion = readTokenVersion(claims.get("ver"));
        List<String> roles = isGuest ? readRolesFromClaims(claims) : accessControlService.getRoleCodes(userId);
        boolean mustChangePassword = false;

        if (!isGuest) {
            AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь больше не найден. Войдите снова."));
            if (!Integer.valueOf(user.getTokenVersion() == null ? 0 : user.getTokenVersion()).equals(tokenVersion)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Сессия устарела. Войдите снова.");
            }
            mustChangePassword = user.isMustChangePassword();
        }

        return new UserResponse(
            isGuest ? 0L : userId,
            isGuest,
            claims.getSubject(),
            String.valueOf(claims.get("name")),
            roles,
            mustChangePassword
        );
    }

    public AuthResponse buildAuthResponseForUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь больше не найден. Войдите снова.");
        }

        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь больше не найден. Войдите снова."));

        return buildAuthResponse(user);
    }

    private Integer readTokenVersion(Object versionClaim) {
        if (versionClaim == null) {
            return 0;
        }
        if (versionClaim instanceof Integer value) {
            return value;
        }
        if (versionClaim instanceof Long value) {
            return value.intValue();
        }
        if (versionClaim instanceof String value && StringUtils.hasText(value)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private Long readUserId(Object uidClaim) {
        if (uidClaim == null) {
            return null;
        }
        if (uidClaim instanceof Integer value) {
            return value.longValue();
        }
        if (uidClaim instanceof Long value) {
            return value;
        }
        if (uidClaim instanceof String value && StringUtils.hasText(value)) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<String> readRolesFromClaims(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List<?> rawRoles) {
            return rawRoles.stream().map(String::valueOf).toList();
        }
        return List.of(RoleCode.GUEST.name());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private Integer nextTokenVersion(AppUser user) {
        return (user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1;
    }

    private AuthResponse buildAuthResponse(AppUser user) {
        String token = jwtService.generateToken(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getTokenVersion() == null ? 0 : user.getTokenVersion()
        );
        List<String> roles = accessControlService.getRoleCodes(user.getId());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getName(), roles, user.isMustChangePassword());
    }

    private String extractBearerToken(String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Отсутствует Bearer токен.");
        }
        return authHeader.substring(7).trim();
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashOpaqueToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 недоступен для хеширования reset token.", ex);
        }
    }

    private void clearPasswordResetState(AppUser user) {
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
    }
}
