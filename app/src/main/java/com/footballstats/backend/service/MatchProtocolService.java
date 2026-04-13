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

    public MatchProtocolService(
        TourMatchRepository tourMatchRepository,
        MatchProtocolRepository matchProtocolRepository,
        MatchEventRepository matchEventRepository,
        MatchLineupRepository matchLineupRepository,
        MatchLineupPlayerRepository matchLineupPlayerRepository,
        PlayerRepository playerRepository,
        AccessControlService accessControlService,
        SeasonPlayerService seasonPlayerService
    ) {
        this.tourMatchRepository = tourMatchRepository;
        this.matchProtocolRepository = matchProtocolRepository;
        this.matchEventRepository = matchEventRepository;
        this.matchLineupRepository = matchLineupRepository;
        this.matchLineupPlayerRepository = matchLineupPlayerRepository;
        this.playerRepository = playerRepository;
        this.accessControlService = accessControlService;
        this.seasonPlayerService = seasonPlayerService;
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
        Long bestPlayerId,
        String notes,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        List<MatchEventDraft> eventDrafts,
        Long actorUserId
    ) {
        TourMatch match = getExistingDetailedMatch(matchId);
        MatchProtocol protocol = getOrCreateProtocol(match, actorUserId);

        matchEventRepository.deleteByMatch_Id(matchId);

        List<MatchEventDraft> drafts = eventDrafts == null ? List.of() : eventDrafts;
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

        protocol.setStatus(status == null ? MatchProtocolStatus.SCHEDULED : status);
        protocol.setHomeScore(homeScore == null ? calculateScore(match, persistedEvents, true) : Math.max(0, homeScore));
        protocol.setAwayScore(awayScore == null ? calculateScore(match, persistedEvents, false) : Math.max(0, awayScore));
        protocol.setBestPlayer(resolvePlayer(bestPlayerId));
        protocol.setNotes(normalizeNullable(notes));
        protocol.setStartedAt(startedAt);
        protocol.setFinishedAt(finishedAt);
        protocol.setUpdatedByUserId(actorUserId);
        protocol.setUpdatedAt(OffsetDateTime.now());
        matchProtocolRepository.save(protocol);

        TourMatch refreshedMatch = getExistingDetailedMatch(matchId);
        MatchLineupsData lineups = loadLineups(refreshedMatch);
        return new MatchDetailsData(refreshedMatch, protocol, persistedEvents, lineups.homeLineup(), lineups.awayLineup());
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
        }

        MatchLineup lineup = matchLineupRepository.findByMatch_IdAndTeam_Id(matchId, lineupTeam.getId()).orElse(null);
        if (normalizedIds.isEmpty()) {
            if (lineup != null) {
                matchLineupPlayerRepository.deleteByLineup_Id(lineup.getId());
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

        Map<Long, List<MatchLineupPlayer>> playersByTeamId = new LinkedHashMap<>();
        for (MatchLineupPlayer lineupPlayer : matchLineupPlayerRepository.findAllDetailedByMatchId(matchId)) {
            Long teamId = lineupPlayer.getLineup().getTeam().getId();
            playersByTeamId.computeIfAbsent(teamId, ignored -> new ArrayList<>()).add(lineupPlayer);
        }

        Map<Long, MatchLineup> lineupsByTeamId = new LinkedHashMap<>();
        for (MatchLineup lineup : matchLineupRepository.findAllDetailedByMatchId(matchId)) {
            lineupsByTeamId.put(lineup.getTeam().getId(), lineup);
        }

        TeamLineupData homeLineup = buildLineupData(match, match.getHomeTeam(), seasonId, lineupsByTeamId, playersByTeamId);
        TeamLineupData awayLineup = buildLineupData(match, match.getAwayTeam(), seasonId, lineupsByTeamId, playersByTeamId);
        return new MatchLineupsData(homeLineup, awayLineup);
    }

    private TeamLineupData buildLineupData(
        TourMatch match,
        Team team,
        Long seasonId,
        Map<Long, MatchLineup> lineupsByTeamId,
        Map<Long, List<MatchLineupPlayer>> playersByTeamId
    ) {
        MatchLineup lineup = lineupsByTeamId.get(team.getId());
        List<LineupPlayerData> players = playersByTeamId.getOrDefault(team.getId(), List.of()).stream()
            .map(item -> new LineupPlayerData(
                item.getPlayer().getId(),
                item.getPlayer().getFullName(),
                item.getSortOrder(),
                seasonId
            ))
            .toList();
        List<AvailablePlayerData> availablePlayers = seasonPlayerService.listEligibleRosterMemberships(team.getId(), seasonId).stream()
            .map(item -> new AvailablePlayerData(
                item.getPlayer().getId(),
                item.getPlayer().getFullName(),
                seasonId
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

    private int calculateScore(TourMatch match, List<MatchEvent> events, boolean homeScore) {
        long ownTeamId = homeScore ? match.getHomeTeam().getId() : match.getAwayTeam().getId();
        long oppositeTeamId = homeScore ? match.getAwayTeam().getId() : match.getHomeTeam().getId();
        int score = 0;
        for (MatchEvent event : events) {
            Long teamId = event.getTeam() == null ? null : event.getTeam().getId();
            if (teamId == null) {
                continue;
            }
            if ((event.getEventType() == MatchEventType.GOAL || event.getEventType() == MatchEventType.PENALTY_GOAL)
                && teamId.equals(ownTeamId)) {
                score += 1;
            }
            if (event.getEventType() == MatchEventType.OWN_GOAL && teamId.equals(oppositeTeamId)) {
                score += 1;
            }
        }
        return score;
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
        int sortOrder,
        Long seasonId
    ) {}

    public record AvailablePlayerData(
        Long playerId,
        String playerName,
        Long seasonId
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
}
