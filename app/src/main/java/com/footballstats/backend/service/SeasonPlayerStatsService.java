package com.footballstats.backend.service;

import com.footballstats.backend.domain.MatchEvent;
import com.footballstats.backend.domain.MatchEventType;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.repository.MatchEventRepository;
import com.footballstats.backend.repository.SeasonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SeasonPlayerStatsService {

    private static final Comparator<PlayerSeasonStatsRow> PLAYER_STATS_COMPARATOR = Comparator
        .comparingInt(PlayerSeasonStatsRow::goals).reversed()
        .thenComparingInt(PlayerSeasonStatsRow::yellowCards).reversed()
        .thenComparingInt(PlayerSeasonStatsRow::redCards).reversed()
        .thenComparing(PlayerSeasonStatsRow::fullName, String.CASE_INSENSITIVE_ORDER);

    private final SeasonRepository seasonRepository;
    private final MatchEventRepository matchEventRepository;

    public SeasonPlayerStatsService(
        SeasonRepository seasonRepository,
        MatchEventRepository matchEventRepository
    ) {
        this.seasonRepository = seasonRepository;
        this.matchEventRepository = matchEventRepository;
    }

    @Transactional(readOnly = true)
    public List<PlayerSeasonStatsRow> getSeasonPlayerStats(Long seasonId) {
        if (!seasonRepository.existsById(seasonId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден.");
        }

        Map<Long, MutablePlayerSeasonStats> statsByPlayerId = new LinkedHashMap<>();
        for (MatchEvent event : matchEventRepository.findAllDetailedBySeasonId(seasonId)) {
            if (!isCountableEvent(event)) {
                continue;
            }

            MutablePlayerSeasonStats stats = statsByPlayerId.computeIfAbsent(
                event.getPlayer().getId(),
                ignored -> new MutablePlayerSeasonStats(event.getPlayer().getId(), event.getPlayer().getFullName())
            );
            if (event.getTeam() != null) {
                String teamName = String.valueOf(event.getTeam().getName() == null ? "" : event.getTeam().getName()).trim();
                if (!teamName.isEmpty()) {
                    stats.teamNames.add(teamName);
                }
            }

            switch (event.getEventType()) {
                case GOAL, PENALTY_GOAL -> stats.goals += 1;
                case YELLOW_CARD -> stats.yellowCards += 1;
                case RED_CARD, SECOND_YELLOW_RED -> stats.redCards += 1;
                default -> {
                }
            }
        }

        return statsByPlayerId.values().stream()
            .map(MutablePlayerSeasonStats::toRow)
            .sorted(PLAYER_STATS_COMPARATOR)
            .toList();
    }

    private boolean isCountableEvent(MatchEvent event) {
        if (event == null || event.getPlayer() == null || event.getMatch() == null || event.getMatch().getTour() == null) {
            return false;
        }

        MatchProtocol protocol = event.getMatch().getProtocol();
        if (!event.getMatch().getTour().isPublished() || protocol == null || protocol.getStatus() != MatchProtocolStatus.VERIFIED) {
            return false;
        }

        MatchEventType eventType = event.getEventType();
        return eventType == MatchEventType.GOAL
            || eventType == MatchEventType.PENALTY_GOAL
            || eventType == MatchEventType.YELLOW_CARD
            || eventType == MatchEventType.RED_CARD
            || eventType == MatchEventType.SECOND_YELLOW_RED;
    }

    public record PlayerSeasonStatsRow(
        Long playerId,
        String fullName,
        String teamName,
        int goals,
        int yellowCards,
        int redCards
    ) {}

    private static final class MutablePlayerSeasonStats {
        private final Long playerId;
        private final String fullName;
        private final Set<String> teamNames = new LinkedHashSet<>();
        private int goals;
        private int yellowCards;
        private int redCards;

        private MutablePlayerSeasonStats(Long playerId, String fullName) {
            this.playerId = playerId;
            this.fullName = String.valueOf(fullName == null ? "" : fullName).trim();
        }

        private PlayerSeasonStatsRow toRow() {
            String teamName = teamNames.isEmpty() ? null : String.join(", ", teamNames);
            return new PlayerSeasonStatsRow(playerId, fullName, teamName, goals, yellowCards, redCards);
        }
    }
}