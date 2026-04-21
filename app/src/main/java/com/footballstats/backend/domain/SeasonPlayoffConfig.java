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
@Table(name = "w_season_playoff_config", schema = "work")
public class SeasonPlayoffConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "team_count")
    private Integer teamCount;

    @Column(name = "third_place_enabled", nullable = false)
    private boolean thirdPlaceEnabled = false;

    @Column(name = "round_of_16_legs", nullable = false)
    private int roundOf16Legs = 1;

    @Column(name = "quarterfinal_legs", nullable = false)
    private int quarterfinalLegs = 1;

    @Column(name = "semifinal_legs", nullable = false)
    private int semifinalLegs = 1;

    @Column(name = "final_legs", nullable = false)
    private int finalLegs = 1;

    @Column(name = "third_place_legs", nullable = false)
    private int thirdPlaceLegs = 1;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getTeamCount() {
        return teamCount;
    }

    public void setTeamCount(Integer teamCount) {
        this.teamCount = teamCount;
    }

    public boolean isThirdPlaceEnabled() {
        return thirdPlaceEnabled;
    }

    public void setThirdPlaceEnabled(boolean thirdPlaceEnabled) {
        this.thirdPlaceEnabled = thirdPlaceEnabled;
    }

    public int getRoundOf16Legs() {
        return roundOf16Legs;
    }

    public void setRoundOf16Legs(int roundOf16Legs) {
        this.roundOf16Legs = roundOf16Legs;
    }

    public int getQuarterfinalLegs() {
        return quarterfinalLegs;
    }

    public void setQuarterfinalLegs(int quarterfinalLegs) {
        this.quarterfinalLegs = quarterfinalLegs;
    }

    public int getSemifinalLegs() {
        return semifinalLegs;
    }

    public void setSemifinalLegs(int semifinalLegs) {
        this.semifinalLegs = semifinalLegs;
    }

    public int getFinalLegs() {
        return finalLegs;
    }

    public void setFinalLegs(int finalLegs) {
        this.finalLegs = finalLegs;
    }

    public int getThirdPlaceLegs() {
        return thirdPlaceLegs;
    }

    public void setThirdPlaceLegs(int thirdPlaceLegs) {
        this.thirdPlaceLegs = thirdPlaceLegs;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Long updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}