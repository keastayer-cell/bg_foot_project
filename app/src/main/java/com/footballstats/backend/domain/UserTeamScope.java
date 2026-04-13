package com.footballstats.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "w_user_team_scope", schema = "work")
public class UserTeamScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "granted_by_user_id")
    private Long grantedByUserId;

    @Column(name = "granted_at", nullable = false)
    private OffsetDateTime grantedAt = OffsetDateTime.now();

    @Column(name = "valid_from")
    private OffsetDateTime validFrom;

    @Column(name = "valid_to")
    private OffsetDateTime validTo;

    @Column(name = "can_edit_roster", nullable = false)
    private boolean canEditRoster = true;

    @Column(name = "can_edit_application", nullable = false)
    private boolean canEditApplication = true;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Long getGrantedByUserId() {
        return grantedByUserId;
    }

    public void setGrantedByUserId(Long grantedByUserId) {
        this.grantedByUserId = grantedByUserId;
    }

    public OffsetDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(OffsetDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(OffsetDateTime validTo) {
        this.validTo = validTo;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
