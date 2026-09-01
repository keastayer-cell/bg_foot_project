package com.footballstats.backend.repository;

import com.footballstats.backend.domain.CompetitionRosterPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompetitionRosterPlayerRepository extends JpaRepository<CompetitionRosterPlayer, Long> {
    @Query("""
        SELECT crp FROM CompetitionRosterPlayer crp
        JOIN FETCH crp.player player
        JOIN FETCH crp.team team
        WHERE crp.competition.id = :competitionId AND crp.active = TRUE
        ORDER BY team.name ASC, player.fullName ASC
        """)
    List<CompetitionRosterPlayer> findAllActiveDetailedByCompetitionId(@Param("competitionId") Long competitionId);
    Optional<CompetitionRosterPlayer> findByCompetition_IdAndPlayer_Id(Long competitionId, Long playerId);
    long countByCompetition_IdAndTeam_IdAndActiveTrue(Long competitionId, Long teamId);
}
