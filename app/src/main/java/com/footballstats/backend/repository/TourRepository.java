package com.footballstats.backend.repository;

import com.footballstats.backend.domain.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TourRepository extends JpaRepository<Tour, Long> {

    @Query("""
        SELECT t
        FROM Tour t
        JOIN FETCH t.season season
        WHERE t.id = :tourId
        """)
    Optional<Tour> findDetailedById(@Param("tourId") Long tourId);

    @Query("""
        SELECT t
        FROM Tour t
        JOIN FETCH t.season season
        LEFT JOIN t.competition competition
        WHERE season.id = :seasonId
          AND (competition IS NULL OR competition.type = com.footballstats.backend.domain.CompetitionType.CHAMPIONSHIP)
          AND t.active = TRUE
        ORDER BY t.sortOrder ASC, t.id ASC
        """)
    List<Tour> findAllActiveDetailedBySeasonId(@Param("seasonId") Long seasonId);

        @Query("""
                SELECT t
                FROM Tour t
                JOIN FETCH t.season season
                LEFT JOIN t.competition competition
                WHERE season.id = :seasonId
                    AND (competition IS NULL OR competition.type = com.footballstats.backend.domain.CompetitionType.CHAMPIONSHIP)
                    AND t.active = TRUE
                    AND t.published = TRUE
                ORDER BY t.sortOrder ASC, t.id ASC
                """)
        List<Tour> findAllPublishedDetailedBySeasonId(@Param("seasonId") Long seasonId);

    @Query("""
        SELECT t
        FROM Tour t
        JOIN FETCH t.season season
        LEFT JOIN t.competition competition
        WHERE season.id = :seasonId
          AND (competition IS NULL OR competition.type = com.footballstats.backend.domain.CompetitionType.CHAMPIONSHIP)
        ORDER BY t.sortOrder ASC, t.id ASC
        """)
    List<Tour> findAllDetailedBySeasonId(@Param("seasonId") Long seasonId);

    List<Tour> findAllBySeason_IdAndStageTypeOrderBySortOrderAscIdAsc(Long seasonId, String stageType);

    List<Tour> findAllBySeason_IdAndPublishedTrueAndActiveTrueOrderBySortOrderAscIdAsc(Long seasonId);

    Optional<Tour> findBySeason_IdAndNameIgnoreCase(Long seasonId, String name);

    List<Tour> findAllByCompetition_IdOrderBySortOrderAscIdAsc(Long competitionId);
    Optional<Tour> findByCompetition_IdAndNameIgnoreCase(Long competitionId, String name);
}
