package com.footballstats.backend.controller;

import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Referee;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.domain.SeasonStandingsConfig;
import com.footballstats.backend.domain.SeasonStandingsRow;
import com.footballstats.backend.domain.SeasonTransferStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.security.AppUserPrincipal;
import com.footballstats.backend.service.MediaAssetService;
import com.footballstats.backend.service.CompetitionService;
import com.footballstats.backend.service.MatchProtocolService;
import com.footballstats.backend.service.SeasonPlayoffService;
import com.footballstats.backend.service.SeasonProtocolArchiveService;
import com.footballstats.backend.service.SeasonPlayerService;
import com.footballstats.backend.service.SeasonPlayerStatsService;
import com.footballstats.backend.service.SeasonQueryService;
import com.footballstats.backend.service.SeasonService;
import com.footballstats.backend.service.SeasonStandingsService;
import com.footballstats.backend.service.StandingsRankingRules;
import com.footballstats.backend.service.TourService;
import com.footballstats.backend.repository.SeasonPlayoffTieMatchRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class SeasonController {

    private final SeasonService seasonService;
    private final TourService tourService;
    private final SeasonStandingsService seasonStandingsService;
    private final SeasonPlayerService seasonPlayerService;
    private final SeasonPlayerStatsService seasonPlayerStatsService;
    private final MatchProtocolService matchProtocolService;
    private final SeasonPlayoffService seasonPlayoffService;
    private final SeasonProtocolArchiveService seasonProtocolArchiveService;
    private final SeasonQueryService seasonQueryService;
    private final MediaAssetService mediaAssetService;
    private final ObjectMapper objectMapper;
    private final SeasonPlayoffTieMatchRepository playoffTieMatchRepository;
    private final CompetitionService competitionService;

    public SeasonController(
        SeasonService seasonService,
        TourService tourService,
        SeasonStandingsService seasonStandingsService,
        SeasonPlayerService seasonPlayerService,
        SeasonPlayerStatsService seasonPlayerStatsService,
        MatchProtocolService matchProtocolService,
        SeasonPlayoffService seasonPlayoffService,
        SeasonProtocolArchiveService seasonProtocolArchiveService,
        SeasonQueryService seasonQueryService,
        MediaAssetService mediaAssetService,
        ObjectMapper objectMapper,
        SeasonPlayoffTieMatchRepository playoffTieMatchRepository,
        CompetitionService competitionService
    ) {
        this.seasonService = seasonService;
        this.tourService = tourService;
        this.seasonStandingsService = seasonStandingsService;
        this.seasonPlayerService = seasonPlayerService;
        this.seasonPlayerStatsService = seasonPlayerStatsService;
        this.matchProtocolService = matchProtocolService;
        this.seasonPlayoffService = seasonPlayoffService;
        this.seasonProtocolArchiveService = seasonProtocolArchiveService;
        this.seasonQueryService = seasonQueryService;
        this.mediaAssetService = mediaAssetService;
        this.objectMapper = objectMapper;
        this.playoffTieMatchRepository = playoffTieMatchRepository;
        this.competitionService = competitionService;
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
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
            request.applicationDeadline(),
            request.status(),
            request.maxRosterSize(),
            request.playersOnField(),
            request.transferWindowStartDate(),
            request.transferWindowEndDate(),
            request.thirdPlaceEnabled(),
            request.rankingRules(),
            request.refereeIds(),
            request.yellowCardsForSuspension(),
            request.yellowSuspensionMatches(),
            request.redCardsForSuspension(),
            actorUserId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(season));
    }

    @PutMapping("/api/seasons/{seasonId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
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
            request.applicationDeadline(),
            request.status(),
            request.maxRosterSize(),
            request.playersOnField(),
            request.transferWindowStartDate(),
            request.transferWindowEndDate(),
            request.thirdPlaceEnabled(),
            request.rankingRules(),
            request.refereeIds(),
            request.yellowCardsForSuspension(),
            request.yellowSuspensionMatches(),
            request.redCardsForSuspension(),
            currentUserId(authentication)
        )));
    }

    @PostMapping("/api/seasons/{seasonId}/complete-regular-season")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<SeasonResponse> completeRegularSeason(@PathVariable Long seasonId, Authentication authentication) {
        Long actorUserId = currentUserId(authentication);
        Season season = seasonPlayoffService.completeRegularSeason(seasonId, actorUserId);
        if (!season.isPlayoffEnabled()) competitionService.markChampionshipFinished(seasonId, actorUserId);
        return ResponseEntity.ok(toResponse(season));
    }

    @DeleteMapping("/api/seasons/{seasonId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<SeasonResponse> deactivateSeason(@PathVariable Long seasonId, Authentication authentication) {
        return ResponseEntity.ok(toResponse(seasonService.deactivateSeason(seasonId, currentUserId(authentication))));
    }

    @GetMapping("/api/seasons/{seasonId}/teams")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
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
            toPlayoffBracketResponse(seasonId, seasonPlayoffService.getSeasonPlayoffBracket(seasonId)),
            toStandingsConfigResponse(standings.config()),
            standings.rows().stream().map(this::toStandingsRowResponse).toList(),
            seasonQueryService.listTransfers(seasonId).stream()
                .map(this::toSeasonTransferResponse)
                .toList()
        ));
    }

    @GetMapping("/api/seasons/{seasonId}/transfers")
    public ResponseEntity<Page<SeasonTransferResponse>> getSeasonTransfers(
        @PathVariable Long seasonId,
        @RequestParam(defaultValue = "0") int pagenum,
        @RequestParam(defaultValue = "20") int pagesize
    ) {
        return ResponseEntity.ok(seasonQueryService.listTransfers(seasonId, pagenum, pagesize).map(this::toSeasonTransferResponse));
    }

    @GetMapping("/api/seasons/{seasonId}/protocols/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<byte[]> getSeasonProtocolsExport(@PathVariable Long seasonId) {
        Season season = seasonService.getSeason(seasonId);
        SeasonProtocolArchiveService.ArchivePayload archive = seasonProtocolArchiveService.buildSeasonProtocolsArchive(
            season.getName(),
            matchProtocolService.getSeasonProtocolExportMatches(seasonId)
        );

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(archive.fileName(), java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20"))
            .contentType(MediaType.parseMediaType("application/zip"))
            .contentLength(archive.bytes().length)
            .body(archive.bytes());
    }

    @GetMapping("/api/seasons/{seasonId}/player-stats")
    public ResponseEntity<List<SeasonPlayerStatsResponse>> getSeasonPlayerStats(@PathVariable Long seasonId) {
        return ResponseEntity.ok(seasonPlayerStatsService.getSeasonPlayerStats(seasonId).stream()
            .map(item -> new SeasonPlayerStatsResponse(
                item.playerId(),
                item.fullName(),
                item.teamName(),
                item.teamShortName(),
                item.goals(),
                item.yellowCards(),
                item.redCards()
            ))
            .toList());
    }

    @PutMapping("/api/seasons/{seasonId}/teams")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
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

    @GetMapping("/api/seasons/{seasonId}/teams/{teamId}/players")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<List<SeasonTeamPlayerResponse>> listSeasonTeamPlayers(
        @PathVariable Long seasonId,
        @PathVariable Long teamId
    ) {
        return ResponseEntity.ok(buildSeasonTeamPlayersResponse(seasonId, teamId));
    }

    @PostMapping("/api/seasons/{seasonId}/teams/{teamId}/players/{playerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<List<SeasonTeamPlayerResponse>> addSeasonTeamPlayer(
        @PathVariable Long seasonId,
        @PathVariable Long teamId,
        @PathVariable Long playerId,
        Authentication authentication
    ) {
        seasonPlayerService.addSeasonPlayer(teamId, seasonId, playerId, currentUserId(authentication));
        return ResponseEntity.ok(buildSeasonTeamPlayersResponse(seasonId, teamId));
    }

    @DeleteMapping("/api/seasons/{seasonId}/teams/{teamId}/players/{playerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','REFEREE')")
    public ResponseEntity<List<SeasonTeamPlayerResponse>> removeSeasonTeamPlayer(
        @PathVariable Long seasonId,
        @PathVariable Long teamId,
        @PathVariable Long playerId,
        Authentication authentication
    ) {
        seasonPlayerService.removeSeasonPlayer(teamId, seasonId, playerId, currentUserId(authentication));
        return ResponseEntity.ok(buildSeasonTeamPlayersResponse(seasonId, teamId));
    }

    private Long currentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.getUserId();
        }
        return null;
    }

    private List<SeasonTeamPlayerResponse> buildSeasonTeamPlayersResponse(Long seasonId, Long teamId) {
        return seasonQueryService.getRoster(seasonId, teamId).stream()
            .map(player -> new SeasonTeamPlayerResponse(
                player.id(), player.fullName(), player.birthDate(), player.residence(), player.isGoalkeeper(),
                player.photoDataUrl(), player.selectedForSeason(), player.inTeamSince()
            ))
            .toList();
    }

    private SeasonResponse toResponse(Season season) {
        SeasonStandingsConfig config = seasonService.getStandingsConfig(season.getId());
        SeasonPlayoffService.SeasonPlayoffConfigData playoffConfig = seasonPlayoffService.getSeasonPlayoffConfig(season.getId());
        return new SeasonResponse(
            season.getId(),
            season.getName(),
            season.getRoundsCount(),
            season.isPlayoffEnabled(),
            season.getPlayoffTeamCount(),
            playoffConfig.thirdPlaceEnabled(),
            season.getApplicationDeadline(),
            season.getStatus(),
            season.getMaxRosterSize(),
            season.getPlayersOnField(),
            season.getTransferWindowStartDate(),
            season.getTransferWindowEndDate(),
            seasonService.calculateRegularToursCount(season.getId()),
            StandingsRankingRules.fromJson(config.getRankingRulesJson(), objectMapper),
            seasonService.listSeasonReferees(season.getId()).stream().map(this::toRefereeResponse).toList(),
            config.getYellowCardsForSuspension(),
            config.getYellowSuspensionMatches(),
            config.getRedCardsForSuspension(),
            season.getRegulationMediaId() != null,
            season.getRegulationUpdatedAt(),
            season.getRegulationMediaId() == null ? null : "/api/seasons/" + season.getId() + "/regulation/pdf",
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

    private RefereeResponse toRefereeResponse(Referee referee) {
        return new RefereeResponse(
            referee.getId(),
            referee.getFullName(),
            referee.getCity(),
            referee.getBirthDate(),
            mediaAssetService.loadDataUrl(MediaAssetService.OWNER_REFEREE, referee.getId(), MediaAssetService.KIND_REFEREE_PHOTO),
            referee.isActive()
        );
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

    private PlayoffBracketResponse toPlayoffBracketResponse(
        Long seasonId,
        SeasonPlayoffService.SeasonPlayoffBracketData data
    ) {
        Map<Long, List<Long>> matchIdsByTieId = playoffTieMatchRepository.findAllDetailedBySeasonId(seasonId).stream()
            .collect(Collectors.groupingBy(
                tieMatch -> tieMatch.getTie().getId(),
                Collectors.mapping(tieMatch -> tieMatch.getMatch().getId(), Collectors.toList())
            ));
        return new PlayoffBracketResponse(
            data.config().enabled(),
            data.config().teamCount(),
            data.config().thirdPlaceEnabled(),
            data.bracket() != null && data.bracket().isRegularSeasonCompleted(),
            data.bracket() == null ? null : data.bracket().getStatus(),
            data.bracket() == null ? null : data.bracket().getGeneratedAt(),
            data.bracket() == null ? null : data.bracket().getBasedOnStandingsCalculatedAt(),
            data.ties().stream()
                .map(tie -> new PlayoffTieResponse(
                    tie.getId(),
                    tie.getRoundCode(),
                    tie.getRoundOrder(),
                    tie.getSlotOrder(),
                    tie.getLegCount(),
                    tie.getTitle(),
                    tie.getHomeSeed(),
                    tie.getAwaySeed(),
                    tie.getHomeTeam() == null ? null : tie.getHomeTeam().getId(),
                    tie.getHomeTeam() == null ? null : tie.getHomeTeam().getName(),
                    tie.getAwayTeam() == null ? null : tie.getAwayTeam().getId(),
                    tie.getAwayTeam() == null ? null : tie.getAwayTeam().getName(),
                    tie.getWinnerTeam() == null ? null : tie.getWinnerTeam().getId(),
                    tie.getWinnerTeam() == null ? null : tie.getWinnerTeam().getName(),
                    tie.getHomeSourceTie() == null ? null : tie.getHomeSourceTie().getId(),
                    tie.getHomeSourceResult(),
                    tie.getAwaySourceTie() == null ? null : tie.getAwaySourceTie().getId(),
                    tie.getAwaySourceResult(),
                    tie.getAggregateHomeScore(),
                    tie.getAggregateAwayScore(),
                    tie.getStatus(),
                    matchIdsByTieId.getOrDefault(tie.getId(), List.of())
                ))
                .toList()
        );
    }

    private StandingsConfigResponse toStandingsConfigResponse(SeasonStandingsConfig config) {
        return new StandingsConfigResponse(
            config.getWinPoints(),
            config.getDrawPoints(),
            config.getLossPoints(),
            StandingsRankingRules.fromJson(config.getRankingRulesJson(), objectMapper),
            config.getYellowCardsForSuspension(),
            config.getYellowSuspensionMatches(),
            config.getRedCardsForSuspension(),
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

    private SeasonTransferResponse toSeasonTransferResponse(SeasonQueryService.SeasonTransferData request) {
        return new SeasonTransferResponse(
            request.id(), request.playerId(), request.playerName(), request.playerGoalkeeper(),
            request.playerPhotoDataUrl(), request.fromTeamId(), request.fromTeamName(),
            request.toTeamId(), request.toTeamName(), request.requestedDate(), request.status(),
            request.requestComment(), request.decisionComment(), request.requestedAt(), request.processedAt()
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
        boolean thirdPlaceEnabled,
        LocalDate applicationDeadline,
        SeasonStatus status,
        Integer maxRosterSize,
        Integer playersOnField,
        LocalDate transferWindowStartDate,
        LocalDate transferWindowEndDate,
        Integer regularToursCount,
        List<String> rankingRules,
        List<RefereeResponse> referees,
        Integer yellowCardsForSuspension,
        Integer yellowSuspensionMatches,
        Integer redCardsForSuspension,
        boolean regulationDocumentAvailable,
        OffsetDateTime regulationUpdatedAt,
        String regulationDownloadUrl,
        boolean active,
        Long createdByUserId,
        Long updatedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}

    public record SeasonUpsertRequest(
        @NotBlank(message = "Название сезона обязательно.") @jakarta.validation.constraints.Size(max = 255) String name,
        @jakarta.validation.constraints.Positive Integer roundsCount,
        Boolean playoffEnabled,
        @jakarta.validation.constraints.Positive Integer playoffTeamCount,
        Boolean thirdPlaceEnabled,
        LocalDate applicationDeadline,
        SeasonStatus status,
        @jakarta.validation.constraints.Positive Integer maxRosterSize,
        @jakarta.validation.constraints.Positive Integer playersOnField,
        LocalDate transferWindowStartDate,
        LocalDate transferWindowEndDate,
        List<String> rankingRules,
        List<Long> refereeIds,
        @jakarta.validation.constraints.PositiveOrZero Integer yellowCardsForSuspension,
        @jakarta.validation.constraints.Positive Integer yellowSuspensionMatches,
        @jakarta.validation.constraints.Positive Integer redCardsForSuspension
    ) {}

    public record RefereeResponse(
        Long id,
        String fullName,
        String city,
        LocalDate birthDate,
        String photoDataUrl,
        boolean active
    ) {}

    public record SeasonTeamResponse(Long id, String name, String shortName, String city, boolean active) {}

    public record SeasonTeamPlayerResponse(
        Long id,
        String fullName,
        java.time.LocalDate birthDate,
        String residence,
        boolean isGoalkeeper,
        String photoDataUrl,
        boolean selectedForSeason,
        java.time.LocalDate inTeamSince
    ) {}

    public record SeasonOverviewResponse(
        SeasonResponse season,
        List<SeasonTeamResponse> teams,
        List<TourOverviewResponse> tours,
        PlayoffBracketResponse playoffBracket,
        StandingsConfigResponse standingsConfig,
        List<SeasonStandingsRowResponse> standings,
        List<SeasonTransferResponse> transfers
    ) {}

    public record PlayoffBracketResponse(
        boolean enabled,
        Integer teamCount,
        boolean thirdPlaceEnabled,
        boolean regularSeasonCompleted,
        String status,
        OffsetDateTime generatedAt,
        OffsetDateTime basedOnStandingsCalculatedAt,
        List<PlayoffTieResponse> ties
    ) {}

    public record PlayoffTieResponse(
        Long id,
        String roundCode,
        Integer roundOrder,
        Integer slotOrder,
        Integer legCount,
        String title,
        Integer homeSeed,
        Integer awaySeed,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName,
        Long winnerTeamId,
        String winnerTeamName,
        Long homeSourceTieId,
        String homeSourceResult,
        Long awaySourceTieId,
        String awaySourceResult,
        Integer aggregateHomeScore,
        Integer aggregateAwayScore,
        String status,
        List<Long> matchIds
    ) {}

    public record SeasonTransferResponse(
        Long id,
        Long playerId,
        String playerName,
        boolean playerGoalkeeper,
        String playerPhotoDataUrl,
        Long fromTeamId,
        String fromTeamName,
        Long toTeamId,
        String toTeamName,
        LocalDate requestedDate,
        SeasonTransferStatus status,
        String requestComment,
        String decisionComment,
        OffsetDateTime requestedAt,
        OffsetDateTime processedAt
    ) {}

    public record StandingsConfigResponse(
        Integer winPoints,
        Integer drawPoints,
        Integer lossPoints,
        List<String> rankingRules,
        Integer yellowCardsForSuspension,
        Integer yellowSuspensionMatches,
        Integer redCardsForSuspension,
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

    public record SeasonPlayerStatsResponse(
        Long playerId,
        String fullName,
        String teamName,
        String teamShortName,
        Integer goals,
        Integer yellowCards,
        Integer redCards
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
