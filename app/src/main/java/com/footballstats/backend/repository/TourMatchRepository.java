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

    Optional<TourMatch> findByIdAndTour_Id(Long matchId, Long tourId);

    @Query("""
      SELECT tm
      FROM TourMatch tm
      JOIN FETCH tm.tour tour
      JOIN FETCH tour.season season
      JOIN FETCH tm.homeTeam homeTeam
      JOIN FETCH tm.awayTeam awayTeam
      LEFT JOIN FETCH tm.protocol protocol
      LEFT JOIN FETCH protocol.bestPlayer bestPlayer
      WHERE tm.id = :matchId
      """)
    Optional<TourMatch> findDetailedById(@Param("matchId") Long matchId);

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
        JOIN FETCH tm.homeTeam homeTeam
        JOIN FETCH tm.awayTeam awayTeam
        LEFT JOIN FETCH tm.protocol protocol
        WHERE tour.season.id = :seasonId
          AND tour.active = TRUE
          AND tour.published = TRUE
          AND tm.active = TRUE
        ORDER BY tour.sortOrder ASC, tm.kickoffAt ASC, tm.id ASC
        """)
    List<TourMatch> findAllActiveDetailedByPublishedSeasonId(@Param("seasonId") Long seasonId);
}