package com.footballstats.backend.service;

import com.footballstats.backend.domain.*;
import com.footballstats.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionHonorsServiceTest {
    @Mock CompetitionRepository competitions;
    @Mock CompetitionTeamRepository competitionTeams;
    @Mock CompetitionRosterPlayerRepository competitionRoster;
    @Mock SeasonPlayerRepository seasonPlayers;
    @Mock PlayerRepository players;
    @Mock TeamRepository teams;
    @Mock CompetitionAwardRepository awards;
    @Mock CompetitionSelectionSlotRepository slots;
    @Mock CompetitionStatsService stats;
    @Mock MediaAssetService media;

    @Test
    void savesEveryTopScorerWhenGoalsAreTied() {
        Season season = mock(Season.class);
        when(season.getId()).thenReturn(1L);
        when(season.getName()).thenReturn("Сезон 2026");
        Competition competition = mock(Competition.class);
        when(competition.getId()).thenReturn(7L);
        when(competition.getSeason()).thenReturn(season);
        when(competition.getType()).thenReturn(CompetitionType.CHAMPIONSHIP);
        when(competition.getRosterMode()).thenReturn(CompetitionRosterMode.OWN);
        when(competition.getPlayersOnField()).thenReturn(5);
        when(competition.isActive()).thenReturn(true);
        when(competition.getName()).thenReturn("Высшая лига");
        when(competitions.findDetailedById(7L)).thenReturn(Optional.of(competition));

        Player first = player(11L, "Иван Первый");
        Player second = player(12L, "Пётр Второй");
        Team team = team(21L, "Атлетик");
        when(teams.findById(21L)).thenReturn(Optional.of(team));
        when(players.findById(11L)).thenReturn(Optional.of(first));
        when(players.findById(12L)).thenReturn(Optional.of(second));
        CompetitionRosterPlayer firstRoster = roster(first, team);
        CompetitionRosterPlayer secondRoster = roster(second, team);
        when(competitionRoster.findAllActiveDetailedByCompetitionId(7L)).thenReturn(List.of(firstRoster, secondRoster));
        when(competitionTeams.findAllDetailedByCompetitionId(7L)).thenReturn(List.of());
        when(awards.findAllByCompetition_IdOrderBySortOrderAscIdAsc(7L)).thenReturn(List.of());
        when(slots.findAllByCompetition_IdOrderBySortOrderAscIdAsc(7L)).thenReturn(List.of());
        when(stats.playerStats(7L)).thenReturn(List.of(
            new CompetitionStatsService.PlayerStats(11L, "Иван Первый", "Атлетик", 8, 0, 0),
            new CompetitionStatsService.PlayerStats(12L, "Пётр Второй", "Атлетик", 8, 1, 0)
        ));

        service().save(1L, 7L, new CompetitionHonorsService.HonorsUpdate(true, "1-2-1", List.of(), List.of()));

        ArgumentCaptor<CompetitionAward> captor = ArgumentCaptor.forClass(CompetitionAward.class);
        verify(awards, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(award -> {
            assertThat(award.getAwardCode()).isEqualTo("TOP_SCORER");
            assertThat(award.getStatValue()).isEqualTo(8);
        });
        verify(competition).setHonorsPublished(true);
        verify(competition).setHonorsFormation("1-2-1");
    }

    private CompetitionHonorsService service() {
        return new CompetitionHonorsService(competitions, competitionTeams, competitionRoster, seasonPlayers, players, teams, awards, slots, stats, media);
    }

    private Player player(Long id, String name) {
        Player player = mock(Player.class);
        when(player.getId()).thenReturn(id);
        when(player.getFullName()).thenReturn(name);
        return player;
    }

    private Team team(Long id, String name) {
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(id);
        when(team.getName()).thenReturn(name);
        return team;
    }

    private CompetitionRosterPlayer roster(Player player, Team team) {
        CompetitionRosterPlayer item = mock(CompetitionRosterPlayer.class);
        when(item.getPlayer()).thenReturn(player);
        when(item.getTeam()).thenReturn(team);
        return item;
    }
}
