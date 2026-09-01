package com.footballstats.backend.repository;

import com.footballstats.backend.domain.CupTie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CupTieRepository extends JpaRepository<CupTie, Long> {
    @Query("""
        SELECT tie FROM CupTie tie
        LEFT JOIN FETCH tie.homeTeam
        LEFT JOIN FETCH tie.awayTeam
        LEFT JOIN FETCH tie.winnerTeam
        LEFT JOIN FETCH tie.homeSourceTie
        LEFT JOIN FETCH tie.awaySourceTie
        WHERE tie.competition.id = :competitionId
        ORDER BY tie.roundOrder ASC, tie.slotOrder ASC
        """)
    List<CupTie> findAllDetailedByCompetitionId(@Param("competitionId") Long competitionId);
    void deleteAllByCompetition_Id(Long competitionId);
}
