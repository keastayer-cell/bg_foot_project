package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonApplication;
import com.footballstats.backend.domain.SeasonApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SeasonApplicationRepository extends JpaRepository<SeasonApplication, Long> {

    @Query("""
        select sa
        from SeasonApplication sa
        join fetch sa.season s
        join fetch sa.team t
        left join fetch sa.representativeUser ru
        where sa.season.id = :seasonId and sa.team.id = :teamId
        """)
    Optional<SeasonApplication> findDetailedBySeasonIdAndTeamId(Long seasonId, Long teamId);

    @Query("""
        select sa
        from SeasonApplication sa
        join fetch sa.season s
        join fetch sa.team t
        left join fetch sa.representativeUser ru
        where sa.id = :id
        """)
    Optional<SeasonApplication> findDetailedById(Long id);

    @Query("""
        select sa
        from SeasonApplication sa
        join fetch sa.season s
        join fetch sa.team t
        left join fetch sa.representativeUser ru
        where sa.status in :statuses
        order by coalesce(sa.submittedAt, sa.updatedAt) desc, sa.id desc
        """)
    List<SeasonApplication> findAllDetailedByStatusInOrderBySubmittedAtDesc(List<SeasonApplicationStatus> statuses);

    @Query("""
        select sa
        from SeasonApplication sa
        join fetch sa.season s
        join fetch sa.team t
        left join fetch sa.representativeUser ru
        where sa.status in :statuses
          and sa.season.id = :seasonId
        order by coalesce(sa.submittedAt, sa.updatedAt) desc, sa.id desc
        """)
    List<SeasonApplication> findAllDetailedBySeasonIdAndStatusInOrderBySubmittedAtDesc(Long seasonId, List<SeasonApplicationStatus> statuses);
}