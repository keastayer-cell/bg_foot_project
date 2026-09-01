package com.footballstats.backend.service;

import com.footballstats.backend.domain.*;
import com.footballstats.backend.repository.MatchEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CompetitionStatsService {
    private final CompetitionService competitionService;
    private final MatchEventRepository eventRepository;
    public CompetitionStatsService(CompetitionService competitionService, MatchEventRepository eventRepository) {
        this.competitionService = competitionService;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<PlayerStats> playerStats(Long competitionId) {
        competitionService.getCompetition(competitionId);
        Map<Long, Mutable> stats = new LinkedHashMap<>();
        for (MatchEvent event : eventRepository.findAllDetailedByCompetitionId(competitionId)) {
            if (event.getPlayer() == null || event.getMatch().getProtocol() == null || event.getMatch().getProtocol().getStatus() != MatchProtocolStatus.VERIFIED) continue;
            Mutable row = stats.computeIfAbsent(event.getPlayer().getId(), ignored -> new Mutable(event.getPlayer().getId(), event.getPlayer().getFullName()));
            if (event.getTeam() != null) row.teams.add(event.getTeam().getShortName() == null || event.getTeam().getShortName().isBlank() ? event.getTeam().getName() : event.getTeam().getShortName());
            switch (event.getEventType()) {
                case GOAL, PENALTY_GOAL -> row.goals++;
                case YELLOW_CARD -> row.yellow++;
                case RED_CARD, SECOND_YELLOW_RED -> row.red++;
                default -> { }
            }
        }
        return stats.values().stream().map(Mutable::data)
            .sorted(Comparator.comparingInt(PlayerStats::goals).reversed().thenComparing(PlayerStats::playerName)).toList();
    }
    public record PlayerStats(Long playerId, String playerName, String teamNames, int goals, int yellowCards, int redCards) {}
    private static final class Mutable {
        final Long id; final String name; final Set<String> teams = new LinkedHashSet<>(); int goals; int yellow; int red;
        Mutable(Long id, String name) { this.id = id; this.name = name; }
        PlayerStats data() { return new PlayerStats(id, name, String.join(", ", teams), goals, yellow, red); }
    }
}
