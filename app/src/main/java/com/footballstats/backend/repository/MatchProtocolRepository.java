package com.footballstats.backend.repository;

import com.footballstats.backend.domain.MatchProtocol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MatchProtocolRepository extends JpaRepository<MatchProtocol, Long> {

    Optional<MatchProtocol> findByMatch_Id(Long matchId);

    @Query("""
        SELECT mp
        FROM MatchProtocol mp
        JOIN FETCH mp.match match
        LEFT JOIN FETCH mp.bestPlayer bestPlayer
        WHERE match.id = :matchId
        """)
    Optional<MatchProtocol> findDetailedByMatchId(@Param("matchId") Long matchId);
}
