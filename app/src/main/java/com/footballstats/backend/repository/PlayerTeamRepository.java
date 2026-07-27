package com.footballstats.backend.repository;

import com.footballstats.backend.domain.PlayerTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface PlayerTeamRepository extends JpaRepository<PlayerTeam, Long> {

    /** Текущий состав команды (активные привязки без даты окончания). */
    @Query("SELECT pt FROM PlayerTeam pt JOIN FETCH pt.player WHERE pt.team.id = :teamId AND pt.active = TRUE ORDER BY pt.player.fullName")
    List<PlayerTeam> findCurrentRosterByTeamId(@Param("teamId") Long teamId);

    @Query("""
        SELECT pt
        FROM PlayerTeam pt
        JOIN FETCH pt.player player
        WHERE pt.team.id = :teamId
          AND pt.active = TRUE
          AND player.active = TRUE
          AND EXISTS (
                SELECT 1
                FROM SeasonPlayer sp
                WHERE sp.player.id = player.id
                  AND sp.team.id = :teamId
                  AND sp.season.id = :seasonId
                  AND sp.active = TRUE
          )
        ORDER BY player.fullName
        """)
    List<PlayerTeam> findCurrentRosterByTeamIdAndSeasonId(@Param("teamId") Long teamId, @Param("seasonId") Long seasonId);

    /** Полная история привязок игрока. */
    @Query("SELECT pt FROM PlayerTeam pt JOIN FETCH pt.team WHERE pt.player.id = :playerId ORDER BY pt.validFrom DESC")
    List<PlayerTeam> findHistoryByPlayerId(@Param("playerId") Long playerId);

    /** Текущая активная привязка игрока к команде (если есть). */
    Optional<PlayerTeam> findByPlayer_IdAndTeam_IdAndActiveTrue(Long playerId, Long teamId);

    /** Все активные привязки игрока (может быть в нескольких командах одновременно не предусмотрено, но безопасно). */
    List<PlayerTeam> findByPlayer_IdAndActiveTrue(Long playerId);

    @Query("""
        SELECT pt
        FROM PlayerTeam pt
        JOIN FETCH pt.player
        JOIN FETCH pt.team
        WHERE pt.player.id IN :playerIds AND pt.active = TRUE
        ORDER BY pt.id
        """)
    List<PlayerTeam> findActiveByPlayerIds(@Param("playerIds") Collection<Long> playerIds);
}
