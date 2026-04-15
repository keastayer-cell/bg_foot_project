package com.footballstats.backend.repository;

import com.footballstats.backend.domain.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Page<Player> findByActiveTrueOrderByFullName(Pageable pageable);

    @Query("""
            SELECT p
            FROM Player p
            WHERE p.active = TRUE
                AND lower(p.fullName) LIKE lower(concat('% ', :prefix, '%'))
            ORDER BY p.fullName
            """)
    Page<Player> findActiveByNamePrefix(@Param("prefix") String prefix, Pageable pageable);

    @Query("""
            SELECT p
            FROM Player p
            WHERE (:activeFlag = 0 OR p.active = TRUE)
              AND (:namePattern IS NULL OR lower(p.fullName) LIKE :namePattern)
              AND (:teamId IS NULL OR EXISTS (
                    SELECT 1
                    FROM PlayerTeam pt
                    WHERE pt.player.id = p.id
                      AND pt.team.id = :teamId
                      AND pt.active = TRUE
              ))
                          AND (:seasonId IS NULL OR EXISTS (
                          SELECT 1
                          FROM SeasonPlayer sp
                          WHERE sp.player.id = p.id
                            AND sp.season.id = :seasonId
                            AND sp.active = TRUE
                            AND (:teamId IS NULL OR sp.team.id = :teamId)
                          ))
              AND (:goals IS NULL OR p.goals = :goals)
              AND (:yellowCards IS NULL OR p.yellowCards = :yellowCards)
              AND (:redCards IS NULL OR p.redCards = :redCards)
            ORDER BY p.fullName
            """)
    Page<Player> searchPlayers(
        @Param("activeFlag") int activeFlag,
      @Param("namePattern") String namePattern,
        @Param("teamId") Long teamId,
        @Param("seasonId") Long seasonId,
        @Param("goals") Integer goals,
        @Param("yellowCards") Integer yellowCards,
        @Param("redCards") Integer redCards,
        Pageable pageable
    );

    @Query("""
        SELECT p
        FROM Player p
        WHERE p.active = TRUE
          AND NOT EXISTS (
                SELECT 1
                FROM SeasonPlayer sp
                WHERE sp.player.id = p.id
                  AND sp.season.id = :seasonId
                  AND sp.active = TRUE
                  AND sp.team.id <> :teamId
          )
          AND NOT EXISTS (
                SELECT 1
                FROM PlayerTeam pt
                WHERE pt.player.id = p.id
                  AND pt.active = TRUE
          )
        ORDER BY p.fullName
        """)
    List<Player> findActiveAvailableForSeasonAndNotInTeam(@Param("seasonId") Long seasonId, @Param("teamId") Long teamId);

    boolean existsByFullNameIgnoreCase(String fullName);
}
