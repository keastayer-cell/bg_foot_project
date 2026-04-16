package com.footballstats.backend.service;

import com.footballstats.backend.domain.MatchEvent;
import com.footballstats.backend.domain.MatchEventType;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStandingsConfig;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.repository.MatchEventRepository;
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.SeasonStandingsConfigRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SeasonDisciplineService {

    private final SeasonRepository seasonRepository;
    private final SeasonStandingsConfigRepository seasonStandingsConfigRepository;
    private final TourMatchRepository tourMatchRepository;
    private final MatchEventRepository matchEventRepository;

    public SeasonDisciplineService(
        SeasonRepository seasonRepository,
        SeasonStandingsConfigRepository seasonStandingsConfigRepository,
        TourMatchRepository tourMatchRepository,
        MatchEventRepository matchEventRepository
    ) {
        this.seasonRepository = seasonRepository;
        this.seasonStandingsConfigRepository = seasonStandingsConfigRepository;
        this.tourMatchRepository = tourMatchRepository;
        this.matchEventRepository = matchEventRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, PlayerMatchDiscipline> getSuspendedPlayersForMatch(Long seasonId, Long matchId) {
        Season season = seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));
        SeasonStandingsConfig config = seasonStandingsConfigRepository.findBySeason_Id(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Конфигурация сезона не найдена."));

        List<TourMatch> matches = tourMatchRepository.findAllActiveDetailedBySeasonId(seasonId);
        if (matches.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<MatchEvent>> eventsByMatchId = new LinkedHashMap<>();
        for (MatchEvent event : matchEventRepository.findAllDetailedBySeasonId(seasonId)) {
            eventsByMatchId.computeIfAbsent(event.getMatch().getId(), ignored -> new ArrayList<>()).add(event);
        }

        Map<Long, TeamDisciplineState> stateByPlayerId = new LinkedHashMap<>();
        for (TourMatch match : matches) {
            Map<Long, PlayerMatchDiscipline> suspendedForCurrentMatch = buildSuspendedMap(match, stateByPlayerId, config, season);
            if (match.getId().equals(matchId)) {
                return suspendedForCurrentMatch;
            }

            consumeSuspensions(match, stateByPlayerId);
            applyVerifiedMatchCards(match, eventsByMatchId.getOrDefault(match.getId(), List.of()), stateByPlayerId, config);
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Матч сезона не найден.");
    }

    private Map<Long, PlayerMatchDiscipline> buildSuspendedMap(
        TourMatch match,
        Map<Long, TeamDisciplineState> stateByPlayerId,
        SeasonStandingsConfig config,
        Season season
    ) {
        Map<Long, PlayerMatchDiscipline> result = new LinkedHashMap<>();
        addTeamSuspensions(result, stateByPlayerId, match.getHomeTeam().getId(), config, season);
        addTeamSuspensions(result, stateByPlayerId, match.getAwayTeam().getId(), config, season);
        return result;
    }

    private void addTeamSuspensions(
        Map<Long, PlayerMatchDiscipline> result,
        Map<Long, TeamDisciplineState> stateByPlayerId,
        Long teamId,
        SeasonStandingsConfig config,
        Season season
    ) {
        for (Map.Entry<Long, TeamDisciplineState> entry : stateByPlayerId.entrySet()) {
            TeamDisciplineState state = entry.getValue();
            if (!teamId.equals(state.teamId) || state.pendingSuspensions <= 0) {
                continue;
            }
            result.put(entry.getKey(), new PlayerMatchDiscipline(entry.getKey(), state.pendingSuspensions, buildReason(state, config, season)));
        }
    }

    private void consumeSuspensions(TourMatch match, Map<Long, TeamDisciplineState> stateByPlayerId) {
        consumeTeamSuspensions(match.getHomeTeam().getId(), stateByPlayerId);
        consumeTeamSuspensions(match.getAwayTeam().getId(), stateByPlayerId);
    }

    private void consumeTeamSuspensions(Long teamId, Map<Long, TeamDisciplineState> stateByPlayerId) {
        for (TeamDisciplineState state : stateByPlayerId.values()) {
            if (teamId.equals(state.teamId) && state.pendingSuspensions > 0) {
                state.pendingSuspensions -= 1;
            }
        }
    }

    private void applyVerifiedMatchCards(
        TourMatch match,
        List<MatchEvent> events,
        Map<Long, TeamDisciplineState> stateByPlayerId,
        SeasonStandingsConfig config
    ) {
        MatchProtocol protocol = match.getProtocol();
        if (protocol == null || protocol.getStatus() != MatchProtocolStatus.VERIFIED) {
            return;
        }

        Map<Long, PlayerCardsInMatch> cardsByPlayerId = new LinkedHashMap<>();
        for (MatchEvent event : events) {
            if (event.getPlayer() == null || event.getTeam() == null) {
                continue;
            }
            if (event.getEventType() != MatchEventType.YELLOW_CARD
                && event.getEventType() != MatchEventType.RED_CARD
                && event.getEventType() != MatchEventType.SECOND_YELLOW_RED) {
                continue;
            }
            PlayerCardsInMatch cards = cardsByPlayerId.computeIfAbsent(
                event.getPlayer().getId(),
                ignored -> new PlayerCardsInMatch(event.getTeam().getId())
            );
            if (event.getEventType() == MatchEventType.YELLOW_CARD) {
                cards.yellowCards += 1;
            } else {
                cards.redCards += 1;
            }
        }

        for (Map.Entry<Long, PlayerCardsInMatch> entry : cardsByPlayerId.entrySet()) {
            TeamDisciplineState state = stateByPlayerId.computeIfAbsent(entry.getKey(), ignored -> new TeamDisciplineState(entry.getValue().teamId));
            PlayerCardsInMatch cards = entry.getValue();
            state.teamId = cards.teamId;

            if (cards.redCards > 0) {
                state.yellowCycle = 0;
            } else if (config.getYellowCardsForSuspension() > 0 && cards.yellowCards > 0) {
                state.yellowCycle += cards.yellowCards;
                while (state.yellowCycle >= config.getYellowCardsForSuspension()) {
                    state.yellowCycle -= config.getYellowCardsForSuspension();
                    state.pendingSuspensions += 1;
                    state.lastSuspensionType = SuspensionType.YELLOW;
                }
            }

            if (config.getRedCardsForSuspension() > 0 && cards.redCards > 0) {
                state.redCycle += cards.redCards;
                while (state.redCycle >= config.getRedCardsForSuspension()) {
                    state.redCycle -= config.getRedCardsForSuspension();
                    state.pendingSuspensions += 1;
                    state.lastSuspensionType = SuspensionType.RED;
                }
            }
        }
    }

    private String buildReason(TeamDisciplineState state, SeasonStandingsConfig config, Season season) {
        if (state.lastSuspensionType == SuspensionType.RED) {
            int threshold = config.getRedCardsForSuspension();
            if (threshold > 0) {
                return "Дисквалификация на матч: достигнут порог КК (" + threshold + ") в сезоне «" + season.getName() + "».";
            }
            return "Игрок дисквалифицирован по красной карточке в сезоне «" + season.getName() + "».";
        }
        int threshold = config.getYellowCardsForSuspension();
        return "Дисквалификация на матч: достигнут порог ЖК (" + threshold + ") в сезоне «" + season.getName() + "».";
    }

    public record PlayerMatchDiscipline(Long playerId, int suspensionMatchesRemaining, String reason) {}

    private static final class TeamDisciplineState {
        private Long teamId;
        private int yellowCycle;
        private int redCycle;
        private int pendingSuspensions;
        private SuspensionType lastSuspensionType = SuspensionType.YELLOW;

        private TeamDisciplineState(Long teamId) {
            this.teamId = teamId;
        }
    }

    private static final class PlayerCardsInMatch {
        private final Long teamId;
        private int yellowCards;
        private int redCards;

        private PlayerCardsInMatch(Long teamId) {
            this.teamId = teamId;
        }
    }

    private enum SuspensionType {
        YELLOW,
        RED
    }
}