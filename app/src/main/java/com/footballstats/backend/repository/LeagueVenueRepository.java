package com.footballstats.backend.repository;

import com.footballstats.backend.domain.LeagueVenue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueVenueRepository extends JpaRepository<LeagueVenue, Long> {

    List<LeagueVenue> findAllByActiveTrueOrderBySortOrderAscIdAsc();

    List<LeagueVenue> findAllByOrderBySortOrderAscIdAsc();
}