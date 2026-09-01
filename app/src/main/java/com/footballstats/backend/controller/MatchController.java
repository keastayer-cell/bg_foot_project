package com.footballstats.backend.controller;

import com.footballstats.backend.domain.MatchEvent;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Referee;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.MatchProtocolService;
import com.footballstats.backend.service.SeasonProtocolArchiveService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchProtocolService matchProtocolService;
    private final SeasonProtocolArchiveService seasonProtocolArchiveService;

    public MatchController(MatchProtocolService matchProtocolService, SeasonProtocolArchiveService seasonProtocolArchiveService) {
        this.matchProtocolService = matchProtocolService;
        this.seasonProtocolArchiveService = seasonProtocolArchiveService;
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchDetailsResponse> getMatch(@PathVariable Long matchId) {
        return ResponseEntity.ok(toResponse(matchProtocolService.getMatchDetails(matchId)));
    }

    @GetMapping("/{matchId}/protocol/pdf")
    public ResponseEntity<byte[]> downloadVerifiedProtocolPdf(@PathVariable Long matchId) {
        MatchProtocolService.MatchDetailsData details = matchProtocolService.getMatchDetails(matchId);
        SeasonProtocolArchiveService.PdfPayload pdf = seasonProtocolArchiveService.buildMatchProtocolPdf(
            details.match().getTour().getSeason().getName(),
            matchProtocolService.getVerifiedMatchProtocolExport(matchId)
        );

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(pdf.fileName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20"))
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(pdf.bytes().length)
            .body(pdf.bytes());
    }

    @PutMapping("/{matchId}/protocol")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
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
            request.chiefRefereeId(),
            request.assistantRefereeOneId(),
            request.assistantRefereeTwoId(),
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
            currentUserId(authentication),
            isSuperAdmin(authentication)
        )));
    }

    @PostMapping("/{matchId}/protocol/reopen")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MatchDetailsResponse> reopenVerifiedProtocol(
        @PathVariable Long matchId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(toResponse(matchProtocolService.reopenVerifiedProtocol(matchId, currentUserId(authentication))));
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
            request.starterPlayerIds(),
            request.substitutePlayerIds(),
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
            match.getTour().getCompetition() == null
                ? match.getTour().getSeason().getPlayersOnField()
                : match.getTour().getCompetition().getPlayersOnField(),
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
                toRefereeResponse(protocol.getChiefReferee()),
                toRefereeResponse(protocol.getAssistantRefereeOne()),
                toRefereeResponse(protocol.getAssistantRefereeTwo()),
                protocol.getStartedAt(),
                protocol.getFinishedAt(),
                protocol.getNotes(),
                data.events().stream().map(this::toEventResponse).toList()
            ),
            data.availableReferees().stream().map(this::toRefereeResponse).toList()
        );
    }

    private RefereeResponse toRefereeResponse(Referee referee) {
        if (referee == null) {
            return null;
        }
        return new RefereeResponse(
            referee.getId(),
            referee.getFullName(),
            referee.getCity(),
            referee.getBirthDate(),
            referee.isActive()
        );
    }

    private MatchLineupResponse toLineupResponse(MatchProtocolService.TeamLineupData data) {
        return new MatchLineupResponse(
            data.teamId(),
            data.teamName(),
            data.submittedAt(),
            data.submittedByUserId(),
            data.players().stream()
                .map(player -> new MatchLineupPlayerResponse(
                    player.playerId(),
                    player.playerName(),
                    player.isGoalkeeper(),
                    player.isStarter(),
                    player.sortOrder(),
                    player.seasonId(),
                    player.suspended(),
                    player.suspensionReason()
                ))
                .toList(),
            data.availablePlayers().stream()
                .map(player -> new AvailableRosterPlayerResponse(
                    player.playerId(),
                    player.playerName(),
                    player.isGoalkeeper(),
                    player.seasonId(),
                    player.suspended(),
                    player.suspensionReason()
                ))
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
        Integer playersOnField,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        OffsetDateTime kickoffAt,
        MatchLineupResponse homeLineup,
        MatchLineupResponse awayLineup,
        MatchProtocolResponse protocol,
        List<RefereeResponse> availableReferees
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
        RefereeResponse chiefReferee,
        RefereeResponse assistantRefereeOne,
        RefereeResponse assistantRefereeTwo,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String notes,
        List<MatchEventResponse> events
    ) {}

    public record RefereeResponse(
        Long id,
        String fullName,
        String city,
        java.time.LocalDate birthDate,
        boolean active
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
        boolean isGoalkeeper,
        boolean isStarter,
        int sortOrder,
        Long seasonId,
        boolean suspended,
        String suspensionReason
    ) {}

    public record AvailableRosterPlayerResponse(
        Long playerId,
        String playerName,
        boolean isGoalkeeper,
        Long seasonId,
        boolean suspended,
        String suspensionReason
    ) {}

    public record MatchProtocolUpsertRequest(
        @NotNull(message = "Статус протокола обязателен.")
        MatchProtocolStatus status,
        @jakarta.validation.constraints.PositiveOrZero(message = "Счет хозяев не может быть отрицательным.")
        Integer homeScore,
        @jakarta.validation.constraints.PositiveOrZero(message = "Счет гостей не может быть отрицательным.")
        Integer awayScore,
        Boolean homeTechnicalDefeat,
        Boolean awayTechnicalDefeat,
        Long bestPlayerId,
        Long chiefRefereeId,
        Long assistantRefereeOneId,
        Long assistantRefereeTwoId,
        @jakarta.validation.constraints.Size(max = 4000, message = "Примечание не должно превышать 4000 символов.")
        String notes,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        List<@Valid MatchPlayerStatUpsertRequest> playerStats
    ) {}

    public record MatchPlayerStatUpsertRequest(
        @NotNull(message = "teamId обязателен.") Long teamId,
        @NotNull(message = "playerId обязателен.") Long playerId,
        @jakarta.validation.constraints.PositiveOrZero Integer goals,
        @jakarta.validation.constraints.PositiveOrZero Integer yellowCards,
        @jakarta.validation.constraints.PositiveOrZero Integer redCards
    ) {}

    public record MatchLineupUpsertRequest(
        @jakarta.validation.constraints.NotNull(message = "Основной состав обязателен.") List<@NotNull Long> starterPlayerIds,
        @jakarta.validation.constraints.NotNull(message = "Список запасных обязателен.") List<@NotNull Long> substitutePlayerIds
    ) {}

    public record MatchEventUpsertRequest(
        @NotNull(message = "eventType обязателен.") com.footballstats.backend.domain.MatchEventType eventType,
        Long teamId,
        Long playerId,
        Long relatedPlayerId,
        @jakarta.validation.constraints.PositiveOrZero Integer extraMinute,
        @jakarta.validation.constraints.Size(max = 1000) String valueText,
        @jakarta.validation.constraints.PositiveOrZero Integer sortOrder,
        @NotNull(message = "minute обязателен.") @jakarta.validation.constraints.PositiveOrZero Integer minute
    ) {}
}
