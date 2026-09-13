package com.footballstats.backend.service;

import com.footballstats.backend.domain.MatchScheduleStatus;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.repository.LeagueVenueRepository;
import com.footballstats.backend.repository.MatchProtocolRepository;
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import com.footballstats.backend.repository.TourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourServiceScheduleTest {

    @Mock private TourRepository tourRepository;
    @Mock private TourMatchRepository tourMatchRepository;
    @Mock private SeasonRepository seasonRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private SeasonTeamRepository seasonTeamRepository;
    @Mock private MatchProtocolRepository matchProtocolRepository;
    @Mock private SeasonStandingsService seasonStandingsService;
    @Mock private SiteNotificationService siteNotificationService;
    @Mock private LeagueVenueRepository leagueVenueRepository;

    private TourService service;
    private TourMatch match;
    private OffsetDateTime originalKickoff;

    @BeforeEach
    void setUp() {
        service = new TourService(tourRepository, tourMatchRepository, seasonRepository, teamRepository,
            seasonTeamRepository, matchProtocolRepository, seasonStandingsService, siteNotificationService,
            leagueVenueRepository);

        Tour tour = new Tour();
        ReflectionTestUtils.setField(tour, "id", 4L);
        tour.setPublished(false);
        match = new TourMatch();
        ReflectionTestUtils.setField(match, "id", 9L);
        match.setTour(tour);
        originalKickoff = OffsetDateTime.parse("2026-09-12T18:00:00+03:00");
        match.setKickoffAt(originalKickoff);
        when(tourMatchRepository.findDetailedById(9L)).thenReturn(Optional.of(match));
    }

    @Test
    void requiresRescheduledStatusWhenKickoffChanges() {
        assertThatThrownBy(() -> service.updateMatchSchedule(4L, 9L, MatchScheduleStatus.SCHEDULED,
            originalKickoff.plusDays(1), null, null, 7L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Перенесён");
    }

    @Test
    void requiresReasonForReschedule() {
        assertThatThrownBy(() -> service.updateMatchSchedule(4L, 9L, MatchScheduleStatus.RESCHEDULED,
            originalKickoff.plusDays(1), null, "", 7L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("причину");
    }

    @Test
    void preservesOriginalKickoffForValidReschedule() {
        when(tourMatchRepository.save(any(TourMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TourMatch saved = service.updateMatchSchedule(4L, 9L, MatchScheduleStatus.RESCHEDULED,
            originalKickoff.plusDays(1), null, "Просьба обеих команд", 7L);

        assertThat(saved.getScheduleStatus()).isEqualTo(MatchScheduleStatus.RESCHEDULED);
        assertThat(saved.getOriginalKickoffAt()).isEqualTo(originalKickoff);
        assertThat(saved.getKickoffAt()).isEqualTo(originalKickoff.plusDays(1));
        assertThat(saved.getScheduleChangeReason()).isEqualTo("Просьба обеих команд");
    }
}
