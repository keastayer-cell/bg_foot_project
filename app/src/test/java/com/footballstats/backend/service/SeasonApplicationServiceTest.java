package com.footballstats.backend.service;

import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonApplication;
import com.footballstats.backend.domain.SeasonApplicationPlayer;
import com.footballstats.backend.domain.SeasonApplicationStatus;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.PlayerTeamRepository;
import com.footballstats.backend.repository.SeasonApplicationPlayerRepository;
import com.footballstats.backend.repository.SeasonApplicationRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.repository.UserRoleRepository;
import com.footballstats.backend.repository.UserTeamScopeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonApplicationServiceTest {

    @Mock private UserTeamScopeRepository userTeamScopeRepository;
    @Mock private SeasonPlayerService seasonPlayerService;
    @Mock private PlayerTeamRepository playerTeamRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private SeasonApplicationRepository seasonApplicationRepository;
    @Mock private SeasonApplicationPlayerRepository seasonApplicationPlayerRepository;
    @Mock private MediaAssetService mediaAssetService;
    @Mock private NotificationEventService notificationEventService;

    private SeasonApplicationService service;

    @BeforeEach
    void setUp() {
        service = new SeasonApplicationService(
            userTeamScopeRepository,
            seasonPlayerService,
            playerTeamRepository,
            playerRepository,
            appUserRepository,
            userRoleRepository,
            teamRepository,
            seasonApplicationRepository,
            seasonApplicationPlayerRepository,
            mediaAssetService,
            notificationEventService
        );
    }

    @Test
    void approvingInitialApplicationReplacesSeasonRoster() {
        Season season = season(1L);
        Team team = team(2L, "Alpha");
        Player first = player(10L, "First");
        Player second = player(11L, "Second");
        SeasonApplication application = application(20L, season, team, SeasonApplicationStatus.SUBMITTED);
        List<SeasonApplicationPlayer> rows = List.of(row(application, first), row(application, second));
        when(seasonApplicationRepository.findDetailedById(20L)).thenReturn(Optional.of(application));
        when(seasonApplicationPlayerRepository.findAllDetailedByApplicationId(20L)).thenReturn(rows);

        SeasonApplicationService.ReviewDetailsData result = service.approve(99L, 20L, "ok");

        assertThat(result.status()).isEqualTo(SeasonApplicationStatus.APPROVED);
        verify(seasonPlayerService).replaceSeasonPlayers(2L, 1L, List.of(10L, 11L), 99L);
        verify(seasonApplicationRepository).save(application);
    }

    @Test
    void addingPlayerToApprovedApplicationImmediatelySyncsSupplementalRoster() {
        Season season = season(3L);
        Team team = team(4L, "Beta");
        Player player = player(12L, "New Player");
        SeasonApplication application = application(21L, season, team, SeasonApplicationStatus.APPROVED);
        List<SeasonApplicationPlayer> rows = new ArrayList<>();
        PlayerTeam membership = new PlayerTeam();
        membership.setPlayer(player);
        membership.setTeam(team);
        membership.setActive(true);

        when(teamRepository.findById(4L)).thenReturn(Optional.of(team));
        when(seasonPlayerService.getSeasonForTeam(4L, 3L)).thenReturn(season);
        when(seasonApplicationRepository.findDetailedBySeasonIdAndTeamId(3L, 4L)).thenReturn(Optional.of(application));
        when(seasonApplicationPlayerRepository.findAllDetailedByApplicationId(21L)).thenAnswer(ignored -> List.copyOf(rows));
        when(playerTeamRepository.findByPlayer_IdAndTeam_IdAndActiveTrue(12L, 4L)).thenReturn(Optional.of(membership));
        when(playerTeamRepository.findCurrentRosterByTeamId(4L)).thenReturn(List.of(membership));
        when(playerRepository.findById(12L)).thenReturn(Optional.of(player));
        when(seasonPlayerService.listActiveSeasonPlayers(4L, 3L)).thenReturn(List.of());
        when(seasonPlayerService.listAvailablePlayersForSeason(4L, 3L)).thenReturn(List.of());
        when(seasonApplicationPlayerRepository.save(any(SeasonApplicationPlayer.class))).thenAnswer(invocation -> {
            SeasonApplicationPlayer saved = invocation.getArgument(0);
            rows.add(saved);
            return saved;
        });

        TeamRepService.TeamRepSeasonPlayersData result =
            service.addPlayers(99L, 3L, List.of(12L), 4L, true);

        assertThat(result.applicationStatus()).isEqualTo(SeasonApplicationStatus.APPROVED);
        assertThat(result.players()).hasSize(1);
        verify(seasonPlayerService).replaceSeasonPlayers(4L, 3L, List.of(12L), 99L);
    }

    private Season season(Long id) {
        Season season = new Season();
        ReflectionTestUtils.setField(season, "id", id);
        season.setName("Season");
        season.setStatus(SeasonStatus.ACTIVE);
        season.setApplicationDeadline(LocalDate.now().plusDays(5));
        season.setMaxRosterSize(25);
        return season;
    }

    private Team team(Long id, String name) {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", id);
        team.setName(name);
        team.setActive(true);
        return team;
    }

    private Player player(Long id, String name) {
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", id);
        player.setFullName(name);
        return player;
    }

    private SeasonApplication application(
        Long id,
        Season season,
        Team team,
        SeasonApplicationStatus status
    ) {
        SeasonApplication application = new SeasonApplication();
        ReflectionTestUtils.setField(application, "id", id);
        application.setSeason(season);
        application.setTeam(team);
        application.setStatus(status);
        return application;
    }

    private SeasonApplicationPlayer row(SeasonApplication application, Player player) {
        SeasonApplicationPlayer row = new SeasonApplicationPlayer();
        row.setApplication(application);
        row.setPlayer(player);
        return row;
    }
}
