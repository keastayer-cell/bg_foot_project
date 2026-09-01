package com.footballstats.backend.repository;

import com.footballstats.backend.domain.TourMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TourMatchRepository extends JpaRepository<TourMatch, Long> {

    boolean existsByTour_Season_IdAndTour_StageTypeAndActiveTrue(Long seasonId, String stageType);

    boolean existsByTour_IdAndActiveTrue(Long tourId);

    long countByTour_IdAndActiveTrue(Long tourId);

    @Query("""
      SELECT COUNT(tm)
      FROM TourMatch tm
      JOIN tm.tour tour
      WHERE tour.season.id = :seasonId
        AND tour.stageType = :stageType
        AND tm.active = TRUE
        AND ((tm.homeTeam.id = :firstTeamId AND tm.awayTeam.id = :secondTeamId)
        OR (tm.homeTeam.id = :secondTeamId AND tm.awayTeam.id = :firstTeamId))
      """)
    long countActiveHeadToHeadMatchesInSeasonStage(
      @Param("seasonId") Long seasonId,
      @Param("stageType") String stageType,
      @Param("firstTeamId") Long firstTeamId,
      @Param("secondTeamId") Long secondTeamId
    );

    Optional<TourMatch> findByIdAndTour_Id(Long matchId, Long tourId);

    @Query("""
      SELECT tm
      FROM TourMatch tm
      JOIN FETCH tm.tour tour
      JOIN FETCH tour.season season
      LEFT JOIN FETCH tour.competition competition
      JOIN FETCH tm.homeTeam homeTeam
      JOIN FETCH tm.awayTeam awayTeam
      LEFT JOIN FETCH tm.protocol protocol
      LEFT JOIN FETCH protocol.bestPlayer bestPlayer
      LEFT JOIN FETCH protocol.chiefReferee chiefReferee
      LEFT JOIN FETCH protocol.assistantRefereeOne assistantRefereeOne
      LEFT JOIN FETCH protocol.assistantRefereeTwo assistantRefereeTwo
      WHERE tm.id = :matchId
      """)
    Optional<TourMatch> findDetailedById(@Param("matchId") Long matchId);

    @Query("""
        SELECT tm
        FROM TourMatch tm
        JOIN FETCH tm.tour tour
        JOIN FETCH tour.season season
        LEFT JOIN tour.competition competition
        JOIN FETCH tm.homeTeam homeTeam
        JOIN FETCH tm.awayTeam awayTeam
        LEFT JOIN FETCH tm.protocol protocol
        WHERE tour.season.id = :seasonId
          AND (competition IS NULL OR competition.type = com.footballstats.backend.domain.CompetitionType.CHAMPIONSHIP)
          AND tour.active = TRUE
          AND tm.active = TRUE
        ORDER BY tour.sortOrder ASC, tm.kickoffAt ASC, tm.id ASC
        """)
    List<TourMatch> findAllActiveDetailedBySeasonId(@Param("seasonId") Long seasonId);

    @Query("""
        SELECT tm
        FROM TourMatch tm
        JOIN FETCH tm.tour tour
        JOIN FETCH tour.season season
        LEFT JOIN FETCH tour.competition competition
        JOIN FETCH tm.homeTeam homeTeam
        JOIN FETCH tm.awayTeam awayTeam
        LEFT JOIN FETCH tm.protocol protocol
        WHERE tour.season.id = :seasonId
          AND tour.active = TRUE
          AND tm.active = TRUE
        ORDER BY tour.sortOrder ASC, tm.kickoffAt ASC, tm.id ASC
        """)
    List<TourMatch> findAllActiveDetailedByAnyCompetitionSeasonId(@Param("seasonId") Long seasonId);

    @Query("""
        SELECT tm
        FROM TourMatch tm
        JOIN FETCH tm.tour tour
        JOIN FETCH tour.season season
        LEFT JOIN FETCH tour.competition competition
        JOIN FETCH tm.homeTeam homeTeam
        JOIN FETCH tm.awayTeam awayTeam
        LEFT JOIN FETCH tm.protocol protocol
        WHERE tour.season.id = :seasonId
        ORDER BY tour.sortOrder ASC, tm.kickoffAt ASC, tm.id ASC
        """)
    List<TourMatch> findAllDetailedByAnyCompetitionSeasonId(@Param("seasonId") Long seasonId);

    @Query("""
        SELECT tm
        FROM TourMatch tm
        JOIN FETCH tm.tour tour
        JOIN FETCH tm.homeTeam homeTeam
        JOIN FETCH tm.awayTeam awayTeam
      LEFT JOIN FETCH tm.protocol protocol
        WHERE tour.id = :tourId
          AND tm.active = TRUE
        ORDER BY tm.kickoffAt ASC, tm.id ASC
        """)
    List<TourMatch> findAllActiveDetailedByTourId(@Param("tourId") Long tourId);

    @Query("""
        SELECT tm
        FROM TourMatch tm
        JOIN FETCH tm.tour tour
        JOIN FETCH tm.homeTeam homeTeam
        JOIN FETCH tm.awayTeam awayTeam
        LEFT JOIN FETCH tm.protocol protocol
        WHERE tour.id = :tourId
        ORDER BY tm.kickoffAt ASC, tm.id ASC
        """)
    List<TourMatch> findAllDetailedByTourId(@Param("tourId") Long tourId);

    @Query("""
        SELECT tm
        FROM TourMatch tm
        JOIN FETCH tm.tour tour
        LEFT JOIN tour.competition competition
        JOIN FETCH tm.homeTeam homeTeam
        JOIN FETCH tm.awayTeam awayTeam
        LEFT JOIN FETCH tm.protocol protocol
        WHERE tour.season.id = :seasonId
          AND (competition IS NULL OR competition.type = com.footballstats.backend.domain.CompetitionType.CHAMPIONSHIP)
          AND tour.active = TRUE
          AND tour.published = TRUE
          AND tm.active = TRUE
        ORDER BY tour.sortOrder ASC, tm.kickoffAt ASC, tm.id ASC
        """)
    List<TourMatch> findAllActiveDetailedByPublishedSeasonId(@Param("seasonId") Long seasonId);

      @Query("""
        SELECT tm
        FROM TourMatch tm
        JOIN FETCH tm.tour tour
        JOIN FETCH tour.season season
        JOIN FETCH tm.homeTeam homeTeam
        JOIN FETCH tm.awayTeam awayTeam
        LEFT JOIN FETCH tm.protocol protocol
        WHERE tm.active = TRUE
          AND tour.published = TRUE
          AND (homeTeam.id = :teamId OR awayTeam.id = :teamId)
        ORDER BY tm.kickoffAt DESC, tm.id DESC
        """)
      List<TourMatch> findAllPublishedDetailedByTeamId(@Param("teamId") Long teamId);
}
