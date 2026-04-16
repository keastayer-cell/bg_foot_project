package com.footballstats.backend.service;

import com.footballstats.backend.domain.MatchEvent;
import com.footballstats.backend.domain.MatchEventType;
import com.footballstats.backend.domain.MatchLineup;
import com.footballstats.backend.domain.MatchLineupPlayer;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.repository.MatchEventRepository;
import com.footballstats.backend.repository.MatchLineupPlayerRepository;
import com.footballstats.backend.repository.MatchLineupRepository;
import com.footballstats.backend.repository.MatchProtocolRepository;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class MatchProtocolService {

    private final TourMatchRepository tourMatchRepository;
    private final MatchProtocolRepository matchProtocolRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchLineupRepository matchLineupRepository;
    private final MatchLineupPlayerRepository matchLineupPlayerRepository;
    private final PlayerRepository playerRepository;
    private final AccessControlService accessControlService;
    private final SeasonPlayerService seasonPlayerService;
    private final SeasonStandingsService seasonStandingsService;
    private final SeasonDisciplineService seasonDisciplineService;

    public MatchProtocolService(
        TourMatchRepository tourMatchRepository,
        MatchProtocolRepository matchProtocolRepository,
        MatchEventRepository matchEventRepository,
        MatchLineupRepository matchLineupRepository,
        MatchLineupPlayerRepository matchLineupPlayerRepository,
        PlayerRepository playerRepository,
        AccessControlService accessControlService,
        SeasonPlayerService seasonPlayerService,
        SeasonStandingsService seasonStandingsService,
        SeasonDisciplineService seasonDisciplineService
    ) {
        this.tourMatchRepository = tourMatchRepository;
        this.matchProtocolRepository = matchProtocolRepository;
        this.matchEventRepository = matchEventRepository;
        this.matchLineupRepository = matchLineupRepository;
        this.matchLineupPlayerRepository = matchLineupPlayerRepository;
        this.playerRepository = playerRepository;
        this.accessControlService = accessControlService;
        this.seasonPlayerService = seasonPlayerService;
        this.seasonStandingsService = seasonStandingsService;
        this.seasonDisciplineService = seasonDisciplineService;
    }

    @Transactional(readOnly = true)
    public MatchDetailsData getMatchDetails(Long matchId) {
        TourMatch match = getExistingDetailedMatch(matchId);
        MatchProtocol protocol = match.getProtocol();
        if (protocol == null) {
            protocol = new MatchProtocol();
            protocol.setMatch(match);
            protocol.setStatus(MatchProtocolStatus.SCHEDULED);
        }
        List<MatchEvent> events = matchEventRepository.findAllDetailedByMatchId(matchId);
        MatchLineupsData lineups = loadLineups(match);
        return new MatchDetailsData(match, protocol, events, lineups.homeLineup(), lineups.awayLineup());
    }

    @Transactional
    public MatchDetailsData upsertProtocol(
        Long matchId,
        MatchProtocolStatus status,
        Integer homeScore,
        Integer awayScore,
        Boolean homeTechnicalDefeat,
        Boolean awayTechnicalDefeat,
        Long bestPlayerId,
        String notes,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        List<PlayerProtocolStatDraft> playerStatDrafts,
        Long actorUserId
    ) {
        TourMatch match = getExistingDetailedMatch(matchId);
        MatchProtocol protocol = getOrCreateProtocol(match, actorUserId);
        MatchProtocolStatus previousStatus = protocol.getStatus();
        MatchProtocolStatus nextStatus = status == null ? MatchProtocolStatus.SCHEDULED : status;

        validateRequiredLineupsForProtocolStatus(match, nextStatus);

        boolean homeTech = Boolean.TRUE.equals(homeTechnicalDefeat);
        boolean awayTech = Boolean.TRUE.equals(awayTechnicalDefeat);
        validateTechnicalDefeat(homeTech, awayTech);

        List<PlayerProtocolStatDraft> normalizedPlayerStats = normalizePlayerStats(match, playerStatDrafts);
        ProtocolScoreData scoreData = resolveProtocolScore(match, homeScore, awayScore, homeTech, awayTech, normalizedPlayerStats);

        matchEventRepository.deleteByMatch_Id(matchId);

        List<MatchEventDraft> drafts = homeTech || awayTech ? List.of() : buildEventDraftsFromPlayerStats(normalizedPlayerStats);
        for (int index = 0; index < drafts.size(); index += 1) {
            MatchEventDraft draft = drafts.get(index);
            MatchEvent event = new MatchEvent();
            event.setMatch(match);
            event.setTeam(resolveEventTeam(match, draft.teamId()));
            event.setPlayer(resolvePlayer(draft.playerId()));
            event.setRelatedPlayer(resolvePlayer(draft.relatedPlayerId()));
            event.setEventType(draft.eventType());
            event.setMinute(Math.max(0, draft.minute()));
            event.setExtraMinute(draft.extraMinute());
            event.setValueText(normalizeNullable(draft.valueText()));
            event.setSortOrder(draft.sortOrder() == null ? index + 1 : draft.sortOrder());
            event.setCreatedByUserId(actorUserId);
            event.setUpdatedByUserId(actorUserId);
            event.setUpdatedAt(OffsetDateTime.now());
            matchEventRepository.save(event);
        }

        List<MatchEvent> persistedEvents = matchEventRepository.findAllDetailedByMatchId(matchId);

        protocol.setStatus(nextStatus);
        protocol.setHomeScore(scoreData.homeScore());
        protocol.setAwayScore(scoreData.awayScore());
        protocol.setHomeTechnicalDefeat(homeTech);
        protocol.setAwayTechnicalDefeat(awayTech);
        protocol.setBestPlayer(resolvePlayer(bestPlayerId));
        protocol.setNotes(normalizeNullable(notes));
        protocol.setStartedAt(startedAt);
        protocol.setFinishedAt(finishedAt);
        protocol.setUpdatedByUserId(actorUserId);
        protocol.setUpdatedAt(OffsetDateTime.now());
        matchProtocolRepository.save(protocol);

        if (previousStatus == MatchProtocolStatus.VERIFIED || protocol.getStatus() == MatchProtocolStatus.VERIFIED) {
            seasonStandingsService.recalculateSeasonStandings(match.getTour().getSeason().getId(), actorUserId);
        }

        TourMatch refreshedMatch = getExistingDetailedMatch(matchId);
        MatchLineupsData lineups = loadLineups(refreshedMatch);
        return new MatchDetailsData(refreshedMatch, protocol, persistedEvents, lineups.homeLineup(), lineups.awayLineup());
    }

    @Transactional
    public MatchDetailsData reopenVerifiedProtocol(Long matchId, Long actorUserId) {
        TourMatch match = getExistingDetailedMatch(matchId);
        MatchProtocol protocol = match.getProtocol();

        if (protocol == null || protocol.getStatus() != MatchProtocolStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Повторно открыть можно только подтвержденный протокол.");
        }

        protocol.setStatus(MatchProtocolStatus.FINISHED);
        protocol.setUpdatedByUserId(actorUserId);
        protocol.setUpdatedAt(OffsetDateTime.now());
        matchProtocolRepository.save(protocol);

        seasonStandingsService.recalculateSeasonStandings(match.getTour().getSeason().getId(), actorUserId);

        return getMatchDetails(matchId);
    }

    @Transactional
    public MatchDetailsData upsertLineup(
        Long matchId,
        Long teamId,
        List<Long> playerIds,
        Long actorUserId,
        boolean superAdmin
    ) {
        TourMatch match = getExistingDetailedMatch(matchId);
        Team lineupTeam = resolveLineupTeam(match, teamId);

        if (!superAdmin && !accessControlService.hasTeamPermission(actorUserId, lineupTeam.getId(), "ROSTER_EDIT")) {
            throw new AccessDeniedException("Нет прав подавать состав этой команды.");
        }

        Long seasonId = match.getTour().getSeason().getId();
        Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> suspendedPlayers = seasonDisciplineService.getSuspendedPlayersForMatch(seasonId, matchId);
        List<PlayerTeam> eligibleRoster = seasonPlayerService.listEligibleRosterMemberships(lineupTeam.getId(), seasonId);
        Map<Long, Player> eligiblePlayers = new LinkedHashMap<>();
        for (PlayerTeam playerTeam : eligibleRoster) {
            eligiblePlayers.put(playerTeam.getPlayer().getId(), playerTeam.getPlayer());
        }

        List<Long> normalizedIds = playerIds == null ? List.of() : playerIds.stream()
            .filter(id -> id != null)
            .toList();
        Set<Long> uniqueIds = new HashSet<>();
        for (Long playerId : normalizedIds) {
            if (!uniqueIds.add(playerId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Игрок не может повторяться в заявке.");
            }
            if (!eligiblePlayers.containsKey(playerId)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "В заявку можно добавлять только игроков команды, заявленных на сезон матча."
                );
            }
            if (suspendedPlayers.containsKey(playerId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, suspendedPlayers.get(playerId).reason());
            }
        }

        MatchLineup lineup = matchLineupRepository.findByMatch_IdAndTeam_Id(matchId, lineupTeam.getId()).orElse(null);
        if (normalizedIds.isEmpty()) {
            if (lineup != null) {
                matchLineupPlayerRepository.deleteByLineup_Id(lineup.getId());
                matchLineupPlayerRepository.flush();
                matchLineupRepository.delete(lineup);
            }
            syncProtocolStatusAfterLineupChange(match, actorUserId);
            return getMatchDetails(matchId);
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (lineup == null) {
            lineup = new MatchLineup();
            lineup.setMatch(match);
            lineup.setTeam(lineupTeam);
            lineup.setCreatedAt(now);
        } else {
            matchLineupPlayerRepository.deleteByLineup_Id(lineup.getId());
            matchLineupPlayerRepository.flush();
        }
        lineup.setSubmittedByUserId(actorUserId);
        lineup.setUpdatedByUserId(actorUserId);
        lineup.setSubmittedAt(now);
        lineup.setUpdatedAt(now);
        MatchLineup savedLineup = matchLineupRepository.save(lineup);

        for (int index = 0; index < normalizedIds.size(); index += 1) {
            Long playerId = normalizedIds.get(index);
            MatchLineupPlayer lineupPlayer = new MatchLineupPlayer();
            lineupPlayer.setLineup(savedLineup);
            lineupPlayer.setPlayer(eligiblePlayers.get(playerId));
            lineupPlayer.setSortOrder(index + 1);
            lineupPlayer.setCreatedByUserId(actorUserId);
            lineupPlayer.setUpdatedByUserId(actorUserId);
            lineupPlayer.setCreatedAt(now);
            lineupPlayer.setUpdatedAt(now);
            matchLineupPlayerRepository.save(lineupPlayer);
        }

        syncProtocolStatusAfterLineupChange(match, actorUserId);
        return getMatchDetails(matchId);
    }

    @Transactional
    public MatchProtocol initializeProtocol(TourMatch match, Long actorUserId) {
        return getOrCreateProtocol(match, actorUserId);
    }

    private MatchProtocol getOrCreateProtocol(TourMatch match, Long actorUserId) {
        MatchProtocol existing = matchProtocolRepository.findDetailedByMatchId(match.getId()).orElse(null);
        if (existing != null) {
            return existing;
        }

        MatchProtocol protocol = new MatchProtocol();
        protocol.setMatch(match);
        protocol.setStatus(MatchProtocolStatus.SCHEDULED);
        protocol.setCreatedByUserId(actorUserId);
        protocol.setUpdatedByUserId(actorUserId);
        protocol.setCreatedAt(OffsetDateTime.now());
        protocol.setUpdatedAt(OffsetDateTime.now());
        return matchProtocolRepository.save(protocol);
    }

    private TourMatch getExistingDetailedMatch(Long matchId) {
        return tourMatchRepository.findDetailedById(matchId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Матч не найден."));
    }

    private Player resolvePlayer(Long playerId) {
        if (playerId == null) {
            return null;
        }
        return playerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Игрок протокола не найден."));
    }

    private Team resolveEventTeam(TourMatch match, Long teamId) {
        if (teamId == null) {
            return null;
        }

        if (teamId.equals(match.getHomeTeam().getId())) {
            return match.getHomeTeam();
        }
        if (teamId.equals(match.getAwayTeam().getId())) {
            return match.getAwayTeam();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Команда события не относится к матчу.");
    }

    private Team resolveLineupTeam(TourMatch match, Long teamId) {
        Team team = resolveEventTeam(match, teamId);
        if (team == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Команда заявки обязательна.");
        }
        return team;
    }

    private MatchLineupsData loadLineups(TourMatch match) {
        Long matchId = match.getId();
        Long seasonId = match.getTour().getSeason().getId();
        Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> suspendedPlayers = seasonDisciplineService.getSuspendedPlayersForMatch(seasonId, matchId);

        Map<Long, List<MatchLineupPlayer>> playersByTeamId = new LinkedHashMap<>();
        for (MatchLineupPlayer lineupPlayer : matchLineupPlayerRepository.findAllDetailedByMatchId(matchId)) {
            Long teamId = lineupPlayer.getLineup().getTeam().getId();
            playersByTeamId.computeIfAbsent(teamId, ignored -> new ArrayList<>()).add(lineupPlayer);
        }

        Map<Long, MatchLineup> lineupsByTeamId = new LinkedHashMap<>();
        for (MatchLineup lineup : matchLineupRepository.findAllDetailedByMatchId(matchId)) {
            lineupsByTeamId.put(lineup.getTeam().getId(), lineup);
        }

        TeamLineupData homeLineup = buildLineupData(match, match.getHomeTeam(), seasonId, lineupsByTeamId, playersByTeamId, suspendedPlayers);
        TeamLineupData awayLineup = buildLineupData(match, match.getAwayTeam(), seasonId, lineupsByTeamId, playersByTeamId, suspendedPlayers);
        return new MatchLineupsData(homeLineup, awayLineup);
    }

    private TeamLineupData buildLineupData(
        TourMatch match,
        Team team,
        Long seasonId,
        Map<Long, MatchLineup> lineupsByTeamId,
        Map<Long, List<MatchLineupPlayer>> playersByTeamId,
        Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> suspendedPlayers
    ) {
        MatchLineup lineup = lineupsByTeamId.get(team.getId());
        List<LineupPlayerData> players = playersByTeamId.getOrDefault(team.getId(), List.of()).stream()
            .map(item -> new LineupPlayerData(
                item.getPlayer().getId(),
                item.getPlayer().getFullName(),
                item.getPlayer().isGoalkeeper(),
                item.getSortOrder(),
                seasonId,
                suspendedPlayers.containsKey(item.getPlayer().getId()),
                suspendedPlayers.containsKey(item.getPlayer().getId()) ? suspendedPlayers.get(item.getPlayer().getId()).reason() : null
            ))
            .toList();
        Set<Long> selectedPlayerIds = players.stream()
            .map(LineupPlayerData::playerId)
            .collect(java.util.stream.Collectors.toSet());
        List<AvailablePlayerData> availablePlayers = seasonPlayerService.listEligibleRosterMemberships(team.getId(), seasonId).stream()
            .filter(item -> !selectedPlayerIds.contains(item.getPlayer().getId()))
            .map(item -> new AvailablePlayerData(
                item.getPlayer().getId(),
                item.getPlayer().getFullName(),
                item.getPlayer().isGoalkeeper(),
                seasonId,
                suspendedPlayers.containsKey(item.getPlayer().getId()),
                suspendedPlayers.containsKey(item.getPlayer().getId()) ? suspendedPlayers.get(item.getPlayer().getId()).reason() : null
            ))
            .toList();

        return new TeamLineupData(
            match.getId(),
            team.getId(),
            team.getName(),
            lineup == null ? null : lineup.getSubmittedAt(),
            lineup == null ? null : lineup.getSubmittedByUserId(),
            players,
            availablePlayers
        );
    }

    private void syncProtocolStatusAfterLineupChange(TourMatch match, Long actorUserId) {
        MatchProtocol protocol = getOrCreateProtocol(match, actorUserId);
        if (protocol.getStatus() != MatchProtocolStatus.SCHEDULED && protocol.getStatus() != MatchProtocolStatus.LINEUPS_SUBMITTED) {
            return;
        }

        boolean homeSubmitted = matchLineupRepository.existsByMatch_IdAndTeam_Id(match.getId(), match.getHomeTeam().getId());
        boolean awaySubmitted = matchLineupRepository.existsByMatch_IdAndTeam_Id(match.getId(), match.getAwayTeam().getId());
        protocol.setStatus(homeSubmitted && awaySubmitted ? MatchProtocolStatus.LINEUPS_SUBMITTED : MatchProtocolStatus.SCHEDULED);
        protocol.setUpdatedByUserId(actorUserId);
        protocol.setUpdatedAt(OffsetDateTime.now());
        matchProtocolRepository.save(protocol);
    }

    private void validateRequiredLineupsForProtocolStatus(TourMatch match, MatchProtocolStatus status) {
        if (status != MatchProtocolStatus.FINISHED && status != MatchProtocolStatus.VERIFIED) {
            return;
        }

        boolean homeSubmitted = matchLineupRepository.existsByMatch_IdAndTeam_Id(match.getId(), match.getHomeTeam().getId());
        boolean awaySubmitted = matchLineupRepository.existsByMatch_IdAndTeam_Id(match.getId(), match.getAwayTeam().getId());
        if (!homeSubmitted || !awaySubmitted) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Нельзя завершить или подтвердить протокол, пока не поданы обе заявки на матч."
            );
        }
    }

    private void validateTechnicalDefeat(boolean homeTechnicalDefeat, boolean awayTechnicalDefeat) {
        if (homeTechnicalDefeat && awayTechnicalDefeat) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя поставить техническое поражение обеим командам одновременно.");
        }
    }

    private ProtocolScoreData resolveProtocolScore(
        TourMatch match,
        Integer homeScore,
        Integer awayScore,
        boolean homeTechnicalDefeat,
        boolean awayTechnicalDefeat,
        List<PlayerProtocolStatDraft> playerStats
    ) {
        if (homeTechnicalDefeat) {
            validateEmptyPlayerStatsForTechnicalDefeat(playerStats);
            return new ProtocolScoreData(0, 3);
        }
        if (awayTechnicalDefeat) {
            validateEmptyPlayerStatsForTechnicalDefeat(playerStats);
            return new ProtocolScoreData(3, 0);
        }

        int normalizedHomeScore = homeScore == null ? 0 : Math.max(0, homeScore);
        int normalizedAwayScore = awayScore == null ? 0 : Math.max(0, awayScore);
        int generatedHomeGoals = countGoals(match.getHomeTeam().getId(), playerStats);
        int generatedAwayGoals = countGoals(match.getAwayTeam().getId(), playerStats);

        if (generatedHomeGoals != normalizedHomeScore || generatedAwayGoals != normalizedAwayScore) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Сумма голов по игрокам должна совпадать со счетом матча."
            );
        }

        return new ProtocolScoreData(normalizedHomeScore, normalizedAwayScore);
    }

    private List<PlayerProtocolStatDraft> normalizePlayerStats(TourMatch match, List<PlayerProtocolStatDraft> playerStatDrafts) {
        List<PlayerProtocolStatDraft> source = playerStatDrafts == null ? List.of() : playerStatDrafts;
        if (source.isEmpty()) {
            return List.of();
        }

        Map<Long, Set<Long>> lineupPlayerIdsByTeam = new LinkedHashMap<>();
        for (MatchLineupPlayer lineupPlayer : matchLineupPlayerRepository.findAllDetailedByMatchId(match.getId())) {
            Long teamId = lineupPlayer.getLineup().getTeam().getId();
            lineupPlayerIdsByTeam.computeIfAbsent(teamId, ignored -> new HashSet<>()).add(lineupPlayer.getPlayer().getId());
        }

        Map<String, PlayerProtocolStatDraft> uniqueStats = new LinkedHashMap<>();
        for (PlayerProtocolStatDraft draft : source) {
            if (draft == null || draft.teamId() == null || draft.playerId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Для статистики игрока обязательны teamId и playerId.");
            }

            Team team = resolveEventTeam(match, draft.teamId());
            Set<Long> lineupPlayerIds = lineupPlayerIdsByTeam.getOrDefault(team.getId(), Set.of());
            if (!lineupPlayerIds.contains(draft.playerId())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Статистику можно заполнять только для игроков, включенных в заявку матча."
                );
            }

            String uniqueKey = team.getId() + ":" + draft.playerId();
            if (uniqueStats.containsKey(uniqueKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Игрок не может повторяться в протоколе дважды.");
            }

            uniqueStats.put(uniqueKey, new PlayerProtocolStatDraft(
                team.getId(),
                draft.playerId(),
                normalizeNonNegative(draft.goals(), "Количество голов не может быть отрицательным."),
                normalizeNonNegative(draft.yellowCards(), "Количество желтых карточек не может быть отрицательным."),
                normalizeNonNegative(draft.redCards(), "Количество красных карточек не может быть отрицательным.")
            ));
        }

        return uniqueStats.values().stream()
            .sorted(Comparator.comparing(PlayerProtocolStatDraft::teamId).thenComparing(PlayerProtocolStatDraft::playerId))
            .toList();
    }

    private List<MatchEventDraft> buildEventDraftsFromPlayerStats(List<PlayerProtocolStatDraft> playerStats) {
        List<MatchEventDraft> eventDrafts = new ArrayList<>();
        int sortOrder = 1;
        for (PlayerProtocolStatDraft playerStat : playerStats) {
            sortOrder = appendRepeatedEvents(eventDrafts, MatchEventType.GOAL, playerStat, playerStat.goals(), sortOrder);
            sortOrder = appendRepeatedEvents(eventDrafts, MatchEventType.YELLOW_CARD, playerStat, playerStat.yellowCards(), sortOrder);
            sortOrder = appendRepeatedEvents(eventDrafts, MatchEventType.RED_CARD, playerStat, playerStat.redCards(), sortOrder);
        }
        return eventDrafts;
    }

    private int appendRepeatedEvents(
        List<MatchEventDraft> eventDrafts,
        MatchEventType eventType,
        PlayerProtocolStatDraft playerStat,
        int count,
        int sortOrderStart
    ) {
        int nextSortOrder = sortOrderStart;
        for (int index = 0; index < count; index += 1) {
            eventDrafts.add(new MatchEventDraft(
                eventType,
                playerStat.teamId(),
                playerStat.playerId(),
                null,
                0,
                null,
                null,
                nextSortOrder
            ));
            nextSortOrder += 1;
        }
        return nextSortOrder;
    }

    private void validateEmptyPlayerStatsForTechnicalDefeat(List<PlayerProtocolStatDraft> playerStats) {
        boolean hasNonZeroStats = playerStats.stream().anyMatch(this::hasAnyStats);
        if (hasNonZeroStats) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "При техническом поражении статистика игроков должна быть пустой."
            );
        }
    }

    private int countGoals(Long teamId, List<PlayerProtocolStatDraft> playerStats) {
        return playerStats.stream()
            .filter(item -> teamId.equals(item.teamId()))
            .mapToInt(PlayerProtocolStatDraft::goals)
            .sum();
    }

    private boolean hasAnyStats(PlayerProtocolStatDraft playerStat) {
        return playerStat.goals() > 0 || playerStat.yellowCards() > 0 || playerStat.redCards() > 0;
    }

    private int normalizeNonNegative(Integer value, String message) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private String normalizeNullable(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record MatchDetailsData(
        TourMatch match,
        MatchProtocol protocol,
        List<MatchEvent> events,
        TeamLineupData homeLineup,
        TeamLineupData awayLineup
    ) {}

    public record TeamLineupData(
        Long matchId,
        Long teamId,
        String teamName,
        OffsetDateTime submittedAt,
        Long submittedByUserId,
        List<LineupPlayerData> players,
        List<AvailablePlayerData> availablePlayers
    ) {}

    public record LineupPlayerData(
        Long playerId,
        String playerName,
        boolean isGoalkeeper,
        int sortOrder,
        Long seasonId,
        boolean suspended,
        String suspensionReason
    ) {}

    public record AvailablePlayerData(
        Long playerId,
        String playerName,
        boolean isGoalkeeper,
        Long seasonId,
        boolean suspended,
        String suspensionReason
    ) {}

    private record MatchLineupsData(
        TeamLineupData homeLineup,
        TeamLineupData awayLineup
    ) {}

    public record MatchEventDraft(
        MatchEventType eventType,
        Long teamId,
        Long playerId,
        Long relatedPlayerId,
        int minute,
        Integer extraMinute,
        String valueText,
        Integer sortOrder
    ) {}

    public record PlayerProtocolStatDraft(
        Long teamId,
        Long playerId,
        int goals,
        int yellowCards,
        int redCards
    ) {}

    private record ProtocolScoreData(
        int homeScore,
        int awayScore
    ) {}
}
