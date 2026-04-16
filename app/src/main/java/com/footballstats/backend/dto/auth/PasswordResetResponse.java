package com.footballstats.backend.dto.auth;

import java.time.OffsetDateTime;

public class PasswordResetResponse {

    private Long userId;
    private String email;
    private String resetPath;
    private OffsetDateTime expiresAt;

    public PasswordResetResponse(Long userId, String email, String resetPath, OffsetDateTime expiresAt) {
        this.userId = userId;
        this.email = email;
        this.resetPath = resetPath;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getResetPath() {
        return resetPath;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}