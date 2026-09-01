package com.footballstats.backend.service;

import com.footballstats.backend.domain.*;
import com.footballstats.backend.repository.CupTieMatchRepository;
import com.footballstats.backend.repository.MatchEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CompetitionDisciplineService {
    private final CompetitionService competitionService;
    private final CupTieMatchRepository tieMatchRepository;
    private final MatchEventRepository eventRepository;

    public CompetitionDisciplineService(CompetitionService competitionService, CupTieMatchRepository tieMatchRepository, MatchEventRepository eventRepository) {
        this.competitionService = competitionService;
        this.tieMatchRepository = tieMatchRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> suspendedForMatch(Long competitionId, Long matchId) {
        Competition competition = competitionService.getCompetition(competitionId);
        Map<Long, List<MatchEvent>> eventsByMatch = new HashMap<>();
        for (MatchEvent event : eventRepository.findAllDetailedByCompetitionId(competitionId)) {
            eventsByMatch.computeIfAbsent(event.getMatch().getId(), ignored -> new ArrayList<>()).add(event);
        }
        Map<Long, State> states = new LinkedHashMap<>();
        for (var link : tieMatchRepository.findAllDetailedByCompetitionId(competitionId)) {
            TourMatch match = link.getMatch();
            Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> current = currentSuspensions(match, states, competition);
            if (match.getId().equals(matchId)) return current;
            consume(match, states);
            if (match.getProtocol() != null && match.getProtocol().getStatus() == MatchProtocolStatus.VERIFIED) {
                applyCards(match, eventsByMatch.getOrDefault(match.getId(), List.of()), states, competition);
            }
        }
        return Map.of();
    }

    private Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> currentSuspensions(TourMatch match, Map<Long, State> states, Competition competition) {
        Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> result = new LinkedHashMap<>();
        Map<Long, Long> currentTeams = currentPlayerTeams(match, competition);
        for (var entry : states.entrySet()) {
            State state = entry.getValue();
            Long currentTeamId = currentTeams.get(entry.getKey());
            if (currentTeamId != null && state.pending > 0) {
                state.teamId = currentTeamId;
                result.put(entry.getKey(), new SeasonDisciplineService.PlayerMatchDiscipline(entry.getKey(), state.pending,
                    "Кубковая дисквалификация в соревновании «" + competition.getName() + "»: осталось матчей — " + state.pending + "."));
            }
        }
        return result;
    }

    private void consume(TourMatch match, Map<Long, State> states) {
        for (State state : states.values()) {
            if ((match.getHomeTeam().getId().equals(state.teamId) || match.getAwayTeam().getId().equals(state.teamId)) && state.pending > 0) state.pending--;
        }
    }

    private Map<Long, Long> currentPlayerTeams(TourMatch match, Competition competition) {
        Map<Long, Long> result = new HashMap<>();
        Long seasonId = competition.getSeason().getId();
        Long competitionId = competition.getId();
        for (Player player : competitionService.eligiblePlayers(seasonId, competitionId, match.getHomeTeam().getId())) {
            result.put(player.getId(), match.getHomeTeam().getId());
        }
        for (Player player : competitionService.eligiblePlayers(seasonId, competitionId, match.getAwayTeam().getId())) {
            result.put(player.getId(), match.getAwayTeam().getId());
        }
        return result;
    }

    private void applyCards(TourMatch match, List<MatchEvent> events, Map<Long, State> states, Competition competition) {
        Map<Long, Cards> cards = new LinkedHashMap<>();
        for (MatchEvent event : events) {
            if (event.getPlayer() == null || event.getTeam() == null) continue;
            if (event.getEventType() != MatchEventType.YELLOW_CARD && event.getEventType() != MatchEventType.RED_CARD && event.getEventType() != MatchEventType.SECOND_YELLOW_RED) continue;
            Cards item = cards.computeIfAbsent(event.getPlayer().getId(), ignored -> new Cards(event.getTeam().getId()));
            if (event.getEventType() == MatchEventType.YELLOW_CARD) item.yellow++; else item.red++;
        }
        for (var entry : cards.entrySet()) {
            Cards item = entry.getValue();
            State state = states.computeIfAbsent(entry.getKey(), ignored -> new State(item.teamId));
            state.teamId = item.teamId;
            if (item.red > 0) {
                state.pending += competition.getRedSuspensionMatches() * item.red;
            }
            if (competition.getYellowCardsForSuspension() > 0 && item.yellow > 0) {
                state.yellow += item.yellow;
                while (state.yellow >= competition.getYellowCardsForSuspension()) {
                    state.yellow -= competition.getYellowCardsForSuspension();
                    state.pending += competition.getYellowSuspensionMatches();
                }
            }
        }
    }

    private static final class State { Long teamId; int yellow; int pending; State(Long teamId) { this.teamId = teamId; } }
    private static final class Cards { final Long teamId; int yellow; int red; Cards(Long teamId) { this.teamId = teamId; } }
}
