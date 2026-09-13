package com.footballstats.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "w_tour_match", schema = "work")
public class TourMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @OneToOne(mappedBy = "match", fetch = FetchType.LAZY)
    private MatchProtocol protocol;

    @Column(name = "kickoff_at", nullable = false)
    private OffsetDateTime kickoffAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private LeagueVenue venue;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_status", nullable = false, length = 24)
    private MatchScheduleStatus scheduleStatus = MatchScheduleStatus.SCHEDULED;

    @Column(name = "original_kickoff_at")
    private OffsetDateTime originalKickoffAt;

    @Column(name = "schedule_change_reason", length = 500)
    private String scheduleChangeReason;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public Tour getTour() {
        return tour;
    }

    public void setTour(Tour tour) {
        this.tour = tour;
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

    public MatchProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(MatchProtocol protocol) {
        this.protocol = protocol;
    }

    public OffsetDateTime getKickoffAt() {
        return kickoffAt;
    }

    public void setKickoffAt(OffsetDateTime kickoffAt) {
        this.kickoffAt = kickoffAt;
    }

    public LeagueVenue getVenue() { return venue; }
    public void setVenue(LeagueVenue venue) { this.venue = venue; }
    public MatchScheduleStatus getScheduleStatus() { return scheduleStatus; }
    public void setScheduleStatus(MatchScheduleStatus value) { scheduleStatus = value; }
    public OffsetDateTime getOriginalKickoffAt() { return originalKickoffAt; }
    public void setOriginalKickoffAt(OffsetDateTime value) { originalKickoffAt = value; }
    public String getScheduleChangeReason() { return scheduleChangeReason; }
    public void setScheduleChangeReason(String value) { scheduleChangeReason = value; }

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
