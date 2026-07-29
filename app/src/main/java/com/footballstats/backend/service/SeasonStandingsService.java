package com.footballstats.backend.service;

import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStandingsConfig;
import com.footballstats.backend.domain.SeasonStandingsRow;
import com.footballstats.backend.domain.SeasonTeam;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.TourMatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.repository.SeasonRepository;
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
    private final SeasonStandingsConfigRepository seasonStandingsConfigRepository;
    private final SeasonStandingsRowRepository seasonStandingsRowRepository;
    private final ObjectMapper objectMapper;

    public SeasonStandingsService(
        SeasonRepository seasonRepository,
        SeasonTeamRepository seasonTeamRepository,
        TourMatchRepository tourMatchRepository,
        SeasonStandingsConfigRepository seasonStandingsConfigRepository,
        SeasonStandingsRowRepository seasonStandingsRowRepository,
        ObjectMapper objectMapper
    ) {
        this.seasonRepository = seasonRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.tourMatchRepository = tourMatchRepository;
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
                match.getHomeTeam().getId(),
                match.getAwayTeam().getId(),
                protocol.getHomeScore(),
                protocol.getAwayScore()
            ));
        }

        List<String> rankingRules = StandingsRankingRules.fromJson(config.getRankingRulesJson(), objectMapper);
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

        Map<Long, Integer> headToHeadPoints = StandingsRankingRules.HEAD_TO_HEAD.equals(currentRule)
            ? calculateHeadToHeadPoints(group, matchResults, config)
            : Map.of();

        Map<Integer, List<StandingsAccumulator>> buckets = new HashMap<>();
        for (StandingsAccumulator accumulator : group) {
            int metric = resolveMetric(currentRule, accumulator, headToHeadPoints);
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

    private Map<Long, Integer> calculateHeadToHeadPoints(
        List<StandingsAccumulator> group,
        List<MatchResult> matchResults,
        SeasonStandingsConfig config
    ) {
        Set<Long> teamIds = new LinkedHashSet<>();
        for (StandingsAccumulator accumulator : group) {
            teamIds.add(accumulator.team().getId());
        }

        Map<Long, Integer> pointsByTeamId = new HashMap<>();
        for (Long teamId : teamIds) {
            pointsByTeamId.put(teamId, 0);
        }

        for (MatchResult matchResult : matchResults) {
            if (!teamIds.contains(matchResult.homeTeamId()) || !teamIds.contains(matchResult.awayTeamId())) {
                continue;
            }

            if (matchResult.homeScore() > matchResult.awayScore()) {
                pointsByTeamId.computeIfPresent(matchResult.homeTeamId(), (ignored, value) -> value + config.getWinPoints());
                pointsByTeamId.computeIfPresent(matchResult.awayTeamId(), (ignored, value) -> value + config.getLossPoints());
            } else if (matchResult.homeScore() < matchResult.awayScore()) {
                pointsByTeamId.computeIfPresent(matchResult.awayTeamId(), (ignored, value) -> value + config.getWinPoints());
                pointsByTeamId.computeIfPresent(matchResult.homeTeamId(), (ignored, value) -> value + config.getLossPoints());
            } else {
                pointsByTeamId.computeIfPresent(matchResult.homeTeamId(), (ignored, value) -> value + config.getDrawPoints());
                pointsByTeamId.computeIfPresent(matchResult.awayTeamId(), (ignored, value) -> value + config.getDrawPoints());
            }
        }

        return pointsByTeamId;
    }

    private int resolveMetric(String rule, StandingsAccumulator accumulator, Map<Long, Integer> headToHeadPoints) {
        return switch (rule) {
            case StandingsRankingRules.POINTS -> accumulator.points();
            case StandingsRankingRules.GOAL_DIFFERENCE -> accumulator.goalDifference();
            case StandingsRankingRules.GOALS_FOR -> accumulator.goalsFor();
            case StandingsRankingRules.WINS -> accumulator.wins();
            case StandingsRankingRules.HEAD_TO_HEAD -> headToHeadPoints.getOrDefault(accumulator.team().getId(), 0);
            default -> 0;
        };
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

        if (homeScore > awayScore) {
            home.wins += 1;
            away.losses += 1;
            home.points += config.getWinPoints();
            away.points += config.getLossPoints();
            return;
        }

        if (homeScore < awayScore) {
            away.wins += 1;
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

    private record MatchResult(Long homeTeamId, Long awayTeamId, int homeScore, int awayScore) {}

    private static final class StandingsAccumulator {
        private final Team team;
        private int matchesPlayed;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;
        private int points;

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
    }
}
