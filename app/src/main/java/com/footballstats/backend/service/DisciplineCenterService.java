package com.footballstats.backend.service;

import com.footballstats.backend.domain.Competition;
import com.footballstats.backend.domain.Player;
import com.footballstats.backend.domain.Team;
import com.footballstats.backend.repository.PlayerRepository;
import com.footballstats.backend.repository.TeamRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DisciplineCenterService {
    private final JdbcTemplate jdbcTemplate;
    private final DisciplineSnapshotService snapshots;
    private final CompetitionService competitionService;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final SiteNotificationService notificationService;

    public DisciplineCenterService(JdbcTemplate jdbcTemplate, CompetitionService competitionService,
        PlayerRepository playerRepository, TeamRepository teamRepository, SiteNotificationService notificationService, DisciplineSnapshotService snapshots) {
        this.jdbcTemplate = jdbcTemplate;
        this.snapshots = snapshots;
        this.competitionService = competitionService;
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCenter(Long competitionId) {
        Competition competition = competitionService.getCompetition(competitionId);
        List<Map<String, Object>> players = snapshots.players(competitionId);
        List<Map<String, Object>> history = jdbcTemplate.queryForList("""
            SELECT adjustment.id, adjustment.player_id, player.full_name AS player_name,
                   adjustment.team_id, team.name AS team_name, adjustment.old_remaining_matches,
                   adjustment.new_remaining_matches, adjustment.adjustment_type, adjustment.reason,
                   adjustment.created_at, app_user.name AS author_name
            FROM work.w_discipline_adjustment adjustment
            JOIN work.w_player player ON player.id = adjustment.player_id
            JOIN work.w_team team ON team.id = adjustment.team_id
            JOIN work.w_user_login app_user ON app_user.id = adjustment.created_by_user_id
            WHERE adjustment.competition_id = ? ORDER BY adjustment.created_at DESC, adjustment.id DESC LIMIT 50
            """, competitionId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("competition", Map.of("id", competition.getId(), "name", competition.getName(),
            "seasonId", competition.getSeason().getId(), "seasonName", competition.getSeason().getName(),
            "yellowThreshold", jdbcTemplate.queryForObject("""
                SELECT CASE WHEN c.competition_type='CHAMPIONSHIP' THEN cfg.yellow_cards_for_suspension ELSE c.yellow_cards_for_suspension END
                FROM work.w_competition c LEFT JOIN work.w_season_standings_config cfg ON cfg.season_id=c.season_id WHERE c.id=?
                """, Integer.class, competitionId)));
        result.put("players", players); result.put("adjustments", history);
        return result;
    }

    @Transactional
    @com.footballstats.backend.audit.AuditedAction(entity="DISCIPLINE",idParam="competitionId",action="DISCIPLINE_ADJUSTED",actorParam="actorUserId")
    public Map<String, Object> adjust(Long competitionId, Long playerId, Long teamId, int oldRemaining, int newRemaining,
        String reason, Long actorUserId) {
        if (actorUserId == null) throw new IllegalArgumentException("Не определён автор корректировки.");
        if (oldRemaining < 0 || newRemaining < 0) throw new IllegalArgumentException("Число матчей не может быть отрицательным.");
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.length() < 5) throw new IllegalArgumentException("Укажите понятную причину корректировки.");
        Competition competition = competitionService.getCompetition(competitionId);
        Player player = playerRepository.findById(playerId).orElseThrow(() -> new IllegalArgumentException("Игрок не найден."));
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("Команда не найдена."));
        String type = newRemaining == 0 ? "CANCELLED" : newRemaining > oldRemaining ? "ADDED" : newRemaining < oldRemaining ? "REDUCED" : "CORRECTED";
        Map<String, Object> inserted = jdbcTemplate.queryForMap("""
            INSERT INTO work.w_discipline_adjustment
              (competition_id, player_id, team_id, old_remaining_matches, new_remaining_matches, adjustment_type, reason, created_by_user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, old_remaining_matches, new_remaining_matches, adjustment_type, reason, created_at
            """, competitionId, playerId, teamId, oldRemaining, newRemaining, type, normalizedReason, actorUserId);
        notificationService.notifyDisciplineAdjusted(competition, player, team, newRemaining, normalizedReason, actorUserId);
        return inserted;
    }

    @Transactional(readOnly = true)
    public Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> suspendedForMatch(Long competitionId, Long matchId, Long homeTeam, Long awayTeam) {
        Map<Long, SeasonDisciplineService.PlayerMatchDiscipline> result = new LinkedHashMap<>();
        for (var row : snapshots.players(competitionId, matchId)) {
            long team = DisciplineSnapshotService.number(row,"team_id");
            int remaining = (int)DisciplineSnapshotService.number(row,"remaining_matches");
            if ((team == homeTeam || team == awayTeam) && remaining > 0) {
                long player = DisciplineSnapshotService.number(row,"player_id");
                result.put(player,new SeasonDisciplineService.PlayerMatchDiscipline(player,remaining,
                    "Дисквалификация в турнире: осталось матчей — " + remaining + "."));
            }
        }
        return result;
    }

}
