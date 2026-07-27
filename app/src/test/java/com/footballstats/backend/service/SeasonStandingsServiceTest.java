package com.footballstats.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStandingsConfig;
import com.footballstats.backend.domain.SeasonStandingsRow;
import com.footballstats.backend.domain.SeasonTeam;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.repository.SeasonRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonStandingsServiceTest {

    @Mock private SeasonRepository seasonRepository;
    @Mock private SeasonTeamRepository seasonTeamRepository;
    @Mock private TourMatchRepository tourMatchRepository;
    @Mock private SeasonStandingsConfigRepository seasonStandingsConfigRepository;
    @Mock private SeasonStandingsRowRepository seasonStandingsRowRepository;

    private SeasonStandingsService service;

    @BeforeEach
    void setUp() {
        service = new SeasonStandingsService(
            seasonRepository,
            seasonTeamRepository,
            tourMatchRepository,
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
