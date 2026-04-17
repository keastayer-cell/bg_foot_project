package com.footballstats.backend.repository;

import com.footballstats.backend.domain.MatchProtocolExportSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MatchProtocolExportSnapshotRepository extends JpaRepository<MatchProtocolExportSnapshot, Long> {

    Optional<MatchProtocolExportSnapshot> findByMatchId(Long matchId);

    void deleteByMatchId(Long matchId);

    void deleteAllBySeasonId(Long seasonId);

    @Query("""
        SELECT snapshot.matchId
        FROM MatchProtocolExportSnapshot snapshot
        WHERE snapshot.seasonId = :seasonId
        """)
    Set<Long> findMatchIdsBySeasonId(@Param("seasonId") Long seasonId);

    @Query("""
        SELECT snapshot
        FROM MatchProtocolExportSnapshot snapshot
        WHERE snapshot.seasonId = :seasonId
        ORDER BY snapshot.tourSortOrder ASC, snapshot.kickoffAt ASC, snapshot.matchId ASC
        """)
    List<MatchProtocolExportSnapshot> findAllBySeasonIdOrderByExportOrder(@Param("seasonId") Long seasonId);
}