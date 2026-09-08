package com.footballstats.backend.service;

import com.footballstats.backend.domain.Competition;
import com.footballstats.backend.domain.CompetitionType;
import com.footballstats.backend.domain.MatchEvent;
import com.footballstats.backend.domain.MatchEventType;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.SeasonStandingsConfig;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.repository.CupTieMatchRepository;
import com.footballstats.backend.repository.MatchEventRepository;
import com.footballstats.backend.repository.SeasonStandingsConfigRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DisciplineNotificationService {

    private final TourMatchRepository matchRepository;
    private final CupTieMatchRepository cupMatchRepository;
    private final MatchEventRepository eventRepository;
    private final SeasonStandingsConfigRepository standingsConfigRepository;
    private final SiteNotificationService notificationService;

    public DisciplineNotificationService(
        TourMatchRepository matchRepository,
        CupTieMatchRepository cupMatchRepository,
        MatchEventRepository eventRepository,
        SeasonStandingsConfigRepository standingsConfigRepository,
        SiteNotificationService notificationService
    ) {
        this.matchRepository = matchRepository;
        this.cupMatchRepository = cupMatchRepository;
        this.eventRepository = eventRepository;
        this.standingsConfigRepository = standingsConfigRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void notifySuspensionsCausedBy(TourMatch match, Long actorUserId) {
        if (match == null || match.getId() == null) return;
        for (Suspension suspension : findSuspensions(match)) {
            notificationService.notifyPlayerSuspended(
                match, suspension.player(), suspension.team(), suspension.cause(), suspension.matches(), actorUserId
            );
        }
    }

    private List<Suspension> findSuspensions(TourMatch currentMatch) {
        Competition competition = currentMatch.getTour().getCompetition();
        if (competition != null && competition.getType() == CompetitionType.CUP) {
            List<TourMatch> matches = cupMatchRepository.findAllDetailedByCompetitionId(competition.getId()).stream()
                .map(link -> link.getMatch()).toList();
            Map<Long, List<MatchEvent>> events = groupEvents(eventRepository.findAllDetailedByCompetitionId(competition.getId()));
            return calculate(
                currentMatch, matches, events, competition.getYellowCardsForSuspension(),
                competition.getYellowSuspensionMatches(), competition.getRedSuspensionMatches(), false
            );
        }

        Long seasonId = currentMatch.getTour().getSeason().getId();
        SeasonStandingsConfig config = standingsConfigRepository.findBySeason_Id(seasonId).orElse(null);
        if (config == null) return List.of();
        return calculate(
            currentMatch,
            matchRepository.findAllActiveDetailedBySeasonId(seasonId),
            groupEvents(eventRepository.findAllDetailedBySeasonId(seasonId)),
            config.getYellowCardsForSuspension(), config.getYellowSuspensionMatches(),
            config.getRedCardsForSuspension(), true
        );
    }

    private List<Suspension> calculate(
        TourMatch currentMatch,
        List<TourMatch> matches,
        Map<Long, List<MatchEvent>> eventsByMatch,
        int yellowThreshold,
        int yellowSuspensionMatches,
        int redSuspensionMatches,
        boolean redResetsYellowCycle
    ) {
        Map<Long, Integer> yellowCycles = new LinkedHashMap<>();
        for (TourMatch match : matches) {
            Map<Long, CardTotals> cards = summarize(eventsByMatch.getOrDefault(match.getId(), List.of()));
            if (match.getId().equals(currentMatch.getId())) {
                return suspensionsFromCurrent(
                    cards, yellowCycles, yellowThreshold, yellowSuspensionMatches,
                    redSuspensionMatches, redResetsYellowCycle
                );
            }
            if (match.getProtocol() == null || match.getProtocol().getStatus() != MatchProtocolStatus.VERIFIED) continue;
            applyPrevious(cards, yellowCycles, yellowThreshold, redResetsYellowCycle);
        }
        return List.of();
    }

    private void applyPrevious(
        Map<Long, CardTotals> cards,
        Map<Long, Integer> yellowCycles,
        int yellowThreshold,
        boolean redResetsYellowCycle
    ) {
        for (Map.Entry<Long, CardTotals> entry : cards.entrySet()) {
            CardTotals totals = entry.getValue();
            if (redResetsYellowCycle && totals.red > 0) {
                yellowCycles.put(entry.getKey(), 0);
            } else if (yellowThreshold > 0 && totals.yellow > 0) {
                yellowCycles.put(entry.getKey(), (yellowCycles.getOrDefault(entry.getKey(), 0) + totals.yellow) % yellowThreshold);
            }
        }
    }

    private List<Suspension> suspensionsFromCurrent(
        Map<Long, CardTotals> cards,
        Map<Long, Integer> yellowCycles,
        int yellowThreshold,
        int yellowSuspensionMatches,
        int redSuspensionMatches,
        boolean redResetsYellowCycle
    ) {
        List<Suspension> result = new ArrayList<>();
        for (Map.Entry<Long, CardTotals> entry : cards.entrySet()) {
            CardTotals totals = entry.getValue();
            if (totals.red > 0 && redSuspensionMatches > 0) {
                result.add(new Suspension(totals.player, totals.team, "RED", totals.red * redSuspensionMatches));
            }
            if (redResetsYellowCycle && totals.red > 0) continue;
            if (yellowThreshold > 0 && yellowSuspensionMatches > 0 && totals.yellow > 0) {
                int crossings = (yellowCycles.getOrDefault(entry.getKey(), 0) + totals.yellow) / yellowThreshold;
                if (crossings > 0) {
                    result.add(new Suspension(totals.player, totals.team, "YELLOW", crossings * yellowSuspensionMatches));
                }
            }
        }
        return result;
    }

    private Map<Long, CardTotals> summarize(List<MatchEvent> events) {
        Map<Long, CardTotals> result = new LinkedHashMap<>();
        for (MatchEvent event : events) {
            if (event.getPlayer() == null || event.getTeam() == null || !isCard(event.getEventType())) continue;
            CardTotals totals = result.computeIfAbsent(
                event.getPlayer().getId(), ignored -> new CardTotals(event.getPlayer(), event.getTeam())
            );
            if (event.getEventType() == MatchEventType.YELLOW_CARD) totals.yellow += 1;
            else totals.red += 1;
        }
        return result;
    }

    private Map<Long, List<MatchEvent>> groupEvents(List<MatchEvent> events) {
        Map<Long, List<MatchEvent>> result = new LinkedHashMap<>();
        for (MatchEvent event : events) {
            result.computeIfAbsent(event.getMatch().getId(), ignored -> new ArrayList<>()).add(event);
        }
        return result;
    }

    private boolean isCard(MatchEventType type) {
        return type == MatchEventType.YELLOW_CARD || type == MatchEventType.RED_CARD || type == MatchEventType.SECOND_YELLOW_RED;
    }

    private record Suspension(Player player, Team team, String cause, int matches) {}

    private static final class CardTotals {
        private final Player player;
        private final Team team;
        private int yellow;
        private int red;

        private CardTotals(Player player, Team team) {
            this.player = player;
            this.team = team;
        }
    }
}
