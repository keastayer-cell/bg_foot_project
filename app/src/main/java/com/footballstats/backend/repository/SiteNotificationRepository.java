package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SiteNotification;
import com.footballstats.backend.domain.SiteNotificationAudienceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface SiteNotificationRepository extends JpaRepository<SiteNotification, Long> {
    Page<SiteNotification> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query("""
        SELECT notification
        FROM SiteNotification notification
        WHERE notification.audienceType = :audienceType
          AND (notification.expiresAt IS NULL OR notification.expiresAt > :now)
        ORDER BY notification.createdAt DESC, notification.id DESC
        """)
    Page<SiteNotification> findVisiblePublic(
        @Param("audienceType") SiteNotificationAudienceType audienceType,
        @Param("now") OffsetDateTime now,
        Pageable pageable
    );
}
