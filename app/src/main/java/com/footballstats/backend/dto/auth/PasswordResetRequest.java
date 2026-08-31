package com.footballstats.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
    @Email(message = "Введите корректный email.")
    @NotBlank(message = "Email обязателен.")
    String email
) {
}
