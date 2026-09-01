package com.footballstats.backend.controller;

import com.footballstats.backend.domain.CompetitionRosterMode;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.CompetitionService;
import com.footballstats.backend.service.AccessControlService;
import com.footballstats.backend.service.SeasonService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/seasons/{seasonId}/competitions")
public class CompetitionController {
    private final CompetitionService competitionService;
    private final com.footballstats.backend.service.CompetitionStatsService competitionStatsService;
    private final AccessControlService accessControlService;
    private final SeasonService seasonService;

    public CompetitionController(CompetitionService competitionService, com.footballstats.backend.service.CompetitionStatsService competitionStatsService,
                                 AccessControlService accessControlService, SeasonService seasonService) {
        this.competitionService = competitionService;
        this.competitionStatsService = competitionStatsService;
        this.accessControlService = accessControlService;
        this.seasonService = seasonService;
    }

    @GetMapping
    public ResponseEntity<List<CompetitionService.CompetitionData>> list(@PathVariable Long seasonId) {
        return ResponseEntity.ok(competitionService.list(seasonId));
    }

    @PostMapping("/cups")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionService.CompetitionData> createCup(
        @PathVariable Long seasonId,
        @Valid @RequestBody CompetitionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competitionService.createCup(seasonId, request.toSettings(), userId(authentication)));
    }

    @PostMapping("/championships")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionService.CompetitionData> createChampionship(
        @PathVariable Long seasonId,
        @Valid @RequestBody ChampionshipRequest request,
        Authentication authentication
    ) {
        Long actorUserId = userId(authentication);
        CompetitionService.CompetitionData result = competitionService.createChampionship(seasonId, request.name(), actorUserId);
        seasonService.initializeChampionship(seasonId, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{competitionId}/championship")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionService.CompetitionData> renameChampionship(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId,
        @Valid @RequestBody ChampionshipRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(competitionService.renameChampionship(seasonId, competitionId, request.name(), userId(authentication)));
    }

    @PutMapping("/{competitionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionService.CompetitionData> updateCup(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId,
        @Valid @RequestBody CompetitionRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(competitionService.updateCup(seasonId, competitionId, request.toSettings(), userId(authentication)));
    }

    @DeleteMapping("/{competitionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionService.CompetitionData> deactivate(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(competitionService.deactivate(seasonId, competitionId, userId(authentication)));
    }

    @PostMapping("/{competitionId}/draw")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionService.CompetitionData> draw(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId,
        @RequestBody(required = false) DrawRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(competitionService.draw(seasonId, competitionId, request == null ? List.of() : request.orderedTeamIds(), userId(authentication)));
    }

    @PostMapping("/{competitionId}/draw/confirm")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionService.CompetitionData> confirmDraw(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(competitionService.confirmDraw(seasonId, competitionId, userId(authentication)));
    }

    @GetMapping("/{competitionId}/roster")
    public ResponseEntity<List<CompetitionService.RosterPlayerData>> roster(@PathVariable Long seasonId, @PathVariable Long competitionId) {
        return ResponseEntity.ok(competitionService.roster(seasonId, competitionId));
    }

    @GetMapping("/{competitionId}/player-stats")
    public ResponseEntity<List<com.footballstats.backend.service.CompetitionStatsService.PlayerStats>> playerStats(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId
    ) {
        if (!competitionService.getCompetition(competitionId).getSeason().getId().equals(seasonId)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Соревнование не найдено.");
        }
        return ResponseEntity.ok(competitionStatsService.playerStats(competitionId));
    }

    @PostMapping("/{competitionId}/ties/{tieId}/matches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionService.CompetitionData> scheduleTie(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId,
        @PathVariable Long tieId,
        @RequestBody TieScheduleRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(competitionService.scheduleTie(seasonId, competitionId, tieId, request.kickoffDates(), userId(authentication)));
    }

    @PostMapping("/{competitionId}/ties/{tieId}/winner")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<CompetitionService.CompetitionData> chooseTieWinner(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId,
        @PathVariable Long tieId,
        @RequestBody TieWinnerRequest request
    ) {
        return ResponseEntity.ok(competitionService.chooseTieWinner(seasonId, competitionId, tieId, request.homePenaltyScore(), request.awayPenaltyScore()));
    }

    @PostMapping("/{competitionId}/roster")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE','TEAM_REP')")
    public ResponseEntity<List<CompetitionService.RosterPlayerData>> addRosterPlayers(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId,
        @RequestBody RosterAddRequest request,
        Authentication authentication
    ) {
        requireRosterPermission(authentication, request.teamId());
        return ResponseEntity.ok(competitionService.addRosterPlayers(seasonId, competitionId, request.teamId(), request.playerIds(), userId(authentication)));
    }

    @GetMapping("/{competitionId}/roster/candidates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE','TEAM_REP')")
    public ResponseEntity<List<CompetitionService.RosterPlayerData>> rosterCandidates(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId,
        @RequestParam Long teamId,
        Authentication authentication
    ) {
        requireRosterPermission(authentication, teamId);
        return ResponseEntity.ok(competitionService.rosterCandidates(seasonId, competitionId, teamId));
    }

    @DeleteMapping("/{competitionId}/roster/{playerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE','TEAM_REP')")
    public ResponseEntity<List<CompetitionService.RosterPlayerData>> removeRosterPlayer(
        @PathVariable Long seasonId,
        @PathVariable Long competitionId,
        @PathVariable Long playerId,
        @RequestParam Long teamId,
        Authentication authentication
    ) {
        requireRosterPermission(authentication, teamId);
        return ResponseEntity.ok(competitionService.removeRosterPlayer(seasonId, competitionId, teamId, playerId, userId(authentication)));
    }

    private void requireRosterPermission(Authentication authentication, Long teamId) {
        boolean privileged = authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN") || authority.getAuthority().equals("ROLE_REFEREE"));
        if (!privileged && (teamId == null || !accessControlService.hasTeamPermission(userId(authentication), teamId, "ROSTER_EDIT"))) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Нет права менять заявку этой команды.");
        }
    }

    private Long userId(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal ? principal.getUserId() : null;
    }

    public record CompetitionRequest(
        @NotBlank String name,
        CompetitionRosterMode rosterMode,
        Integer maxRosterSize,
        Integer matchRosterSize,
        Integer playersOnField,
        Integer regularTieLegs,
        Integer finalLegs,
        boolean thirdPlaceEnabled,
        Integer thirdPlaceLegs,
        boolean extraTimeEnabled,
        Integer extraTimeMinutes,
        boolean penaltiesEnabled,
        int yellowCardsForSuspension,
        Integer yellowSuspensionMatches,
        Integer redSuspensionMatches,
        List<Long> teamIds
    ) {
        CompetitionService.CompetitionSettings toSettings() {
            return new CompetitionService.CompetitionSettings(name, rosterMode, maxRosterSize, matchRosterSize, playersOnField,
                regularTieLegs, finalLegs, thirdPlaceEnabled, thirdPlaceLegs, extraTimeEnabled, extraTimeMinutes,
                penaltiesEnabled, yellowCardsForSuspension, yellowSuspensionMatches, redSuspensionMatches, teamIds);
        }
    }
    public record DrawRequest(List<Long> orderedTeamIds) {}
    public record RosterAddRequest(Long teamId, List<Long> playerIds) {}
    public record TieScheduleRequest(List<OffsetDateTime> kickoffDates) {}
    public record TieWinnerRequest(Integer homePenaltyScore, Integer awayPenaltyScore) {}
    public record ChampionshipRequest(@NotBlank String name) {}
}
