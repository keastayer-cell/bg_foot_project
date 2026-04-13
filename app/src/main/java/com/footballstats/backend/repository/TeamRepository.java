package com.footballstats.backend.repository;

import com.footballstats.backend.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

	List<Team> findAllByActiveTrueOrderByNameAsc();

	List<Team> findAllByOrderByNameAsc();

	Optional<Team> findByNameIgnoreCase(String name);
}
