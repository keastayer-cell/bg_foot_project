package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonApplicationPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Collection;

public interface SeasonApplicationPlayerRepository extends JpaRepository<SeasonApplicationPlayer, Long> {

    @Query("""
        select sap
        from SeasonApplicationPlayer sap
        join fetch sap.player p
        where sap.application.id = :applicationId
        order by lower(p.fullName), p.id
        """)
    List<SeasonApplicationPlayer> findAllDetailedByApplicationId(Long applicationId);

    @Query("""
        select sap.application.id as applicationId, count(sap.id) as playersCount
        from SeasonApplicationPlayer sap
        where sap.application.id in :applicationIds
        group by sap.application.id
        """)
    List<ApplicationPlayerCount> countByApplicationIds(Collection<Long> applicationIds);

    void deleteByApplication_Id(Long applicationId);

    interface ApplicationPlayerCount {
        Long getApplicationId();
        long getPlayersCount();
    }
}
