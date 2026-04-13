package com.footballstats.backend.repository;

import com.footballstats.backend.domain.MatchLineupPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchLineupPlayerRepository extends JpaRepository<MatchLineupPlayer, Long> {

    @Query("""
        SELECT mlp
        FROM MatchLineupPlayer mlp
        JOIN FETCH mlp.lineup lineup
        JOIN FETCH lineup.team team
        JOIN FETCH mlp.player player
        WHERE lineup.match.id = :matchId
        ORDER BY team.id ASC, mlp.sortOrder ASC, mlp.id ASC
        """)
    List<MatchLineupPlayer> findAllDetailedByMatchId(@Param("matchId") Long matchId);

    void deleteByLineup_Id(Long lineupId);
}