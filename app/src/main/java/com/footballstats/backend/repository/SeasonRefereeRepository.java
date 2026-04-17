package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonReferee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeasonRefereeRepository extends JpaRepository<SeasonReferee, Long> {

    @Query("""
        SELECT sr
        FROM SeasonReferee sr
        JOIN FETCH sr.referee referee
        WHERE sr.season.id = :seasonId
        ORDER BY LOWER(referee.fullName) ASC, referee.id ASC
        """)
    List<SeasonReferee> findAllBySeasonIdOrderByRefereeFullNameAsc(@Param("seasonId") Long seasonId);

    void deleteAllBySeason_Id(Long seasonId);

    boolean existsBySeason_IdAndReferee_Id(Long seasonId, Long refereeId);
}