package com.footballstats.backend.repository;

import com.footballstats.backend.domain.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    List<Season> findAllByActiveTrueOrderByCreatedAtDescIdDesc();

    List<Season> findAllByOrderByCreatedAtDescIdDesc();

    boolean existsByNameIgnoreCase(String name);

    Optional<Season> findByNameIgnoreCase(String name);
}
