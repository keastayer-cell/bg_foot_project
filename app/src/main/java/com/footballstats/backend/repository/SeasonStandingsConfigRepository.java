package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonStandingsConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeasonStandingsConfigRepository extends JpaRepository<SeasonStandingsConfig, Long> {

    Optional<SeasonStandingsConfig> findBySeason_Id(Long seasonId);
}