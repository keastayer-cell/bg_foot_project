package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SiteNotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteNotificationTemplateRepository extends JpaRepository<SiteNotificationTemplate, Long> {
    Optional<SiteNotificationTemplate> findByCodeAndActiveTrue(String code);
}
