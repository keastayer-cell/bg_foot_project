package com.footballstats.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.domain.MatchEvent;
import com.footballstats.backend.domain.MatchEventType;
import com.footballstats.backend.domain.MatchLineup;
import com.footballstats.backend.domain.MatchLineupPlayer;
import com.footballstats.backend.domain.MatchProtocolExportSnapshot;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.Referee;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.repository.MatchEventRepository;
import com.footballstats.backend.repository.MatchProtocolExportSnapshotRepository;
import com.footballstats.backend.repository.MatchLineupPlayerRepository;
import com.footballstats.backend.repository.MatchLineupRepository;
import com.footballstats.backend.repository.MatchProtocolRepository;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.RefereeRepository;
import com.footballstats.backend.repository.SeasonRefereeRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class MatchProtocolService {

    private static final DateTimeFormatter EXPORT_FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final TypeReference<List<SeasonProtocolExportRefereeData>> EXPORT_REFEREES_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<SeasonProtocolExportTeamData>> EXPORT_TEAMS_TYPE = new TypeReference<>() {};

    private final TourMatchRepository tourMatchRepository;
    private final MatchProtocolRepository matchProtocolRepository;
    private final MatchProtocolExportSnapshotRepository matchProtocolExportSnapshotRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchLineupRepository matchLineupRepository;
    private final MatchLineupPlayerRepository matchLineupPlayerRepository;
    private final PlayerRepository playerRepository;
    private final RefereeRepository refereeRepository;
    private final SeasonRefereeRepository seasonRefereeRepository;
    private final AccessControlService accessControlService;
    private final SeasonPlayerService seasonPlayerService;
    private final SeasonStandingsService seasonStandingsService;
    private final SeasonDisciplineService seasonDisciplineService;
    private final ObjectMapper objectMapper;
    private final CompetitionService competitionService;
    private final CompetitionDisciplineService competitionDisciplineService;

    public MatchProtocolService(
        TourMatchRepository tourMatchRepository,
        MatchProtocolRepository matchProtocolRepository,
        MatchProtocolExportSnapshotRepository matchProtocolExportSnapshotRepository,
        MatchEventRepository matchEventRepository,
        MatchLineupRepository matchLineupRepository,
        MatchLineupPlayerRepository matchLineupPlayerRepository,
        PlayerRepository playerRepository,
        RefereeRepository refereeRepository,
        SeasonRefereeRepository seasonRefereeRepository,
        AccessControlService accessControlService,
        SeasonPlayerService seasonPlayerService,
        SeasonStandingsService seasonStandingsService,
        SeasonDisciplineService seasonDisciplineService,
        ObjectMapper objectMapper,
        CompetitionService competitionService,
        CompetitionDisciplineService competitionDisciplineService
    ) {
        this.tourMatchRepository = tourMatchRepository;
        this.matchProtocolRepository = matchProtocolRepository;
        this.matchProtocolExportSnapshotRepository = matchProtocolExportSnapshotRepository;
        this.matchEventRepository = matchEventRepository;
        this.matchLineupRepository = matchLineupRepository;
        this.matchLineupPlayerRepository = matchLineupPlayerRepository;
        this.playerRepository = playerRepository;
        this.refereeRepository = refereeRepository;
        this.seasonRefereeRepository = seasonRefereeRepository;
        this.accessControlService = accessControlService;
        this.seasonPlayerService = seasonPlayerService;
        this.seasonStandingsService = seasonStandingsService;
        this.seasonDisciplineService = seasonDisciplineService;
        this.objectMapper = objectMapper;
        this.competitionService = competitionService;
        this.competitionDisciplineService = competitionDisciplineService;
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
        return new MatchDetailsData(match, protocol, events, lineups.homeLineup(), lineups.awayLineup(), loadSeasonReferees(match));
    }

    @Transactional
    public List<SeasonProtocolExportMatchData> getSeasonProtocolExportMatches(Long seasonId) {
        ensureSeasonProtocolExportSnapshots(seasonId);
        return matchProtocolExportSnapshotRepository.findAllBySeasonIdOrderByExportOrder(seasonId).stream()
            .map(this::toSeasonProtocolExportMatchData)
            .toList();
    }

    @Transactional
    public SeasonProtocolExportMatchData getVerifiedMatchProtocolExport(Long matchId) {
        MatchProtocolExportSnapshot snapshot = matchProtocolExportSnapshotRepository.findByMatchId(matchId).orElse(null);
        if (snapshot != null) {
            return toSeasonProtocolExportMatchData(snapshot);
        }

        MatchDetailsData data = getMatchDetails(matchId);
        if (data.protocol().getStatus() != MatchProtocolStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Скачать PDF можно только для подтвержденного протокола.");
        }

        return toSeasonProtocolExportMatchData(data);
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
        Long chiefRefereeId,
        Long assistantRefereeOneId,
        Long assistantRefereeTwoId,
        String notes,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        List<PlayerProtocolStatDraft> playerStatDrafts,
        Long actorUserId,
        boolean superAdmin
    ) {
        TourMatch match = getExistingDetailedMatch(matchId);
        MatchProtocol protocol = getOrCreateProtocol(match, actorUserId);
        if (!superAdmin && protocol.getStatus() == MatchProtocolStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Подтвержденный протокол может изменять только супер администратор.");
        }
        MatchProtocolStatus previousStatus = protocol.getStatus();
        MatchProtocolStatus nextStatus = status == null ? MatchProtocolStatus.SCHEDULED : status;
        boolean allowSuperAdminScoreOnlyProtocol = canUseSuperAdminScoreOnlyProtocol(superAdmin, playerStatDrafts);

        validateRequiredLineupsForProtocolStatus(match, nextStatus, allowSuperAdminScoreOnlyProtocol);

        boolean homeTech = Boolean.TRUE.equals(homeTechnicalDefeat);
        boolean awayTech = Boolean.TRUE.equals(awayTechnicalDefeat);
        validateTechnicalDefeat(homeTech, awayTech);

        List<PlayerProtocolStatDraft> normalizedPlayerStats = allowSuperAdminScoreOnlyProtocol
            ? List.of()
            : normalizePlayerStats(match, playerStatDrafts);
        ProtocolScoreData scoreData = resolveProtocolScore(
            match,
            homeScore,
            awayScore,
            homeTech,
            awayTech,
            normalizedPlayerStats,
            allowSuperAdminScoreOnlyProtocol
        );

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
        RefereeAssignments refereeAssignments = resolveRefereeAssignments(match, chiefRefereeId, assistantRefereeOneId, assistantRefereeTwoId);
        protocol.setChiefReferee(refereeAssignments.chiefReferee());
        protocol.setAssistantRefereeOne(refereeAssignments.assistantRefereeOne());
        protocol.setAssistantRefereeTwo(refereeAssignments.assistantRefereeTwo());
        protocol.setNotes(normalizeNullable(notes));
        protocol.setStartedAt(startedAt);
        protocol.setFinishedAt(finishedAt);
        protocol.setUpdatedByUserId(actorUserId);
        protocol.setUpdatedAt(OffsetDateTime.now());
        matchProtocolRepository.save(protocol);

        if (protocol.getStatus() == MatchProtocolStatus.VERIFIED && isCupMatch(match)) {
            competitionService.refreshCupAfterMatch(matchId);
        }

        if (previousStatus == MatchProtocolStatus.VERIFIED || protocol.getStatus() == MatchProtocolStatus.VERIFIED) {
            seasonStandingsService.recalculateSeasonStandings(match.getTour().getSeason().getId(), actorUserId);
        }

        TourMatch refreshedMatch = getExistingDetailedMatch(matchId);
        MatchLineupsData lineups = loadLineups(refreshedMatch);
        MatchDetailsData result = new MatchDetailsData(
            refreshedMatch,
            protocol,
            persistedEvents,
            lineups.homeLineup(),
            lineups.awayLineup(),
            loadSeasonReferees(refreshedMatch)
        );
        syncSeasonProtocolExportSnapshot(result);
        return result;
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

        matchProtocolExportSnapshotRepository.deleteByMatchId(matchId);

        seasonStandingsService.recalculateSeasonStandings(match.getTour().getSeason().getId(), actorUserId);

        return getMatchDetails(matchId);
    }

    @Transactional
    public MatchDetailsData upsertLineup(
        Long matchId,
        Long teamId,
        List<Long> starterPlayerIds,
        List<Long> substitutePlayerIds,
        Long actorUserId,
        boolean superAdmin
    ) {
        TourMatch match = getExistingDetailedMatch(matchId);
        Team lineupTeam = resolveLineupTeam(match, teamId);

        if (!superAdmin && !accessControlService.hasTeamPermission(actorUserId, lineupTeam.getId(), "ROSTER_EDIT")) {
            throw new AccessDeniedException("Нет прав подавать состав этой команды.");
        }

        Long seasonId = match.getTour().getSeason().getId();
        Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> suspendedPlayers = isCupMatch(match)
            ? competitionDisciplineService.suspendedForMatch(match.getTour().getCompetition().getId(), matchId)
            : seasonDisciplineService.getSuspendedPlayersForMatch(seasonId, matchId);
        Map<Long, Player> eligiblePlayers = new LinkedHashMap<>();
        if (isCupMatch(match)) {
            for (Player player : competitionService.eligiblePlayers(seasonId, match.getTour().getCompetition().getId(), lineupTeam.getId())) {
                eligiblePlayers.put(player.getId(), player);
            }
        } else {
            for (PlayerTeam playerTeam : seasonPlayerService.listEligibleRosterMemberships(lineupTeam.getId(), seasonId)) {
                eligiblePlayers.put(playerTeam.getPlayer().getId(), playerTeam.getPlayer());
            }
        }

        List<Long> normalizedStarterIds = starterPlayerIds == null ? List.of() : starterPlayerIds.stream()
            .filter(id -> id != null)
            .toList();
        List<Long> normalizedSubstituteIds = substitutePlayerIds == null ? List.of() : substitutePlayerIds.stream()
            .filter(id -> id != null)
            .toList();
        int requiredStarters = isCupMatch(match)
            ? match.getTour().getCompetition().getPlayersOnField()
            : match.getTour().getSeason().getPlayersOnField();
        if (normalizedStarterIds.size() != requiredStarters) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Основной состав должен содержать ровно " + requiredStarters + " игроков."
            );
        }
        List<Long> normalizedIds = new java.util.ArrayList<>(normalizedStarterIds);
        normalizedIds.addAll(normalizedSubstituteIds);
        Integer matchRosterLimit = isCupMatch(match) ? match.getTour().getCompetition().getMatchRosterSize() : null;
        if (matchRosterLimit != null && normalizedIds.size() > matchRosterLimit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "В протокол Кубка можно включить не более " + matchRosterLimit + " игроков.");
        }
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
            lineupPlayer.setStarter(index < normalizedStarterIds.size());
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

    private Referee resolveSeasonReferee(TourMatch match, Long refereeId) {
        if (refereeId == null) {
            return null;
        }
        boolean assignedToSeason = seasonRefereeRepository.existsBySeason_IdAndReferee_Id(match.getTour().getSeason().getId(), refereeId);
        if (!assignedToSeason) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Судья не привязан к сезону этого матча.");
        }
        return refereeRepository.findById(refereeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Судья не найден."));
    }

    private RefereeAssignments resolveRefereeAssignments(
        TourMatch match,
        Long chiefRefereeId,
        Long assistantRefereeOneId,
        Long assistantRefereeTwoId
    ) {
        Referee chiefReferee = resolveSeasonReferee(match, chiefRefereeId);
        Referee assistantRefereeOne = resolveSeasonReferee(match, assistantRefereeOneId);
        Referee assistantRefereeTwo = resolveSeasonReferee(match, assistantRefereeTwoId);

        Set<Long> uniqueRefereeIds = new HashSet<>();
        validateUniqueReferee(uniqueRefereeIds, chiefReferee);
        validateUniqueReferee(uniqueRefereeIds, assistantRefereeOne);
        validateUniqueReferee(uniqueRefereeIds, assistantRefereeTwo);

        return new RefereeAssignments(chiefReferee, assistantRefereeOne, assistantRefereeTwo);
    }

    private void validateUniqueReferee(Set<Long> uniqueRefereeIds, Referee referee) {
        if (referee == null) {
            return;
        }
        if (!uniqueRefereeIds.add(referee.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Одного и того же судью нельзя назначить на несколько ролей в матче.");
        }
    }

    private List<Referee> loadSeasonReferees(TourMatch match) {
        return seasonRefereeRepository.findAllBySeasonIdOrderByRefereeFullNameAsc(match.getTour().getSeason().getId()).stream()
            .map(seasonReferee -> seasonReferee.getReferee())
            .toList();
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

    private void ensureSeasonProtocolExportSnapshots(Long seasonId) {
        List<TourMatch> verifiedMatches = tourMatchRepository.findAllActiveDetailedBySeasonId(seasonId).stream()
            .filter(match -> match.getProtocol() != null && match.getProtocol().getStatus() == MatchProtocolStatus.VERIFIED)
            .toList();

        if (verifiedMatches.isEmpty()) {
            matchProtocolExportSnapshotRepository.deleteAllBySeasonId(seasonId);
            return;
        }

        Set<Long> verifiedMatchIds = verifiedMatches.stream()
            .map(TourMatch::getId)
            .collect(java.util.stream.Collectors.toSet());
        Set<Long> snapshotMatchIds = new HashSet<>(matchProtocolExportSnapshotRepository.findMatchIdsBySeasonId(seasonId));

        for (Long snapshotMatchId : snapshotMatchIds) {
            if (!verifiedMatchIds.contains(snapshotMatchId)) {
                matchProtocolExportSnapshotRepository.deleteByMatchId(snapshotMatchId);
            }
        }

        for (TourMatch verifiedMatch : verifiedMatches) {
            if (!snapshotMatchIds.contains(verifiedMatch.getId())) {
                upsertSeasonProtocolExportSnapshot(getMatchDetails(verifiedMatch.getId()));
            }
        }
    }

    private MatchLineupsData loadLineups(TourMatch match) {
        Long matchId = match.getId();
        Long seasonId = match.getTour().getSeason().getId();
        Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> suspendedPlayers = isCupMatch(match)
            ? competitionDisciplineService.suspendedForMatch(match.getTour().getCompetition().getId(), matchId)
            : seasonDisciplineService.getSuspendedPlayersForMatch(seasonId, matchId);

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

    private SeasonProtocolExportMatchData toSeasonProtocolExportMatchData(MatchDetailsData data) {
        Map<String, ProtocolPlayerStats> statsByPlayerKey = buildProtocolPlayerStatsMap(data.events());
        MatchProtocol protocol = data.protocol();

        String note = normalizeExportNote(data);

        return new SeasonProtocolExportMatchData(
            data.match().getId(),
            data.match().getTour().getName(),
            data.match().getKickoffAt(),
            data.match().getHomeTeam().getName(),
            data.match().getAwayTeam().getName(),
            protocol.getHomeScore(),
            protocol.getAwayScore(),
            protocol.isHomeTechnicalDefeat(),
            protocol.isAwayTechnicalDefeat(),
            note,
            List.of(
                new SeasonProtocolExportRefereeData("Главный арбитр", protocol.getChiefReferee() == null ? null : protocol.getChiefReferee().getFullName()),
                new SeasonProtocolExportRefereeData("Помощник 1", protocol.getAssistantRefereeOne() == null ? null : protocol.getAssistantRefereeOne().getFullName()),
                new SeasonProtocolExportRefereeData("Помощник 2", protocol.getAssistantRefereeTwo() == null ? null : protocol.getAssistantRefereeTwo().getFullName())
            ),
            List.of(
                toSeasonProtocolExportTeamData(data.homeLineup(), statsByPlayerKey),
                toSeasonProtocolExportTeamData(data.awayLineup(), statsByPlayerKey)
            ),
            buildSeasonProtocolExportFileName(data.match())
        );
    }

    private SeasonProtocolExportMatchData toSeasonProtocolExportMatchData(MatchProtocolExportSnapshot snapshot) {
        Map<Long, Boolean> starterByPlayerId = matchLineupPlayerRepository.findAllDetailedByMatchId(snapshot.getMatchId()).stream()
            .collect(java.util.stream.Collectors.toMap(
                lineupPlayer -> lineupPlayer.getPlayer().getId(),
                MatchLineupPlayer::isStarter,
                (existing, ignored) -> existing,
                LinkedHashMap::new
            ));
        List<SeasonProtocolExportTeamData> teams = readSnapshotJson(snapshot.getTeamsJson(), EXPORT_TEAMS_TYPE);
        List<SeasonProtocolExportTeamData> teamsWithLineupRoles = teams.stream()
            .map(team -> new SeasonProtocolExportTeamData(
                team.teamName(),
                team.players().stream()
                    .map(player -> new SeasonProtocolExportPlayerData(
                        player.playerId(),
                        player.playerName(),
                        player.sortOrder(),
                        starterByPlayerId.getOrDefault(player.playerId(), player.isStarter()),
                        player.goals(),
                        player.yellowCards(),
                        player.redCards()
                    ))
                    .toList()
            ))
            .toList();

        return new SeasonProtocolExportMatchData(
            snapshot.getMatchId(),
            snapshot.getTourName(),
            snapshot.getKickoffAt(),
            snapshot.getHomeTeamName(),
            snapshot.getAwayTeamName(),
            snapshot.getHomeScore(),
            snapshot.getAwayScore(),
            snapshot.isHomeTechnicalDefeat(),
            snapshot.isAwayTechnicalDefeat(),
            snapshot.getNote(),
            readSnapshotJson(snapshot.getRefereesJson(), EXPORT_REFEREES_TYPE),
            teamsWithLineupRoles,
            buildProtocolExportFileName(
                snapshot.getKickoffAt(),
                firstNonBlank(snapshot.getHomeTeamShortName(), snapshot.getHomeTeamName()),
                firstNonBlank(snapshot.getAwayTeamShortName(), snapshot.getAwayTeamName())
            )
        );
    }

    private void syncSeasonProtocolExportSnapshot(MatchDetailsData data) {
        if (data.protocol().getStatus() == MatchProtocolStatus.VERIFIED) {
            upsertSeasonProtocolExportSnapshot(data);
            return;
        }
        matchProtocolExportSnapshotRepository.deleteByMatchId(data.match().getId());
    }

    private void upsertSeasonProtocolExportSnapshot(MatchDetailsData data) {
        SeasonProtocolExportMatchData exportData = toSeasonProtocolExportMatchData(data);
        MatchProtocolExportSnapshot snapshot = matchProtocolExportSnapshotRepository.findByMatchId(data.match().getId())
            .orElseGet(MatchProtocolExportSnapshot::new);

        snapshot.setSeasonId(data.match().getTour().getSeason().getId());
        snapshot.setMatchId(data.match().getId());
        snapshot.setTourSortOrder(data.match().getTour().getSortOrder());
        snapshot.setKickoffAt(exportData.kickoffAt());
        snapshot.setTourName(exportData.tourName());
        snapshot.setHomeTeamName(exportData.homeTeamName());
        snapshot.setAwayTeamName(exportData.awayTeamName());
        snapshot.setHomeTeamShortName(firstNonBlank(data.match().getHomeTeam().getShortName(), data.match().getHomeTeam().getName()));
        snapshot.setAwayTeamShortName(firstNonBlank(data.match().getAwayTeam().getShortName(), data.match().getAwayTeam().getName()));
        snapshot.setHomeScore(exportData.homeScore());
        snapshot.setAwayScore(exportData.awayScore());
        snapshot.setHomeTechnicalDefeat(exportData.homeTechnicalDefeat());
        snapshot.setAwayTechnicalDefeat(exportData.awayTechnicalDefeat());
        snapshot.setNote(exportData.note());
        snapshot.setFileName(exportData.fileName());
        snapshot.setRefereesJson(writeSnapshotJson(exportData.referees()));
        snapshot.setTeamsJson(writeSnapshotJson(exportData.teams()));
        snapshot.setUpdatedAt(OffsetDateTime.now());
        matchProtocolExportSnapshotRepository.save(snapshot);
    }

    private String writeSnapshotJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось сохранить снимок подтвержденного протокола.", exception);
        }
    }

    private <T> T readSnapshotJson(String rawJson, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(rawJson, typeReference);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Не удалось прочитать снимок подтвержденного протокола.", exception);
        }
    }

    private SeasonProtocolExportTeamData toSeasonProtocolExportTeamData(TeamLineupData lineup, Map<String, ProtocolPlayerStats> statsByPlayerKey) {
        return new SeasonProtocolExportTeamData(
            lineup.teamName(),
            lineup.players().stream()
                .map(player -> {
                    ProtocolPlayerStats stats = statsByPlayerKey.getOrDefault(playerStatsKey(lineup.teamId(), player.playerId()), ProtocolPlayerStats.EMPTY);
                    return new SeasonProtocolExportPlayerData(
                        player.playerId(),
                        player.playerName(),
                        player.sortOrder(),
                        player.isStarter(),
                        stats.goals(),
                        stats.yellowCards(),
                        stats.redCards()
                    );
                })
                .toList()
        );
    }

    private Map<String, ProtocolPlayerStats> buildProtocolPlayerStatsMap(List<MatchEvent> events) {
        Map<String, ProtocolPlayerStats> statsByPlayerKey = new LinkedHashMap<>();

        for (MatchEvent event : events) {
            if (event.getTeam() == null || event.getPlayer() == null) {
                continue;
            }

            String key = playerStatsKey(event.getTeam().getId(), event.getPlayer().getId());
            ProtocolPlayerStats current = statsByPlayerKey.getOrDefault(key, ProtocolPlayerStats.EMPTY);
            statsByPlayerKey.put(key, switch (event.getEventType()) {
                case GOAL, PENALTY_GOAL -> current.withGoals(current.goals() + 1);
                case YELLOW_CARD -> current.withYellowCards(current.yellowCards() + 1);
                case RED_CARD, SECOND_YELLOW_RED -> current.withRedCards(current.redCards() + 1);
                default -> current;
            });
        }

        return statsByPlayerKey;
    }

    private String normalizeExportNote(MatchDetailsData data) {
        MatchProtocol protocol = data.protocol();
        if (protocol.isHomeTechnicalDefeat()) {
            return "Зафиксировано техническое поражение команды " + data.match().getHomeTeam().getName() + ".";
        }
        if (protocol.isAwayTechnicalDefeat()) {
            return "Зафиксировано техническое поражение команды " + data.match().getAwayTeam().getName() + ".";
        }
        return normalizeNullable(protocol.getNotes()) == null ? "Дополнительные замечания по матчу не указаны." : normalizeNullable(protocol.getNotes());
    }

    private String buildSeasonProtocolExportFileName(TourMatch match) {
        return buildProtocolExportFileName(
            match.getKickoffAt(),
            firstNonBlank(match.getHomeTeam().getShortName(), match.getHomeTeam().getName()),
            firstNonBlank(match.getAwayTeam().getShortName(), match.getAwayTeam().getName())
        );
    }

    static String buildProtocolExportFileName(OffsetDateTime kickoffAt, String homeTeamName, String awayTeamName) {
        String date = kickoffAt == null ? "date-unknown" : kickoffAt.format(EXPORT_FILE_DATE_FORMATTER);
        String home = sanitizeFileNamePart(homeTeamName, "home");
        String away = sanitizeFileNamePart(awayTeamName, "away");
        return "protocol_" + home + "_" + away + "_" + date + ".pdf";
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private static String sanitizeFileNamePart(String value, String fallback) {
        String normalized = value == null ? fallback : value.trim();
        if (normalized.isEmpty()) {
            normalized = fallback;
        }
        String sanitized = normalized
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .replaceAll("\\s+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
        return sanitized.isEmpty() ? fallback : sanitized;
    }

    private String playerStatsKey(Long teamId, Long playerId) {
        return teamId + ":" + playerId;
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
                item.isStarter(),
                item.getSortOrder(),
                seasonId,
                suspendedPlayers.containsKey(item.getPlayer().getId()),
                suspendedPlayers.containsKey(item.getPlayer().getId()) ? suspendedPlayers.get(item.getPlayer().getId()).reason() : null
            ))
            .toList();
        Set<Long> selectedPlayerIds = players.stream()
            .map(LineupPlayerData::playerId)
            .collect(java.util.stream.Collectors.toSet());
        List<Player> eligiblePlayers = isCupMatch(match)
            ? competitionService.eligiblePlayers(seasonId, match.getTour().getCompetition().getId(), team.getId())
            : seasonPlayerService.listEligibleRosterMemberships(team.getId(), seasonId).stream().map(PlayerTeam::getPlayer).toList();
        List<AvailablePlayerData> availablePlayers = eligiblePlayers.stream()
            .filter(item -> !selectedPlayerIds.contains(item.getId()))
            .map(item -> new AvailablePlayerData(
                item.getId(),
                item.getFullName(),
                item.isGoalkeeper(),
                seasonId,
                suspendedPlayers.containsKey(item.getId()),
                suspendedPlayers.containsKey(item.getId()) ? suspendedPlayers.get(item.getId()).reason() : null
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

    private boolean isCupMatch(TourMatch match) {
        return match.getTour().getCompetition() != null
            && match.getTour().getCompetition().getType() == com.footballstats.backend.domain.CompetitionType.CUP;
    }

    private void validateRequiredLineupsForProtocolStatus(TourMatch match, MatchProtocolStatus status, boolean allowMissingLineups) {
        if (status != MatchProtocolStatus.FINISHED && status != MatchProtocolStatus.VERIFIED) {
            return;
        }
        if (allowMissingLineups) {
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
        List<PlayerProtocolStatDraft> playerStats,
        boolean allowManualScoreWithoutPlayerStats
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
        if (allowManualScoreWithoutPlayerStats) {
            return new ProtocolScoreData(normalizedHomeScore, normalizedAwayScore);
        }

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

    private boolean canUseSuperAdminScoreOnlyProtocol(boolean superAdmin, List<PlayerProtocolStatDraft> playerStatDrafts) {
        if (!superAdmin) {
            return false;
        }

        List<PlayerProtocolStatDraft> source = playerStatDrafts == null ? List.of() : playerStatDrafts;
        return source.stream().noneMatch(this::hasAnyStats);
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
        TeamLineupData awayLineup,
        List<Referee> availableReferees
    ) {}

    public record SeasonProtocolExportMatchData(
        Long matchId,
        String tourName,
        OffsetDateTime kickoffAt,
        String homeTeamName,
        String awayTeamName,
        Integer homeScore,
        Integer awayScore,
        boolean homeTechnicalDefeat,
        boolean awayTechnicalDefeat,
        String note,
        List<SeasonProtocolExportRefereeData> referees,
        List<SeasonProtocolExportTeamData> teams,
        String fileName
    ) {}

    public record SeasonProtocolExportRefereeData(
        String label,
        String name
    ) {}

    public record SeasonProtocolExportTeamData(
        String teamName,
        List<SeasonProtocolExportPlayerData> players
    ) {}

    public record SeasonProtocolExportPlayerData(
        Long playerId,
        String playerName,
        int sortOrder,
        boolean isStarter,
        int goals,
        int yellowCards,
        int redCards
    ) {}

    private record ProtocolPlayerStats(
        int goals,
        int yellowCards,
        int redCards
    ) {
        private static final ProtocolPlayerStats EMPTY = new ProtocolPlayerStats(0, 0, 0);

        private ProtocolPlayerStats withGoals(int value) {
            return new ProtocolPlayerStats(value, yellowCards, redCards);
        }

        private ProtocolPlayerStats withYellowCards(int value) {
            return new ProtocolPlayerStats(goals, value, redCards);
        }

        private ProtocolPlayerStats withRedCards(int value) {
            return new ProtocolPlayerStats(goals, yellowCards, value);
        }
    }

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
        boolean isStarter,
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

    private record RefereeAssignments(
        Referee chiefReferee,
        Referee assistantRefereeOne,
        Referee assistantRefereeTwo
    ) {}
}
