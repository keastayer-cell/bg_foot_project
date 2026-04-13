package com.footballstats.backend.dto.auth;

import jakarta.validation.constraints.NotNull;

public class AssignTeamScopeRequest {

    @NotNull(message = "teamId обязателен.")
    private Long teamId;

    private boolean canEditRoster = true;
    private boolean canEditApplication = true;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public boolean isCanEditRoster() {
        return canEditRoster;
    }

    public void setCanEditRoster(boolean canEditRoster) {
        this.canEditRoster = canEditRoster;
    }

    public boolean isCanEditApplication() {
        return canEditApplication;
    }

    public void setCanEditApplication(boolean canEditApplication) {
        this.canEditApplication = canEditApplication;
    }
}
