package com.footballstats.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "w_season_standings_config", schema = "work")
public class SeasonStandingsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false, unique = true)
    private Season season;

    @Column(name = "win_points", nullable = false)
    private int winPoints = 3;

    @Column(name = "draw_points", nullable = false)
    private int drawPoints = 1;

    @Column(name = "loss_points", nullable = false)
    private int lossPoints = 0;

    @Column(name = "ranking_rules_json", nullable = false)
    private String rankingRulesJson = "[\"POINTS\",\"GOAL_DIFFERENCE\",\"GOALS_FOR\",\"ALPHABETICAL\"]";

    @Column(name = "yellow_cards_for_suspension", nullable = false)
    private int yellowCardsForSuspension = 0;

    @Column(name = "red_cards_for_suspension", nullable = false)
    private int redCardsForSuspension = 0;

    @Column(name = "last_calculated_at")
    private OffsetDateTime lastCalculatedAt;

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

    public int getWinPoints() {
        return winPoints;
    }

    public void setWinPoints(int winPoints) {
        this.winPoints = winPoints;
    }

    public int getDrawPoints() {
        return drawPoints;
    }

    public void setDrawPoints(int drawPoints) {
        this.drawPoints = drawPoints;
    }

    public int getLossPoints() {
        return lossPoints;
    }

    public void setLossPoints(int lossPoints) {
        this.lossPoints = lossPoints;
    }

    public String getRankingRulesJson() {
        return rankingRulesJson;
    }

    public void setRankingRulesJson(String rankingRulesJson) {
        this.rankingRulesJson = rankingRulesJson;
    }

    public int getYellowCardsForSuspension() {
        return yellowCardsForSuspension;
    }

    public void setYellowCardsForSuspension(int yellowCardsForSuspension) {
        this.yellowCardsForSuspension = yellowCardsForSuspension;
    }

    public int getRedCardsForSuspension() {
        return redCardsForSuspension;
    }

    public void setRedCardsForSuspension(int redCardsForSuspension) {
        this.redCardsForSuspension = redCardsForSuspension;
    }

    public OffsetDateTime getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void setLastCalculatedAt(OffsetDateTime lastCalculatedAt) {
        this.lastCalculatedAt = lastCalculatedAt;
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