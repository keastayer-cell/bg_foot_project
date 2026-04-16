package com.footballstats.backend.dto.auth;

import java.util.List;

public class UserResponse {

    private Long id;
    private boolean guest;
    private String email;
    private String name;
    private List<String> roles;
    private boolean mustChangePassword;

    public UserResponse(Long id, boolean guest, String email, String name, List<String> roles, boolean mustChangePassword) {
        this.id = id;
        this.guest = guest;
        this.email = email;
        this.name = name;
        this.roles = roles;
        this.mustChangePassword = mustChangePassword;
    }

    public Long getId() {
        return id;
    }

    public boolean isGuest() {
        return guest;
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

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
