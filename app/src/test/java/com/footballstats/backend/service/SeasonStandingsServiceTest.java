package com.footballstats.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.MatchEventRepository;
import com.footballstats.backend.repository.SeasonStandingsConfigRepository;
import com.footballstats.backend.repository.SeasonStandingsRowRepository;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonStandingsServiceTest {

    @Mock private SeasonRepository seasonRepository;
    @Mock private SeasonTeamRepository seasonTeamRepository;
    @Mock private TourMatchRepository tourMatchRepository;
    @Mock private MatchEventRepository matchEventRepository;
    @Mock private SeasonStandingsConfigRepository seasonStandingsConfigRepository;
    @Mock private SeasonStandingsRowRepository seasonStandingsRowRepository;

    private SeasonStandingsService service;

    @BeforeEach
    void setUp() {
        service = new SeasonStandingsService(
            seasonRepository,
            seasonTeamRepository,
            tourMatchRepository,
            matchEventRepository,
            seasonStandingsConfigRepository,
            seasonStandingsRowRepository,
            new ObjectMapper()
        );
    }

    @Test
    void recalculatesPointsOnlyFromVerifiedMatchProtocols() {
        Season season = new Season();
        ReflectionTestUtils.setField(season, "id", 1L);
        Team home = team(10L, "Alpha");
        Team away = team(11L, "Beta");
        SeasonStandingsConfig config = new SeasonStandingsConfig();
        config.setSeason(season);
        config.setWinPoints(3);
        config.setDrawPoints(1);
        config.setLossPoints(0);
        config.setRankingRulesJson("[\"POINTS\",\"GOAL_DIFFERENCE\",\"ALPHABETICAL\"]");

        TourMatch match = new TourMatch();
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        MatchProtocol protocol = new MatchProtocol();
        protocol.setStatus(MatchProtocolStatus.VERIFIED);
        protocol.setHomeScore(2);
        protocol.setAwayScore(1);
        match.setProtocol(protocol);

        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(seasonStandingsConfigRepository.findBySeason_Id(1L)).thenReturn(Optional.of(config));
        when(seasonTeamRepository.findAllBySeasonIdOrderByTeamNameAsc(1L))
            .thenReturn(List.of(seasonTeam(season, home), seasonTeam(season, away)));
        when(tourMatchRepository.findAllActiveDetailedByPublishedSeasonId(1L)).thenReturn(List.of(match));

        service.recalculateSeasonStandings(1L, 99L);

        ArgumentCaptor<SeasonStandingsRow> rows = ArgumentCaptor.forClass(SeasonStandingsRow.class);
        verify(seasonStandingsRowRepository, times(2)).save(rows.capture());
        assertThat(rows.getAllValues())
            .extracting(SeasonStandingsRow::getPosition, SeasonStandingsRow::getPoints, SeasonStandingsRow::getGoalDifference)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, 3, 1),
                org.assertj.core.groups.Tuple.tuple(2, 0, -1)
            );
    }

    @Test
    void headToHeadUsesItsGoalDifferenceBeforeOverallGoalDifference() {
        Season season = season(2L);
        Team alpha = team(20L, "Alpha");
        Team beta = team(21L, "Beta");
        SeasonStandingsConfig config = config(season, "[\"POINTS\",\"HEAD_TO_HEAD\",\"GOAL_DIFFERENCE\",\"ALPHABETICAL\"]");

        prepareRecalculation(
            season,
            config,
            List.of(alpha, beta),
            List.of(match(201L, alpha, beta, 3, 0), match(202L, beta, alpha, 1, 0))
        );

        service.recalculateSeasonStandings(2L, 99L);

        assertThat(savedRows())
            .extracting(row -> row.getTeam().getName(), SeasonStandingsRow::getPosition)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("Alpha", 1),
                org.assertj.core.groups.Tuple.tuple("Beta", 2)
            );
    }

    @Test
    void headToHeadOutranksBetterOverallGoalDifferenceInsidePointsBucket() {
        Season season = season(3L);
        Team alpha = team(30L, "Alpha");
        Team beta = team(31L, "Beta");
        Team delta = team(32L, "Delta");
        Team echo = team(33L, "Echo");
        SeasonStandingsConfig config = config(season, "[\"POINTS\",\"HEAD_TO_HEAD\",\"GOAL_DIFFERENCE\",\"ALPHABETICAL\"]");

        prepareRecalculation(
            season,
            config,
            List.of(alpha, beta, delta, echo),
            List.of(
                match(301L, alpha, beta, 1, 0),
                match(302L, delta, alpha, 5, 0),
                match(303L, beta, delta, 10, 0),
                match(304L, delta, echo, 1, 0)
            )
        );

        service.recalculateSeasonStandings(3L, 99L);

        assertThat(savedRows())
            .extracting(row -> row.getTeam().getName(), SeasonStandingsRow::getPosition)
            .containsSubsequence(
                org.assertj.core.groups.Tuple.tuple("Alpha", 2),
                org.assertj.core.groups.Tuple.tuple("Beta", 3)
            );
    }

    @Test
    void supportsAwayWinsAndFewerDisciplinaryPoints() {
        Season season = season(4L);
        Team alpha = team(40L, "Alpha");
        Team beta = team(41L, "Beta");
        Team delta = team(42L, "Delta");
        Team echo = team(43L, "Echo");
        SeasonStandingsConfig config = config(
            season,
            "[\"POINTS\",\"AWAY_WINS\",\"DISCIPLINARY_POINTS\",\"ALPHABETICAL\"]"
        );
        TourMatch alphaMatch = match(401L, alpha, delta, 1, 0);
        TourMatch betaMatch = match(402L, echo, beta, 0, 1);
        prepareRecalculation(season, config, List.of(alpha, beta, delta, echo), List.of(alphaMatch, betaMatch));

        MatchEvent alphaYellow = event(alphaMatch, alpha, MatchEventType.YELLOW_CARD);
        MatchEvent betaRed = event(betaMatch, beta, MatchEventType.RED_CARD);
        when(matchEventRepository.findAllDetailedBySeasonId(4L)).thenReturn(List.of(alphaYellow, betaRed));

        service.recalculateSeasonStandings(4L, 99L);

        assertThat(savedRows())
            .extracting(row -> row.getTeam().getName(), SeasonStandingsRow::getPosition)
            .containsSubsequence(
                org.assertj.core.groups.Tuple.tuple("Beta", 1),
                org.assertj.core.groups.Tuple.tuple("Alpha", 2)
            );
    }

    private void prepareRecalculation(
        Season season,
        SeasonStandingsConfig config,
        List<Team> teams,
        List<TourMatch> matches
    ) {
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        when(seasonStandingsConfigRepository.findBySeason_Id(season.getId())).thenReturn(Optional.of(config));
        when(seasonTeamRepository.findAllBySeasonIdOrderByTeamNameAsc(season.getId()))
            .thenReturn(teams.stream().map(team -> seasonTeam(season, team)).toList());
        when(tourMatchRepository.findAllActiveDetailedByPublishedSeasonId(season.getId())).thenReturn(matches);
    }

    private List<SeasonStandingsRow> savedRows() {
        ArgumentCaptor<SeasonStandingsRow> rows = ArgumentCaptor.forClass(SeasonStandingsRow.class);
        verify(seasonStandingsRowRepository, atLeastOnce()).save(rows.capture());
        return rows.getAllValues();
    }

    private Season season(Long id) {
        Season season = new Season();
        ReflectionTestUtils.setField(season, "id", id);
        return season;
    }

    private SeasonStandingsConfig config(Season season, String rankingRules) {
        SeasonStandingsConfig config = new SeasonStandingsConfig();
        config.setSeason(season);
        config.setWinPoints(3);
        config.setDrawPoints(1);
        config.setLossPoints(0);
        config.setRankingRulesJson(rankingRules);
        return config;
    }

    private TourMatch match(Long id, Team home, Team away, int homeScore, int awayScore) {
        TourMatch match = new TourMatch();
        ReflectionTestUtils.setField(match, "id", id);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        MatchProtocol protocol = new MatchProtocol();
        protocol.setStatus(MatchProtocolStatus.VERIFIED);
        protocol.setHomeScore(homeScore);
        protocol.setAwayScore(awayScore);
        match.setProtocol(protocol);
        return match;
    }

    private MatchEvent event(TourMatch match, Team team, MatchEventType type) {
        MatchEvent event = new MatchEvent();
        event.setMatch(match);
        event.setTeam(team);
        event.setEventType(type);
        return event;
    }

    private Team team(Long id, String name) {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", id);
        team.setName(name);
        team.setActive(true);
        return team;
    }

    private SeasonTeam seasonTeam(Season season, Team team) {
        SeasonTeam result = new SeasonTeam();
        result.setSeason(season);
        result.setTeam(team);
        return result;
    }
}
