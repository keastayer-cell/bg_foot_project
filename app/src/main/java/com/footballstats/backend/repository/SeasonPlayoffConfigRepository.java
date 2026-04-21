package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonPlayoffConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeasonPlayoffConfigRepository extends JpaRepository<SeasonPlayoffConfig, Long> {

    Optional<SeasonPlayoffConfig> findBySeason_Id(Long seasonId);
}