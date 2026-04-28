package com.footballstats.backend.repository;

import com.footballstats.backend.domain.LeagueOfficial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueOfficialRepository extends JpaRepository<LeagueOfficial, Long> {

    List<LeagueOfficial> findAllByActiveTrueOrderBySortOrderAscIdAsc();

    List<LeagueOfficial> findAllByOrderBySortOrderAscIdAsc();
}