package com.footballstats.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "w_match_protocol", schema = "work")
public class MatchProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private TourMatch match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MatchProtocolStatus status = MatchProtocolStatus.SCHEDULED;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "home_technical_defeat", nullable = false)
    private boolean homeTechnicalDefeat;

    @Column(name = "away_technical_defeat", nullable = false)
    private boolean awayTechnicalDefeat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "best_player_id")
    private Player bestPlayer;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

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

    public TourMatch getMatch() {
        return match;
    }

    public void setMatch(TourMatch match) {
        this.match = match;
    }

    public MatchProtocolStatus getStatus() {
        return status;
    }

    public void setStatus(MatchProtocolStatus status) {
        this.status = status;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    public boolean isHomeTechnicalDefeat() {
        return homeTechnicalDefeat;
    }

    public void setHomeTechnicalDefeat(boolean homeTechnicalDefeat) {
        this.homeTechnicalDefeat = homeTechnicalDefeat;
    }

    public boolean isAwayTechnicalDefeat() {
        return awayTechnicalDefeat;
    }

    public void setAwayTechnicalDefeat(boolean awayTechnicalDefeat) {
        this.awayTechnicalDefeat = awayTechnicalDefeat;
    }

    public Player getBestPlayer() {
        return bestPlayer;
    }

    public void setBestPlayer(Player bestPlayer) {
        this.bestPlayer = bestPlayer;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
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
