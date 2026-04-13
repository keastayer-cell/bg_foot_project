package com.footballstats.backend.dto.auth;

import java.time.OffsetDateTime;

public class TeamScopeResponse {

    private Long teamId;
    private String teamName;
    private boolean canEditRoster;
    private boolean canEditApplication;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;

    public TeamScopeResponse(
        Long teamId,
        String teamName,
        boolean canEditRoster,
        boolean canEditApplication,
        OffsetDateTime validFrom,
        OffsetDateTime validTo
    ) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.canEditRoster = canEditRoster;
        this.canEditApplication = canEditApplication;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public boolean isCanEditRoster() {
        return canEditRoster;
    }

    public boolean isCanEditApplication() {
        return canEditApplication;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }
}
