package com.footballstats.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @Email(message = "Введите корректный email.")
    @NotBlank(message = "Email обязателен.")
    private String email;

    @NotBlank(message = "Пароль обязателен.")
    @Size(min = 6, max = 120, message = "Пароль должен быть от 6 до 120 символов.")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
