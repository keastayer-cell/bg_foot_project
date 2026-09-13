package com.footballstats.backend.service;

import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.MatchEvent;
import com.footballstats.backend.domain.MatchEventType;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStandingsConfig;
import com.footballstats.backend.domain.SeasonStandingsRow;
import com.footballstats.backend.domain.SeasonTeam;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.TourMatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.MatchEventRepository;
import com.footballstats.backend.repository.SeasonStandingsConfigRepository;
import com.footballstats.backend.repository.SeasonStandingsRowRepository;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SeasonStandingsService {

    private final SeasonRepository seasonRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final TourMatchRepository tourMatchRepository;
    private final MatchEventRepository matchEventRepository;
    private final SeasonStandingsConfigRepository seasonStandingsConfigRepository;
    private final SeasonStandingsRowRepository seasonStandingsRowRepository;
    private final ObjectMapper objectMapper;

    public SeasonStandingsService(
        SeasonRepository seasonRepository,
        SeasonTeamRepository seasonTeamRepository,
        TourMatchRepository tourMatchRepository,
        MatchEventRepository matchEventRepository,
        SeasonStandingsConfigRepository seasonStandingsConfigRepository,
        SeasonStandingsRowRepository seasonStandingsRowRepository,
        ObjectMapper objectMapper
    ) {
        this.seasonRepository = seasonRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.tourMatchRepository = tourMatchRepository;
        this.matchEventRepository = matchEventRepository;
        this.seasonStandingsConfigRepository = seasonStandingsConfigRepository;
        this.seasonStandingsRowRepository = seasonStandingsRowRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SeasonStandingsSnapshot getSeasonStandings(Long seasonId) {
        getExistingSeason(seasonId);
        SeasonStandingsConfig config = seasonStandingsConfigRepository.findBySeason_Id(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Конфигурация турнирной таблицы для сезона не найдена."));
        List<SeasonStandingsRow> rows = seasonStandingsRowRepository.findAllDetailedBySeasonId(seasonId);
        return new SeasonStandingsSnapshot(config, rows);
    }

    @Transactional
    public void initializeSeasonStandings(Long seasonId, Long actorUserId) {
        Season season = getExistingSeason(seasonId);
        getOrCreateConfig(season, actorUserId);
        recalculateSeasonStandings(seasonId, actorUserId);
    }

    @Transactional
    public void recalculateSeasonStandings(Long seasonId, Long actorUserId) {
        Season season = getExistingSeason(seasonId);
        SeasonStandingsConfig config = getOrCreateConfig(season, actorUserId);
        List<Team> teams = seasonTeamRepository.findAllBySeasonIdOrderByTeamNameAsc(seasonId).stream()
            .map(SeasonTeam::getTeam)
            .filter(Team::isActive)
            .toList();

        Map<Long, StandingsAccumulator> table = new LinkedHashMap<>();
        for (Team team : teams) {
            table.put(team.getId(), new StandingsAccumulator(team));
        }

        List<MatchResult> matchResults = new ArrayList<>();
        List<TourMatch> matches = tourMatchRepository.findAllActiveDetailedByPublishedSeasonId(seasonId);
        for (TourMatch match : matches) {
            if (match.getScheduleStatus() == com.footballstats.backend.domain.MatchScheduleStatus.CANCELLED) {
                continue;
            }
            if (match.getTour() != null
                && !SeasonStructureService.REGULAR_STAGE.equalsIgnoreCase(match.getTour().getStageType())) {
                continue;
            }
            MatchProtocol protocol = match.getProtocol();
            if (protocol == null || protocol.getStatus() != MatchProtocolStatus.VERIFIED) {
                continue;
            }
            if (protocol.getHomeScore() == null || protocol.getAwayScore() == null) {
                continue;
            }

            StandingsAccumulator home = table.get(match.getHomeTeam().getId());
            StandingsAccumulator away = table.get(match.getAwayTeam().getId());
            if (home == null || away == null) {
                continue;
            }

            applyMatchResult(home, away, protocol.getHomeScore(), protocol.getAwayScore(), config);
            matchResults.add(new MatchResult(
                match.getId(),
                match.getHomeTeam().getId(),
                match.getAwayTeam().getId(),
                protocol.getHomeScore(),
                protocol.getAwayScore()
            ));
        }

        List<String> rankingRules = StandingsRankingRules.fromJson(config.getRankingRulesJson(), objectMapper);
        if (rankingRules.contains(StandingsRankingRules.DISCIPLINARY_POINTS)) {
            applyDisciplinaryPoints(seasonId, table, matchResults);
        }
        List<StandingsAccumulator> sortedRows = sortByRules(new ArrayList<>(table.values()), rankingRules, matchResults, config);

        seasonStandingsRowRepository.deleteAllBySeason_Id(seasonId);
        seasonStandingsRowRepository.flush();

        OffsetDateTime now = OffsetDateTime.now();
        for (int index = 0; index < sortedRows.size(); index += 1) {
            StandingsAccumulator accumulator = sortedRows.get(index);
            SeasonStandingsRow row = new SeasonStandingsRow();
            row.setSeason(season);
            row.setTeam(accumulator.team());
            row.setPosition(index + 1);
            row.setMatchesPlayed(accumulator.matchesPlayed());
            row.setWins(accumulator.wins());
            row.setDraws(accumulator.draws());
            row.setLosses(accumulator.losses());
            row.setGoalsFor(accumulator.goalsFor());
            row.setGoalsAgainst(accumulator.goalsAgainst());
            row.setGoalDifference(accumulator.goalDifference());
            row.setPoints(accumulator.points());
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            seasonStandingsRowRepository.save(row);
        }

        config.setLastCalculatedAt(now);
        config.setUpdatedByUserId(actorUserId);
        config.setUpdatedAt(now);
        seasonStandingsConfigRepository.save(config);
    }

    private List<StandingsAccumulator> sortByRules(
        List<StandingsAccumulator> group,
        List<String> rules,
        List<MatchResult> matchResults,
        SeasonStandingsConfig config
    ) {
        if (group.size() <= 1) {
            return group;
        }
        if (rules.isEmpty()) {
            group.sort((left, right) -> left.team().getName().compareToIgnoreCase(right.team().getName()));
            return group;
        }

        String currentRule = rules.get(0);
        List<String> remainingRules = rules.subList(1, rules.size());
        if (StandingsRankingRules.ALPHABETICAL.equals(currentRule)) {
            group.sort((left, right) -> left.team().getName().compareToIgnoreCase(right.team().getName()));
            return group;
        }

        if (StandingsRankingRules.HEAD_TO_HEAD.equals(currentRule)) {
            return sortByHeadToHead(group, remainingRules, matchResults, config);
        }

        Map<Integer, List<StandingsAccumulator>> buckets = new HashMap<>();
        for (StandingsAccumulator accumulator : group) {
            int metric = resolveMetric(currentRule, accumulator);
            buckets.computeIfAbsent(metric, ignored -> new ArrayList<>()).add(accumulator);
        }

        List<Integer> orderedMetrics = new ArrayList<>(buckets.keySet());
        orderedMetrics.sort((left, right) -> Integer.compare(right, left));

        List<StandingsAccumulator> ranked = new ArrayList<>();
        for (Integer metric : orderedMetrics) {
            ranked.addAll(sortByRules(buckets.get(metric), remainingRules, matchResults, config));
        }
        return ranked;
    }

    private List<StandingsAccumulator> sortByHeadToHead(
        List<StandingsAccumulator> group,
        List<String> remainingRules,
        List<MatchResult> matchResults,
        SeasonStandingsConfig config
    ) {
        Set<Long> teamIds = new LinkedHashSet<>();
        for (StandingsAccumulator accumulator : group) {
            teamIds.add(accumulator.team().getId());
        }

        Map<Long, HeadToHeadAccumulator> miniTable = new HashMap<>();
        for (Long teamId : teamIds) {
            miniTable.put(teamId, new HeadToHeadAccumulator());
        }

        for (MatchResult matchResult : matchResults) {
            if (!teamIds.contains(matchResult.homeTeamId()) || !teamIds.contains(matchResult.awayTeamId())) {
                continue;
            }

            HeadToHeadAccumulator home = miniTable.get(matchResult.homeTeamId());
            HeadToHeadAccumulator away = miniTable.get(matchResult.awayTeamId());
            home.goalsFor += matchResult.homeScore();
            home.goalsAgainst += matchResult.awayScore();
            away.goalsFor += matchResult.awayScore();
            away.goalsAgainst += matchResult.homeScore();
            if (matchResult.homeScore() > matchResult.awayScore()) {
                home.wins += 1;
                home.points += config.getWinPoints();
                away.points += config.getLossPoints();
            } else if (matchResult.homeScore() < matchResult.awayScore()) {
                away.wins += 1;
                away.points += config.getWinPoints();
                home.points += config.getLossPoints();
            } else {
                home.points += config.getDrawPoints();
                away.points += config.getDrawPoints();
            }
        }

        return sortByHeadToHeadMetrics(group, 0, miniTable, remainingRules, matchResults, config);
    }

    private List<StandingsAccumulator> sortByHeadToHeadMetrics(
        List<StandingsAccumulator> group,
        int metricIndex,
        Map<Long, HeadToHeadAccumulator> miniTable,
        List<String> remainingRules,
        List<MatchResult> matchResults,
        SeasonStandingsConfig config
    ) {
        if (group.size() <= 1) {
            return group;
        }
        if (metricIndex >= 4) {
            return sortByRules(group, remainingRules, matchResults, config);
        }

        Map<Integer, List<StandingsAccumulator>> buckets = new HashMap<>();
        for (StandingsAccumulator accumulator : group) {
            HeadToHeadAccumulator metric = miniTable.get(accumulator.team().getId());
            int value = switch (metricIndex) {
                case 0 -> metric.points;
                case 1 -> metric.wins;
                case 2 -> metric.goalDifference();
                default -> metric.goalsFor;
            };
            buckets.computeIfAbsent(value, ignored -> new ArrayList<>()).add(accumulator);
        }

        List<Integer> orderedMetrics = new ArrayList<>(buckets.keySet());
        orderedMetrics.sort((left, right) -> Integer.compare(right, left));
        List<StandingsAccumulator> ranked = new ArrayList<>();
        for (Integer metric : orderedMetrics) {
            ranked.addAll(sortByHeadToHeadMetrics(
                buckets.get(metric), metricIndex + 1, miniTable, remainingRules, matchResults, config
            ));
        }
        return ranked;
    }

    private int resolveMetric(String rule, StandingsAccumulator accumulator) {
        return switch (rule) {
            case StandingsRankingRules.POINTS -> accumulator.points();
            case StandingsRankingRules.GOAL_DIFFERENCE -> accumulator.goalDifference();
            case StandingsRankingRules.GOALS_FOR -> accumulator.goalsFor();
            case StandingsRankingRules.WINS -> accumulator.wins();
            case StandingsRankingRules.GOALS_AGAINST -> -accumulator.goalsAgainst();
            case StandingsRankingRules.AWAY_WINS -> accumulator.awayWins();
            case StandingsRankingRules.AWAY_GOALS -> accumulator.awayGoals();
            case StandingsRankingRules.DISCIPLINARY_POINTS -> -accumulator.disciplinaryPoints();
            default -> 0;
        };
    }

    private void applyDisciplinaryPoints(
        Long seasonId,
        Map<Long, StandingsAccumulator> table,
        List<MatchResult> matchResults
    ) {
        Set<Long> includedMatchIds = new LinkedHashSet<>();
        for (MatchResult matchResult : matchResults) {
            includedMatchIds.add(matchResult.matchId());
        }
        if (includedMatchIds.isEmpty()) {
            return;
        }

        for (MatchEvent event : matchEventRepository.findAllDetailedBySeasonId(seasonId)) {
            if (event.getMatch() == null || !includedMatchIds.contains(event.getMatch().getId()) || event.getTeam() == null) {
                continue;
            }
            StandingsAccumulator accumulator = table.get(event.getTeam().getId());
            if (accumulator == null) {
                continue;
            }
            if (event.getEventType() == MatchEventType.YELLOW_CARD) {
                accumulator.disciplinaryPoints += 1;
            } else if (event.getEventType() == MatchEventType.RED_CARD
                || event.getEventType() == MatchEventType.SECOND_YELLOW_RED) {
                accumulator.disciplinaryPoints += 3;
            }
        }
    }

    private void applyMatchResult(
        StandingsAccumulator home,
        StandingsAccumulator away,
        int homeScore,
        int awayScore,
        SeasonStandingsConfig config
    ) {
        home.matchesPlayed += 1;
        away.matchesPlayed += 1;
        home.goalsFor += homeScore;
        home.goalsAgainst += awayScore;
        away.goalsFor += awayScore;
        away.goalsAgainst += homeScore;
        away.awayGoals += awayScore;

        if (homeScore > awayScore) {
            home.wins += 1;
            away.losses += 1;
            home.points += config.getWinPoints();
            away.points += config.getLossPoints();
            return;
        }

        if (homeScore < awayScore) {
            away.wins += 1;
            away.awayWins += 1;
            home.losses += 1;
            away.points += config.getWinPoints();
            home.points += config.getLossPoints();
            return;
        }

        home.draws += 1;
        away.draws += 1;
        home.points += config.getDrawPoints();
        away.points += config.getDrawPoints();
    }

    private SeasonStandingsConfig getOrCreateConfig(Season season, Long actorUserId) {
        return seasonStandingsConfigRepository.findBySeason_Id(season.getId()).orElseGet(() -> {
            OffsetDateTime now = OffsetDateTime.now();
            SeasonStandingsConfig config = new SeasonStandingsConfig();
            config.setSeason(season);
            config.setCreatedByUserId(actorUserId);
            config.setUpdatedByUserId(actorUserId);
            config.setCreatedAt(now);
            config.setUpdatedAt(now);
            return seasonStandingsConfigRepository.save(config);
        });
    }

    private Season getExistingSeason(Long seasonId) {
        return seasonRepository.findById(seasonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден."));
    }

    public record SeasonStandingsSnapshot(SeasonStandingsConfig config, List<SeasonStandingsRow> rows) {}

    private record MatchResult(Long matchId, Long homeTeamId, Long awayTeamId, int homeScore, int awayScore) {}

    private static final class HeadToHeadAccumulator {
        private int points;
        private int wins;
        private int goalsFor;
        private int goalsAgainst;

        private int goalDifference() {
            return goalsFor - goalsAgainst;
        }
    }

    private static final class StandingsAccumulator {
        private final Team team;
        private int matchesPlayed;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;
        private int points;
        private int awayWins;
        private int awayGoals;
        private int disciplinaryPoints;

        private StandingsAccumulator(Team team) {
            this.team = team;
        }

        private Team team() {
            return team;
        }

        private int matchesPlayed() {
            return matchesPlayed;
        }

        private int wins() {
            return wins;
        }

        private int draws() {
            return draws;
        }

        private int losses() {
            return losses;
        }

        private int goalsFor() {
            return goalsFor;
        }

        private int goalsAgainst() {
            return goalsAgainst;
        }

        private int goalDifference() {
            return goalsFor - goalsAgainst;
        }

        private int points() {
            return points;
        }

        private int awayWins() {
            return awayWins;
        }

        private int awayGoals() {
            return awayGoals;
        }

        private int disciplinaryPoints() {
            return disciplinaryPoints;
        }
    }
}
