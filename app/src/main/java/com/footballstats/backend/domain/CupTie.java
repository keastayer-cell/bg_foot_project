package com.footballstats.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "w_cup_tie", schema = "work")
public class CupTie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "competition_id", nullable = false) private Competition competition;
    @Column(name = "round_code", nullable = false, length = 40) private String roundCode;
    @Column(name = "round_order", nullable = false) private int roundOrder;
    @Column(name = "slot_order", nullable = false) private int slotOrder;
    @Column(name = "leg_count", nullable = false) private int legCount = 1;
    @Column(nullable = false, length = 160) private String title;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "home_source_tie_id") private CupTie homeSourceTie;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "away_source_tie_id") private CupTie awaySourceTie;
    @Column(name = "home_source_result", length = 16) private String homeSourceResult;
    @Column(name = "away_source_result", length = 16) private String awaySourceResult;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "home_team_id") private Team homeTeam;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "away_team_id") private Team awayTeam;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "winner_team_id") private Team winnerTeam;
    @Column(nullable = false, length = 24) private String status = "PLANNED";
    @Column(name = "aggregate_home_score") private Integer aggregateHomeScore;
    @Column(name = "aggregate_away_score") private Integer aggregateAwayScore;
    @Column(name = "home_penalty_score") private Integer homePenaltyScore;
    @Column(name = "away_penalty_score") private Integer awayPenaltyScore;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt = OffsetDateTime.now();
    public Long getId() { return id; }
    public Competition getCompetition() { return competition; }
    public void setCompetition(Competition value) { competition = value; }
    public String getRoundCode() { return roundCode; }
    public void setRoundCode(String value) { roundCode = value; }
    public int getRoundOrder() { return roundOrder; }
    public void setRoundOrder(int value) { roundOrder = value; }
    public int getSlotOrder() { return slotOrder; }
    public void setSlotOrder(int value) { slotOrder = value; }
    public int getLegCount() { return legCount; }
    public void setLegCount(int value) { legCount = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public CupTie getHomeSourceTie() { return homeSourceTie; }
    public void setHomeSourceTie(CupTie value) { homeSourceTie = value; }
    public CupTie getAwaySourceTie() { return awaySourceTie; }
    public void setAwaySourceTie(CupTie value) { awaySourceTie = value; }
    public String getHomeSourceResult() { return homeSourceResult; }
    public void setHomeSourceResult(String value) { homeSourceResult = value; }
    public String getAwaySourceResult() { return awaySourceResult; }
    public void setAwaySourceResult(String value) { awaySourceResult = value; }
    public Team getHomeTeam() { return homeTeam; }
    public void setHomeTeam(Team value) { homeTeam = value; }
    public Team getAwayTeam() { return awayTeam; }
    public void setAwayTeam(Team value) { awayTeam = value; }
    public Team getWinnerTeam() { return winnerTeam; }
    public void setWinnerTeam(Team value) { winnerTeam = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Integer getAggregateHomeScore() { return aggregateHomeScore; }
    public void setAggregateHomeScore(Integer value) { aggregateHomeScore = value; }
    public Integer getAggregateAwayScore() { return aggregateAwayScore; }
    public void setAggregateAwayScore(Integer value) { aggregateAwayScore = value; }
    public Integer getHomePenaltyScore() { return homePenaltyScore; }
    public void setHomePenaltyScore(Integer value) { homePenaltyScore = value; }
    public Integer getAwayPenaltyScore() { return awayPenaltyScore; }
    public void setAwayPenaltyScore(Integer value) { awayPenaltyScore = value; }
    public void setUpdatedAt(OffsetDateTime value) { updatedAt = value; }
}
