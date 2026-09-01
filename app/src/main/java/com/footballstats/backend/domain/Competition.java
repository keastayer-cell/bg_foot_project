package com.footballstats.backend.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "w_competition", schema = "work")
public class Competition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "season_id", nullable = false)
    private Season season;
    @Column(nullable = false, length = 160)
    private String name;
    @Enumerated(EnumType.STRING) @Column(name = "competition_type", nullable = false, length = 24)
    private CompetitionType type;
    @Column(nullable = false, length = 24)
    private String status = "DRAFT";
    @Enumerated(EnumType.STRING) @Column(name = "roster_mode", nullable = false, length = 24)
    private CompetitionRosterMode rosterMode = CompetitionRosterMode.SEASON_SHARED;
    @Column(name = "max_roster_size") private Integer maxRosterSize;
    @Column(name = "match_roster_size") private Integer matchRosterSize;
    @Column(name = "players_on_field", nullable = false) private int playersOnField = 11;
    @Column(name = "regular_tie_legs", nullable = false) private int regularTieLegs = 1;
    @Column(name = "final_legs", nullable = false) private int finalLegs = 1;
    @Column(name = "third_place_enabled", nullable = false) private boolean thirdPlaceEnabled;
    @Column(name = "third_place_legs", nullable = false) private int thirdPlaceLegs = 1;
    @Column(name = "extra_time_enabled", nullable = false) private boolean extraTimeEnabled = true;
    @Column(name = "extra_time_minutes", nullable = false) private int extraTimeMinutes = 30;
    @Column(name = "penalties_enabled", nullable = false) private boolean penaltiesEnabled = true;
    @Column(name = "yellow_cards_for_suspension", nullable = false) private int yellowCardsForSuspension;
    @Column(name = "yellow_suspension_matches", nullable = false) private int yellowSuspensionMatches = 1;
    @Column(name = "red_suspension_matches", nullable = false) private int redSuspensionMatches = 1;
    @Column(name = "draw_status", nullable = false, length = 24) private String drawStatus = "NOT_DRAWN";
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_by_user_id") private Long createdByUserId;
    @Column(name = "updated_by_user_id") private Long updatedByUserId;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public Season getSeason() { return season; }
    public void setSeason(Season season) { this.season = season; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CompetitionType getType() { return type; }
    public void setType(CompetitionType type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public CompetitionRosterMode getRosterMode() { return rosterMode; }
    public void setRosterMode(CompetitionRosterMode rosterMode) { this.rosterMode = rosterMode; }
    public Integer getMaxRosterSize() { return maxRosterSize; }
    public void setMaxRosterSize(Integer maxRosterSize) { this.maxRosterSize = maxRosterSize; }
    public Integer getMatchRosterSize() { return matchRosterSize; }
    public void setMatchRosterSize(Integer matchRosterSize) { this.matchRosterSize = matchRosterSize; }
    public int getPlayersOnField() { return playersOnField; }
    public void setPlayersOnField(int playersOnField) { this.playersOnField = playersOnField; }
    public int getRegularTieLegs() { return regularTieLegs; }
    public void setRegularTieLegs(int regularTieLegs) { this.regularTieLegs = regularTieLegs; }
    public int getFinalLegs() { return finalLegs; }
    public void setFinalLegs(int finalLegs) { this.finalLegs = finalLegs; }
    public boolean isThirdPlaceEnabled() { return thirdPlaceEnabled; }
    public void setThirdPlaceEnabled(boolean thirdPlaceEnabled) { this.thirdPlaceEnabled = thirdPlaceEnabled; }
    public int getThirdPlaceLegs() { return thirdPlaceLegs; }
    public void setThirdPlaceLegs(int thirdPlaceLegs) { this.thirdPlaceLegs = thirdPlaceLegs; }
    public boolean isExtraTimeEnabled() { return extraTimeEnabled; }
    public void setExtraTimeEnabled(boolean extraTimeEnabled) { this.extraTimeEnabled = extraTimeEnabled; }
    public int getExtraTimeMinutes() { return extraTimeMinutes; }
    public void setExtraTimeMinutes(int extraTimeMinutes) { this.extraTimeMinutes = extraTimeMinutes; }
    public boolean isPenaltiesEnabled() { return penaltiesEnabled; }
    public void setPenaltiesEnabled(boolean penaltiesEnabled) { this.penaltiesEnabled = penaltiesEnabled; }
    public int getYellowCardsForSuspension() { return yellowCardsForSuspension; }
    public void setYellowCardsForSuspension(int value) { this.yellowCardsForSuspension = value; }
    public int getYellowSuspensionMatches() { return yellowSuspensionMatches; }
    public void setYellowSuspensionMatches(int value) { this.yellowSuspensionMatches = value; }
    public int getRedSuspensionMatches() { return redSuspensionMatches; }
    public void setRedSuspensionMatches(int value) { this.redSuspensionMatches = value; }
    public String getDrawStatus() { return drawStatus; }
    public void setDrawStatus(String drawStatus) { this.drawStatus = drawStatus; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long value) { this.createdByUserId = value; }
    public Long getUpdatedByUserId() { return updatedByUserId; }
    public void setUpdatedByUserId(Long value) { this.updatedByUserId = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt = value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt = value; }
}
