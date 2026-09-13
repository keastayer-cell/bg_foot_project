package com.footballstats.backend.controller;

import com.footballstats.backend.domain.Competition;
import com.footballstats.backend.domain.CompetitionType;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.service.CompetitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonCompetitionSelectionTest {
    @Mock CompetitionService competitions;
    @InjectMocks SeasonController controller;

    private Competition competition(long seasonId, CompetitionType type) {
        Season season = new Season();
        ReflectionTestUtils.setField(season,"id",seasonId);
        Competition competition = new Competition();
        competition.setSeason(season); competition.setType(type);
        return competition;
    }

    @Test void overviewRejectsCompetitionFromAnotherSeason() {
        when(competitions.getCompetition(9L)).thenReturn(competition(2L,CompetitionType.CHAMPIONSHIP));
        assertThatThrownBy(() -> controller.getSeasonOverview(1L,9L))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
    }
    @Test void regularOverviewAndStatsRejectCupInsteadOfReturningChampionshipData() {
        when(competitions.getCompetition(9L)).thenReturn(competition(1L,CompetitionType.CUP));
        assertThatThrownBy(() -> controller.getSeasonOverview(1L,9L))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        assertThatThrownBy(() -> controller.getSeasonPlayerStats(1L,9L))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
    }
}
