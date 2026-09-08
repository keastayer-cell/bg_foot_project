package com.footballstats.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "w_competition_award", schema = "work")
public class CompetitionAward {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "competition_id", nullable = false) private Competition competition;
    @Column(name = "award_code", length = 48) private String awardCode;
    @Column(nullable = false, length = 120) private String title;
    @Column(name = "winner_type", nullable = false, length = 16) private String winnerType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "player_id") private Player player;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id") private Team team;
    @Column(name = "winner_name_snapshot", nullable = false, length = 180) private String winnerNameSnapshot;
    @Column(name = "team_name_snapshot", length = 180) private String teamNameSnapshot;
    @Column(name = "stat_value") private Integer statValue;
    @Column(name = "sort_order", nullable = false) private int sortOrder = 100;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt = OffsetDateTime.now();
    public Long getId() { return id; }
    public Competition getCompetition() { return competition; }
    public void setCompetition(Competition value) { competition = value; }
    public String getAwardCode() { return awardCode; }
    public void setAwardCode(String value) { awardCode = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getWinnerType() { return winnerType; }
    public void setWinnerType(String value) { winnerType = value; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player value) { player = value; }
    public Team getTeam() { return team; }
    public void setTeam(Team value) { team = value; }
    public String getWinnerNameSnapshot() { return winnerNameSnapshot; }
    public void setWinnerNameSnapshot(String value) { winnerNameSnapshot = value; }
    public String getTeamNameSnapshot() { return teamNameSnapshot; }
    public void setTeamNameSnapshot(String value) { teamNameSnapshot = value; }
    public Integer getStatValue() { return statValue; }
    public void setStatValue(Integer value) { statValue = value; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int value) { sortOrder = value; }
    public void setUpdatedAt(OffsetDateTime value) { updatedAt = value; }
}
