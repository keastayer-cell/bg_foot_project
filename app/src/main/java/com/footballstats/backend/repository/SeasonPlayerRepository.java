package com.footballstats.backend.repository;

import com.footballstats.backend.domain.Season;
import com.footballstats.backend.domain.SeasonPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeasonPlayerRepository extends JpaRepository<SeasonPlayer, Long> {

    @Query("""
        SELECT sp
        FROM SeasonPlayer sp
        JOIN FETCH sp.player player
        WHERE sp.team.id = :teamId
          AND sp.active = TRUE
          AND player.active = TRUE
        ORDER BY player.fullName ASC, sp.id ASC
        """)
    List<SeasonPlayer> findAllActiveDetailedByTeamId(@Param("teamId") Long teamId);

    @Query("""
        SELECT sp
        FROM SeasonPlayer sp
        JOIN FETCH sp.player player
        JOIN FETCH sp.season season
        WHERE sp.team.id = :teamId
          AND sp.player.id = :playerId
          AND sp.active = TRUE
          AND season.active = TRUE
        ORDER BY season.createdAt DESC, season.id DESC
        """)
    List<SeasonPlayer> findAllActiveDetailedByTeamIdAndPlayerId(@Param("teamId") Long teamId, @Param("playerId") Long playerId);

    @Query("""
        SELECT sp
        FROM SeasonPlayer sp
        JOIN FETCH sp.player player
        WHERE sp.season.id = :seasonId
          AND sp.team.id = :teamId
          AND sp.active = TRUE
          AND player.active = TRUE
        ORDER BY player.fullName ASC, sp.id ASC
        """)
    List<SeasonPlayer> findAllActiveDetailedBySeasonIdAndTeamId(@Param("seasonId") Long seasonId, @Param("teamId") Long teamId);

    @Query("""
        SELECT DISTINCT season
        FROM SeasonPlayer sp
        JOIN sp.season season
        WHERE sp.team.id = :teamId
          AND sp.active = TRUE
          AND season.active = TRUE
        ORDER BY season.createdAt DESC, season.id DESC
        """)
    List<Season> findDistinctActiveSeasonsByTeamId(@Param("teamId") Long teamId);

    List<SeasonPlayer> findBySeason_IdAndTeam_IdAndActiveTrue(Long seasonId, Long teamId);

    Optional<SeasonPlayer> findBySeason_IdAndTeam_IdAndPlayer_IdAndActiveTrue(Long seasonId, Long teamId, Long playerId);

    Optional<SeasonPlayer> findBySeason_IdAndPlayer_IdAndActiveTrue(Long seasonId, Long playerId);

    boolean existsBySeason_IdAndTeam_IdAndPlayer_IdAndActiveTrue(Long seasonId, Long teamId, Long playerId);

    boolean existsBySeason_IdAndPlayer_IdAndActiveTrue(Long seasonId, Long playerId);

    long countBySeason_IdAndTeam_IdAndActiveTrue(Long seasonId, Long teamId);
}