package com.footballstats.backend.repository;

import com.footballstats.backend.domain.DemoDataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DemoDatasetRepository extends JpaRepository<DemoDataset, Long> {

    Optional<DemoDataset> findByCode(String code);
}
