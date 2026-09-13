package com.footballstats.backend.service;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class AdminDashboardServiceTest {
    private JdbcTemplate database(String roster) {
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForMap(anyString(),eq(2L),eq(1L))).thenReturn(Map.of("roster_mode",roster,"players_on_field",5));
        when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class))).thenReturn(0L);
        return jdbc;
    }
    @Test void refereeTasksAreScopedAndDoNotIncludeLeagueNotifications() {
        var jdbc=database("SEASON_SHARED");
        var snapshots=mock(DisciplineSnapshotService.class);
        var tasks=new AdminDashboardService(jdbc,snapshots).getTasks(false,1L,2L);
        assertThat(tasks).extracting(AdminDashboardService.DashboardTask::key).containsExactly("applications","transfers","protocols","lineups","unpublished","suspensions");
        assertThat(tasks).allMatch(t -> t.scope().equals("COMPETITION") || t.scope().equals("SEASON"));
        verify(snapshots).players(2L);
        verify(snapshots,never()).players(null);
        verify(jdbc,times(3)).queryForObject(contains("tour.season_id=? AND tour.competition_id=?"),eq(Long.class),eq(1L),eq(2L));
    }
    @Test void ownTournamentRosterUsesCompetitionPlayersAndOwnFormat() {
        var jdbc=database("OWN");
        var tasks=new AdminDashboardService(jdbc,mock(DisciplineSnapshotService.class)).getTasks(true,1L,2L);
        assertThat(tasks).hasSize(8);
        verify(jdbc).queryForObject(contains("w_competition_roster_player"),eq(Long.class),eq(2L),eq(2L),eq(5));
        assertThat(tasks.stream().filter(t -> t.key().equals("rosters")).findFirst().orElseThrow().adminTab()).isEqualTo("competitions");
        assertThat(tasks.stream().filter(t -> t.key().equals("acknowledgements")).findFirst().orElseThrow().scope()).isEqualTo("LEAGUE");
    }
    @Test void missingOrMismatchedContextDoesNotReturnGlobalCounts() {
        var jdbc=mock(JdbcTemplate.class);
        var service=new AdminDashboardService(jdbc,mock(DisciplineSnapshotService.class));
        assertThatThrownBy(() -> service.getTasks(true,null,null)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("400");
        when(jdbc.queryForMap(anyString(),eq(2L),eq(99L))).thenThrow(new EmptyResultDataAccessException(1));
        assertThatThrownBy(() -> service.getTasks(true,99L,2L)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
    }
    @Test void onlyRemainingBansInSelectedTournamentAreCounted() {
        var jdbc=database("SEASON_SHARED");var snapshots=mock(DisciplineSnapshotService.class);
        when(snapshots.players(2L)).thenReturn(List.of(Map.of("competition_id",2L,"remaining_matches",2),Map.of("competition_id",2L,"remaining_matches",0)));
        var task=new AdminDashboardService(jdbc,snapshots).getTasks(true,1L,2L).stream().filter(t -> t.key().equals("suspensions")).findFirst().orElseThrow();
        assertThat(task.count()).isEqualTo(1);
        assertThat(task.path()).isEqualTo("/discipline?season=1&competition=2");
    }
}
