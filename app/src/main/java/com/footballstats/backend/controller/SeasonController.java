package com.footballstats.backend.controller;

import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.domain.SeasonStandingsConfig;
import com.footballstats.backend.domain.SeasonStandingsRow;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.SeasonService;
import com.footballstats.backend.service.SeasonStandingsService;
import com.footballstats.backend.service.TourService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
public class SeasonController {

    private final SeasonService seasonService;
    private final TourService tourService;
    private final SeasonStandingsService seasonStandingsService;

    public SeasonController(SeasonService seasonService, TourService tourService, SeasonStandingsService seasonStandingsService) {
        this.seasonService = seasonService;
        this.tourService = tourService;
        this.seasonStandingsService = seasonStandingsService;
    }

    @GetMapping("/api/seasons")
    public ResponseEntity<List<SeasonResponse>> listSeasons(
        @RequestParam(name = "active_flag", defaultValue = "1") Integer activeFlag
    ) {
        boolean includeInactive = Integer.valueOf(0).equals(activeFlag);
        List<Season> seasons = includeInactive ? seasonService.listAllSeasons() : seasonService.listActiveSeasons();
        return ResponseEntity.ok(seasons.stream().map(this::toResponse).toList());
    }

    @PostMapping("/api/seasons")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SeasonResponse> createSeason(
        @Valid @RequestBody SeasonUpsertRequest request,
        Authentication authentication
    ) {
        Long actorUserId = currentUserId(authentication);
        Season season = seasonService.createSeason(
            request.name(),
            request.roundsCount(),
            request.playoffEnabled(),
            request.playoffTeamCount(),
            actorUserId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(season));
    }

    @PutMapping("/api/seasons/{seasonId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SeasonResponse> updateSeason(
        @PathVariable Long seasonId,
        @Valid @RequestBody SeasonUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(toResponse(seasonService.updateSeason(
            seasonId,
            request.name(),
            request.roundsCount(),
            request.playoffEnabled(),
            request.playoffTeamCount(),
            currentUserId(authentication)
        )));
    }

    @DeleteMapping("/api/seasons/{seasonId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SeasonResponse> deactivateSeason(@PathVariable Long seasonId, Authentication authentication) {
        return ResponseEntity.ok(toResponse(seasonService.deactivateSeason(seasonId, currentUserId(authentication))));
    }

    @GetMapping("/api/seasons/{seasonId}/teams")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<SeasonTeamResponse>> listSeasonTeams(@PathVariable Long seasonId) {
        return ResponseEntity.ok(seasonService.listSeasonTeams(seasonId).stream().map(this::toTeamResponse).toList());
    }

    @GetMapping("/api/seasons/{seasonId}/overview")
    public ResponseEntity<SeasonOverviewResponse> getSeasonOverview(@PathVariable Long seasonId) {
        TourService.SeasonOverviewData overview = tourService.getPublishedSeasonOverview(seasonId);
        SeasonStandingsService.SeasonStandingsSnapshot standings = seasonStandingsService.getSeasonStandings(seasonId);
        List<TourOverviewResponse> tours = overview.tours().stream()
            .map(tour -> toTourOverviewResponse(tour, overview.matchesByTourId().getOrDefault(tour.getId(), List.of())))
            .toList();
        return ResponseEntity.ok(new SeasonOverviewResponse(
            toResponse(overview.season()),
            overview.teams().stream().map(this::toTeamResponse).toList(),
            tours,
            toStandingsConfigResponse(standings.config()),
            standings.rows().stream().map(this::toStandingsRowResponse).toList()
        ));
    }

    @PutMapping("/api/seasons/{seasonId}/teams")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<SeasonTeamResponse>> replaceSeasonTeams(
        @PathVariable Long seasonId,
        @Valid @RequestBody SeasonTeamsUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(seasonService.replaceSeasonTeams(seasonId, request.teamIds(), currentUserId(authentication))
            .stream()
            .map(this::toTeamResponse)
            .toList());
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getUserId();
        }
        return null;
    }

    private SeasonResponse toResponse(Season season) {
        return new SeasonResponse(
            season.getId(),
            season.getName(),
            season.getRoundsCount(),
            season.isPlayoffEnabled(),
            season.getPlayoffTeamCount(),
            seasonService.calculateRegularToursCount(season.getId()),
            season.isActive(),
            season.getCreatedByUserId(),
            season.getUpdatedByUserId(),
            season.getCreatedAt(),
            season.getUpdatedAt()
        );
    }

    private SeasonTeamResponse toTeamResponse(Team team) {
        return new SeasonTeamResponse(team.getId(), team.getName(), team.getShortName(), team.getCity(), team.isActive());
    }

    private TourOverviewResponse toTourOverviewResponse(Tour tour, List<TourMatch> matches) {
        return new TourOverviewResponse(
            tour.getId(),
            tour.getName(),
            tour.getStageType(),
            tour.getRoundNumber(),
            tour.getSortOrder(),
            tour.isPublished(),
            matches.stream()
                .map(match -> new TourMatchOverviewResponse(
                    match.getId(),
                    match.getHomeTeam().getId(),
                    match.getHomeTeam().getName(),
                    match.getAwayTeam().getId(),
                    match.getAwayTeam().getName(),
                    match.getKickoffAt(),
                    protocolStatus(match.getProtocol()),
                    protocolScore(match.getProtocol(), true),
                    protocolScore(match.getProtocol(), false)
                ))
                .toList()
        );
    }

    private StandingsConfigResponse toStandingsConfigResponse(SeasonStandingsConfig config) {
        return new StandingsConfigResponse(
            config.getWinPoints(),
            config.getDrawPoints(),
            config.getLossPoints(),
            config.getLastCalculatedAt()
        );
    }

    private SeasonStandingsRowResponse toStandingsRowResponse(SeasonStandingsRow row) {
        return new SeasonStandingsRowResponse(
            row.getPosition(),
            row.getTeam().getId(),
            row.getTeam().getName(),
            row.getTeam().getShortName(),
            row.getTeam().getLogoDataUrl(),
            row.getMatchesPlayed(),
            row.getGoalsFor(),
            row.getGoalsAgainst(),
            row.getGoalDifference(),
            row.getPoints()
        );
    }

    private MatchProtocolStatus protocolStatus(MatchProtocol protocol) {
        return protocol == null || protocol.getStatus() == null ? MatchProtocolStatus.SCHEDULED : protocol.getStatus();
    }

    private Integer protocolScore(MatchProtocol protocol, boolean homeScore) {
        if (protocol == null) {
            return null;
        }
        return homeScore ? protocol.getHomeScore() : protocol.getAwayScore();
    }

    public record SeasonResponse(
        Long id,
        String name,
        Integer roundsCount,
        boolean playoffEnabled,
        Integer playoffTeamCount,
        Integer regularToursCount,
        boolean active,
        Long createdByUserId,
        Long updatedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}

    public record SeasonUpsertRequest(
        @NotBlank(message = "Название сезона обязательно.") String name,
        Integer roundsCount,
        Boolean playoffEnabled,
        Integer playoffTeamCount
    ) {}

    public record SeasonTeamResponse(Long id, String name, String shortName, String city, boolean active) {}

    public record SeasonOverviewResponse(
        SeasonResponse season,
        List<SeasonTeamResponse> teams,
        List<TourOverviewResponse> tours,
        StandingsConfigResponse standingsConfig,
        List<SeasonStandingsRowResponse> standings
    ) {}

    public record StandingsConfigResponse(
        Integer winPoints,
        Integer drawPoints,
        Integer lossPoints,
        OffsetDateTime lastCalculatedAt
    ) {}

    public record SeasonStandingsRowResponse(
        Integer position,
        Long teamId,
        String teamName,
        String teamShortName,
        String teamLogoDataUrl,
        Integer matchesPlayed,
        Integer goalsFor,
        Integer goalsAgainst,
        Integer goalDifference,
        Integer points
    ) {}

    public record TourOverviewResponse(
        Long id,
        String name,
        String stageType,
        Integer roundNumber,
        Integer sortOrder,
        boolean published,
        List<TourMatchOverviewResponse> matches
    ) {}

    public record TourMatchOverviewResponse(
        Long id,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName,
        OffsetDateTime kickoffAt,
        MatchProtocolStatus status,
        Integer homeScore,
        Integer awayScore
    ) {}

    public record SeasonTeamsUpsertRequest(@jakarta.validation.constraints.NotEmpty(message = "Нужно выбрать хотя бы одну команду.") List<Long> teamIds) {}
}
