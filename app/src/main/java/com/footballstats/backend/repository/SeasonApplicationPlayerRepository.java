package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonApplicationPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SeasonApplicationPlayerRepository extends JpaRepository<SeasonApplicationPlayer, Long> {

    @Query("""
        select sap
        from SeasonApplicationPlayer sap
        join fetch sap.player p
        where sap.application.id = :applicationId
        order by lower(p.fullName), p.id
        """)
    List<SeasonApplicationPlayer> findAllDetailedByApplicationId(Long applicationId);

    void deleteByApplication_Id(Long applicationId);
}