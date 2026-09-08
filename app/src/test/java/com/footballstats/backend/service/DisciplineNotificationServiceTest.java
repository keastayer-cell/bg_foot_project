package com.footballstats.backend.service;

import com.footballstats.backend.domain.MatchEvent;
import com.footballstats.backend.domain.MatchEventType;
import com.footballstats.backend.domain.MatchProtocol;
import com.footballstats.backend.domain.MatchProtocolStatus;
import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStandingsConfig;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.domain.Tour;
import com.footballstats.backend.domain.TourMatch;
import com.footballstats.backend.repository.CupTieMatchRepository;
import com.footballstats.backend.repository.MatchEventRepository;
import com.footballstats.backend.repository.SeasonStandingsConfigRepository;
import com.footballstats.backend.repository.TourMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisciplineNotificationServiceTest {

    @Mock private TourMatchRepository matchRepository;
    @Mock private CupTieMatchRepository cupMatchRepository;
    @Mock private MatchEventRepository eventRepository;
    @Mock private SeasonStandingsConfigRepository standingsConfigRepository;
    @Mock private SiteNotificationService notificationService;

    private DisciplineNotificationService service;

    @BeforeEach
    void setUp() {
        service = new DisciplineNotificationService(
            matchRepository, cupMatchRepository, eventRepository, standingsConfigRepository, notificationService
        );
    }

    @Test
    void secondYellowAtConfiguredThresholdNotifiesTeamRepresentatives() {
        Fixture fixture = fixture();
        when(standingsConfigRepository.findBySeason_Id(1L)).thenReturn(Optional.of(config(2, 1, 1)));
        when(matchRepository.findAllActiveDetailedBySeasonId(1L)).thenReturn(List.of(fixture.previousMatch, fixture.currentMatch));
        when(eventRepository.findAllDetailedBySeasonId(1L)).thenReturn(List.of(
            event(fixture.previousMatch, fixture.player, fixture.team, MatchEventType.YELLOW_CARD),
            event(fixture.currentMatch, fixture.player, fixture.team, MatchEventType.YELLOW_CARD)
        ));

        service.notifySuspensionsCausedBy(fixture.currentMatch, 99L);

        verify(notificationService).notifyPlayerSuspended(
            fixture.currentMatch, fixture.player, fixture.team, "YELLOW", 1, 99L
        );
    }

    @Test
    void redCardNotifiesEvenWithoutYellowThreshold() {
        Fixture fixture = fixture();
        when(standingsConfigRepository.findBySeason_Id(1L)).thenReturn(Optional.of(config(0, 1, 2)));
        when(matchRepository.findAllActiveDetailedBySeasonId(1L)).thenReturn(List.of(fixture.currentMatch));
        when(eventRepository.findAllDetailedBySeasonId(1L)).thenReturn(List.of(
            event(fixture.currentMatch, fixture.player, fixture.team, MatchEventType.RED_CARD)
        ));

        service.notifySuspensionsCausedBy(fixture.currentMatch, 99L);

        verify(notificationService).notifyPlayerSuspended(
            fixture.currentMatch, fixture.player, fixture.team, "RED", 2, 99L
        );
    }

    @Test
    void ordinaryYellowBeforeThresholdDoesNotNotify() {
        Fixture fixture = fixture();
        when(standingsConfigRepository.findBySeason_Id(1L)).thenReturn(Optional.of(config(3, 1, 1)));
        when(matchRepository.findAllActiveDetailedBySeasonId(1L)).thenReturn(List.of(fixture.currentMatch));
        when(eventRepository.findAllDetailedBySeasonId(1L)).thenReturn(List.of(
            event(fixture.currentMatch, fixture.player, fixture.team, MatchEventType.YELLOW_CARD)
        ));

        service.notifySuspensionsCausedBy(fixture.currentMatch, 99L);

        verify(notificationService, never()).notifyPlayerSuspended(
            fixture.currentMatch, fixture.player, fixture.team, "YELLOW", 1, 99L
        );
    }

    private Fixture fixture() {
        Season season = new Season();
        ReflectionTestUtils.setField(season, "id", 1L);
        season.setName("Сезон 2026");

        Tour tour = new Tour();
        ReflectionTestUtils.setField(tour, "id", 2L);
        tour.setSeason(season);
        tour.setName("5 тур");

        Team team = team(10L, "Атлетик Богородск");
        Team opponent = team(11L, "Волна Дуденево");
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", 20L);
        player.setFullName("Александр Белов");

        TourMatch previous = match(30L, tour, team, opponent, MatchProtocolStatus.VERIFIED);
        TourMatch current = match(31L, tour, team, opponent, MatchProtocolStatus.VERIFIED);
        return new Fixture(previous, current, player, team);
    }

    private SeasonStandingsConfig config(int yellowThreshold, int yellowMatches, int redMatches) {
        SeasonStandingsConfig config = new SeasonStandingsConfig();
        config.setYellowCardsForSuspension(yellowThreshold);
        config.setYellowSuspensionMatches(yellowMatches);
        config.setRedCardsForSuspension(redMatches);
        return config;
    }

    private Team team(Long id, String name) {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", id);
        team.setName(name);
        return team;
    }

    private TourMatch match(Long id, Tour tour, Team home, Team away, MatchProtocolStatus status) {
        TourMatch match = new TourMatch();
        ReflectionTestUtils.setField(match, "id", id);
        match.setTour(tour);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        MatchProtocol protocol = new MatchProtocol();
        protocol.setMatch(match);
        protocol.setStatus(status);
        ReflectionTestUtils.setField(match, "protocol", protocol);
        return match;
    }

    private MatchEvent event(TourMatch match, Player player, Team team, MatchEventType type) {
        MatchEvent event = new MatchEvent();
        event.setMatch(match);
        event.setPlayer(player);
        event.setTeam(team);
        event.setEventType(type);
        return event;
    }

    private record Fixture(TourMatch previousMatch, TourMatch currentMatch, Player player, Team team) {}
}
