package com.footballstats.backend.repository;

import com.footballstats.backend.domain.CupTieMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CupTieMatchRepository extends JpaRepository<CupTieMatch, Long> {
    @Query("""
        SELECT ctm FROM CupTieMatch ctm
        JOIN FETCH ctm.match match
        JOIN FETCH match.homeTeam
        JOIN FETCH match.awayTeam
        LEFT JOIN FETCH match.protocol
        WHERE ctm.tie.id = :tieId
        ORDER BY ctm.legNumber ASC
        """)
    List<CupTieMatch> findAllDetailedByTieId(@Param("tieId") Long tieId);

    @Query("""
        SELECT ctm FROM CupTieMatch ctm
        JOIN FETCH ctm.tie tie
        JOIN FETCH tie.competition
        WHERE ctm.match.id = :matchId
        """)
    Optional<CupTieMatch> findDetailedByMatchId(@Param("matchId") Long matchId);

    @Query("""
        SELECT ctm FROM CupTieMatch ctm
        JOIN FETCH ctm.tie tie
        JOIN FETCH ctm.match match
        JOIN FETCH match.tour tour
        JOIN FETCH match.homeTeam
        JOIN FETCH match.awayTeam
        LEFT JOIN FETCH match.protocol
        WHERE tie.competition.id = :competitionId
          AND match.active = TRUE
        ORDER BY match.kickoffAt ASC, match.id ASC
        """)
    List<CupTieMatch> findAllDetailedByCompetitionId(@Param("competitionId") Long competitionId);
}
