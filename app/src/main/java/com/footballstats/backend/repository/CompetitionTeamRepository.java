package com.footballstats.backend.repository;

import com.footballstats.backend.domain.CompetitionTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompetitionTeamRepository extends JpaRepository<CompetitionTeam, Long> {
    @Query("""
        SELECT ct FROM CompetitionTeam ct
        JOIN FETCH ct.team team
        WHERE ct.competition.id = :competitionId
        ORDER BY COALESCE(ct.seedNumber, 2147483647), team.name ASC
        """)
    List<CompetitionTeam> findAllDetailedByCompetitionId(@Param("competitionId") Long competitionId);
    void deleteAllByCompetition_Id(Long competitionId);
    boolean existsByCompetition_IdAndTeam_Id(Long competitionId, Long teamId);
}
