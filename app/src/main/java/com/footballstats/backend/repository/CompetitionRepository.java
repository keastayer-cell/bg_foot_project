package com.footballstats.backend.repository;

import com.footballstats.backend.domain.Competition;
import com.footballstats.backend.domain.CompetitionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    @Query("""
        SELECT c FROM Competition c
        JOIN FETCH c.season season
        WHERE season.id = :seasonId AND c.active = TRUE
        ORDER BY c.type ASC, c.createdAt ASC, c.id ASC
        """)
    List<Competition> findAllActiveDetailedBySeasonId(@Param("seasonId") Long seasonId);

    @Query("SELECT c FROM Competition c JOIN FETCH c.season WHERE c.id = :id")
    Optional<Competition> findDetailedById(@Param("id") Long id);

    Optional<Competition> findBySeason_IdAndTypeAndActiveTrue(Long seasonId, CompetitionType type);
    boolean existsBySeason_IdAndNameIgnoreCaseAndActiveTrue(Long seasonId, String name);
}
