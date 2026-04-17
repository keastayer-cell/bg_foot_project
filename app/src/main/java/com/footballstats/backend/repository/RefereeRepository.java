package com.footballstats.backend.repository;

import com.footballstats.backend.domain.Referee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefereeRepository extends JpaRepository<Referee, Long> {

    List<Referee> findAllByActiveTrueOrderByFullNameAsc();

    List<Referee> findAllByOrderByFullNameAsc();

    boolean existsByFullNameIgnoreCase(String fullName);

    Optional<Referee> findByFullNameIgnoreCase(String fullName);
}