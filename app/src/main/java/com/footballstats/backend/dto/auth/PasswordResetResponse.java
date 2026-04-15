package com.footballstats.backend.dto.auth;

public class PasswordResetResponse {

    private Long userId;
    private String email;
    private boolean mustChangePassword;
    private String temporaryPassword;

    public PasswordResetResponse(Long userId, String email, boolean mustChangePassword, String temporaryPassword) {
        this.userId = userId;
        this.email = email;
        this.mustChangePassword = mustChangePassword;
        this.temporaryPassword = temporaryPassword;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }
}