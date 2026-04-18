package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeasonTeamRepository extends JpaRepository<SeasonTeam, Long> {

    @Query("""
        SELECT st
        FROM SeasonTeam st
        JOIN FETCH st.team t
        WHERE st.season.id = :seasonId
        ORDER BY t.name
        """)
    List<SeasonTeam> findAllBySeasonIdOrderByTeamNameAsc(@Param("seasonId") Long seasonId);

    @Query("""
        SELECT st
        FROM SeasonTeam st
        JOIN FETCH st.season season
        WHERE st.team.id = :teamId
          AND season.active = TRUE
        ORDER BY season.createdAt DESC, season.id DESC
        """)
    List<SeasonTeam> findAllByTeamIdOrderBySeasonCreatedAtDesc(@Param("teamId") Long teamId);

    @Query("""
        SELECT st
        FROM SeasonTeam st
        JOIN FETCH st.season season
        WHERE st.team.id = :teamId
        ORDER BY season.createdAt DESC, season.id DESC
        """)
    List<SeasonTeam> findAllDetailedByTeamId(@Param("teamId") Long teamId);

    @Query("""
        SELECT st.team.id AS teamId
        FROM SeasonTeam st
        WHERE st.season.id = :seasonId
        """)
    List<TeamSeasonProjection> findTeamIdsBySeasonId(@Param("seasonId") Long seasonId);

    boolean existsBySeason_IdAndTeam_Id(Long seasonId, Long teamId);

    long countBySeason_Id(Long seasonId);

    void deleteAllBySeason_Id(Long seasonId);

    interface TeamSeasonProjection {
        Long getTeamId();
    }
}