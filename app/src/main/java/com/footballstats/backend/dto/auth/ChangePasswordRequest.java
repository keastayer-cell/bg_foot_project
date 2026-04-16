package com.footballstats.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank(message = "Текущий пароль обязателен.")
    @Size(min = 6, max = 120, message = "Текущий пароль должен быть от 6 до 120 символов.")
    private String currentPassword;

    @NotBlank(message = "Новый пароль обязателен.")
    @Size(min = 6, max = 120, message = "Новый пароль должен быть от 6 до 120 символов.")
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}