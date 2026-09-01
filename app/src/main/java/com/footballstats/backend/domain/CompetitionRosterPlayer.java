package com.footballstats.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "w_competition_roster_player", schema = "work")
public class CompetitionRosterPlayer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "competition_id", nullable = false) private Competition competition;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "team_id", nullable = false) private Team team;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "player_id", nullable = false) private Player player;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_by_user_id") private Long createdByUserId;
    @Column(name = "updated_by_user_id") private Long updatedByUserId;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt = OffsetDateTime.now();
    public Long getId() { return id; }
    public Competition getCompetition() { return competition; }
    public void setCompetition(Competition value) { competition = value; }
    public Team getTeam() { return team; }
    public void setTeam(Team value) { team = value; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player value) { player = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
    public void setCreatedByUserId(Long value) { createdByUserId = value; }
    public void setUpdatedByUserId(Long value) { updatedByUserId = value; }
    public void setUpdatedAt(OffsetDateTime value) { updatedAt = value; }
}
