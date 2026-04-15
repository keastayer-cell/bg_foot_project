package com.footballstats.backend.controller;

import com.footballstats.backend.domain.MatchEvent;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.MatchProtocolService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchProtocolService matchProtocolService;

    public MatchController(MatchProtocolService matchProtocolService) {
        this.matchProtocolService = matchProtocolService;
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchDetailsResponse> getMatch(@PathVariable Long matchId) {
        return ResponseEntity.ok(toResponse(matchProtocolService.getMatchDetails(matchId)));
    }

    @PutMapping("/{matchId}/protocol")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MatchDetailsResponse> upsertProtocol(
        @PathVariable Long matchId,
        @Valid @RequestBody MatchProtocolUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(toResponse(matchProtocolService.upsertProtocol(
            matchId,
            request.status(),
            request.homeScore(),
            request.awayScore(),
            request.homeTechnicalDefeat(),
            request.awayTechnicalDefeat(),
            request.bestPlayerId(),
            request.notes(),
            request.startedAt(),
            request.finishedAt(),
            request.playerStats() == null ? List.of() : request.playerStats().stream()
                .map(playerStat -> new MatchProtocolService.PlayerProtocolStatDraft(
                    playerStat.teamId(),
                    playerStat.playerId(),
                    playerStat.goals(),
                    playerStat.yellowCards(),
                    playerStat.redCards()
                ))
                .toList(),
            currentUserId(authentication)
        )));
    }

    @PutMapping("/{matchId}/lineups/{teamId}")
    public ResponseEntity<MatchDetailsResponse> upsertLineup(
        @PathVariable Long matchId,
        @PathVariable Long teamId,
        @Valid @RequestBody MatchLineupUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(toResponse(matchProtocolService.upsertLineup(
            matchId,
            teamId,
            request.playerIds(),
            currentUserId(authentication),
            isSuperAdmin(authentication)
        )));
    }

    private MatchDetailsResponse toResponse(MatchProtocolService.MatchDetailsData data) {
        TourMatch match = data.match();
        MatchProtocol protocol = data.protocol();
        return new MatchDetailsResponse(
            match.getId(),
            match.getTour().getId(),
            match.getTour().getName(),
            match.getTour().getSeason().getId(),
            match.getTour().getSeason().getName(),
            toTeamResponse(match.getHomeTeam()),
            toTeamResponse(match.getAwayTeam()),
            match.getKickoffAt(),
            toLineupResponse(data.homeLineup()),
            toLineupResponse(data.awayLineup()),
            new MatchProtocolResponse(
                protocol.getStatus(),
                protocol.getHomeScore(),
                protocol.getAwayScore(),
                protocol.isHomeTechnicalDefeat(),
                protocol.isAwayTechnicalDefeat(),
                protocol.getBestPlayer() == null ? null : protocol.getBestPlayer().getId(),
                protocol.getBestPlayer() == null ? null : protocol.getBestPlayer().getFullName(),
                protocol.getStartedAt(),
                protocol.getFinishedAt(),
                protocol.getNotes(),
                data.events().stream().map(this::toEventResponse).toList()
            )
        );
    }

    private MatchLineupResponse toLineupResponse(MatchProtocolService.TeamLineupData data) {
        return new MatchLineupResponse(
            data.teamId(),
            data.teamName(),
            data.submittedAt(),
            data.submittedByUserId(),
            data.players().stream()
                .map(player -> new MatchLineupPlayerResponse(player.playerId(), player.playerName(), player.sortOrder(), player.seasonId()))
                .toList(),
            data.availablePlayers().stream()
                .map(player -> new AvailableRosterPlayerResponse(player.playerId(), player.playerName(), player.seasonId()))
                .toList()
        );
    }

    private TeamResponse toTeamResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getShortName(), team.getCity(), team.getLogoDataUrl());
    }

    private MatchEventResponse toEventResponse(MatchEvent event) {
        return new MatchEventResponse(
            event.getId(),
            event.getEventType(),
            event.getTeam() == null ? null : event.getTeam().getId(),
            event.getTeam() == null ? null : event.getTeam().getName(),
            event.getPlayer() == null ? null : event.getPlayer().getId(),
            event.getPlayer() == null ? null : event.getPlayer().getFullName(),
            event.getRelatedPlayer() == null ? null : event.getRelatedPlayer().getId(),
            event.getRelatedPlayer() == null ? null : event.getRelatedPlayer().getFullName(),
            event.getMinute(),
            event.getExtraMinute(),
            event.getValueText(),
            event.getSortOrder()
        );
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getUserId();
        }
        return null;
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    public record MatchDetailsResponse(
        Long id,
        Long tourId,
        String tourName,
        Long seasonId,
        String seasonName,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        OffsetDateTime kickoffAt,
        MatchLineupResponse homeLineup,
        MatchLineupResponse awayLineup,
        MatchProtocolResponse protocol
    ) {}

    public record TeamResponse(Long id, String name, String shortName, String city, String logoDataUrl) {}

    public record MatchProtocolResponse(
        MatchProtocolStatus status,
        Integer homeScore,
        Integer awayScore,
        boolean homeTechnicalDefeat,
        boolean awayTechnicalDefeat,
        Long bestPlayerId,
        String bestPlayerName,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String notes,
        List<MatchEventResponse> events
    ) {}

    public record MatchEventResponse(
        Long id,
        com.footballstats.backend.domain.MatchEventType eventType,
        Long teamId,
        String teamName,
        Long playerId,
        String playerName,
        Long relatedPlayerId,
        String relatedPlayerName,
        int minute,
        Integer extraMinute,
        String valueText,
        int sortOrder
    ) {}

    public record MatchLineupResponse(
        Long teamId,
        String teamName,
        OffsetDateTime submittedAt,
        Long submittedByUserId,
        List<MatchLineupPlayerResponse> players,
        List<AvailableRosterPlayerResponse> availablePlayers
    ) {}

    public record MatchLineupPlayerResponse(
        Long playerId,
        String playerName,
        int sortOrder,
        Long seasonId
    ) {}

    public record AvailableRosterPlayerResponse(
        Long playerId,
        String playerName,
        Long seasonId
    ) {}

    public record MatchProtocolUpsertRequest(
        MatchProtocolStatus status,
        Integer homeScore,
        Integer awayScore,
        Boolean homeTechnicalDefeat,
        Boolean awayTechnicalDefeat,
        Long bestPlayerId,
        String notes,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        List<MatchPlayerStatUpsertRequest> playerStats
    ) {}

    public record MatchPlayerStatUpsertRequest(
        @NotNull(message = "teamId обязателен.") Long teamId,
        @NotNull(message = "playerId обязателен.") Long playerId,
        Integer goals,
        Integer yellowCards,
        Integer redCards
    ) {}

    public record MatchLineupUpsertRequest(
        List<Long> playerIds
    ) {}

    public record MatchEventUpsertRequest(
        @NotNull(message = "eventType обязателен.") com.footballstats.backend.domain.MatchEventType eventType,
        Long teamId,
        Long playerId,
        Long relatedPlayerId,
        Integer extraMinute,
        String valueText,
        Integer sortOrder,
        @NotNull(message = "minute обязателен.") Integer minute
    ) {}
}
