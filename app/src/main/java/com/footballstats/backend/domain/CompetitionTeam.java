package com.footballstats.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "w_competition_team", schema = "work")
public class CompetitionTeam {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "competition_id", nullable = false) private Competition competition;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "team_id", nullable = false) private Team team;
    @Column(name = "seed_number") private Integer seedNumber;
    @Column(name = "created_by_user_id") private Long createdByUserId;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    public Long getId() { return id; }
    public Competition getCompetition() { return competition; }
    public void setCompetition(Competition value) { competition = value; }
    public Team getTeam() { return team; }
    public void setTeam(Team value) { team = value; }
    public Integer getSeedNumber() { return seedNumber; }
    public void setSeedNumber(Integer value) { seedNumber = value; }
    public void setCreatedByUserId(Long value) { createdByUserId = value; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }
}
