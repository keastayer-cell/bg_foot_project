package com.footballstats.backend.repository;

import com.footballstats.backend.domain.MatchLineup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchLineupRepository extends JpaRepository<MatchLineup, Long> {

    Optional<MatchLineup> findByMatch_IdAndTeam_Id(Long matchId, Long teamId);

    boolean existsByMatch_IdAndTeam_Id(Long matchId, Long teamId);

    @Query("""
        SELECT ml
        FROM MatchLineup ml
        JOIN FETCH ml.match match
        JOIN FETCH ml.team team
        WHERE match.id = :matchId
        ORDER BY ml.id ASC
        """)
    List<MatchLineup> findAllDetailedByMatchId(@Param("matchId") Long matchId);
}