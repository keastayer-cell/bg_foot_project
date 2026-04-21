package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SeasonPlayoffBracket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeasonPlayoffBracketRepository extends JpaRepository<SeasonPlayoffBracket, Long> {

    Optional<SeasonPlayoffBracket> findBySeason_Id(Long seasonId);
}