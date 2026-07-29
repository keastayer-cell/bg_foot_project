package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonPlayoffTieMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeasonPlayoffTieMatchRepository extends JpaRepository<SeasonPlayoffTieMatch, Long> {

    @Query("""
        SELECT tieMatch
        FROM SeasonPlayoffTieMatch tieMatch
        JOIN FETCH tieMatch.tie tie
        JOIN FETCH tieMatch.match match
        WHERE tie.bracket.season.id = :seasonId
        ORDER BY tie.roundOrder ASC, tie.slotOrder ASC, tieMatch.legNumber ASC
        """)
    List<SeasonPlayoffTieMatch> findAllDetailedBySeasonId(@Param("seasonId") Long seasonId);
}
