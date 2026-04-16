package com.footballstats.backend.dto.auth;

import java.util.List;

public class UserAccessResponse {

    private Long userId;
    private String email;
    private String name;
    private List<String> roles;
    private List<RoleAnnotationResponse> roleAnnotations;
    private List<TeamScopeResponse> teamScopes;
    private boolean mustChangePassword;

    public UserAccessResponse(
        Long userId,
        String email,
        String name,
        List<String> roles,
        List<RoleAnnotationResponse> roleAnnotations,
        List<TeamScopeResponse> teamScopes,
        boolean mustChangePassword
    ) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.roles = roles;
        this.roleAnnotations = roleAnnotations;
        this.teamScopes = teamScopes;
        this.mustChangePassword = mustChangePassword;
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

    public List<RoleAnnotationResponse> getRoleAnnotations() {
        return roleAnnotations;
    }

    public List<TeamScopeResponse> getTeamScopes() {
        return teamScopes;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
