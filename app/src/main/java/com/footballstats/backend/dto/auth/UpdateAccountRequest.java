package com.footballstats.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
    @NotBlank(message = "Email обязателен.")
    @Email(message = "Укажите корректный email.")
    @Size(max = 255, message = "Email не должен быть длиннее 255 символов.")
    String email,
    @NotBlank(message = "Имя обязательно.")
    @Size(min = 2, max = 120, message = "Имя должно содержать от 2 до 120 символов.")
    String name
) {
}
