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
        LEFT JOIN FETCH me.team team
        LEFT JOIN FETCH me.player player
        LEFT JOIN FETCH me.relatedPlayer relatedPlayer
        WHERE match.id = :matchId
        ORDER BY me.sortOrder ASC, me.minute ASC, me.id ASC
        """)
    List<MatchEvent> findAllDetailedByMatchId(@Param("matchId") Long matchId);

    void deleteByMatch_Id(Long matchId);
}
