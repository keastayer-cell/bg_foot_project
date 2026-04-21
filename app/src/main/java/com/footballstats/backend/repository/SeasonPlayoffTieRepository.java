package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonPlayoffTie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeasonPlayoffTieRepository extends JpaRepository<SeasonPlayoffTie, Long> {

    @Query("""
        SELECT tie
        FROM SeasonPlayoffTie tie
        JOIN FETCH tie.bracket bracket
        LEFT JOIN FETCH tie.homeTeam homeTeam
        LEFT JOIN FETCH tie.awayTeam awayTeam
        LEFT JOIN FETCH tie.winnerTeam winnerTeam
        LEFT JOIN FETCH tie.homeSourceTie homeSourceTie
        LEFT JOIN FETCH tie.awaySourceTie awaySourceTie
        WHERE bracket.season.id = :seasonId
        ORDER BY tie.roundOrder ASC, tie.slotOrder ASC, tie.id ASC
        """)
    List<SeasonPlayoffTie> findAllDetailedBySeasonId(@Param("seasonId") Long seasonId);

    void deleteAllByBracket_Id(Long bracketId);

    boolean existsByBracket_Season_Id(Long seasonId);
}