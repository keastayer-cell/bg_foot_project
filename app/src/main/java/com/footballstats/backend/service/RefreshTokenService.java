package com.footballstats.backend.service;

import com.footballstats.backend.domain.AppUser;
import com.footballstats.backend.domain.RefreshTokenSession;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.RefreshTokenSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final AppUserRepository appUserRepository;
    private final long refreshExpiresDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
        RefreshTokenSessionRepository refreshTokenSessionRepository,
        AppUserRepository appUserRepository,
        @Value("${JWT_REFRESH_EXPIRES_DAYS:30}") long refreshExpiresDays
    ) {
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.appUserRepository = appUserRepository;
        this.refreshExpiresDays = refreshExpiresDays;
    }

    @Transactional
    public String issueTokenForUserId(Long userId, String userAgent, String ipAddress) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь больше не найден. Войдите снова."));
        return createSession(user, sanitizeUserAgent(userAgent), sanitizeIpAddress(ipAddress)).rawToken();
    }

    @Transactional
    public RefreshTokenRotation rotateToken(String rawToken, String userAgent, String ipAddress) {
        if (!StringUtils.hasText(rawToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Сессия истекла. Войдите снова.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        RefreshTokenSession currentSession = refreshTokenSessionRepository.findLockedByTokenHash(hashToken(rawToken))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Сессия истекла. Войдите снова."));

        if (currentSession.getRevokedAt() != null || currentSession.getExpiresAt() == null || now.isAfter(currentSession.getExpiresAt())) {
            revokeSession(currentSession, null, now);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Сессия истекла. Войдите снова.");
        }

        AppUser user = currentSession.getUser();
        if (user == null) {
            revokeSession(currentSession, null, now);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь больше не найден. Войдите снова.");
        }

        Integer currentTokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (!currentTokenVersion.equals(currentSession.getTokenVersion())) {
            revokeSession(currentSession, null, now);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Сессия устарела. Войдите снова.");
        }

        RefreshTokenRecord replacement = createSession(user, sanitizeUserAgent(userAgent), sanitizeIpAddress(ipAddress));
        currentSession.setLastUsedAt(now);
        currentSession.setRevokedAt(now);
        currentSession.setReplacedByTokenHash(replacement.tokenHash());
        refreshTokenSessionRepository.save(currentSession);

        return new RefreshTokenRotation(user.getId(), replacement.rawToken());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return;
        }

        refreshTokenSessionRepository.findByTokenHash(hashToken(rawToken))
            .ifPresent(session -> revokeSession(session, null, OffsetDateTime.now()));
    }

    private RefreshTokenRecord createSession(AppUser user, String userAgent, String ipAddress) {
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        RefreshTokenSession session = new RefreshTokenSession();
        session.setUser(user);
        session.setTokenHash(tokenHash);
        session.setTokenVersion(user.getTokenVersion() == null ? 0 : user.getTokenVersion());
        session.setCreatedAt(OffsetDateTime.now());
        session.setExpiresAt(OffsetDateTime.now().plusDays(refreshExpiresDays));
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        refreshTokenSessionRepository.save(session);

        return new RefreshTokenRecord(rawToken, tokenHash);
    }

    private void revokeSession(RefreshTokenSession session, String replacedByTokenHash, OffsetDateTime now) {
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(now);
        }
        if (StringUtils.hasText(replacedByTokenHash)) {
            session.setReplacedByTokenHash(replacedByTokenHash);
        }
        refreshTokenSessionRepository.save(session);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 недоступен для хеширования refresh token.", ex);
        }
    }

    private String sanitizeUserAgent(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }

    private String sanitizeIpAddress(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    private record RefreshTokenRecord(String rawToken, String tokenHash) {
    }

    public record RefreshTokenRotation(Long userId, String refreshToken) {
    }
}
