package com.footballstats.backend.repository;

import com.footballstats.backend.domain.SiteNotificationRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface SiteNotificationRecipientRepository extends JpaRepository<SiteNotificationRecipient, Long> {

    @EntityGraph(attributePaths = "notification")
    @Query("""
        SELECT recipient
        FROM SiteNotificationRecipient recipient
        WHERE recipient.user.id = :userId
          AND (recipient.notification.expiresAt IS NULL OR recipient.notification.expiresAt > :now)
        ORDER BY recipient.notification.createdAt DESC,
                 recipient.id DESC
        """)
    Page<SiteNotificationRecipient> findVisibleForUser(
        @Param("userId") Long userId,
        @Param("now") OffsetDateTime now,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(recipient)
        FROM SiteNotificationRecipient recipient
        WHERE recipient.user.id = :userId
          AND recipient.readAt IS NULL
          AND (recipient.notification.expiresAt IS NULL OR recipient.notification.expiresAt > :now)
        """)
    long countUnreadForUser(@Param("userId") Long userId, @Param("now") OffsetDateTime now);

    @EntityGraph(attributePaths = "notification")
    Optional<SiteNotificationRecipient> findByIdAndUser_Id(Long id, Long userId);

    long countByNotification_Id(Long notificationId);
    long countByNotification_IdAndReadAtIsNotNull(Long notificationId);
    long countByNotification_IdAndAcknowledgedAtIsNotNull(Long notificationId);

    @Modifying
    @Query("""
        UPDATE SiteNotificationRecipient recipient
        SET recipient.readAt = :now
        WHERE recipient.user.id = :userId
          AND recipient.readAt IS NULL
        """)
    int markAllRead(@Param("userId") Long userId, @Param("now") OffsetDateTime now);
}
