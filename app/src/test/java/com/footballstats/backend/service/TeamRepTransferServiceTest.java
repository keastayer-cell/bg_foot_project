package com.footballstats.backend.service;

import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonStatus;
import com.footballstats.backend.domain.SeasonTransferRequest;
import com.footballstats.backend.domain.SeasonTransferStatus;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.repository.AppUserRepository;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.SeasonRepository;
import com.footballstats.backend.repository.SeasonTeamRepository;
import com.footballstats.backend.repository.SeasonTransferRequestRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.repository.UserTeamScopeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamRepTransferServiceTest {

    @Mock private UserTeamScopeRepository userTeamScopeRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private SeasonRepository seasonRepository;
    @Mock private SeasonTeamRepository seasonTeamRepository;
    @Mock private SeasonPlayerService seasonPlayerService;
    @Mock private SeasonTransferRequestRepository seasonTransferRequestRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private MediaAssetService mediaAssetService;

    private TeamRepTransferService service;

    @BeforeEach
    void setUp() {
        service = new TeamRepTransferService(
            userTeamScopeRepository,
            teamRepository,
            playerRepository,
            seasonRepository,
            seasonTeamRepository,
            seasonPlayerService,
            seasonTransferRequestRepository,
            appUserRepository,
            mediaAssetService
        );
    }

    @Test
    void approvingPendingTransferMovesSeasonPlayerAndRecordsDecision() {
        Season season = new Season();
        ReflectionTestUtils.setField(season, "id", 1L);
        season.setName("Season");
        season.setStatus(SeasonStatus.ACTIVE);
        season.setTransferWindowStartDate(LocalDate.now().minusDays(1));
        season.setTransferWindowEndDate(LocalDate.now().plusDays(1));
        Team from = team(2L, "Alpha");
        Team to = team(3L, "Beta");
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", 4L);
        player.setFullName("Player");
        SeasonTransferRequest request = new SeasonTransferRequest();
        request.setSeason(season);
        request.setPlayer(player);
        request.setFromTeam(from);
        request.setToTeam(to);
        request.setStatus(SeasonTransferStatus.PENDING);

        when(seasonTransferRequestRepository.findDetailedByIdForUpdate(5L)).thenReturn(Optional.of(request));
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(seasonTeamRepository.findAllBySeasonIdOrderByTeamNameAsc(1L)).thenReturn(List.of());
        when(seasonTransferRequestRepository.findPageDetailedBySeasonId(any(Long.class), any()))
            .thenReturn(new PageImpl<>(List.of()));

        TeamRepTransferService.TeamRepTransferOverviewData result = service.approveTransferRequest(
            new TeamRepTransferService.TransferActor(99L, false, true, false),
            5L,
            "approved"
        );

        assertThat(request.getStatus()).isEqualTo(SeasonTransferStatus.APPROVED);
        assertThat(request.getDecisionComment()).isEqualTo("approved");
        assertThat(result.privilegedAccess()).isTrue();
        verify(seasonPlayerService).transferSeasonPlayer(1L, 4L, 2L, 3L, 99L);
        verify(seasonTransferRequestRepository).save(request);
    }

    private Team team(Long id, String name) {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", id);
        team.setName(name);
        team.setActive(true);
        return team;
    }
}
