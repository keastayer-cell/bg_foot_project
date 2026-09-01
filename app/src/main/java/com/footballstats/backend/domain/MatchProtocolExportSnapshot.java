package com.footballstats.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "w_match_protocol_export_snapshot", schema = "work")
public class MatchProtocolExportSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_id", nullable = false)
    private Long seasonId;

    @Column(name = "match_id", nullable = false, unique = true)
    private Long matchId;

    @Column(name = "tour_sort_order", nullable = false)
    private int tourSortOrder;

    @Column(name = "kickoff_at")
    private OffsetDateTime kickoffAt;

    @Column(name = "tour_name", nullable = false, length = 120)
    private String tourName;

    @Column(name = "home_team_name", nullable = false, length = 160)
    private String homeTeamName;

    @Column(name = "away_team_name", nullable = false, length = 160)
    private String awayTeamName;

    @Column(name = "home_team_short_name", length = 60)
    private String homeTeamShortName;

    @Column(name = "away_team_short_name", length = 60)
    private String awayTeamShortName;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "home_technical_defeat", nullable = false)
    private boolean homeTechnicalDefeat;

    @Column(name = "away_technical_defeat", nullable = false)
    private boolean awayTechnicalDefeat;

    @Column(name = "note", nullable = false, columnDefinition = "TEXT")
    private String note;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "referees_json", nullable = false, columnDefinition = "TEXT")
    private String refereesJson;

    @Column(name = "teams_json", nullable = false, columnDefinition = "TEXT")
    private String teamsJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public Long getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(Long seasonId) {
        this.seasonId = seasonId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public int getTourSortOrder() {
        return tourSortOrder;
    }

    public void setTourSortOrder(int tourSortOrder) {
        this.tourSortOrder = tourSortOrder;
    }

    public OffsetDateTime getKickoffAt() {
        return kickoffAt;
    }

    public void setKickoffAt(OffsetDateTime kickoffAt) {
        this.kickoffAt = kickoffAt;
    }

    public String getTourName() {
        return tourName;
    }

    public void setTourName(String tourName) {
        this.tourName = tourName;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public void setHomeTeamName(String homeTeamName) {
        this.homeTeamName = homeTeamName;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public void setAwayTeamName(String awayTeamName) {
        this.awayTeamName = awayTeamName;
    }

    public String getHomeTeamShortName() {
        return homeTeamShortName;
    }

    public void setHomeTeamShortName(String homeTeamShortName) {
        this.homeTeamShortName = homeTeamShortName;
    }

    public String getAwayTeamShortName() {
        return awayTeamShortName;
    }

    public void setAwayTeamShortName(String awayTeamShortName) {
        this.awayTeamShortName = awayTeamShortName;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRefereesJson() {
        return refereesJson;
    }

    public void setRefereesJson(String refereesJson) {
        this.refereesJson = refereesJson;
    }

    public String getTeamsJson() {
        return teamsJson;
    }

    public void setTeamsJson(String teamsJson) {
        this.teamsJson = teamsJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
