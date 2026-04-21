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
@Table(name = "w_season_playoff_tie", schema = "work")
public class SeasonPlayoffTie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bracket_id", nullable = false)
    private SeasonPlayoffBracket bracket;

    @Column(name = "round_code", nullable = false, length = 32)
    private String roundCode;

    @Column(name = "round_order", nullable = false)
    private int roundOrder;

    @Column(name = "slot_order", nullable = false)
    private int slotOrder;

    @Column(name = "leg_count", nullable = false)
    private int legCount = 1;

    @Column(length = 120)
    private String title;

    @Column(name = "home_seed")
    private Integer homeSeed;

    @Column(name = "away_seed")
    private Integer awaySeed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_source_tie_id")
    private SeasonPlayoffTie homeSourceTie;

    @Column(name = "home_source_result", length = 16)
    private String homeSourceResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_source_tie_id")
    private SeasonPlayoffTie awaySourceTie;

    @Column(name = "away_source_result", length = 16)
    private String awaySourceResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;

    @Column(name = "aggregate_home_score")
    private Integer aggregateHomeScore;

    @Column(name = "aggregate_away_score")
    private Integer aggregateAwayScore;

    @Column(nullable = false, length = 32)
    private String status = "PLANNED";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public SeasonPlayoffBracket getBracket() {
        return bracket;
    }

    public void setBracket(SeasonPlayoffBracket bracket) {
        this.bracket = bracket;
    }

    public String getRoundCode() {
        return roundCode;
    }

    public void setRoundCode(String roundCode) {
        this.roundCode = roundCode;
    }

    public int getRoundOrder() {
        return roundOrder;
    }

    public void setRoundOrder(int roundOrder) {
        this.roundOrder = roundOrder;
    }

    public int getSlotOrder() {
        return slotOrder;
    }

    public void setSlotOrder(int slotOrder) {
        this.slotOrder = slotOrder;
    }

    public int getLegCount() {
        return legCount;
    }

    public void setLegCount(int legCount) {
        this.legCount = legCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getHomeSeed() {
        return homeSeed;
    }

    public void setHomeSeed(Integer homeSeed) {
        this.homeSeed = homeSeed;
    }

    public Integer getAwaySeed() {
        return awaySeed;
    }

    public void setAwaySeed(Integer awaySeed) {
        this.awaySeed = awaySeed;
    }

    public SeasonPlayoffTie getHomeSourceTie() {
        return homeSourceTie;
    }

    public void setHomeSourceTie(SeasonPlayoffTie homeSourceTie) {
        this.homeSourceTie = homeSourceTie;
    }

    public String getHomeSourceResult() {
        return homeSourceResult;
    }

    public void setHomeSourceResult(String homeSourceResult) {
        this.homeSourceResult = homeSourceResult;
    }

    public SeasonPlayoffTie getAwaySourceTie() {
        return awaySourceTie;
    }

    public void setAwaySourceTie(SeasonPlayoffTie awaySourceTie) {
        this.awaySourceTie = awaySourceTie;
    }

    public String getAwaySourceResult() {
        return awaySourceResult;
    }

    public void setAwaySourceResult(String awaySourceResult) {
        this.awaySourceResult = awaySourceResult;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(Team homeTeam) {
        this.homeTeam = homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(Team awayTeam) {
        this.awayTeam = awayTeam;
    }

    public Team getWinnerTeam() {
        return winnerTeam;
    }

    public void setWinnerTeam(Team winnerTeam) {
        this.winnerTeam = winnerTeam;
    }

    public Integer getAggregateHomeScore() {
        return aggregateHomeScore;
    }

    public void setAggregateHomeScore(Integer aggregateHomeScore) {
        this.aggregateHomeScore = aggregateHomeScore;
    }

    public Integer getAggregateAwayScore() {
        return aggregateAwayScore;
    }

    public void setAggregateAwayScore(Integer aggregateAwayScore) {
        this.aggregateAwayScore = aggregateAwayScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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