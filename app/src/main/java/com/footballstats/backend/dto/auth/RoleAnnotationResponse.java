package com.footballstats.backend.dto.auth;

public class RoleAnnotationResponse {

    private String code;
    private String userTitle;
    private String description;

    public RoleAnnotationResponse(String code, String userTitle, String description) {
        this.code = code;
        this.userTitle = userTitle;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getUserTitle() {
        return userTitle;
    }

    public String getDescription() {
        return description;
    }
}
