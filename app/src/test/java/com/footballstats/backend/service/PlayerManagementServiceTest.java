package com.footballstats.backend.service;

import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.PlayerTeam;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.PlayerTeamRepository;
import com.footballstats.backend.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerManagementServiceTest {

    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerTeamRepository playerTeamRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private MediaAssetService mediaAssetService;
    @Mock private SeasonPlayerService seasonPlayerService;

    private PlayerManagementService service;

    @BeforeEach
    void setUp() {
        service = new PlayerManagementService(
            playerRepository, playerTeamRepository, teamRepository, mediaAssetService, seasonPlayerService
        );
    }

    @Test
    void listPlayersLoadsTeamsAssignmentsAndPhotosInBatches() {
        Player first = player(1L, "First");
        Player second = player(2L, "Second");
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", 10L);
        team.setName("Team");
        PlayerTeam link = new PlayerTeam();
        link.setPlayer(first);
        link.setTeam(team);
        link.setActive(true);

        when(playerRepository.searchPlayers(
            anyInt(), nullable(String.class), nullable(Long.class), nullable(Long.class),
            nullable(Integer.class), nullable(Integer.class), nullable(Integer.class), any()
        ))
            .thenReturn(new PageImpl<>(List.of(first, second)));
        when(playerTeamRepository.findActiveByPlayerIds(List.of(1L, 2L))).thenReturn(List.of(link));
        when(seasonPlayerService.getLatestActiveSeasonAssignments(List.of(1L, 2L))).thenReturn(Map.of());
        when(mediaAssetService.loadDataUrls(
            MediaAssetService.OWNER_PLAYER, List.of(1L, 2L), MediaAssetService.KIND_PLAYER_PHOTO
        )).thenReturn(Map.of(1L, "data:image/png;base64,AA"));

        var result = service.listPlayers(new PlayerManagementService.PlayerSearch(
            1, null, null, null, null, null, null, 0, 20
        ));

        assertThat(result).hasSize(2);
        assertThat(result.getContent().getFirst().currentTeamName()).isEqualTo("Team");
        assertThat(result.getContent().getFirst().photoDataUrl()).startsWith("data:image/png");
        verify(playerTeamRepository).findActiveByPlayerIds(List.of(1L, 2L));
        verify(mediaAssetService, never()).loadDataUrl(any(), any(), any());
    }

    private Player player(Long id, String name) {
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", id);
        player.setFullName(name);
        player.setActive(true);
        return player;
    }
}
