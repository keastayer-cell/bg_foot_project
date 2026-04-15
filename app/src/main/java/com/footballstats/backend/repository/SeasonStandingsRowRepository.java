package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonStandingsRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeasonStandingsRowRepository extends JpaRepository<SeasonStandingsRow, Long> {

    @Query("""
        SELECT row
        FROM SeasonStandingsRow row
        JOIN FETCH row.team team
        WHERE row.season.id = :seasonId
        ORDER BY row.position ASC, team.name ASC
        """)
    List<SeasonStandingsRow> findAllDetailedBySeasonId(@Param("seasonId") Long seasonId);

    void deleteAllBySeason_Id(Long seasonId);
}