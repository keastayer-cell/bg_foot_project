package com.footballstats.backend.repository;

import com.footballstats.backend.domain.MatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    @Query("""
        SELECT me
        FROM MatchEvent me
        JOIN FETCH me.match match
        LEFT JOIN FETCH match.protocol protocol
        LEFT JOIN FETCH me.team team
        LEFT JOIN FETCH me.player player
        LEFT JOIN FETCH me.relatedPlayer relatedPlayer
        WHERE match.id = :matchId
        ORDER BY me.sortOrder ASC, me.minute ASC, me.id ASC
        """)
    List<MatchEvent> findAllDetailedByMatchId(@Param("matchId") Long matchId);

        @Query("""
                SELECT me
                FROM MatchEvent me
                JOIN FETCH me.match match
                JOIN FETCH match.tour tour
                LEFT JOIN tour.competition competition
                LEFT JOIN FETCH match.protocol protocol
                LEFT JOIN FETCH me.team team
                LEFT JOIN FETCH me.player player
                LEFT JOIN FETCH me.relatedPlayer relatedPlayer
                WHERE tour.season.id = :seasonId
                    AND (competition IS NULL OR competition.type = com.footballstats.backend.domain.CompetitionType.CHAMPIONSHIP)
                    AND tour.active = TRUE
                    AND match.active = TRUE
                ORDER BY tour.sortOrder ASC, match.kickoffAt ASC, match.id ASC, me.sortOrder ASC, me.minute ASC, me.id ASC
                """)
        List<MatchEvent> findAllDetailedBySeasonId(@Param("seasonId") Long seasonId);

    @Query("""
        SELECT me FROM MatchEvent me
        JOIN FETCH me.match match
        JOIN FETCH match.tour tour
        LEFT JOIN FETCH match.protocol protocol
        LEFT JOIN FETCH me.team team
        LEFT JOIN FETCH me.player player
        LEFT JOIN FETCH me.relatedPlayer relatedPlayer
        WHERE tour.competition.id = :competitionId
          AND tour.active = TRUE
          AND match.active = TRUE
        ORDER BY tour.sortOrder ASC, match.kickoffAt ASC, match.id ASC, me.sortOrder ASC, me.id ASC
        """)
    List<MatchEvent> findAllDetailedByCompetitionId(@Param("competitionId") Long competitionId);

    void deleteByMatch_Id(Long matchId);
}
