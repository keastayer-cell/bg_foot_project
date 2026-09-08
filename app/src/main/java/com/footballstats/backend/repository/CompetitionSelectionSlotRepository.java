package com.footballstats.backend.repository;

import com.footballstats.backend.domain.CompetitionSelectionSlot;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface CompetitionSelectionSlotRepository extends JpaRepository<CompetitionSelectionSlot, Long> {
    @EntityGraph(attributePaths = "player")
    List<CompetitionSelectionSlot> findAllByCompetition_IdOrderBySortOrderAscIdAsc(Long competitionId);
    void deleteAllByCompetition_Id(Long competitionId);
}
