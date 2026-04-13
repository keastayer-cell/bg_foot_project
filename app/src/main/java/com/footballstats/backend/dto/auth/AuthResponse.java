package com.footballstats.backend.dto.auth;

import java.util.List;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private String name;
    private List<String> roles;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long userId, String email, String name, List<String> roles) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public List<String> getRoles() {
        return roles;
    }
}
