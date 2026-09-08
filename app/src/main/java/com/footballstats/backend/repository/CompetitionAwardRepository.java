package com.footballstats.backend.repository;

import com.footballstats.backend.domain.CompetitionAward;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CompetitionAwardRepository extends JpaRepository<CompetitionAward, Long> {
    @EntityGraph(attributePaths = {"player", "team"})
    List<CompetitionAward> findAllByCompetition_IdOrderBySortOrderAscIdAsc(Long competitionId);
    void deleteAllByCompetition_Id(Long competitionId);
    @Query("SELECT a FROM CompetitionAward a JOIN FETCH a.competition c JOIN FETCH c.season WHERE c.honorsPublished = TRUE AND c.active = TRUE ORDER BY c.season.createdAt DESC, c.id DESC, a.sortOrder, a.id")
    List<CompetitionAward> findAllPublished();
}
