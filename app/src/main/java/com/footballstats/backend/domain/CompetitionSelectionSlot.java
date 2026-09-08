package com.footballstats.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "w_competition_selection_slot", schema = "work")
public class CompetitionSelectionSlot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "competition_id", nullable = false) private Competition competition;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "player_id", nullable = false) private Player player;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id") private Team team;
    @Column(name = "player_name_snapshot", nullable = false, length = 180) private String playerNameSnapshot;
    @Column(name = "team_name_snapshot", length = 180) private String teamNameSnapshot;
    @Column(name = "team_short_name_snapshot", length = 80) private String teamShortNameSnapshot;
    @Column(name = "position_label", nullable = false, length = 40) private String positionLabel;
    @Column(name = "x_percent", nullable = false) private int xPercent;
    @Column(name = "y_percent", nullable = false) private int yPercent;
    @Column(name = "sort_order", nullable = false) private int sortOrder = 100;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    public Long getId() { return id; }
    public void setCompetition(Competition value) { competition = value; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player value) { player = value; }
    public Team getTeam() { return team; }
    public void setTeam(Team value) { team = value; }
    public String getPlayerNameSnapshot() { return playerNameSnapshot; }
    public void setPlayerNameSnapshot(String value) { playerNameSnapshot = value; }
    public String getTeamNameSnapshot() { return teamNameSnapshot; }
    public void setTeamNameSnapshot(String value) { teamNameSnapshot = value; }
    public String getTeamShortNameSnapshot() { return teamShortNameSnapshot; }
    public void setTeamShortNameSnapshot(String value) { teamShortNameSnapshot = value; }
    public String getPositionLabel() { return positionLabel; }
    public void setPositionLabel(String value) { positionLabel = value; }
    public int getXPercent() { return xPercent; }
    public void setXPercent(int value) { xPercent = value; }
    public int getYPercent() { return yPercent; }
    public void setYPercent(int value) { yPercent = value; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int value) { sortOrder = value; }
}
